package com.samsung.shademod.xposed

import com.samsung.shademod.PrefsConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ShadeHookEntry : IXposedHookLoadPackage {

    companion object {
        private const val MODULE_PKG = "com.samsung.shademod"
        val prefs: XSharedPreferences by lazy {
            XSharedPreferences(MODULE_PKG, PrefsConfig.PREFS_NAME).apply {
                makeWorldReadable()
            }
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        XposedBridge.log("ShadeMod: Hooking Samsung SystemUI")
        BrightnessSliderHook.install(lpparam.classLoader, prefs)
        QsPanelHook.install(lpparam.classLoader, prefs)
    }
}
