package com.zeekoorg.mobsofglory

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.random.Random

class KingdomMapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class CastleType { PLAYER, BOSS_MOBS_OF_GLORY, ENEMY }

    data class Castle(
        val id: Int, 
        val name: String, 
        val type: CastleType, 
        var x: Float, 
        var y: Float, 
        var level: Int, 
        var power: Int, 
        val resId: Int
    )

    private var mapBitmap: Bitmap? = null
    private val castles = mutableListOf<Castle>()
    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    private val textPaint = Paint().apply { color = Color.WHITE; textSize = 35f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; setShadowLayer(4f, 0f, 0f, Color.BLACK) }
    private val levelBgPaint = Paint().apply { color = Color.parseColor("#A6000000") }

    private val matrix = Matrix()
    private val inverseMatrix = Matrix()
    private val floatValues = FloatArray(9)
    
    private var scaleFactor = 1.0f
    private val minScale = 0.3f 
    private val maxScale = 2.5f

    private var focusX = 0f
    private var focusY = 0f

    var onCastleClickListener: ((Castle) -> Unit)? = null

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(minScale, maxScale)
            focusX = detector.focusX
            focusY = detector.focusY
            
            matrix.reset()
            matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
            constrainPan()
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            matrix.postTranslate(-distanceX, -distanceY)
            constrainPan()
            invalidate()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            handleTap(e.x, e.y)
            return true
        }
    })

    fun setMapBackground(bitmap: Bitmap?) {
        this.mapBitmap = bitmap
        invalidate()
    }

    fun initializeFixedWorld(playerLevel: Int, playerName: String) {
        castles.clear()
        val worldWidth = 5000f
        val worldHeight = 5000f

        val playerResId = context.resources.getIdentifier("ic_map_castle_player", "drawable", context.packageName)
        val playerCastle = Castle(0, playerName, CastleType.PLAYER, 2500f, 2500f, playerLevel, playerLevel * 15000, playerResId)
        castles.add(playerCastle)

        val bossResId = context.resources.getIdentifier("ic_map_castle_boss", "drawable", context.packageName)
        val bossCastle = Castle(1, "وحوش المجد", CastleType.BOSS_MOBS_OF_GLORY, 2750f, 2350f, playerLevel + 5, 999999, bossResId)
        castles.add(bossCastle)

        val fixedRandom = Random(12345) 
        val arabicNames = listOf("السفاح", "أسد", "صقر", "الجلاد", "فارس", "المدمر", "عاصفة", "زلزال", "كابوس", "رعد")
        val englishNames = listOf("Shadow", "DarkKnight", "Killer", "Titan", "Ghost", "Viper", "Doom", "Ninja", "Reaper", "Blade")
        
        for (i in 2..101) {
            val randomX = fixedRandom.nextFloat() * (worldWidth - 300f) + 150f
            val randomY = fixedRandom.nextFloat() * (worldHeight - 300f) + 150f
            if (kotlin.math.abs(randomX - 2500f) < 300f && kotlin.math.abs(randomY - 2500f) < 300f) continue

            val randomLevel = fixedRandom.nextInt(1, 100)
            val randomPower = randomLevel * fixedRandom.nextInt(1000, 5000)
            val name = if (fixedRandom.nextBoolean()) arabicNames.random(fixedRandom) else englishNames.random(fixedRandom)
            
            val resId = context.resources.getIdentifier("ic_map_castle_$i", "drawable", context.packageName)
            castles.add(Castle(i, "$name ($i)", CastleType.ENEMY, randomX, randomY, randomLevel, randomPower, resId))
        }
        invalidate()
    }

    fun getCurrentVisibleCenter(): PointF {
        matrix.getValues(floatValues)
        val transX = floatValues[Matrix.MTRANS_X]
        val transY = floatValues[Matrix.MTRANS_Y]
        val currentScale = floatValues[Matrix.MSCALE_X]
        val centerX = (-transX + (width / 2f)) / currentScale
        val centerY = (-transY + (height / 2f)) / currentScale
        return PointF(centerX, centerY)
    }

    fun getCurrentScale(): Float {
        matrix.getValues(floatValues)
        return floatValues[Matrix.MSCALE_X]
    }

    fun centerOnPoint(x: Float, y: Float) {
        val targetScale = 1.0f 
        scaleFactor = targetScale
        val transX = (width / 2f) - (x * targetScale)
        val transY = (height / 2f) - (y * targetScale)
        matrix.reset()
        matrix.postScale(targetScale, targetScale)
        matrix.postTranslate(transX, transY)
        constrainPan()
        invalidate()
    }

    private fun handleTap(x: Float, y: Float) {
        matrix.invert(inverseMatrix)
        val pts = floatArrayOf(x, y)
        inverseMatrix.mapPoints(pts)
        val tapX = pts[0]
        val tapY = pts[1]

        for (i in castles.indices.reversed()) {
            val castle = castles[i]
            val bmp = getCastleBitmap(castle.resId)
            val width = bmp.width.toFloat()
            val height = bmp.height.toFloat()
            
            if (tapX >= castle.x && tapX <= castle.x + width && tapY >= castle.y && tapY <= castle.y + height) {
                onCastleClickListener?.invoke(castle)
                break
            }
        }
    }

    private fun constrainPan() {
        if (mapBitmap == null) return
        matrix.getValues(floatValues)
        val transX = floatValues[Matrix.MTRANS_X]
        val transY = floatValues[Matrix.MTRANS_Y]
        val currentScale = floatValues[Matrix.MSCALE_X]

        val mapWidth = mapBitmap!!.width * currentScale
        val mapHeight = mapBitmap!!.height * currentScale

        var newTransX = transX
        var newTransY = transY

        if (mapWidth < width) newTransX = (width - mapWidth) / 2f
        else newTransX = transX.coerceIn(width - mapWidth, 0f)

        if (mapHeight < height) newTransY = (height - mapHeight) / 2f
        else newTransY = transY.coerceIn(height - mapHeight, 0f)

        matrix.getValues(floatValues)
        floatValues[Matrix.MTRANS_X] = newTransX
        floatValues[Matrix.MTRANS_Y] = newTransY
        matrix.setValues(floatValues)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    // 💡 الحماية القصوى: تحويل آمن للصور لتجنب الانهيار إذا كانت الصورة مفقودة
    private fun getCastleBitmap(resId: Int): Bitmap {
        if (resId == 0) return createFallbackBitmap()
        return bitmapCache.getOrPut(resId) {
            try {
                val drawable = ContextCompat.getDrawable(context, resId)
                if (drawable != null) {
                    val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bmp
                } else {
                    createFallbackBitmap()
                }
            } catch (e: Exception) {
                createFallbackBitmap()
            }
        }
    }

    private fun createFallbackBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply { color = Color.DKGRAY }
        canvas.drawRect(0f, 0f, 200f, 200f, paint)
        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("مفقود", 100f, 110f, paint)
        return bmp
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.concat(matrix)

        mapBitmap?.let {
            canvas.drawBitmap(it, null, RectF(0f, 0f, 5000f, 5000f), null)
        } ?: canvas.drawColor(Color.parseColor("#4A3B2C"))

        for (castle in castles) {
            val bmp = getCastleBitmap(castle.resId)
            canvas.drawBitmap(bmp, castle.x, castle.y, null)
            
            val textY = castle.y - 15f 
            val centerX = castle.x + (bmp.width / 2f)
            
            val textBounds = Rect()
            val fullText = "${castle.name} | Lvl ${castle.level}"
            textPaint.getTextBounds(fullText, 0, fullText.length, textBounds)
            val bgRect = RectF(centerX - textBounds.width()/2 - 15, textY - textBounds.height() - 10, centerX + textBounds.width()/2 + 15, textY + 15)
            canvas.drawRoundRect(bgRect, 10f, 10f, levelBgPaint)
            
            textPaint.color = when (castle.type) {
                CastleType.PLAYER -> Color.parseColor("#4CAF50")
                CastleType.BOSS_MOBS_OF_GLORY -> Color.parseColor("#FF5252")
                CastleType.ENEMY -> Color.WHITE
            }
            canvas.drawText(fullText, centerX, textY, textPaint)
        }
        canvas.restore()
    }
}
