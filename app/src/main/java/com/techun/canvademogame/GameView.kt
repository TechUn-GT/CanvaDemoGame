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
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    /**
     * Main Character
     */
    //Parametros Ajustables
    private val gravity = dp(0.5f)
    private val jumpImpulse = dp(-10f)
    private val hitboxMainCharacter = 0.5f

    //Main Character
    private val mainCharacterBmpSrc = BitmapFactory.decodeResource(resources, R.drawable.main_character)
    private var mainCharacterBmp: Bitmap? = null

    //Main Character States
    private var mainCharacterX = 0f
    private var mainCharacterY = 0f
    private var mainCharacterW = 0f
    private var mainCharacterH = 0f
    private var mainCharacterVelY = 0f

    /**
     * Rewards
     */

    //Rewards Parameters
    private val rewardSpeed = dp(4f)
    private val rewardSpawnChance = 0.02f
    private val hitboxRewards = 0.9f

    //Rewards Sprites
    private val rewardsBmpSrc = BitmapFactory.decodeResource(resources, R.drawable.reward)
    private var rewardBmp: Bitmap? = null

    //Rewards States
    private var rewardWidth = 0f
    private var rewardHeight = 0f

    //Reward Pojo
    data class Reward(var x: Float, var y: Float, var alive: Boolean = true)

    private val rewards = mutableListOf<Reward>()

    /**
     * Debug
     */
    private val showHitbox = true
    private val mainCharacterRect = RectF()
    private val rewardRect = RectF()
    private val hitMainCharacter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = Color.RED
    }

    private val hitReward = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        color = Color.YELLOW
    }

    //Game Controls
    private var running = true

    //Screen Details
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        //Main Character
        mainCharacterW = w * 0.2f
        mainCharacterH = mainCharacterW
        mainCharacterBmp = mainCharacterBmpSrc.scale(mainCharacterW.toInt(), mainCharacterH.toInt())

        //Initial position of the main character
        mainCharacterX = w * 0.4f
        mainCharacterY = h * 0.5f

        //Initial vertical velocity
        mainCharacterVelY = 0f

        //Reward
        rewardWidth = w * 0.2f
        rewardHeight = rewardWidth
        rewardBmp = rewardsBmpSrc.scale(rewardWidth.toInt(), rewardHeight.toInt())

        rewards.clear()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && running) {
            mainCharacterVelY = jumpImpulse
            return true
        }
        return super.onTouchEvent(event)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        //bg
        canvas.drawColor(Color.BLUE)

        if (running) {
            //Main Character
            mainCharacterVelY += gravity
            mainCharacterY = (mainCharacterY + mainCharacterVelY).coerceIn(0f, height - mainCharacterH)

            //Rewards
            if (Random.nextFloat() < rewardSpawnChance) {
                val y = Random.nextFloat() * (height * 0.7f) + (height * 0.1f)
                rewards += Reward(x = width + rewardWidth, y = y)
            }

            val it = rewards.iterator()
            while (it.hasNext()) {
                val r = it.next()
                r.x -= rewardSpeed

                //Sale de pantalla (Izquierda)
                if (r.x + rewardWidth < 0f) {
                    it.remove()
                    continue
                }

                //Colision Main Character < -> Reward
                if (checkMainCharacterRewardCollision(r)) {
                    //counter ++
                    it.remove()
                }
            }
        }

        //Draw Main Character
        mainCharacterBmp?.let {
            canvas.drawBitmap(it, mainCharacterX, mainCharacterY, null)
        }

        rewardBmp?.let { bmp ->
            rewards.forEach { reward ->
                canvas.drawBitmap(bmp, reward.x, reward.y, null)
            }
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
            canvas.drawRect(mainCharacterRect, hitMainCharacter)

            rewards.forEach { reward ->
                setCenteredRect(
                    rewardRect,
                    reward.x,
                    reward.y,
                    rewardWidth,
                    rewardHeight,
                    hitboxRewards
                )
                canvas.drawRect(rewardRect, hitReward)
            }
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

    private fun checkMainCharacterRewardCollision(reward: Reward): Boolean {
        setCenteredRect(
            mainCharacterRect,
            mainCharacterX,
            mainCharacterY,
            mainCharacterW,
            mainCharacterH,
            hitboxMainCharacter
        )
        setCenteredRect(
            rewardRect,
            reward.x,
            reward.y,
            rewardWidth,
            rewardHeight,
            hitboxRewards
        )
        return RectF.intersects(mainCharacterRect,rewardRect)
    }
}