package org.example.kotlin_coroutines_ios_interop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
@Preview
fun App() {
    MaterialTheme {
        val controller = remember { ProgressBarsController() }
        val bars by controller.bars.collectAsStateWithLifecycle()
        val isRunning = bars.any { it.running }

        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { controller.start() }, enabled = !isRunning) {
                    Text("Start")
                }
                Button(onClick = { controller.cancel() }, enabled = isRunning) {
                    Text("Cancel")
                }
            }

            bars.forEach { bar ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Bar ${bar.id} — ${(bar.progress * 100).toInt()}%")
                    LinearProgressIndicator(
                        progress = { bar.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
