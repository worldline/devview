package com.worldline.devview.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.worldline.devview.sample.network.SampleApi
import com.worldline.devview.sample.network.rememberHttpClientWithMocking
import devview_root.sample.shared.generated.resources.Res
import devview_root.sample.shared.generated.resources.compose_multiplatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
public fun App(openDevView: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    var showContent by remember { mutableStateOf(value = false) }
    var showCounterScreen by remember { mutableStateOf(value = true) }
    var ktorDocs by remember { mutableStateOf(value = "To be called...") }
    val coroutineScope = rememberCoroutineScope()

    val httpClient = rememberHttpClientWithMocking()
    val api = remember(key1 = httpClient) { SampleApi(client = httpClient) }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .imePadding()
                .verticalScroll(state = rememberScrollState())
                .padding(all = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 24.dp)
        ) {
            Button(onClick = { openDevView(true) }) {
                Text(text = "Open DevView")
            }

            Column(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                Text(
                    text = "Compose",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium
                )
                FilledTonalButton(onClick = { showContent = !showContent }) {
                    Text(text = if (showContent) "Hide greeting" else "Show greeting")
                }
                AnimatedVisibility(visible = showContent) {
                    val greeting = remember { Greeting().greet() }
                    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                        Image(
                            painter = painterResource(
                                resource = Res.drawable.compose_multiplatform
                            ),
                            contentDescription = null,
                            modifier = Modifier.sizeIn(maxWidth = 160.dp, maxHeight = 160.dp)
                        )
                        Text(text = greeting)
                    }
                }
            }

            Column(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                Text(
                    text = "TimeCapsule",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium
                )
                // Hiding disposes CounterScreen and resets its recorded history.
                FilledTonalButton(onClick = { showCounterScreen = !showCounterScreen }) {
                    Text(
                        text = if (showCounterScreen) "Hide Counter Screen" else "Show Counter Screen"
                    )
                }
                if (showCounterScreen) {
                    CounterScreen(modifier = Modifier.fillMaxWidth())
                }
            }

            Column(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                Text(
                    text = "Network",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                @Suppress("TooGenericExceptionCaught")
                                ktorDocs = try {
                                    api.getKtorDocs()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    "Error: ${e.message}"
                                }
                            }
                        }
                    ) {
                        Text(text = "Make Ktor call")
                    }
                    OutlinedButton(onClick = { ktorDocs = "To be called..." }) {
                        Text(text = "Reset Ktor call")
                    }
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                @Suppress("TooGenericExceptionCaught")
                                ktorDocs = try {
                                    api.getUser(userId = 1)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    "Error: ${e.message}"
                                }
                            }
                        }
                    ) {
                        Text(text = "Test Network Mock (Get User)")
                    }
                }
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = ktorDocs,
                        modifier = Modifier.padding(all = 16.dp),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
