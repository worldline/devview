package com.worldline.devview.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.assertNotNull
import org.junit.Test

class DataStoreDelegateUiTest {

    @Test
    fun dataStoreDelegate_init_in_composition_allows_get_afterwards() = runComposeUiTest {
        val delegate = DataStoreDelegate()

        setContent {
            InitDelegate(delegate = delegate)
        }

        runOnIdle {
            assertNotNull(delegate.get())
        }
    }
}

@Composable
private fun InitDelegate(delegate: DataStoreDelegate) {
    delegate.init(dataStoreName = "devview_utils_ui_test.preferences_pb")
}
