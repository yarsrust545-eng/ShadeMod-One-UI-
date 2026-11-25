package com.samsung.shademod.xposed

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.ProgressBar
import android.widget.SeekBar
import com.samsung.shademod.PrefsConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.util.WeakHashMap

object BrightnessSliderHook {

    private val activeAnimators = WeakHashMap<ProgressBar, ValueAnimator>()

    fun install(classLoader: ClassLoader, prefs: XSharedPreferences) {
        hookSystemUiSliderViews(classLoader, prefs)
        hookProgressBarSetProgress(prefs)
    }

    private fun hookSystemUiSliderViews(classLoader: ClassLoader, prefs: XSharedPreferences) {
        val candidateClasses = listOf(
            "com.android.systemui.settings.brightness.BrightnessSliderView",
            "com.android.systemui.settings.brightness.ToggleSeekBar",
            "com.android.systemui.settings.brightness.SecBrightnessSliderView",
            "com.android.systemui.qs.bar.BrightnessBar"
        )

        for (clsName in candidateClasses) {
            val cls = XposedHelpers.findClassIfExists(clsName, classLoader) ?: continue
            try {
                XposedBridge.hookAllMethods(cls, "onFinishInflate", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as? View ?: return
                        attachTouchSpring(view, prefs)
                    }
                })
            } catch (t: Throwable) {
                XposedBridge.log("ShadeMod: skip $clsName onFinishInflate: ${t.message}")
            }
        }
    }

    private fun hookProgressBarSetProgress(prefs: XSharedPreferences) {
        try {
            XposedHelpers.findAndHookMethod(
                ProgressBar::class.java,
                "setProgress",
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val pb = param.thisObject as? SeekBar ?: return
                        val clsName = pb.javaClass.name
                        if (!clsName.contains("ToggleSeekBar", ignoreCase = true) &&
                            !clsName.contains("Brightness", ignoreCase = true) &&
                            !clsName.contains("Volume", ignoreCase = true)
                        ) return

                        prefs.reload()
                        if (!prefs.getBoolean(PrefsConfig.KEY_SLIDER_ANIM_ENABLED, true)) return

                        val target = param.args[0] as Int
                        val current = pb.progress
                        if (kotlin.math.abs(target - current) < 2) return

                        param.result = null
                        animateSeekBarProgress(pb, current, target, prefs)
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("ShadeMod: hookProgressBarSetProgress error: ${t.message}")
        }
    }

    private fun animateSeekBarProgress(
        seekBar: SeekBar,
        from: Int,
        to: Int,
        prefs: XSharedPreferences
    ) {
        activeAnimators[seekBar]?.cancel()
        val duration = prefs.getInt(PrefsConfig.KEY_ANIM_DURATION_MS, 260).toLong().coerceIn(80L, 600L)
        val style = prefs.getString(PrefsConfig.KEY_ANIM_STYLE, "ios")

        val animator = ValueAnimator.ofInt(from, to).apply {
            this.duration = duration
            interpolator = when (style) {
                "hyperos" -> OvershootInterpolator(1.25f)
                "pixel" -> DecelerateInterpolator(1.8f)
                else -> PathInterpolator(0.22f, 1.0f, 0.36f, 1.0f) // iOS-like fluid curve
            }
            addUpdateListener { anim ->
                val value = anim.animatedValue as Int
                XposedHelpers.callMethod(seekBar, "setProgressInternal", value, false, false)
            }
        }
        activeAnimators[seekBar] = animator
        animator.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachTouchSpring(view: View, prefs: XSharedPreferences) {
        view.setOnTouchListener { v, event ->
            prefs.reload()
            if (prefs.getBoolean(PrefsConfig.KEY_SLIDER_BOUNCE_ENABLED, true)) {
                val style = prefs.getString(PrefsConfig.KEY_ANIM_STYLE, "ios")
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val scaleY = if (style == "hyperos") 1.14f else 1.08f
                        v.animate()
                            .scaleX(1.02f)
                            .scaleY(scaleY)
                            .setDuration(150L)
                            .setInterpolator(PathInterpolator(0.22f, 1f, 0.36f, 1f))
                            .start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(240L)
                            .setInterpolator(OvershootInterpolator(1.6f))
                            .start()
                    }
                }
            }
            false
        }
    }
}
