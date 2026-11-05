import CryptoKit
import Foundation

// A JSON array gives each field an unambiguous boundary, including nil versus an empty reason.
func crashOccurrenceId(
    startMillis: Int64, endMillis: Int64, callStackJSON: String, reason: String?,
    signal: String? = nil, exceptionType: String? = nil, exceptionCode: String? = nil
) throws -> String {
    let fields: [Any] = [
        "devview-crash-v1", String(startMillis), String(endMillis), callStackJSON,
        reason as Any? ?? NSNull(), signal as Any? ?? NSNull(),
        exceptionType as Any? ?? NSNull(), exceptionCode as Any? ?? NSNull()
    ]
    let bytes = try JSONSerialization.data(withJSONObject: fields, options: [.sortedKeys, .withoutEscapingSlashes])
    return SHA256.hash(data: bytes).map { String(format: "%02x", $0) }.joined()
}

func canonicalCrashJSON(_ data: Data) throws -> String {
    let object = try JSONSerialization.jsonObject(with: data)
    let canonical = try JSONSerialization.data(withJSONObject: object, options: [.sortedKeys, .withoutEscapingSlashes])
    return String(decoding: canonical, as: UTF8.self)
}
