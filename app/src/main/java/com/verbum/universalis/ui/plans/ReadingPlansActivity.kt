package com.verbum.universalis.ui.plans

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.verbum.universalis.core.theme.VerbumTheme

@Composable
fun ReadingPlansScreen() {
    VerbumTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Reading Plans (Management)")
        }
    }
}
