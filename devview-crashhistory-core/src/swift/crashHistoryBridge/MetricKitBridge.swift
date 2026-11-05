import Foundation

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
    @objc(startWithOnDiagnostic:)
    public func start(onDiagnostic: @escaping (DevViewCrashDiagnostic) -> Void) {
        // shortcut: SDK 27 implementation is pending, complete and verify both paths in Handoff B.
    }
}
