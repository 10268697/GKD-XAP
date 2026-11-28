package li.gkd.app.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import li.gkd.app.ui.share.LocalGkGlassTopBar
import li.gkd.app.ui.share.LocalLayerBackdrop
import li.gkd.app.ui.share.LocalMiuixBlurActive
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏：始终使用 MIUIX [TopAppBar]。
 * 搜索等自定义内容放 [bottomContent]；[titleText]/[subtitle] 为字符串标题。
 * 毛玻璃激活时容器透明，由外层 textureBlur 提供模糊底。
 */
@Composable
fun PerfTopAppBar(
    modifier: Modifier = Modifier,
    titleText: String = "",
    subtitle: String = "",
    bottomContent: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    color: Color = Color.Unspecified,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    largeTitleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitleColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    miuixScrollBehavior: ScrollBehavior? = null,
    canScroll: Boolean = true,
) {
    val blurActive = LocalMiuixBlurActive.current
    val barColor = when {
        color != Color.Unspecified -> color
        blurActive -> Color.Transparent
        else -> MiuixTheme.colorScheme.surface
    }
    // 玻璃顶栏：只在调用方没指定 color、没改文字色、且页面提供了 backdrop 时启用。
    val glassBar = blurActive &&
        color == Color.Unspecified &&
        LocalLayerBackdrop.current != null &&
        titleColor == MiuixTheme.colorScheme.onSurface &&
        largeTitleColor == MiuixTheme.colorScheme.onSurface &&
        subtitleColor == MiuixTheme.colorScheme.onSurfaceVariantSummary
    if (glassBar) {
        CompositionLocalProvider(LocalGkGlassTopBar provides true) {
            GlassTopAppBar(
                title = titleText,
                modifier = modifier,
                // 模糊交给外层 textureBlur（壳层已开），这里只取渐进色带与玻璃按钮材质。
                expandable = true,
                backdrop = null,
                largeTitle = titleText,
                subtitle = subtitle,
                scrollBehavior = if (canScroll) miuixScrollBehavior else null,
                navigationIcon = navigationIcon,
                actions = actions,
                bottomContent = bottomContent,
                defaultWindowInsetsPadding = true,
            )
        }
        return
    }
    TopAppBar(
        title = titleText,
        subtitle = subtitle,
        bottomContent = bottomContent,
        modifier = modifier,
        color = barColor,
        titleColor = titleColor,
        largeTitleColor = largeTitleColor,
        subtitleColor = subtitleColor,
        titlePadding = titlePadding,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = if (canScroll) miuixScrollBehavior else null,
        defaultWindowInsetsPadding = true,
    )
}
