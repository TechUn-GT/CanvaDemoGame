package com.techun.canvademogame

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.scale

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    //Parametros Ajustables
    private val gravity = dp(0.5f)
    private val jumpImpulse = dp(-10f)
    private val hitboxMainCharacter = 0.5f

    //Main Character
    private val mainCharacterBmpSrc =
        BitmapFactory.decodeResource(resources, R.drawable.main_character)
    private var mainCharacterBmp: Bitmap? = null

    //Main Character States
    private var mainCharacterX = 0f
    private var mainCharacterY = 0f
    private var mainCharacterW = 0f
    private var mainCharacterH = 0f
    private var mainCharacterVelY = 0f

    //Debug
    private val showHitbox = true
    private val mainCharacterRect = RectF()
    private val hitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = Color.RED
    }

    //Game Controls
    private var running = true

    //Screen Details
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mainCharacterW = w * 0.2f
        mainCharacterH = mainCharacterW
        mainCharacterBmp = mainCharacterBmpSrc.scale(mainCharacterW.toInt(), mainCharacterH.toInt())

        //Initial position of the main character
        mainCharacterX = w * 0.4f
        mainCharacterY = h * 0.5f

        //Initial vertical velocity
        mainCharacterVelY = 0f
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && running) {
            mainCharacterVelY = jumpImpulse
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        //bg
        canvas.drawColor(Color.BLUE)

        if (running) {
            mainCharacterVelY += gravity
            mainCharacterY =
                (mainCharacterY + mainCharacterVelY).coerceIn(0f, height - mainCharacterH)
        }

        //Draw Main Character
        mainCharacterBmp?.let {
            canvas.drawBitmap(it, mainCharacterX, mainCharacterY, null)
        }

        //Hitbox
        if (showHitbox) {
            setCenteredRect(
                mainCharacterRect,
                mainCharacterX,
                mainCharacterY,
                mainCharacterW,
                mainCharacterH,
                hitboxMainCharacter
            )
            canvas.drawRect(mainCharacterRect, hitPaint)
        }

        if (running) postInvalidateOnAnimation()
    }

    //Utils
    private fun dp(v: Float) = v * resources.displayMetrics.density

    private fun setCenteredRect(out: RectF, x: Float, y: Float, w: Float, h: Float, scale: Float) {
        val cw = w * scale
        val ch = h * scale
        out.left = x + (w - cw) / 2f
        out.top = y + (h - ch) / 2f
        out.right = out.left + cw
        out.bottom = out.top + ch
    }
}