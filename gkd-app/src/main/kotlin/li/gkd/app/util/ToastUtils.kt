package li.gkd.app.util

import android.content.ClipData
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.text.LineBreaker
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.hjq.toast.ActivityToast
import com.hjq.toast.SystemToast
import com.hjq.toast.ToastParams
import com.hjq.toast.ToastStrategy
import com.hjq.toast.Toaster
import com.hjq.toast.config.IToast
import com.hjq.toast.style.WhiteToastStyle
import li.gkd.app.notif.ActionTipNotif
import li.gkd.app.text.UiStrings
import li.gkd.app.app
import li.gkd.app.data.ResolvedRule
import li.gkd.app.permission.PermissionStates
import li.gkd.app.platform.lifecycle.MainActivityVisibility
import li.gkd.app.service.A11yService
import li.gkd.app.store.AppStore.actionCountFlow
import li.gkd.app.store.AppStore.storeFlow
import li.songe.codeorigin.CallSite

object ToastUtils {
    fun toast(
        text: CharSequence,
        forced: Boolean = false,
        delayMillis: Long = 0L,
        @CallSite loc: String = "",
    ) {
        if (delayMillis > 0) {
            runMainPost(delayMillis) {
                toast(text = text, forced = forced, loc = loc)
            }
            return
        }
        if (forced || MainActivityVisibility.isVisible) {
            Toaster.show(text)
        }
        if (loc.isNotEmpty()) {
            LogUtils.d(text, loc = loc)
        }
    }

    private val darkTheme: Boolean
        get() = storeFlow.value.enableDarkTheme ?: app.resources.configuration.let {
            it.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }

    private val toastYOffset: Int
        get() = (ScreenUtils.getScreenHeight() * 0.12f).toInt()

