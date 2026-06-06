package cz.obchodnik.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cz.obchodnik.ui.theme.Obchodnik

@Composable
fun ObchodnikCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val c = Obchodnik.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Obchodnik.radii.radius))
            .background(c.surface)
            .border(BorderStroke(1.dp, c.border), RoundedCornerShape(Obchodnik.radii.radius))
            .padding(padding),
    ) {
        content()
    }
}
