package io.github.szpontium

import android.os.Bundle
import android.widget.TextView
import android.widget.ScrollView
import android.widget.LinearLayout
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.szpontium.session.initAndroidDataStoreContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val trace = throwable.stackTraceToString()
            val msg = "${throwable.message}\n\n$trace"
            runOnUiThread {
                val tv = TextView(this).apply {
                    text = msg
                    setTextColor(Color.RED)
                    setPadding(32, 32, 32, 32)
                    textSize = 12f
                }
                val scroll = ScrollView(this).apply { addView(tv) }
                setContentView(scroll)
            }
        }
        initAndroidDataStoreContext(this)
        setContent {
            App()
        }
    }
}
