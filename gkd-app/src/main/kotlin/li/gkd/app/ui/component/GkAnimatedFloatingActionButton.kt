package li.gkd.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import li.gkd.app.ui.component.rememberGkVisibilityProgress
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.ui.liquid.lens
import li.gkd.app.ui.liquid.vibrancy
import li.gkd.app.ui.share.LocalDarkTheme
import li.gkd.app.ui.share.LocalLayerBackdrop
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.material.EdgeLightSource
import top.yukonga.miuix.kmp.material.EdgePeak
import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.edgeStroke
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val elevationDurationMillis = 50

// miuix 0.9.5: Highlight/BloomStroke/LightSource were replaced by EdgeStroke/EdgeLightSource.
// LightPosition(x, y, z) is now inlined into EdgeLightSource(x, y, z), `dualPeak = true` maps to
// `peak = EdgePeak.Dual`, and Highlight.alpha moved to the `alpha` argument of Modifier.edgeStroke.
private val FabGlassEdgeStroke = EdgeStroke(
    width = 1.dp,
    bevel = 2.0.dp,
    color = Color.White.copy(alpha = 0.16f),
    primary = EdgeLightSource(
        x = 0.5f,
        y = -0.3f,
        z = -0.05f,
        color = Color.White,
        intensity = 1f,
    ),
    secondary = EdgeLightSource(
        x = 0.5f,
        y = 0.8f,
        z = -0.5f,
        color = Color.White,
        intensity = 0.4f,
    ),
    peak = EdgePeak.Dual,
)

@Composable
fun GkAnimatedFloatingActionButton(
    visible: Boolean,
    onClick: () -> Unit,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    onClickLabel: String? = null,
    contentDescription: String? = getIconDefaultDesc(imageVector),
) {
    val density = LocalDensity.current
    val maxTranslationX = remember(density.density) { density.run { 24.dp.toPx() } }
    val visibility = rememberGkVisibilityProgress(
        visible = visible,
        durationMillis = androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis - elevationDurationMillis,
    )
    val percent = visibility.fraction
    if (visibility.mounted) {
        val fabModifier = modifier
            .graphicsLayer(
                alpha = percent,
                translationX = (1f - percent) * maxTranslationX
            )
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
        GkTooltipIconButtonBox(contentDescription) {
            LiquidGlassFab(
                onClick = throttle(onClick),
                imageVector = imageVector,
                enabled = visible,
                onClickLabel = onClickLabel,
                modifier = fabModifier,
            )
        }
    }
}

@Composable
private fun LiquidGlassFab(
    onClick: () -> Unit,
    imageVector: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClickLabel: String? = null,
) {
    val store by storeFlow.collectAsState()
    val backdrop = LocalLayerBackdrop.current
    val isDark = LocalDarkTheme.current
    val shaderOk = isRuntimeShaderSupported()
    val liquidBackdrop = if (store.enableLiquidGlass && store.enableMiuixBlur && shaderOk) backdrop else null
    val liquidOk = liquidBackdrop != null
    val shape = remember { CircleShape }
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val containerColor = if (liquidOk) surfaceContainer.copy(alpha = 0.4f) else surfaceContainer
    val iconTint = MiuixTheme.colorScheme.onSurface
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(56.dp)
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 10.dp,
                    color = Color.Black,
                    alpha = if (isDark) 0.22f else 0.12f,
                ),
            )
            .then(
                if (liquidOk) {
                    Modifier.drawBackdrop(
                        backdrop = checkNotNull(liquidBackdrop),
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx(), 4.dp.toPx())
                            padding = maxOf(padding, 28.dp.toPx())
                            lens(
                                refractionHeight = 14.dp.toPx(),
                                refractionAmount = 16.dp.toPx(),
                            )
                        },
                        onDrawSurface = { drawRect(containerColor) },
                    ).edgeStroke(
                        shape = shape,
                        stroke = FabGlassEdgeStroke,
                        alpha = 0.75f,
                    )
                } else {
                    Modifier
                        .clip(shape)
                        .background(containerColor)
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = onClickLabel,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        PerfIcon(
            imageVector = imageVector,
            contentDescription = null,
            tint = iconTint,
        )
    }
}
