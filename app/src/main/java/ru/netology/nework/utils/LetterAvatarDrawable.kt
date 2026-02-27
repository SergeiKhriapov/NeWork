package ru.netology.nework.utils

import android.graphics.*
import android.graphics.drawable.Drawable

class LetterAvatarDrawable(
    private val letter: String,
    private val backgroundColor: Int,
    private val textColor: Int = Color.WHITE
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun draw(canvas: Canvas) {
        val rect = bounds
        canvas.drawCircle(rect.exactCenterX(), rect.exactCenterY(), rect.width() / 2f, paint)

        // Размер текста = половина высоты
        textPaint.textSize = rect.height() * 0.6f
        val y = rect.exactCenterY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(letter, rect.exactCenterX(), y, textPaint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        textPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}