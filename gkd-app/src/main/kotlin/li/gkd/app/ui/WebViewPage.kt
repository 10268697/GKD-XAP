package li.gkd.app.ui

import li.gkd.app.MainViewModel
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.nav.core.NavKey
import com.kevinnzou.web.AccompanistWebViewClient
import com.kevinnzou.web.LoadingState
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewState
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import li.gkd.app.text.UiStrings
import li.gkd.app.META
import li.gkd.app.MainActivity
import li.gkd.app.ui.component.PerfDropdownMenu
import li.gkd.app.ui.component.PerfDropdownMenuItem
import li.gkd.app.ui.style.scaffoldPadding
import li.gkd.app.util.AndroidTarget
import li.gkd.app.util.LogUtils
import li.gkd.app.util.client
import li.gkd.app.util.ToastUtils.copyText
import li.gkd.app.ui.share.launchUiAction
import li.gkd.app.util.IntentUtils
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkPageScaffold
import li.gkd.app.ui.component.GkTopAppBar

@Serializable
data class WebViewRoute(val initUrl: String) : NavKey

@Composable
fun WebViewPage(route: WebViewRoute) {
    val initUrl = route.initUrl
    val mainVm = MainViewModel.requireCurrent()
    val webViewState = rememberWebViewState(url = initUrl)
    val webViewClient = remember { GkdWebViewClient() }
    var webView by remember { mutableStateOf<WebView?>(null) }
    GkPageScaffold(enableContentBlur = false, topBar = {
        GkTopAppBar(
            color = barColor,
            modifier = Modifier.fillMaxWidth(),
            navigationIcon = {
                GkIconButton(
                    imageVector = GkIcons.ArrowBack,
                    onClick = { mainVm.popPage() },
                )
            },
            titleText = if (webViewState.loadingState is LoadingState.Loading) {
                UiStrings.loading_progress
            } else {
                // webViewState.pageTitle 在调用 reload 后会变成 null
                webViewState.pageTitle ?: webView?.title ?: ""
            },
            actions = {
                if (chromeVersion in 1..<MINI_CHROME_VERSION) {
                    GkIconButton(
                        imageVector = GkIcons.WarningAmber,
                        onClick = throttle(mainVm.scope.launchUiAction {
                            mainVm.dialogRequests.showMessage(
                                title = UiStrings.compatibility_notice,
                                text = UiStrings.webview_outdated_notice(chromeVersion),
                            )
                        }),
                    )
                }
                var expanded by remember { mutableStateOf(false) }
                PerfDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    enableWindowDim = false,
                    anchor = {
                        GkIconButton(imageVector = GkIcons.MoreVert, onClick = { expanded = true })
                    },
                ) {
                    if (webViewState.loadingState !is LoadingState.Loading) {
                        PerfDropdownMenuItem(
                            text = UiStrings.webview_reload,
                            onClick = {
                                expanded = false
                                webView?.reload()
                            }
                        )
                    }
                    PerfDropdownMenuItem(
                        text = UiStrings.link_copy,
                        onClick = {
                            expanded = false
                            copyText(webView?.url ?: initUrl)
                        }
                    )
                    PerfDropdownMenuItem(
                        text = UiStrings.link_open_external,
                        onClick = {
                            expanded = false
                            IntentUtils.openUri(webView?.url ?: initUrl)
                        }
                    )
                }
            }
        )
    }) { contentPadding ->
        WebView(
            modifier = Modifier
                .fillMaxSize()
                .scaffoldPadding(contentPadding),
            state = webViewState,
            client = webViewClient,
            onCreated = {
                webView = it
                it.addJavascriptInterface(GkdWebViewJsApi, "gkd")
                it.settings.apply {
                    @SuppressLint("SetJavaScriptEnabled")
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    if (AndroidTarget.TIRAMISU) {
                        setAlgorithmicDarkeningAllowed(false)
                    }
                }
            }
        )
    }
}

@Suppress("unused")
private object GkdWebViewJsApi {
    @JavascriptInterface
    fun getAppId() = META.appId

    @JavascriptInterface
    fun getAppName() = META.appName

    @JavascriptInterface
    fun getVersionCode() = META.versionCode

    @JavascriptInterface
    fun getVersionName() = META.versionName

    @JavascriptInterface
    fun getChannel() = META.channel

    @JavascriptInterface
    fun getDebuggable() = META.debuggable
}

private const val MINI_CHROME_VERSION = 107
private val chromeVersion by lazy {
    WebView.getCurrentWebViewPackage()?.versionName?.run {
        splitToSequence('.').first().toIntOrNull()
    } ?: 0
}

private const val DOC_CONFIG_URL =
    "https://registry.npmmirror.com/@gkd-kit/docs/latest/files/_config.json"

private const val DEBUG_JS_TEXT = """
<script src="https://registry.npmmirror.com/eruda/latest/files"></script>
<script>eruda.init();</script>
"""


@Serializable
private data class DocConfig(
    val mirrorBaseUrl: String,
    val htmlUrlMap: Map<String, String>
)

/**
 * 文档站路径映射表。`shouldInterceptRequest` 对**每一次**主框架导航都会调用，
 * 原来每次都要先拉一遍这个 json 再拉正文：两次串行请求，配合 15s/30s 的超时，
 * 最坏一次点击要白屏 45 秒。映射表几乎不变，进程内缓存即可，失败不写入缓存下次重试。
 * 正文保持实时拉取，避免文档更新后读到旧内容。
 */
private object DocConfigCache {
    @Volatile
    private var cached: DocConfig? = null

    suspend fun get(): DocConfig {
        cached?.let { return it }
        return client.get(DOC_CONFIG_URL).body<DocConfig>().also { cached = it }
    }
}

private class GkdWebViewClient() : AccompanistWebViewClient() {
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url
        if (uri != null && uri.host != "gkd.li") {
            if (uri.scheme == "gkd") {
                (view?.context as? MainActivity)?.mainVm?.handleGkdUri(uri)
            } else {
                IntentUtils.openUri(uri)
            }
            return true
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        try {
            if (request != null && request.run { isForMainFrame && url.host == "gkd.li" && method == "GET" }) {
                LogUtils.d(request.method, request.url)
                runBlocking(Dispatchers.IO) {
                    val docConfig = DocConfigCache.get()
                    val path = request.url.path.let { if (it.isNullOrEmpty()) "/" else it }
                    val textUrl = docConfig.htmlUrlMap[path]?.let { docConfig.mirrorBaseUrl + it }
                    if (textUrl != null) {
                        val textContent = client.get(textUrl).body<String>().let {
                            if (META.debuggable) {
                                DEBUG_JS_TEXT + it
                            } else {
                                it
                            }
                        }
                        return@runBlocking WebResourceResponse(
                            "text/html",
                            "UTF-8",
                            textContent.byteInputStream()
                        )
                    }
                    return@runBlocking null
                }?.let { return it }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return super.shouldInterceptRequest(view, request)
    }
}
