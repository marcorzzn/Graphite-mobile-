package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val GraphiteDarkColorScheme = darkColorScheme(
    primary = DataVector,
    onPrimary = GraphiteNearBlack,
    primaryContainer = DataVectorDim,
    onPrimaryContainer = GraphiteNearWhite,
    secondary = DataGraphic,
    onSecondary = GraphiteNearBlack,
    secondaryContainer = DataGraphicDim,
    onSecondaryContainer = GraphiteNearWhite,
    tertiary = DataRaster,
    onTertiary = GraphiteNearBlack,
    tertiaryContainer = DataRasterDim,
    onTertiaryContainer = GraphiteNearWhite,
    background = GraphiteNearBlack,
    onBackground = GraphiteNearWhite,
    surface = GraphitePanelBg,
    onSurface = GraphiteNearWhite,
    surfaceVariant = GraphiteDarkGray,
    onSurfaceVariant = GraphiteLightGray,
    outline = GraphiteDimGray,
    outlineVariant = GraphiteDullGray,
    error = GraphiteErrorRed,
    onError = GraphiteWhite
)

val GraphiteShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun GraphiteMobileTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GraphiteDarkColorScheme,
        typography = Typography,
        shapes = GraphiteShapes,
        content = content
    )
}
