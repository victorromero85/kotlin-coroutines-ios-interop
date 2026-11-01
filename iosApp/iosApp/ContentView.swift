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
                Button("Cancel all") { controller.cancel() }
                    .disabled(!isRunning)
            }
            .buttonStyle(.borderedProminent)

            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(bars, id: \.id) { bar in
                        BarRow(
                            bar: bar,
                            onAddChild: { controller.addChild(parentId: bar.id) },
                            onCancel: { controller.cancelBar(id: bar.id) }
                        )
                    }
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

private struct BarRow: View {
    let bar: BarState
    let onAddChild: () -> Void
    let onCancel: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("Bar \(bar.id) — \(Int(bar.progress * 100))% · \(bar.status.label)")
                    .font(.subheadline)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if bar.depth < BarStateKt.MAX_DEPTH {
                    Button("Add child", action: onAddChild)
                        .disabled(!bar.canAddChild)
                }
                Button("Cancel", action: onCancel)
                    .disabled(!bar.running)
            }
            .buttonStyle(.borderless)
            .font(.subheadline)
            ProgressView(value: Double(bar.progress))
                .tint(bar.status.color)
        }
        .padding(.leading, CGFloat(bar.depth) * 24)
    }
}

private extension BarStatus {
    var label: String {
        switch self {
        case .running: "running"
        case .waitingForChildren: "waiting for children"
        case .completed: "completed"
        case .cancelled: "cancelled"
        }
    }

    var color: Color {
        switch self {
        case .running: .accentColor
        case .waitingForChildren: .orange
        case .completed: .green
        case .cancelled: .red
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
