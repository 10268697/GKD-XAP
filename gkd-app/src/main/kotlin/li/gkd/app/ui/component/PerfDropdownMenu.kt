package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

/**
 * MIUIX 下拉菜单。使用 [WindowListPopup]，避免在毛玻璃顶栏内开 Overlay 触发 RenderThread 崩溃。
 * Prefer [anchor] so the button and popup share the measured parent used by Miuix for positioning.
 * Without [anchor], callers must put both in the same non-empty Box; an empty sibling Box is not
 * an anchor. The native window popup handles outside taps and Back without navigating the page.
 * [enableWindowDim] only controls the scrim, not outside-tap dismissal.
 */
@Composable
fun PerfDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.End,
    enableWindowDim: Boolean = true,
    anchor: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (anchor != null) {
        Box(
            modifier = modifier.wrapContentSize(Alignment.TopEnd),
        ) {
            anchor()
            DropdownPopup(
                expanded = expanded,
                onDismissRequest = onDismissRequest,
                alignment = alignment,
                enableWindowDim = enableWindowDim,
                content = content,
            )
        }
    } else {
        // 兼容旧用法：调用方自备锚点 Box，且须把按钮放进该 Box
        DropdownPopup(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            alignment = alignment,
            enableWindowDim = enableWindowDim,
            content = content,
        )
    }
}

@Composable
private fun DropdownPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: PopupPositionProvider.Align,
    enableWindowDim: Boolean,
    content: @Composable () -> Unit,
) {
    WindowListPopup(
        show = expanded,
        popupModifier = modifier,
        popupPositionProvider = ListPopupDefaults.dropdownPositionProvider(verticalMargin = 0.dp),
        alignment = alignment,
        enableWindowDim = enableWindowDim,
        onDismissRequest = onDismissRequest,
    ) {
        ListPopupColumn {
            content()
        }
    }
}

/** Native Miuix menu row. Use [titleColor] for destructive actions; callbacks own dismissal. */
@Composable
fun PerfDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    titleColor: Color = Color.Unspecified,
    startAction: (@Composable () -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    role: Role = Role.Button,
    onClickLabel: String? = null,
) {
    val colors = if (titleColor == Color.Unspecified) {
        BasicComponentDefaults.titleColor()
    } else {
        BasicComponentColors(
            color = titleColor,
            disabledColor = titleColor.copy(alpha = 0.4f),
        )
    }
    // 早期这里把 startAction / endActions 塞进 content 自绘，是为了绕开 miuix 0.9.4 的
    // BasicComponent 溢出：那两个槽位有内容时，measure 会算
    // endHardCap = (maxWidth - startWidth - spacer) * 6 / 10，而 ListPopupColumn 用
    // maxIntrinsicWidth(Int.MAX_VALUE) 探测宽度，乘 6 溢出成负数，随后 Constraints.copy()
    // 抛 "maxWidth must be >= than minWidth"（上游 issue compose-miuix-ui/miuix#468）。
    // 0.9.5 已用 constraints.hasBoundedWidth 判断 + coerceAtLeast(0) 修掉，故还原成官方槽位。
    BasicComponent(
        modifier = modifier.semantics(mergeDescendants = true) {
            this.role = role
            if (!enabled) disabled()
        },
        startAction = startAction,
        endActions = endActions,
        enabled = enabled,
        onClick = onClick,
        onClickLabel = onClickLabel,
        role = role,
    ) {
        Text(
            text = text,
            fontSize = MiuixTheme.textStyles.headline1.fontSize,
            fontWeight = FontWeight.Medium,
            color = if (enabled) colors.color else colors.disabledColor,
        )
    }
}
