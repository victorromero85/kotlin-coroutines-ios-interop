package org.example.kotlin_coroutines_ios_interop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                    Text("Cancel all")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(bars, key = { it.id }) { bar ->
                    BarRow(
                        bar = bar,
                        onAddChild = { controller.addChild(bar.id) },
                        onCancel = { controller.cancelBar(bar.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BarRow(bar: BarState, onAddChild: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (bar.depth * 24).dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Bar ${bar.id} — ${(bar.progress * 100).toInt()}% · ${bar.status.label}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (bar.depth < MAX_DEPTH) {
                TextButton(onClick = onAddChild, enabled = bar.canAddChild) {
                    Text("Add child")
                }
            }
            TextButton(onClick = onCancel, enabled = bar.running) {
                Text("Cancel")
            }
        }
        LinearProgressIndicator(
            progress = { bar.progress },
            modifier = Modifier.fillMaxWidth(),
            color = bar.status.color,
        )
    }
}

private val BarStatus.label: String
    get() = when (this) {
        BarStatus.Running -> "running"
        BarStatus.WaitingForChildren -> "waiting for children"
        BarStatus.Completed -> "completed"
        BarStatus.Cancelled -> "cancelled"
    }

private val BarStatus.color: Color
    @Composable get() = when (this) {
        BarStatus.Running -> MaterialTheme.colorScheme.primary
        BarStatus.WaitingForChildren -> MaterialTheme.colorScheme.tertiary
        BarStatus.Completed -> Color(0xFF2E7D32)
        BarStatus.Cancelled -> MaterialTheme.colorScheme.error
    }
