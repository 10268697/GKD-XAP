package li.gkd.app.feature.settings

import li.gkd.app.data.settings.ScanBoost
import li.gkd.app.service.TrackService
import li.gkd.app.text.UiStrings
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.store.AppStore
import li.gkd.app.ui.share.BaseViewModel
import li.gkd.app.util.ToastUtils.toast

class AdvancedVm : BaseViewModel() {

    fun setTrackServiceEnabled(enabled: Boolean) {
        if (enabled) TrackService.start() else TrackService.stop()
    }

    fun saveHttpServerPort(value: String): Boolean {
        val newPort = value.toIntOrNull()
        if (newPort == null || newPort !in 1000..65535) {
            toast(UiStrings.http_port_input_hint)
            return false
        }
        if (newPort == storeFlow.value.httpServerPort) {
            return true
        }
        AppStore.updateSettings { it.copy(httpServerPort = newPort) }
        toast(UiStrings.update_success)
        return true
    }

    fun setAutoClearMemorySubs(enabled: Boolean) {
        AppStore.updateSettings { it.copy(autoClearMemorySubs = enabled) }
    }

    fun setScanBoostEnabled(enabled: Boolean) {
        AppStore.updateSettings { it.copy(scanBoostEnabled = enabled) }
    }

    fun saveScanBoostWindowSeconds(value: String): Boolean {
        val seconds = value.toIntOrNull()
        val min = ScanBoost.MIN_WINDOW_MILLIS / 1000
        val max = ScanBoost.MAX_WINDOW_MILLIS / 1000
        if (seconds == null || seconds !in min..max) {
            toast("窗口时长需在 $min–$max 秒之间")
            return false
        }
        if (seconds * 1000 == storeFlow.value.scanBoostWindowMillis) return true
        AppStore.updateSettings { it.copy(scanBoostWindowMillis = seconds * 1000) }
        toast(UiStrings.update_success)
        return true
    }

    fun saveScanBoostIntervalMs(value: String): Boolean {
        val millis = value.toIntOrNull()
        if (millis == null || millis !in ScanBoost.MIN_INTERVAL_MILLIS..ScanBoost.MAX_INTERVAL_MILLIS) {
            toast("扫描间隔需在 ${ScanBoost.MIN_INTERVAL_MILLIS}–${ScanBoost.MAX_INTERVAL_MILLIS} 毫秒之间")
            return false
        }
        if (millis == storeFlow.value.scanBoostIntervalMillis) return true
        AppStore.updateSettings { it.copy(scanBoostIntervalMillis = millis) }
        toast(UiStrings.update_success)
        return true
    }

    fun setAiEnable(enabled: Boolean) {
        AppStore.updateSettings { it.copy(aiEnable = enabled) }
    }
}
