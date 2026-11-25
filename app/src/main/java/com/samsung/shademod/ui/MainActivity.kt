package com.samsung.shademod.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.samsung.shademod.PrefsConfig
import com.samsung.shademod.R
import com.samsung.shademod.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    @SuppressLint("WorldReadableFiles")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = try {
            @Suppress("DEPRECATION")
            getSharedPreferences(PrefsConfig.PREFS_NAME, Context.MODE_WORLD_READABLE)
        } catch (_: SecurityException) {
            getSharedPreferences(PrefsConfig.PREFS_NAME, Context.MODE_PRIVATE)
        }

        loadState()
        setupListeners()
        setupPreviewSlider()
    }

    private fun loadState() {
        binding.switchSliderAnim.isChecked = prefs.getBoolean(PrefsConfig.KEY_SLIDER_ANIM_ENABLED, true)
        binding.switchSliderBounce.isChecked = prefs.getBoolean(PrefsConfig.KEY_SLIDER_BOUNCE_ENABLED, true)
        when (prefs.getString(PrefsConfig.KEY_ANIM_STYLE, "ios")) {
            "hyperos" -> binding.rbHyperOs.isChecked = true
            "pixel" -> binding.rbPixelSmooth.isChecked = true
            else -> binding.rbIosSpring.isChecked = true
        }
        val duration = prefs.getInt(PrefsConfig.KEY_ANIM_DURATION_MS, 260)
        binding.seekAnimDuration.progress = duration
        binding.tvSpeedLabel.text = "Плавность анимации: ${duration} мс"

        val cols = prefs.getInt(PrefsConfig.KEY_QS_COLUMNS, 0)
        binding.seekColumns.progress = cols
        binding.tvColumnsLabel.text = "Плиток в ряду (0 = стандарт): $cols"

        val alpha = prefs.getInt(PrefsConfig.KEY_SHADE_ALPHA, 100)
        binding.seekShadeAlpha.progress = alpha
        binding.tvAlphaLabel.text = "Прозрачность фона шторки: $alpha%"

        binding.switchHideMediaDevice.isChecked = prefs.getBoolean(PrefsConfig.KEY_HIDE_MEDIA_DEVICE, false)
    }

    private fun setupListeners() {
        binding.switchSliderAnim.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(PrefsConfig.KEY_SLIDER_ANIM_ENABLED, checked).apply()
        }
        binding.switchSliderBounce.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(PrefsConfig.KEY_SLIDER_BOUNCE_ENABLED, checked).apply()
        }
        binding.rgAnimStyle.setOnCheckedChangeListener { _, checkedId ->
            val style = when (checkedId) {
                R.id.rbHyperOs -> "hyperos"
                R.id.rbPixelSmooth -> "pixel"
                else -> "ios"
            }
            prefs.edit().putString(PrefsConfig.KEY_ANIM_STYLE, style).apply()
        }
        binding.seekAnimDuration.setOnSeekBarChangeListener(simpleSeekListener { p ->
            val clamped = p.coerceAtLeast(80)
            binding.tvSpeedLabel.text = "Плавность анимации: $clamped мс"
            prefs.edit().putInt(PrefsConfig.KEY_ANIM_DURATION_MS, clamped).apply()
        })
        binding.seekColumns.setOnSeekBarChangeListener(simpleSeekListener { p ->
            binding.tvColumnsLabel.text = "Плиток в ряду (0 = стандарт): $p"
            prefs.edit().putInt(PrefsConfig.KEY_QS_COLUMNS, p).apply()
        })
        binding.seekShadeAlpha.setOnSeekBarChangeListener(simpleSeekListener { p ->
            val clamped = p.coerceAtLeast(25)
            binding.tvAlphaLabel.text = "Прозрачность фона шторки: $clamped%"
            prefs.edit().putInt(PrefsConfig.KEY_SHADE_ALPHA, clamped).apply()
        })
        binding.switchHideMediaDevice.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(PrefsConfig.KEY_HIDE_MEDIA_DEVICE, checked).apply()
        }

        binding.btnRestartSystemUi.setOnClickListener {
            restartSystemUiWithRoot()
        }
    }

    private fun setupPreviewSlider() {
        binding.seekPreview.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) = Unit
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                if (!binding.switchSliderBounce.isChecked) return
                val springX = SpringAnimation(seekBar, DynamicAnimation.SCALE_X, 1.03f)
                val springY = SpringAnimation(seekBar, DynamicAnimation.SCALE_Y, 1.18f)
                val stiffness = if (binding.rbHyperOs.isChecked) SpringForce.STIFFNESS_MEDIUM else SpringForce.STIFFNESS_LOW
                val damping = if (binding.rbHyperOs.isChecked) SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY else SpringForce.DAMPING_RATIO_NO_BOUNCY
                springX.spring.stiffness = stiffness
                springX.spring.dampingRatio = damping
                springY.spring.stiffness = stiffness
                springY.spring.dampingRatio = damping
                springX.start()
                springY.start()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val springX = SpringAnimation(seekBar, DynamicAnimation.SCALE_X, 1.0f)
                val springY = SpringAnimation(seekBar, DynamicAnimation.SCALE_Y, 1.0f)
                springX.spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                springY.spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                springX.start()
                springY.start()
            }
        })
    }

    private fun restartSystemUiWithRoot() {
        Thread {
            try {
                val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "pkill -f com.android.systemui"))
                val code = proc.waitFor()
                runOnUiThread {
                    val msg = if (code == 0) "SystemUI перезапущен" else "Не удалось получить root (KernelSU)"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка root: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun simpleSeekListener(onChanged: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) onChanged(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }
}
