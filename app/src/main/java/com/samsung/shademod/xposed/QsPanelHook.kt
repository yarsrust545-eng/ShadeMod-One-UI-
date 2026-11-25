package com.samsung.shademod.xposed

import android.view.View
import com.samsung.shademod.PrefsConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object QsPanelHook {

    fun install(classLoader: ClassLoader, prefs: XSharedPreferences) {
        hookTileColumns(classLoader, prefs)
        hookShadeBackgroundAlpha(classLoader, prefs)
        hookHideMediaAndDeviceBars(classLoader, prefs)
    }

    private fun hookTileColumns(classLoader: ClassLoader, prefs: XSharedPreferences) {
        val tileLayoutClasses = listOf(
            "com.android.systemui.qs.TileLayout",
            "com.android.systemui.qs.SecTileLayout"
        )
        for (clsName in tileLayoutClasses) {
            val cls = XposedHelpers.findClassIfExists(clsName, classLoader) ?: continue
            try {
                XposedBridge.hookAllMethods(cls, "updateColumns", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        prefs.reload()
                        val customCols = prefs.getInt(PrefsConfig.KEY_QS_COLUMNS, 0)
                        if (customCols in 3..8) {
                            XposedHelpers.setIntField(param.thisObject, "mColumns", customCols)
                        }
                    }
                })
            } catch (t: Throwable) {
                XposedBridge.log("ShadeMod: skip $clsName updateColumns: ${t.message}")
            }
        }
    }

    private fun hookShadeBackgroundAlpha(classLoader: ClassLoader, prefs: XSharedPreferences) {
        val panelClasses = listOf(
            "com.android.systemui.shade.NotificationPanelViewController",
            "com.android.systemui.statusbar.phone.NotificationPanelViewController"
        )
        for (clsName in panelClasses) {
            val cls = XposedHelpers.findClassIfExists(clsName, classLoader) ?: continue
            try {
                XposedBridge.hookAllMethods(cls, "setExpandedFraction", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        prefs.reload()
                        val alphaPct = prefs.getInt(PrefsConfig.KEY_SHADE_ALPHA, 100)
                        if (alphaPct < 100) {
                            val view = runCatching {
                                XposedHelpers.getObjectField(param.thisObject, "mView") as? View
                            }.getOrNull() ?: return
                            view.alpha = (alphaPct / 100f).coerceIn(0.25f, 1.0f)
                        }
                    }
                })
            } catch (t: Throwable) {
                XposedBridge.log("ShadeMod: skip $clsName alpha: ${t.message}")
            }
        }
    }

    private fun hookHideMediaAndDeviceBars(classLoader: ClassLoader, prefs: XSharedPreferences) {
        val barClasses = listOf(
            "com.android.systemui.qs.bar.QSMediaPlayerBar",
            "com.android.systemui.qs.bar.MediaDevicesBar"
        )
        for (clsName in barClasses) {
            val cls = XposedHelpers.findClassIfExists(clsName, classLoader) ?: continue
            try {
                XposedBridge.hookAllMethods(cls, "inflateViews", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        prefs.reload()
                        if (prefs.getBoolean(PrefsConfig.KEY_HIDE_MEDIA_DEVICE, false)) {
                            val barView = runCatching {
                                XposedHelpers.getObjectField(param.thisObject, "mBarRootView") as? View
                            }.getOrNull()
                            barView?.visibility = View.GONE
                        }
                    }
                })
            } catch (t: Throwable) {
                XposedBridge.log("ShadeMod: skip $clsName hide bar: ${t.message}")
            }
        }
    }
}
