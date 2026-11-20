package com.sonora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        // The blurred artwork backdrop bleeds under the system bars;
        // screens pad their own content below the status bar.
        enableEdgeToEdge()
        setContent {
            SonoraApp()
        }
    }
}
