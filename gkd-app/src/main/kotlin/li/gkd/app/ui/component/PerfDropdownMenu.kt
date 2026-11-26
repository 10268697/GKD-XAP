package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
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
    // 不要改回 startAction / endActions 槽位：miuix 0.9.4 的 BasicComponent 在这两个槽位
    // 有内容时，measure 里会算 endHardCap = (maxWidth - startWidth - spacer) * 6 / 10。
    // 而 ListPopupColumn 的固有测量是用 maxIntrinsicWidth(Int.MAX_VALUE) 探测宽度的，
    // 此时 maxWidth = Int.MAX_VALUE，乘 6 直接溢出成负数，随后
    // looseConstraints.copy(maxWidth = 负数) 抛
    // IllegalArgumentException: maxWidth must be >= than minWidth，下拉菜单必崩。
    // （上游 issue：compose-miuix-ui/miuix#468，0.9.5-beta 用 hasBoundedWidth 判断修掉了。）
    // 这里把图标/尾部控件放进 content 槽位自己摆一行，测量只走普通 Column，宽度仍自适应。
    BasicComponent(
        modifier = modifier.semantics(mergeDescendants = true) {
            this.role = role
            if (!enabled) disabled()
        },
        enabled = enabled,
        onClick = onClick,
        onClickLabel = onClickLabel,
        role = role,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (startAction != null) {
                startAction()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                color = if (enabled) colors.color else colors.disabledColor,
            )
            if (endActions != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    endActions()
                }
            }
        }
    }
}
