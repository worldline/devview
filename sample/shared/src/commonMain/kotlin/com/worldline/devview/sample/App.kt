package com.worldline.devview.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import devview_root.sample.shared.generated.resources.Res
import devview_root.sample.shared.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource

@Composable
public fun App(modifier: Modifier = Modifier, openDevView: (Boolean) -> Unit) {
    var showContent by remember { mutableStateOf(value = false) }
    Column(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
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
    }
}
