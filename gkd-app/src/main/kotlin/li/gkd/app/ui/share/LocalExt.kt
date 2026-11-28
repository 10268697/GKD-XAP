package li.gkd.app.ui.share

import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.blur.LayerBackdrop

val LocalDarkTheme = staticCompositionLocalOf { false }

val LocalIsTalkbackEnabled = staticCompositionLocalOf {
    false
}

/** MIUIX：顶栏是否处于毛玻璃透明态（由壳层提供） */
val LocalMiuixBlurActive = staticCompositionLocalOf { false }

/** 页面内容 [LayerBackdrop]，供液态玻璃 FAB 等采样背景 */
val LocalLayerBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * 当前 composition 是否位于玻璃顶栏内。
 *
 * 由 [li.gkd.app.ui.component.GkTopAppBar] / [li.gkd.app.ui.component.PerfTopAppBar]
 * 在改用 `GlassTopAppBar` 时提供，使 `actions` 里的 [li.gkd.app.ui.component.GkIconButton]
 * 自动换成 `GlassIconButton`，与顶栏材质一致；页面其他位置的图标按钮不受影响。
 */
val LocalGkGlassTopBar = staticCompositionLocalOf { false }
