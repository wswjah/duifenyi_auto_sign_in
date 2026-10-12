package com.duifenyi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.duifenyi.app.ui.navigation.NavGraph
import com.duifenyi.app.ui.theme.DuifenyiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DuifenyiTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
