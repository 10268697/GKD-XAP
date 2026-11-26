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
import li.gkd.app.ui.share.LocalDarkTheme
import li.gkd.app.ui.share.LocalLayerBackdrop
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.glass.GlassFloatingActionButton
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val elevationDurationMillis = 50

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
    val shape = remember { CircleShape }
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val iconTint = MiuixTheme.colorScheme.onSurface

    if (liquidBackdrop != null) {
        // 液态玻璃交给 miuix-glass，形状/阴影/按压动效都由 GlassButtonSurface 负责。
        // 官方组件没有 enabled/onClickLabel 参数，启用态在 onClick 里守住，避免淡出动画期间误点。
        GlassFloatingActionButton(
            onClick = { if (enabled) onClick() },
            modifier = modifier,
            backdrop = liquidBackdrop,
            size = 56.dp,
        ) {
            PerfIcon(
                imageVector = imageVector,
                contentDescription = null,
                tint = iconTint,
            )
        }
    } else {
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
                .clip(shape)
                .background(surfaceContainer)
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
}
