// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package li.gkd.app.ui.liquid

// Adapted from Kyant0/AndroidLiquidGlass — https://github.com/Kyant0/AndroidLiquidGlass (Apache 2.0).
// 同步自 miuix@39c40f9 (v0.9.4) example/shared/src/commonMain/kotlin/component/liquid/CombinedBackdrop.kt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import top.yukonga.miuix.kmp.blur.Backdrop

/**
 * A [Backdrop] that draws [first] then [second] in order, allowing a tinted/overlay
 * backdrop to be sampled on top of a base backdrop. Mirrors Kyant's `CombinedBackdrop`
 * pattern used in `LiquidBottomTabs` to layer a recorded "tinted tabs" pass over the
 * underlying app background as a single sampling source for an indicator.
 */
@Stable
class CombinedBackdrop(
    val first: Backdrop,
    val second: Backdrop,
) : Backdrop {

    override val isCoordinatesDependent: Boolean = first.isCoordinatesDependent || second.isCoordinatesDependent

    override val offsetResidualX: Float get() = first.offsetResidualX
    override val offsetResidualY: Float get() = first.offsetResidualY

    // miuix 0.9.5: Backdrop gained `backdropCoordinates` (used to clamp blur padding at edges).
    // The combined surface is anchored to [first], so inherit its coordinates.
    override val backdropCoordinates: LayoutCoordinates? get() = first.backdropCoordinates

    // miuix 0.9.5: `downscaleFactor` changed from Int to Float.
    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Float,
    ) {
        with(first) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
        with(second) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
    }
}

@Composable
fun rememberCombinedBackdrop(first: Backdrop, second: Backdrop): Backdrop = remember(first, second) { CombinedBackdrop(first, second) }
