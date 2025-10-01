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
    private val mainCharacterBmpSrc =
        BitmapFactory.decodeResource(resources, R.drawable.main_character)
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
     * Obstacles
     */

    private val obstacleSpeed = dp(5f)
    private val hitboxObstacle = 0.9f

    //Obstacles Sprites
    private val obstacleBmpSrc = BitmapFactory.decodeResource(resources, R.drawable.obstacle)

    //Obstacle specs
    private val obstacleAspectMin = 0.6f
    private val obstacleAspectMax = 1.4f

    //Obstacle Pojo
    data class Obstacle(
        var x: Float,
        var y: Float,
        var w: Float,
        var h: Float,
        var alive: Boolean = true,
        val bmp: Bitmap
    )

    private var obstacle: Obstacle? = null

    //Obstacle Inits
    private var obstacleHeightMin = 0f
    private var obstacleHeightMax = 0f
    private var obstacleWidthMinPx = 0f
    private var obstacleWidthMaxPx = 0f
    private var lastObstacleSpawn = 0L
    private var nextObstacleIntervalMs = 1400L

    /**
     * Debug
     */
    private val showHitbox = true
    private val mainCharacterRect = RectF()
    private val rewardRect = RectF()
    private val obstacleRect = RectF()
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

    private val hitObstacle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = Color.BLACK
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

        //Obstacle
        obstacleHeightMin = h * 0.08f
        obstacleHeightMax = h / 3f

        obstacleWidthMinPx = w * 0.1f
        obstacleWidthMaxPx = w * 0.3f

        rewards.clear()
        obstacle = null
        lastObstacleSpawn = System.currentTimeMillis()
        nextObstacleIntervalMs = randomObstacleInterval()
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
            mainCharacterY =
                (mainCharacterY + mainCharacterVelY).coerceIn(0f, height - mainCharacterH)

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

            //Obstacle
            val now = System.currentTimeMillis()
            if ((obstacle == null || obstacle?.alive == false) && now - lastObstacleSpawn > nextObstacleIntervalMs) {
                lastObstacleSpawn = now
                nextObstacleIntervalMs = randomObstacleInterval()

                //Random Height
                val t = Random.nextFloat()
                val heightRand =
                    (obstacleHeightMin + t * (obstacleHeightMax - obstacleHeightMin)).coerceIn(
                        obstacleHeightMin,
                        obstacleHeightMax
                    )

                //Width
                val ratio = Random.nextFloat() * (obstacleAspectMax - obstacleAspectMin) + obstacleAspectMin
                var widthRand = heightRand * ratio
                widthRand = widthRand.coerceIn(obstacleWidthMinPx, obstacleWidthMaxPx)

                val bmp = obstacleBmpSrc.scale(widthRand.toInt(), heightRand.toInt())

                //Anclar al suelo
                obstacle = Obstacle(
                    x = width + widthRand, y = height - heightRand, w = widthRand, h = heightRand, bmp = bmp
                )
            }

            obstacle?.let { obs ->
                if (obs.alive){
                    obs.x -= obstacleSpeed
                    if (obs.x + obs.w < 0f) obs.alive = false
                }
            }

            if (obstacleCollides())
                running = false
        }

        //Draw Main Character
        mainCharacterBmp?.let {
            canvas.drawBitmap(it, mainCharacterX, mainCharacterY, null)
        }

        //Draw Rewards
        rewardBmp?.let { bmp ->
            rewards.forEach { reward ->
                canvas.drawBitmap(bmp, reward.x, reward.y, null)
            }
        }

        //Draw Obstacles
        obstacle?.takeIf { it.alive }?.let { obs ->
            canvas.drawBitmap(obs.bmp, obs.x,obs.y, null)
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
                    rewardRect, reward.x, reward.y, rewardWidth, rewardHeight, hitboxRewards
                )
                canvas.drawRect(rewardRect, hitReward)
            }

            obstacle?.takeIf { it.alive }?.let { obs ->
                setCenteredRect(obstacleRect, obs.x,obs.y,obs.w,obs.h, hitboxObstacle)
                canvas.drawRect(obstacleRect,hitObstacle)
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
            rewardRect, reward.x, reward.y, rewardWidth, rewardHeight, hitboxRewards
        )
        return RectF.intersects(mainCharacterRect, rewardRect)
    }

    private fun randomObstacleInterval():Long = Random.nextLong(1100L,1500L)

    private fun obstacleCollides(): Boolean{
        val obs = obstacle?:return false
        if(!obs.alive) return false
        setCenteredRect(mainCharacterRect, mainCharacterX,mainCharacterY, mainCharacterW, mainCharacterH,hitboxMainCharacter)
        setCenteredRect(obstacleRect, obs.x,obs.y, obs.w, obs.h,hitboxObstacle)
        return RectF.intersects(mainCharacterRect,obstacleRect)
    }
}