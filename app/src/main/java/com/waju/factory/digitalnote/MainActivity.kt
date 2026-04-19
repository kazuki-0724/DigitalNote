package com.waju.factory.digitalnote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.waju.factory.digitalnote.ui.DigitalNoteApp
import com.waju.factory.digitalnote.ui.theme.DigitalNoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DigitalNoteTheme(darkTheme = false, dynamicColor = false) {
                DigitalNoteApp()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DigitalNotePreview() {
    DigitalNoteTheme(darkTheme = false, dynamicColor = false) {
        DigitalNoteApp()
    }
}