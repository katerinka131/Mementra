package com.example.mementra

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.mementra.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val motivationCards = listOf(
        MotivationCard(
            R.drawable.ic_memory,
            "Сохраняй моменты",
            "Записывай важные события и мысли"
        ),
        MotivationCard(
            R.drawable.ic_photo,
            "Добавляй фото",
            "Визуализируй свои воспоминания"
        ),
        MotivationCard(
            R.drawable.ic_time,
            "Возвращайся назад",
            "Переживай моменты заново"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFonts()
        setupMotivationViewPager()
        setupDotsIndicator()
        setupButton()
    }

    private fun setupFonts() {
        // Устанавливаем шрифт Comfortaa для всех элементов
        val comfortaaFont = ResourcesCompat.getFont(this, R.font.comforta)

        binding.titleText.typeface = comfortaaFont
        binding.startButton.typeface = comfortaaFont

        // Делаем заголовок жирным
        binding.titleText.setTypeface(comfortaaFont, android.graphics.Typeface.BOLD)
    }

    private fun setupMotivationViewPager() {
        val adapter = OnboardingAdapter(motivationCards)
        binding.motivationViewPager.adapter = adapter
    }

    private fun setupDotsIndicator() {
        val dots = arrayOfNulls<ImageView>(motivationCards.size)

        for (i in motivationCards.indices) {
            dots[i] = ImageView(this)

            val params = android.widget.LinearLayout.LayoutParams(
                convertDpToPx(16),
                convertDpToPx(16)
            )
            params.setMargins(convertDpToPx(8), 0, convertDpToPx(8), 0)

            if (i == 0) {
                dots[i]?.setBackgroundResource(R.drawable.dot_active)
            } else {
                dots[i]?.setBackgroundResource(R.drawable.dot_inactive)
            }

            binding.dotsContainer.addView(dots[i], params)
        }

        binding.motivationViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                for (i in motivationCards.indices) {
                    if (i == position) {
                        dots[i]?.setBackgroundResource(R.drawable.dot_active)
                    } else {
                        dots[i]?.setBackgroundResource(R.drawable.dot_inactive)
                    }
                }
            }
        })
    }

    private fun convertDpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupButton() {
        binding.startButton.setOnClickListener {
            // Сохраняем флаг, что onboarding был показан
            val prefs = getSharedPreferences("mementra_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean("onboarding_completed", true).apply()
            
            // Переходим на главный экран
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}

data class MotivationCard(
    val iconRes: Int,
    val title: String,
    val description: String
)