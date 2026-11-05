// Run from the repo root:
// xcrun swiftc devview-crashhistory-core/src/swift/crashHistoryBridge/CrashOccurrenceId.swift scripts/check_crash_occurrence_id.swift -o /tmp/check-crash-id && /tmp/check-crash-id
import Foundation

@main
struct CrashOccurrenceIdCheck {
    static func main() throws {
        let first = try canonicalCrashJSON(Data(#"{"b":2,"a":1}"#.utf8))
        let replay = try canonicalCrashJSON(Data(#"{"a":1,"b":2}"#.utf8))
        let id = try crashOccurrenceId(startMillis: 1, endMillis: 23, callStackJSON: first, reason: "crash")
        let repeated = try crashOccurrenceId(startMillis: 1, endMillis: 23, callStackJSON: replay, reason: "crash")
        precondition(id == repeated && id.count == 64 && id.allSatisfy { "0123456789abcdef".contains($0) })
        let changes = try [
            crashOccurrenceId(startMillis: 12, endMillis: 3, callStackJSON: first, reason: "crash"),
            crashOccurrenceId(startMillis: 2, endMillis: 23, callStackJSON: first, reason: "crash"),
            crashOccurrenceId(startMillis: 1, endMillis: 24, callStackJSON: first, reason: "crash"),
            crashOccurrenceId(startMillis: 1, endMillis: 23, callStackJSON: "{}", reason: "crash"),
            crashOccurrenceId(startMillis: 1, endMillis: 23, callStackJSON: first, reason: "other"),
            crashOccurrenceId(startMillis: 1, endMillis: 23, callStackJSON: first, reason: nil),
            crashOccurrenceId(startMillis: 1, endMillis: 23, callStackJSON: first, reason: ""),
            crashOccurrenceId(startMillis: 1, endMillis: 23, callStackJSON: first, reason: "crash", signal: "6"),
            crashOccurrenceId(startMillis: 1, endMillis: 23, callStackJSON: first, reason: "crash", signal: "11"),
            crashOccurrenceId(startMillis: 1, endMillis: 23, callStackJSON: first, reason: "crash", exceptionType: "1"),
            crashOccurrenceId(startMillis: 1, endMillis: 23, callStackJSON: first, reason: "crash", exceptionType: "2"),
            crashOccurrenceId(startMillis: 1, endMillis: 23, callStackJSON: first, reason: "crash", exceptionCode: "0"),
            crashOccurrenceId(startMillis: 1, endMillis: 23, callStackJSON: first, reason: "crash", exceptionCode: "18446744073709551615")
        ]
        precondition(Set(changes + [id]).count == changes.count + 1)
        print("PASS: replay/key order stable; changed fields (including signal/Mach exception), boundaries and nil/empty distinct; lowercase SHA256 \(id)")
    }
}
