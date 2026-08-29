package com.jeremy.rootwebrtcstreamer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StreamerControlScreen()
                }
            }
        }
    }
}

@Composable
fun StreamerControlScreen() {
    var isStreaming by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Idle") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isStreaming) "Streamer Status: ACTIVE" else "Streamer Status: STOPPED",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Log: $statusMessage",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    if (!isStreaming) {
                        statusMessage = "Requesting root & starting bridge..."
                        val success = startRootBridgeWorker()
                        if (success) {
                            isStreaming = true
                            statusMessage = "Bridge worker running"
                        } else {
                            statusMessage = "Failed (Root denied or error)"
                        }
                    } else {
                        statusMessage = "Stopping bridge worker..."
                        stopRootBridgeWorker()
                        isStreaming = false
                        statusMessage = "Stopped"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (isStreaming) "Stop Stream" else "Start Stream")
        }
    }
}

suspend fun startRootBridgeWorker(): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "/data/local/tmp/stream"))
        val exitCode = process.waitFor()
        exitCode == 0
    } catch (e: Exception) {
        false
    }
}

suspend fun stopRootBridgeWorker() = withContext(Dispatchers.IO) {
    try {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "killall stream")).waitFor()
    } catch (_: Exception) {}
}
