package li.gkd.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the startup path on a real device and turns it into baseline profile rules.
 *
 * The rules are written to `benchmark/src/main/baseline-prof.txt` because `saveInSrc` is on; copy
 * that file to `gkd-app/src/main/baseline-prof.txt` afterwards, since that is where AGP looks for
 * the app module's own rules before merging them with the ones shipped by the dependencies.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupProfile() {
        val target = InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: "li.songe.gkdxap"
        rule.collect(packageName = target, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()
            // Let the first frame, the store load and the initial list population all land inside
            // the sampled window instead of stopping at "activity resumed".
            Thread.sleep(2_000)
            // Bottom bar has four evenly spaced tabs; index 2 is the app list, which also exercises
            // the sort dropdown whose measurement used to overflow.
            val barY = device.displayHeight - 60
            device.click(device.displayWidth * 5 / 8, barY)
            device.waitForIdle()
            Thread.sleep(1_500)
            device.click(device.displayWidth * 7 / 8, barY)
            device.waitForIdle()
            Thread.sleep(1_000)
        }
    }
}
