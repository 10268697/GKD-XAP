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
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏：统一使用 MIUIX 的 [TopAppBar] / [SmallTopAppBar]。
 *
 * 与旧 M3 版本相比：
 * - [titleText] 为字符串标题；副标题 [subtitle]；[bottomContent] 放搜索等自定义内容。
 * - [scrollBehavior] 为 MIUIX [ScrollBehavior]，配合 `rememberListScrollState` 等使用。
 * - 不再需要 windowInsets 参数，MIUIX 通过 [defaultWindowInsetsPadding] 处理状态栏内边距。
 *
 * [small] 用于「标题自带渲染（如应用名内联图标）、且不需要大标题」的顶栏：
 * MIUIX 的 [TopAppBar] 是**大标题**栏，标题为空串时它仍会占掉一整行大标题高度
 * （`CollapsedHeight + 一行 title1 + LargeTitleBottomPadding`），
 * 再把 [bottomContent] 顶到更下面，于是出现「图标行与标题之间一大片留白」。
 * 这种场景应改用 [SmallTopAppBar]：固定高度，[bottomContent] 紧贴图标行下方。
 */
@Composable
fun GkTopAppBar(
    titleText: String = "",
    subtitle: String = "",
    modifier: Modifier = Modifier,
    bottomContent: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    color: Color = MiuixTheme.colorScheme.surface,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    largeTitleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitleColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    scrollBehavior: ScrollBehavior? = null,
    canScroll: Boolean = true,
    small: Boolean = false,
) {
    // 玻璃顶栏：仅在「液态玻璃开启 + 页面有 backdrop + 调用方没改任何颜色 + 非 small」时启用。
    // 自定义色（多选栏的破坏性红、图片查看器的白字、截图浮层的黑底）一律留在原 TopAppBar，
    // 因为 GlassTopAppBar 的颜色由主题推导，没有 titleColor/largeTitleColor 入参。
    val surfaceColor = MiuixTheme.colorScheme.surface
    val glassBar = LocalMiuixBlurActive.current &&
        !small &&
        LocalLayerBackdrop.current != null &&
        (color == Color.Transparent || color == surfaceColor) &&
        titleColor == MiuixTheme.colorScheme.onSurface &&
        largeTitleColor == MiuixTheme.colorScheme.onSurface &&
        subtitleColor == MiuixTheme.colorScheme.onSurfaceVariantSummary
    if (glassBar) {
        CompositionLocalProvider(LocalGkGlassTopBar provides true) {
            GlassTopAppBar(
                title = titleText,
                modifier = modifier,
                // backdrop 传 null：模糊仍由外层壳的 textureBlur 负责，避免同一块顶栏被
                // 采样两次；这里只取它的渐进色带（bandBrush）与玻璃按钮材质。
                expandable = true,
                backdrop = null,
                largeTitle = titleText,
                subtitle = subtitle,
                scrollBehavior = if (canScroll) scrollBehavior else null,
                navigationIcon = navigationIcon,
                actions = actions,
                bottomContent = bottomContent,
                defaultWindowInsetsPadding = true,
            )
        }
        return
    }
    if (small) {
        SmallTopAppBar(
            title = titleText,
            subtitle = subtitle,
            bottomContent = bottomContent,
            modifier = modifier,
            color = color,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
            titlePadding = titlePadding,
            navigationIcon = navigationIcon,
            actions = actions,
            // 必须把 scrollBehavior 交出去：rememberListScrollState 造的
            // TopAppBarState 初始 heightOffsetLimit 是 -Float.MAX_VALUE，
            // 而 ScrollBehavior.onPreScroll 会无条件把滚动量加到 heightOffset 上。
            // SmallTopAppBar 的 SideEffect 会把该 limit 收敛成 0，滚动才不会被顶栏吃掉。
            scrollBehavior = if (canScroll) scrollBehavior else null,
            defaultWindowInsetsPadding = true,
        )
        return
    }
    TopAppBar(
        title = titleText,
        subtitle = subtitle,
        bottomContent = bottomContent,
        modifier = modifier,
        color = color,
        titleColor = titleColor,
        largeTitleColor = largeTitleColor,
        subtitleColor = subtitleColor,
        titlePadding = titlePadding,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = if (canScroll) scrollBehavior else null,
        defaultWindowInsetsPadding = true,
    )
}
