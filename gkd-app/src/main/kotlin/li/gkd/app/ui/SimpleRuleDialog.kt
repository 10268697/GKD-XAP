package li.gkd.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkActionButton
import li.gkd.app.ui.component.GkAlertDialog
import li.gkd.app.ui.component.GkFormTextField
import li.gkd.app.ui.style.GkUi
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 简易规则输入：一个定位弹窗的条件，一个关闭弹窗的方式。
 *
 * 写法与李跳跳一致（`+` 开头、`-` 结尾、`=` 完全相同、`&` 多条件），转换逻辑在
 * [li.gkd.app.util.simpleRuleSelector] / [li.gkd.app.util.buildSimpleRuleMatches]。
 * 关闭方式留空表示按返回键，与旧版「通过返回键关闭」开关等价。
 */
@Composable
fun SimpleRuleDialog(
    onDismissRequest: () -> Unit,
    onSubmit: (locator: String, closeLocator: String) -> Unit,
) {
    var locatorValue by remember { mutableStateOf("") }
    var closeValue by remember { mutableStateOf("") }
    var showExample by remember { mutableStateOf(false) }

    GkAlertDialog(
        properties = DialogProperties(dismissOnClickOutside = false),
        onDismissRequest = onDismissRequest,
        title = { Text(text = UiStrings.simple_rule_title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(GkUi.SectionGap),
            ) {
                GkFormTextField(
                    value = locatorValue,
                    onValueChange = { locatorValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = UiStrings.simple_rule_locator_label,
                    singleLine = true,
                )
                GkFormTextField(
                    value = closeValue,
                    onValueChange = { closeValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = UiStrings.simple_rule_close_label,
                    singleLine = true,
                )
                Text(
                    text = UiStrings.simple_rule_close_desc,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                if (showExample) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = UiStrings.simple_rule_example_title,
                            style = MiuixTheme.textStyles.subtitle,
                            color = MiuixTheme.colorScheme.primary,
                        )
                        Text(
                            text = UiStrings.simple_rule_example_content,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            GkActionButton(
                emphasized = true,
                enabled = locatorValue.isNotBlank() || closeValue.isNotBlank(),
                onClick = throttle { onSubmit(locatorValue, closeValue) },
            ) { Text(UiStrings.simple_rule_add) }
        },
        dismissButton = {
            GkActionButton(onClick = { showExample = !showExample }) {
                Text(UiStrings.simple_rule_example)
            }
            GkActionButton(onClick = throttle(fn = onDismissRequest)) {
                Text(UiStrings.simple_rule_cancel)
            }
        },
    )
}
