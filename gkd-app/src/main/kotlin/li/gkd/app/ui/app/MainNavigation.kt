package li.gkd.app.ui.app

import li.gkd.app.MainViewModel

import li.gkd.app.feature.subscription.CategoryEditorPage
import li.gkd.app.feature.subscription.CategoryEditorRoute
import li.gkd.app.feature.subscription.RuleExcludeEditorPage
import li.gkd.app.feature.subscription.RuleExcludeEditorRoute
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import kotlin.reflect.KClass
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavEntryBuilder
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import li.gkd.app.ui.A11YScopeAppListRoute
import li.gkd.app.feature.log.A11yEventLogPage
import li.gkd.app.feature.log.A11yEventLogRoute
import li.gkd.app.ui.A11yScopeAppListPage
import li.gkd.app.feature.settings.AboutPage
import li.gkd.app.feature.settings.AboutRoute
import li.gkd.app.feature.log.ActionLogPage
import li.gkd.app.feature.log.ActionLogRoute
import li.gkd.app.feature.log.ActivityLogPage
import li.gkd.app.feature.log.ActivityLogRoute
import li.gkd.app.feature.settings.AdvancedPage
import li.gkd.app.feature.settings.AdvancedPageRoute
import li.gkd.app.feature.settings.ai.AiHelpPage
import li.gkd.app.feature.settings.ai.AiHelpPageRoute
import li.gkd.app.feature.settings.ai.AiProviderDetailPage
import li.gkd.app.feature.settings.ai.AiProviderDetailRoute
import li.gkd.app.feature.settings.ai.AiProvidersPage
import li.gkd.app.feature.settings.ai.AiProvidersPageRoute
import li.gkd.app.ui.AppConfigPage
import li.gkd.app.ui.AppConfigRoute
import li.gkd.app.ui.BlockA11yAppListPage
import li.gkd.app.ui.BlockA11yAppListRoute
import li.gkd.app.ui.CrashReportPage
import li.gkd.app.ui.CrashReportRoute
import li.gkd.app.ui.EditBlockAppListPage
import li.gkd.app.ui.EditBlockAppListRoute
import li.gkd.app.ui.ImagePreviewPage
import li.gkd.app.ui.ImagePreviewRoute
import li.gkd.app.ui.PrivilegeServicePage
import li.gkd.app.ui.PrivilegeServiceRoute
import li.gkd.app.feature.snapshot.SnapshotNodesPage
import li.gkd.app.feature.snapshot.SnapshotNodesRoute
import li.gkd.app.feature.snapshot.SnapshotPage
import li.gkd.app.feature.snapshot.SnapshotPageRoute
import li.gkd.app.feature.snapshot.SnapshotPreviewPage
import li.gkd.app.feature.snapshot.SnapshotPreviewRoute
import li.gkd.app.feature.snapshot.SnapshotSettingsPage
import li.gkd.app.feature.snapshot.SnapshotSettingsRoute
import li.gkd.app.feature.subscription.SubsAppGroupListPage
import li.gkd.app.feature.subscription.SubsAppGroupListRoute
import li.gkd.app.feature.subscription.SubsAppListPage
import li.gkd.app.feature.subscription.SubsAppListRoute
import li.gkd.app.feature.subscription.SubsCategoryGroupPage
import li.gkd.app.feature.subscription.SubsCategoryGroupRoute
import li.gkd.app.feature.subscription.SubsCategoryPage
import li.gkd.app.feature.subscription.SubsCategoryRoute
import li.gkd.app.feature.subscription.SubsGlobalGroupExcludePage
import li.gkd.app.feature.subscription.SubsGlobalGroupExcludeRoute
import li.gkd.app.feature.subscription.SubsGlobalGroupListPage
import li.gkd.app.feature.subscription.SubsGlobalGroupListRoute
import li.gkd.app.feature.subscription.UpsertRuleGroupPage
import li.gkd.app.feature.subscription.UpsertRuleGroupRoute
import li.gkd.app.ui.WebViewPage
import li.gkd.app.ui.WebViewRoute
import li.gkd.app.feature.settings.WorkModePage
import li.gkd.app.feature.settings.WorkModeRoute
import li.gkd.app.ui.home.HomePage
import li.gkd.app.ui.home.BlockA11ySetupPage
import li.gkd.app.ui.home.ActionToastPage
import li.gkd.app.ui.home.ActionToastRoute
import li.gkd.app.ui.home.NotificationTextPage
import li.gkd.app.ui.home.NotificationTextRoute
import li.gkd.app.ui.home.BlockA11ySetupRoute
import li.gkd.app.ui.home.HomeRoute


