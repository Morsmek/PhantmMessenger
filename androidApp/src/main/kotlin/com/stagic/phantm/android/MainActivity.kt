package com.stagic.phantm.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stagic.phantm.android.ui.PhantmNavGraph
import com.stagic.phantm.android.ui.theme.PhantmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhantmTheme {
                PhantmNavGraph()
            }
        }
    }
}
