package com.my.app.pytide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.my.app.pytide.ui.PytIdeApp
import com.my.app.pytide.ui.theme.PytIDETheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        setContent {
            PytIDETheme {
                PytIdeApp()
            }
        }
    }
}