    private val circleOutlineProvider by lazy {
        object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                if (view != null && outline != null) {
                    // 20.sp : line height, 12.dp : top/bottom padding
                    outline.setRoundRect(
                        0,
                        0,
                        view.width,
                        view.height,
                        (12.dp.px * 2 + 20.sp.px) / 2f
                    )
                }
            }
        }
    }

    private fun View.updateToastView() {
        setPaddingRelative(
            16.dp.px.toInt(),
            12.dp.px.toInt(),
            16.dp.px.toInt(),
            12.dp.px.toInt(),
        )
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        if (this is TextView) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 14.sp.px)
            setTextColor(if (darkTheme) Color.WHITE else Color.BLACK)
            if (AndroidTarget.Q) {
                breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
            }
        }
        background = GradientDrawable().apply {
            setColor((if (darkTheme) "#303030" else "#fafafa").toColorInt())
        }
        outlineProvider = circleOutlineProvider
        clipToOutline = true
        elevation = 2.dp.px
    }

    private fun setReactiveToastStyle() {
        Toaster.setStyle(object : WhiteToastStyle() {
            override fun getGravity() = Gravity.BOTTOM
            override fun getYOffset() = toastYOffset
            override fun getTranslationZ(context: Context?) = 0f
            override fun createView(context: Context?): View {
                return super.createView(context).apply {
                    updateToastView()
                }
            }
        })
    }

    private var triggerTime = 0L
    private const val triggerInterval = 2000L
    fun showActionToast(rule: ResolvedRule) {
        if (!storeFlow.value.toastWhenClick) return
        runMainPost {
            val t = System.currentTimeMillis()
            if (t - triggerTime > triggerInterval + 100) { // 100ms 保证二次显示的时候上一次已经完全消失
                triggerTime = t
                val text = ActionToastTemplate.render(
                    storeFlow.value.actionToast,
                    rule.rule.name.toString(),
                    rule.g.group.name,
                    actionCountFlow.value,
                )
                showActionTip(text)
            }
        }
    }

    /** 按当前「提示样式」分发：悬浮窗 / 系统 Toast / 实时通知（返回值仅实时通知有） */
    fun showActionTip(text: CharSequence): ActionTipNotif.PostResult? {
        return when (storeFlow.value.resolveActionTipStyle()) {
            ActionTipStyleOption.LiveNotif -> ActionTipNotif.show(
                text,
                autoDismissMs = storeFlow.value.actionTipLiveDurationMs,
            )
            ActionTipStyleOption.SystemToast -> {
                showSystemToast(text)
                null
            }
            ActionTipStyleOption.Overlay -> {
                showA11yToast(text)
                null
            }
        }
    }

    fun previewActionToast(text: CharSequence, useSystemToast: Boolean) {
        runMainPost {
            if (useSystemToast) showSystemToast(text) else showA11yToast(text)
        }
    }

    private var cancelCustomToast: (() -> Unit)? = null
    private var cacheToast: Toast? = null
    private fun showSystemToast(message: CharSequence) {
        cancelCustomToast?.invoke()
        cacheToast?.cancel()
        cacheToast = Toast.makeText(app, message, Toast.LENGTH_SHORT).apply {
            show()
        }
    }

    // 1.使用 WeakReference<View> 在某些机型上导致无法取消
    // 2.使用协程 delay + cacheView 也可能导致无法取消
    // https://github.com/gkd-kit/gkd/issues/697
    // https://github.com/gkd-kit/gkd/issues/698
    private fun showA11yToast(message: CharSequence) {
        cancelCustomToast?.invoke()
        cacheToast?.cancel()
        cacheToast = null
        val wm = A11yService.instance?.wm
            ?: if (PermissionStates.drawOverlays.updateAndGet()) app.windowManager else null
        if (wm == null) {
            showSystemToast(message)
            return
        }
        val textView = TextView(app).apply {
            text = message
            id = android.R.id.message
            gravity = Gravity.CENTER
            updateToastView()
        }
        val layoutParams = WindowManager.LayoutParams().apply {
            type = if (wm == app.windowManager) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            packageName = app.packageName
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.BOTTOM
            y = toastYOffset
            windowAnimations = android.R.style.Animation_Toast
        }
        wm.addView(textView, layoutParams)
        val cancel: () -> Unit = {
            cancelCustomToast = null
            try {
                wm.removeViewImmediate(textView)
            } catch (_: Exception) {
            }
        }
        cancelCustomToast = cancel
        runMainPost(triggerInterval) {
            if (cancelCustomToast === cancel) {
                cancel()
            }
        }
    }

    fun copyText(text: String) {
        app.clipboardManager.setPrimaryClip(ClipData.newPlainText(app.packageName, text))
        toast(UiStrings.copy_success)
    }

    fun initToast() {
        Toaster.init(app)
        Toaster.setDebugMode(false)
        Toaster.setInterceptor { false } // 覆盖默认拦截器
        setToastStrategy()
        setReactiveToastStyle()
    }

    /**
     * Toaster 的默认策略会在「已授予悬浮窗权限」时选中 GlobalToast —— 它用 Application 的
     * WindowManager 直接挂一个 TYPE_APPLICATION_OVERLAY 窗口来画 toast，系统随之弹出
     * 「正在其他应用的上层显示」。
     *
     * GKD 自己已经有无障碍与悬浮窗两套提示体系，不需要第三方库再开一个应用级窗口；
     * 而且这个分支只取决于系统里是否给过悬浮窗权限，会出现「同一个 App 换包名后
     * toast 样式突然变了、还多一条系统提示」这种诡异差异。固定走 ActivityToast
     * （画在 Activity 上）或 SystemToast（系统渲染），两者都不占用悬浮窗权限。
     *
     * 注意：默认实现是在 createToast() 内部调 diyToastStyle() 把 params.style 的 view
     * 注入 toast 的（ToastStrategy 15.0 的 ShowToastRunnable 只做 setDuration/setText/show，
     * 不碰 view）。这里自建实例必须自己补这一步，否则 ActivityToast 拿不到 view，
     * 表现就是「toast 完全不出现」。
     */
    private fun setToastStrategy() {
        Toaster.setStrategy(object : ToastStrategy(app) {
            override fun createToast(params: ToastParams): IToast {
                val activity = toastActivity
                return if (activity != null) {
                    ActivityToast(activity).also { diyToastStyle(it, params.style) }
                } else {
                    // SDK>=30 时自定义 view 的系统 toast 会被系统丢弃，交给系统画纯文本
                    SystemToast(app)
                }
            }
        })
    }
}
