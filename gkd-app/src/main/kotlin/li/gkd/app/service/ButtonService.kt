package li.gkd.app.service

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import li.gkd.app.R
import li.gkd.app.notif.NotificationCatalog
import li.gkd.app.permission.PermissionStates
import li.gkd.app.snapshot.SnapshotCapture
import li.gkd.app.snapshot.SnapshotCaptureMode
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.share.launchUi
import li.gkd.app.util.AiRuleGenerator
import li.gkd.app.util.IntentUtils
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.GlassEdgeStrokes
import top.yukonga.miuix.kmp.material.edgeStroke

// Keep the snapshot overlay light independently of the overlay host theme.
private val SnapshotIconTint = Color(0xFF0D84FF)
private val SnapshotDividerColor = Color(0xFFE5E5E5)

/**
 * 仿玻璃底色。
 *
 * 悬浮窗做不出真正的液态玻璃：`Modifier.glass()` 和 `GlassSurface()` 都要一个 [Backdrop]
 * 做同树内容采样，而 overlay 背后是别的应用窗口，Compose 树里没有可采样内容；窗口是
 * `TYPE_APPLICATION_OVERLAY`，也没有 `FLAG_BLUR_BEHIND` 可供系统跨应用模糊。
 *
 * 所以这里只模拟玻璃的层次：白色微透明填充 + 1dp 亮边 + 外阴影。alpha 保持在 0.92 以上
 * 是为了守住 v1.5.7 修的那个问题——底色太透会在深色应用上糊成一片。
 */
private val SnapshotGlassFill = Color.White.copy(alpha = 0.92f)
private val SnapshotGlassFillMenu = Color.White.copy(alpha = 0.96f)
private val SnapshotGlassCorner = 12.dp
// 阴影画在内容边界之外，overlay 窗口是 WRAP_CONTENT，必须留出等量的外沿才不会被裁掉。
private val SnapshotShadowInset = 5.dp
private val SnapshotGlassShape: Shape = RoundedCornerShape(SnapshotGlassCorner)

private fun Modifier.snapshotGlass(elevation: Dp, stroke: EdgeStroke): Modifier = this
    .shadow(elevation, SnapshotGlassShape, clip = false)
    .edgeStroke(SnapshotGlassShape, stroke)

class ButtonService : OverlayWindowService(
    positionKey = "button"
) {
    private val menuOpenFlow = MutableStateFlow(false)

    override fun onClickView() {
        if (isOverlayContentHidden) return
        // 展开态下动作由菜单项自己处理；这里不能再动，否则点「取消」会连带执行一次快照。
        if (menuOpenFlow.value) return
        menuOpenFlow.value = true
    }

    override fun onLongClickView() = stopSelf()

    override fun shouldDispatchTouchToCompose(): Boolean = menuOpenFlow.value

    @Composable
    override fun ComposeContent() {
        val menuOpen by menuOpenFlow.collectAsStateWithLifecycle()
        if (!menuOpen) {
            // 只渲染 Miuix 外观并暴露无障碍动作；物理触摸留给 overlay host 处理拖动、点击和长按。
            // 外层留出阴影外沿，否则 WRAP_CONTENT 窗口会把投影裁掉。
            Box(modifier = Modifier.padding(SnapshotShadowInset)) {
                Card(
                    modifier = Modifier
                        .size(48.dp)
                        .snapshotGlass(elevation = 6.dp, stroke = GlassEdgeStrokes.SmallLight)
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            onClick {
                                if (isOverlayContentHidden) false else {
                                    onClickView()
                                    true
                                }
                            }
                            onLongClick {
                                if (isOverlayContentHidden) false else {
                                    onLongClickView()
                                    true
                                }
                            }
                        },
                    cornerRadius = SnapshotGlassCorner,
                    colors = CardDefaults.defaultColors(color = SnapshotGlassFill),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GkIcon(
                            imageVector = GkIcons.CenterFocusWeak,
                            contentDescription = stringResource(R.string.snapshot_button),
                            modifier = Modifier.size(24.dp),
                            tint = SnapshotIconTint,
                        )
                    }
                }
            }
            return
        }
        // 非聚焦悬浮窗收不到外部点击，始终保留一个明确的收起入口。
        Box(modifier = Modifier.padding(SnapshotShadowInset)) {
            Card(
                modifier = Modifier.width(224.dp).snapshotGlass(elevation = 8.dp, stroke = GlassEdgeStrokes.MiddleLight),
                cornerRadius = SnapshotGlassCorner,
                colors = CardDefaults.defaultColors(color = SnapshotGlassFillMenu),
            ) {
                SmallTitle(
                    text = UiStrings.snapshot_title,
                    modifier = Modifier.semantics { heading() },
                    textColor = Color.Black,
                    insideMargin = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 4.dp),
                )
                MenuItem(UiStrings.button_menu_snapshot, GkIcons.CenterFocusWeak) {
                    runCapture(SnapshotCaptureMode.SNAPSHOT_ONLY)
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = SnapshotDividerColor)
                MenuItem(UiStrings.button_menu_snapshot_generate, GkIcons.Edit) {
                    runCapture(SnapshotCaptureMode.SNAPSHOT_AND_GENERATE)
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = SnapshotDividerColor)
                MenuItem(UiStrings.action_cancel, GkIcons.Close) {
                    menuOpenFlow.value = false
                }
            }
        }
    }

    private fun runCapture(mode: SnapshotCaptureMode) {
        menuOpenFlow.value = false
        lifecycleScope.launchUi {
            mode.run(
                capture = { withAllOverlaysHidden { SnapshotCapture.capture().id } },
                aiEnabled = li.gkd.app.store.AppStore.storeFlow.value.aiEnable,
                generate = { id, bypass -> AiRuleGenerator.generateRule(id, allowWhenAiDisabled = bypass) },
            )
        }
    }

    @Composable
    private fun MenuItem(
        label: String,
        icon: ImageVector,
        onClick: () -> Unit,
    ) {
        BasicComponent(
            title = label,
            titleColor = BasicComponentDefaults.titleColor(color = Color.Black),
            modifier = Modifier.heightIn(min = 48.dp),
            startAction = {
                GkIcon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = SnapshotIconTint,
                )
            },
            onClick = onClick,
        )
    }

    init {
        useServicePresence(
            stateFlow = isRunning,
            name = UiStrings.snapshot_button_service,
        )
        onCreated {
            NotificationCatalog.button().startForeground()
        }
    }

    companion object {
        val isRunning: StateFlow<Boolean>
            field = MutableStateFlow(false)
        fun start() {
            if (!PermissionStates.drawOverlays.checkOrToast()) return
            IntentUtils.startForegroundServiceByClass(ButtonService::class)
        }

        fun stop() = IntentUtils.stopServiceByClass(ButtonService::class)

    }
}