private fun NavEntryBuilder.mainRoutes() {
    entry<HomeRoute> { HomePage() }
    entry<WorkModeRoute> { WorkModePage() }
    entry<AboutRoute> { AboutPage() }
    entry<ActionToastRoute>(transition = NavTransitions.Modal) { ActionToastPage() }
    entry<NotificationTextRoute>(transition = NavTransitions.Modal) { NotificationTextPage() }
    entry<BlockA11ySetupRoute>(transition = NavTransitions.Modal) { BlockA11ySetupPage() }
    entry<BlockA11yAppListRoute> { BlockA11yAppListPage() }
    entry<AdvancedPageRoute> { AdvancedPage() }
    entry<AiProvidersPageRoute> { AiProvidersPage() }
    entry<AiProviderDetailRoute> { AiProviderDetailPage(it) }
    entry<AiHelpPageRoute> { AiHelpPage() }
    entry<PrivilegeServiceRoute> { PrivilegeServicePage() }
    entry<SnapshotPageRoute> { SnapshotPage() }
    entry<SnapshotPreviewRoute> { SnapshotPreviewPage(it) }
    entry<SnapshotNodesRoute> { SnapshotNodesPage(it) }
    entry<SnapshotSettingsRoute> { SnapshotSettingsPage() }
    entry<A11YScopeAppListRoute> { A11yScopeAppListPage() }
    entry<ActivityLogRoute> { ActivityLogPage() }
    entry<A11yEventLogRoute> { A11yEventLogPage() }
    entry<EditBlockAppListRoute>(transition = NavTransitions.Modal) { EditBlockAppListPage() }
    entry<SubsAppListRoute> { SubsAppListPage(it) }
    entry<WebViewRoute> { WebViewPage(it) }
    entry<SubsCategoryRoute> { SubsCategoryPage(it) }
    entry<SubsGlobalGroupListRoute> { SubsGlobalGroupListPage(it) }
    entry<SubsGlobalGroupExcludeRoute> { SubsGlobalGroupExcludePage(it) }
    entry<ActionLogRoute> { ActionLogPage(it) }
    entry<ImagePreviewRoute> { ImagePreviewPage(it) }
    entry<UpsertRuleGroupRoute>(transition = NavTransitions.Modal) { UpsertRuleGroupPage(it) }
    entry<CategoryEditorRoute>(transition = NavTransitions.Modal) { CategoryEditorPage(it) }
    entry<RuleExcludeEditorRoute>(transition = NavTransitions.Modal) { RuleExcludeEditorPage(it) }
    entry<SubsAppGroupListRoute> { SubsAppGroupListPage(it) }
    entry<AppConfigRoute> { AppConfigPage(it) }
    entry<CrashReportRoute> { CrashReportPage() }
    entry<SubsCategoryGroupRoute> { SubsCategoryGroupPage(it) }
}

/**
 * 进页面当帧就自己抢焦点的路由。这些页面内部的输入区用
 * `autoFocus(immediateFocus = true)` / `FocusRequester.requestFocus()` 主动要焦点，
 * 前进导航时不能替它们 clearFocus，否则刚到手的焦点会被父节点的 effect 打掉。
 *
 * 新增「进页面即输入」的页面时必须加进来，否则键盘不会自动弹出。
 */
private val selfFocusRouteClasses: Set<KClass<*>> = setOf(
    UpsertRuleGroupRoute::class,
    CategoryEditorRoute::class,
    RuleExcludeEditorRoute::class,
    A11YScopeAppListRoute::class,
    BlockA11yAppListRoute::class,
    EditBlockAppListRoute::class,
)

@Composable
fun MainNavigation() {
    val mainVm = MainViewModel.requireCurrent()
    // 被盖住的 entry 仍然保持 composed，所以返回之后上一页的焦点不会自己交还：
    // 在编辑器页点过输入框、返回后键盘还挂着、光标在看不见的上一页闪。
    // 上游 miuix-nav 在 7052841 里修了这个，但那是库内私有实现，这里做应用侧等价处理。
    //
    // 前进方向同样要交还焦点：被盖住的页面继续保持 composed，若上一页的搜索框仍持有焦点，
    // 输入法会跟着到新页面上（新页面再抢焦点时输入法连接重建，表现为键盘连弹两次）。
    // 唯一例外是「进页面当帧就自己抢焦点」的页面——它们靠 autoFocus(immediateFocus = true)
    // 在子节点 effect 里请求焦点，而本 effect 在子节点之后执行，一起清会把新页面刚拿到的
    // 焦点也打掉，所以按路由排除。
    val focusManager = LocalFocusManager.current
    val topKey = mainVm.backStack.lastOrNull()
    val stackSize = mainVm.backStack.size
    val previousTop = remember { mutableStateOf<Pair<NavKey?, Int>?>(null) }
    DisposableEffect(topKey, stackSize) {
        val previous = previousTop.value
        previousTop.value = topKey to stackSize
        if (previous != null && previous.first != topKey) {
            val wentBack = previous.second > stackSize
            val destinationWantsFocus = topKey != null && topKey::class in selfFocusRouteClasses
            if (wentBack || !destinationWantsFocus) {
                focusManager.clearFocus()
            }
        }
        onDispose { }
    }
    NavDisplay(
        backStack = mainVm.backStack,
        onBack = mainVm::popPage,
        transition = NavTransitions.MiuixDefault,
    ) {
        mainRoutes()
    }
}
