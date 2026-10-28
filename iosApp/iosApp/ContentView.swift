import SwiftUI
import Shared

struct ContentView: View {
    @State private var controller = ProgressBarsController()
    @State private var bars: [BarState] = []

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(bars, id: \.id) { bar in
                Text("Bar \(bar.id): progress=\(bar.progress) running=\(bar.running)")
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .padding()
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
