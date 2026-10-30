package com.rabbitear.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.rabbitear.app.ui.theme.RabbitEarTheme
import com.rabbitear.app.ui.Root
import com.rabbitear.app.ui.VM

class MainActivity : ComponentActivity() {
    private val vm: VM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openScreen = intent.getStringExtra("openScreen")
        setContent { RabbitEarTheme(themeMode = vm.themeMode) { Root(vm, openScreen = openScreen) } }
    }
}
