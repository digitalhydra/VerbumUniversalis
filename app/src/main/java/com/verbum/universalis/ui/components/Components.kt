package com.verbum.universalis.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verbum.universalis.core.theme.LightOutline
import com.verbum.universalis.core.theme.DarkOutline
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun Hairline() {
    val color = if (isSystemInDarkTheme()) DarkOutline else LightOutline
    Divider(
        modifier = Modifier.fillMaxWidth().height(1.dp),
        color = color
    )
}

@Composable
fun GoldButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = com.verbum.universalis.core.theme.VerbunGold
        ),
        elevation = null // No elevation
    ) {
        content()
    }
}
