package com.comaii.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extrair companyId da deep link: comaii.com/{companyId}
        val companyId = intent?.data?.lastPathSegment ?: "default"

        setContent {
            App(companyId = companyId)
        }
    }
}
