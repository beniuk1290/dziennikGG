package io.github.szpontium

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.szpontium.session.initAndroidDataStoreContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try { enableEdgeToEdge() } catch (_: Exception) {}
        super.onCreate(savedInstanceState)
        initAndroidDataStoreContext(this)
        setContent {
            App()
        }
    }
}
