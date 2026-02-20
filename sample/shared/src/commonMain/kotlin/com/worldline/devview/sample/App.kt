package com.worldline.devview.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldline.devview.sample.network.SampleApi
import com.worldline.devview.sample.network.rememberHttpClientWithMocking
import devview_root.sample.shared.generated.resources.Res
import devview_root.sample.shared.generated.resources.compose_multiplatform
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
public fun App(
    openDevView: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(value = false) }
    var ktorDocs by remember { mutableStateOf(value = "To be called...") }
    val coroutineScope = rememberCoroutineScope()

    val httpClient = rememberHttpClientWithMocking()
    val api = remember(key1 = httpClient) { SampleApi(client = httpClient) }

    Column(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Button(onClick = { showContent = !showContent }) {
            Text(text = "Click me!")
        }
        AnimatedVisibility(visible = showContent) {
            val greeting = remember { Greeting().greet() }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(resource = Res.drawable.compose_multiplatform),
                    contentDescription = null
                )
                Text(text = "Compose: $greeting")
            }
        }
        Button(
            onClick = {
                openDevView(true)
            }
        ) {
            Text(text = "Open DevView")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        @Suppress("TooGenericExceptionCaught")
                        ktorDocs = try {
                            api.getKtorDocs()
                        } catch (e: Exception) {
                            "Error: ${e.message}"
                        }
                    }
                }
            ) {
                Text(text = "Make Ktor call")
            }

            Button(
                onClick = {
                    ktorDocs = "To be called..."
                }
            ) {
                Text(text = "Reset Ktor call")
            }
        }

        // New button to test Network Mock with JSONPlaceholder
        Button(
            onClick = {
                coroutineScope.launch {
                    @Suppress("TooGenericExceptionCaught")
                    ktorDocs = try {
                        api.getUser(userId = 1)
                    } catch (e: Exception) {
                        "Error: ${e.message}"
                    }
                }
            }
        ) {
            Text(text = "Test Network Mock (Get User)")
        }

        Text(text = ktorDocs)
    }
}
