package li.gkd.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import li.gkd.app.app
import li.gkd.app.appRuntimeReady
import li.gkd.app.appScope
import li.gkd.app.permission.PermissionStates
import li.gkd.app.priv.CrashRecovery
import li.gkd.app.service.A11yService
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.util.LogUtils
import li.gkd.app.util.launchLogged
import kotlin.time.Duration.Companion.milliseconds

/**
 * 开机后把本应用的无障碍条目写回系统设置。
 *
 * ColorOS 等系统会在重启或清理后台时移除第三方无障碍条目。只要曾经拿到过
 * `WRITE_SECURE_SETTINGS`（Root 自授，或 Shizuku 下由特权服务执行 `pm grant`），
 * 这条权限本身会持久保留，因此开机时不需要特权服务在线也能直接写回。
 *
 * 与 [BootReceiver] 并存，职责不同：BootReceiver 触发应用内的状态同步（依赖特权服务在
 * 线），这里补的是「服务条目已被系统删掉」的情况。
 */
class BootRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != ACTION_QUICKBOOT_POWERON) return
        val pendingResult = goAsync()
        appScope.launchLogged(Dispatchers.IO) {
            try {
                restoreA11yAfterBoot()
            } catch (e: Throwable) {
                LogUtils.d("restoreA11yAfterBoot failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"

// goAsync 的广播执行窗口约 10s，这里的总耗时必须留在窗口内：
// 2500ms 等迁移 + 3 轮 ×(600ms 修复 + 1200ms 启动) ≈ 7.9s。
private const val RESTORE_ATTEMPTS = 3
private val RUNTIME_WAIT = 2500.milliseconds
private val PERMISSION_WAIT = 800.milliseconds
private val FIX_DELAY = 600.milliseconds
private val START_DELAY = 1200.milliseconds

private suspend fun restoreA11yAfterBoot() {
    // 开机广播可能早于数据迁移完成，此时读设置会拿到空库。运行时就绪标志就是为此存在的。
    if (withTimeoutOrNull(RUNTIME_WAIT) { appRuntimeReady.await() } == null) {
        LogUtils.d("restoreA11yAfterBoot skipped: runtime not ready")
        return
    }
    val store = storeFlow.value
    if (!store.enableAutomator || !store.useA11y) {
        LogUtils.d("restoreA11yAfterBoot skipped: automation disabled or not a11y mode")
        return
    }
    repeat(RESTORE_ATTEMPTS) { attempt ->
        if (A11yService.isRunning.value) return
        if (!PermissionStates.writeSecureSettings.updateAndGet()) {
            // 权限由 Root 自授或 Shizuku pm grant 持久化，通常瞬间可得；等不到就放弃，
            // 避免在广播窗口内反复空转。
            delay(PERMISSION_WAIT)
            return@repeat
        }
        val names = app.getSecureA11yServices()
        if (names.contains(A11yService.a11yCn)) {
            // 条目在但服务没跑，按故障重启处理：先移除，等系统反应，再加回。
            names.remove(A11yService.a11yCn)
            app.putSecureA11yServices(names)
            delay(FIX_DELAY)
        }
        app.putSecureInt(Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        names.add(A11yService.a11yCn)
        app.putSecureA11yServices(names)
        delay(START_DELAY)
        LogUtils.d("restoreA11yAfterBoot", "attempt=${attempt + 1} running=${A11yService.isRunning.value}")
    }
}
