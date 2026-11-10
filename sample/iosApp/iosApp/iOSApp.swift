import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        // Installs the crash handlers before the first screen so early crashes are recorded.
        CrashHistoryIos.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
