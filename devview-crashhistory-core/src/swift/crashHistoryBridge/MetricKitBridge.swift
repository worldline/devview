import Foundation
import MetricKit

/// Objective-C-safe snapshot of a crash diagnostic; timestamps delimit its reporting period.
@objcMembers public final class DevViewCrashDiagnostic: NSObject {
    public var occurrenceId: String = ""
    public var signal: NSNumber?
    public var exceptionType: NSNumber?
    public var exceptionCode: NSNumber?
    public var terminationReason: String?
    public var callStackTreeJson: String?
    public var appVersion: String?
    public var appBuildVersion: String?
    public var osVersion: String?
    public var deviceType: String?
    public var periodStartMillis: Int64 = 0
    public var periodEndMillis: Int64 = 0
}

@objcMembers public final class DevViewMetricKitBridge: NSObject {
    private var subscriber: LegacyCrashSubscriber?
    private var diagnosticTask: Task<Void, Never>?

    @objc(startWithOnDiagnostic:)
    public func start(onDiagnostic: @escaping (DevViewCrashDiagnostic) -> Void) {
        guard subscriber == nil && diagnosticTask == nil else { return }
        if #available(iOS 27.0, *) {
            diagnosticTask = Task {
                let manager = MetricManager()
                for await report in manager.diagnosticReports {
                    if Task.isCancelled { break }
                    guard case let .crash(crash) = report.result else { continue }
                    do {
                        let snapshot = DevViewCrashDiagnostic()
                        snapshot.periodStartMillis = Int64(report.timeRange.start.timeIntervalSince1970 * 1000)
                        snapshot.periodEndMillis = Int64(report.timeRange.end.timeIntervalSince1970 * 1000)
                        snapshot.signal = crash.signal.map { NSNumber(value: $0) }
                        snapshot.exceptionType = crash.exceptionType.map { NSNumber(value: $0) }
                        snapshot.exceptionCode = crash.exceptionCode.map { NSNumber(value: $0) }
                        snapshot.terminationReason = crash.terminationReason?.rawValue
                        snapshot.callStackTreeJson = try crashCallStackJSON(crash.callStackTree)
                        snapshot.appVersion = report.environment.applicationVersion
                        snapshot.appBuildVersion = report.environment.applicationBuildVersion
                        let os = report.environment.osVersion
                        snapshot.osVersion = "\(os.platform) \(os.number) (\(os.buildNumber))"
                        snapshot.deviceType = report.environment.deviceType
                        try deliver(snapshot, to: onDiagnostic)
                    } catch {
                        NSLog("DevView: could not serialize MetricKit crash: %@", String(describing: error))
                    }
                }
            }
        } else {
            let legacy = LegacyCrashSubscriber(onDiagnostic: onDiagnostic)
            subscriber = legacy
            MXMetricManager.shared.add(legacy)
            legacy.didReceive(MXMetricManager.shared.pastDiagnosticPayloads)
        }
    }

    deinit {
        diagnosticTask?.cancel()
        if let subscriber { MXMetricManager.shared.remove(subscriber) }
    }
}

// UUID-keyed dictionaries otherwise encode as an unordered JSON array of key/value pairs.
@available(iOS 27.0, *)
private struct CrashCallStack: Encodable {
    let callStackThreads: ContiguousArray<CallStackThread>
    let callStackPerThread: Bool
    let binaryInfo: [String: CallStackTree.BinaryInfo]
}

@available(iOS 27.0, *)
private func crashCallStackJSON(_ tree: CallStackTree) throws -> String {
    let snapshot = CrashCallStack(
        callStackThreads: tree.callStackThreads,
        callStackPerThread: tree.callStackPerThread,
        binaryInfo: Dictionary(uniqueKeysWithValues: tree.binaryInfo.map { ($0.key.uuidString, $0.value) })
    )
    return try canonicalCrashJSON(JSONEncoder().encode(snapshot))
}

private func deliver(_ snapshot: DevViewCrashDiagnostic, to callback: (DevViewCrashDiagnostic) -> Void) throws {
    snapshot.occurrenceId = try crashOccurrenceId(
        startMillis: snapshot.periodStartMillis,
        endMillis: snapshot.periodEndMillis,
        callStackJSON: snapshot.callStackTreeJson ?? "",
        reason: snapshot.terminationReason
    )
    callback(snapshot)
}

private final class LegacyCrashSubscriber: NSObject, MXMetricManagerSubscriber {
    private let onDiagnostic: (DevViewCrashDiagnostic) -> Void

    init(onDiagnostic: @escaping (DevViewCrashDiagnostic) -> Void) {
        self.onDiagnostic = onDiagnostic
    }

    func didReceive(_ payloads: [MXDiagnosticPayload]) {
        for payload in payloads {
            for crash in payload.crashDiagnostics ?? [] {
                do {
                    let snapshot = DevViewCrashDiagnostic()
                    snapshot.periodStartMillis = Int64(payload.timeStampBegin.timeIntervalSince1970 * 1000)
                    snapshot.periodEndMillis = Int64(payload.timeStampEnd.timeIntervalSince1970 * 1000)
                    snapshot.signal = crash.signal
                    snapshot.exceptionType = crash.exceptionType
                    snapshot.exceptionCode = crash.exceptionCode
                    snapshot.terminationReason = crash.terminationReason
                    snapshot.callStackTreeJson = try canonicalCrashJSON(crash.callStackTree.jsonRepresentation())
                    snapshot.appVersion = crash.applicationVersion
                    snapshot.appBuildVersion = crash.metaData.applicationBuildVersion
                    snapshot.osVersion = crash.metaData.osVersion
                    snapshot.deviceType = crash.metaData.deviceType
                    try deliver(snapshot, to: onDiagnostic)
                } catch {
                    NSLog("DevView: could not serialize MetricKit crash: %@", String(describing: error))
                }
            }
        }
    }
}
