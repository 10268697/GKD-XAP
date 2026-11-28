package li.gkd.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import li.gkd.app.ui.share.LocalLayerBackdrop
import li.gkd.app.util.Option
import li.gkd.app.util.OptionMenuLabel
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

/**
 * 下拉选项。
 *
 * [glass] 为 true 时用 [GlassDropdownPreference]：它的弹层挂在根 scaffold 内，可以采样
 * 页面内容做真实背景模糊，液态玻璃模式下不再出现一块突变的实色面板。
 *
 * [glass] 为 false 时退回 [WindowDropdownPreference]（独立窗口弹层）：用于「全屏 Dialog
 * 里的设置项」——那里没有 Miuix Scaffold 承载弹层，玻璃版的 Popup 会找不到宿主，
 * 表现为点了没反应。
 */
@Composable
fun <T> TextMenu(
    modifier: Modifier = Modifier,
    title: String,
    option: Option<T>,
    glass: Boolean = true,
    onOptionChange: ((Option<T>) -> Unit),
) {
    val items = remember(option.options) {
        option.options.map { other ->
            if (other is OptionMenuLabel) other.menuLabel else other.label
        }
    }
    val selectedIndex = remember(option.value, option.options) {
        option.options.indexOfFirst { it.value == option.value }.coerceAtLeast(0)
    }
    val onSelectedIndexChange: (Int) -> Unit = { index ->
        val selected = option.options.getOrNull(index)
        if (selected != null && selected != option) {
            onOptionChange(selected)
        }
    }
    if (glass) {
        GlassDropdownPreference(
            onSelectedIndexChange = onSelectedIndexChange,
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            backdrop = LocalLayerBackdrop.current,
            modifier = modifier,
        )
    } else {
        WindowDropdownPreference(
            modifier = modifier,
            title = title,
            items = items,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    }
}
