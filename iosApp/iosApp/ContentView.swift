import SwiftUI
import Shared

struct ContentView: View {
    @State private var controller = ProgressBarsController()
    @State private var bars: [BarState] = []

    private var isRunning: Bool { bars.contains { $0.running } }

    var body: some View {
        VStack(spacing: 16) {
            HStack(spacing: 12) {
                Button("Start") { controller.start() }
                    .disabled(isRunning)
                Button("Cancel") { controller.cancel() }
                    .disabled(!isRunning)
            }
            .buttonStyle(.borderedProminent)

            ForEach(bars, id: \.id) { bar in
                VStack(alignment: .leading, spacing: 4) {
                    Text("Bar \(bar.id) — \(Int(bar.progress * 100))%")
                    ProgressView(value: Double(bar.progress))
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .padding(24)
        .task {
            for await value in controller.bars {
                bars = value
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
