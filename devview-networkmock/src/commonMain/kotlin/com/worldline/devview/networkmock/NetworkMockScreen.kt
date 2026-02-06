package com.worldline.devview.networkmock

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
public fun NetworkMockScreen(
    modifier: Modifier = Modifier,
) {

}

@Preview
@Composable
private fun NetworkMockScreenPreview() {
    MaterialTheme {
        Scaffold {
            NetworkMockScreen()
        }
    }
}