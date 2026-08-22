package com.example.hangsha_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.hangsha_android.data.local.LocalDataMigration
import com.example.hangsha_android.ui.HangshaApp
import com.example.hangsha_android.ui.theme.HangshaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var localDataMigration: LocalDataMigration

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(if (localDataMigration.didResetThisLaunch) null else savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HangshaTheme {
                HangshaApp()
            }
        }
    }
}
