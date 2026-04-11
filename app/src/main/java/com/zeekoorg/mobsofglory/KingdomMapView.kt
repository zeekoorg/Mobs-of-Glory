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
    
    // 💡 تحديد التكبير والتصغير محدود (القيود): رفع الحد الأدنى للتصغير (minScale)
    // لجعل القلاع واضحة دائماً كنقاط هامة وليست نقاط صغيرة جداً.
    private val minScale = 0.8f // تم رفعه من 0.3f لتجنب النقاط الصغيرة
    private val maxScale = 2.0f // تم رفعه قليلاً من 1.5f لتكبير أكثر وضوحاً

    private var focusX = 0f
    private var focusY = 0f

    // مستطيل كشف الكاميرا (View Frustum Culling)
    private val viewRect = RectF()
    private val castleRect = RectF()

    var onCastleClickListener: ((Castle) -> Unit)? = null

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            // تطبيق القيود الجديدة للتكبير والتصغير
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

    fun setMapBackground(bitmap: Bitmap) {
        this.mapBitmap = bitmap
        invalidate()
    }

    fun initializeFixedWorld(playerLevel: Int, playerName: String) {
        castles.clear()
        val worldWidth = 5000f
        val worldHeight = 5000f

        val playerResId = context.resources.getIdentifier("ic_map_castle_player", "drawable", context.packageName).takeIf { it != 0 } ?: R.drawable.ic_shop_scroll
        val playerCastle = Castle(0, playerName, CastleType.PLAYER, 2500f, 2500f, playerLevel, playerLevel * 15000, playerResId)
        castles.add(playerCastle)

        val bossResId = context.resources.getIdentifier("ic_map_castle_boss", "drawable", context.packageName).takeIf { it != 0 } ?: R.drawable.ic_shop_scroll
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
            
            // سحب الصورة، وإذا لم يجدها سيرجع المربع الرمادي (Missing)
            val resId = context.resources.getIdentifier("ic_map_castle_1", "drawable", context.packageName).takeIf { it != 0 } ?: R.drawable.ic_shop_scroll
            castles.add(Castle(i, "$name ($i)", CastleType.ENEMY, randomX, randomY, randomLevel, randomPower, resId))
        }
        invalidate()
    }

    fun getCurrentVisibleCenter(): PointF {
        matrix.getValues(floatValues)
        val transX = floatValues[Matrix.MTRANS_X]
        val transY = floatValues[Matrix.MTRANS_Y]
        val currentScale = floatValues[Matrix.MSCALE_X]
        return PointF((-transX + (width / 2f)) / currentScale, (-transY + (height / 2f)) / currentScale)
    }

    fun getCurrentScale(): Float = floatValues.apply { matrix.getValues(this) }[Matrix.MSCALE_X]

    fun centerOnPoint(x: Float, y: Float) {
        val targetScale = 1.0f 
        scaleFactor = targetScale
        matrix.reset()
        matrix.postScale(targetScale, targetScale)
        matrix.postTranslate((width / 2f) - (x * targetScale), (height / 2f) - (y * targetScale))
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
            if (tapX >= castle.x && tapX <= castle.x + bmp.width && tapY >= castle.y && tapY <= castle.y + bmp.height) {
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

        val newTransX = if (mapWidth < width) (width - mapWidth) / 2f else transX.coerceIn(width - mapWidth, 0f)
        val newTransY = if (mapHeight < height) (height - mapHeight) / 2f else transY.coerceIn(height - mapHeight, 0f)

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

    private fun getCastleBitmap(resId: Int): Bitmap {
        return bitmapCache.getOrPut(resId) {
            try {
                // ضغط الصورة العملاقة (OOM) عبر ضغطها أثناء التحميل
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeResource(context.resources, resId, options)
                options.inSampleSize = calculateInSampleSize(options, 200, 200)
                options.inJustDecodeBounds = false
                
                val original = BitmapFactory.decodeResource(context.resources, resId, options)
                if (original != null) Bitmap.createScaledBitmap(original, 200, 200, true) else fallbackBitmap
            } catch (e: Exception) {
                fallbackBitmap
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) inSampleSize *= 2
        }
        return inSampleSize
    }

    // المربع الرمادي الاحتياطي (Missing Castle) في حالة عدم وجود صورة
    private val fallbackBitmap: Bitmap by lazy {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply { color = Color.parseColor("#555555") }
        canvas.drawRect(0f, 0f, 200f, 200f, paint)
        paint.color = Color.WHITE
        paint.textSize = 35f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("مفقود", 100f, 110f, paint)
        bmp
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.concat(matrix)

        mapBitmap?.let { canvas.drawBitmap(it, null, RectF(0f, 0f, 5000f, 5000f), null) } ?: canvas.drawColor(Color.parseColor("#4A3B2C"))

        // نظام كشف الكاميرا (View Frustum Culling): حساب ما تراه الكاميرا حالياً
        matrix.invert(inverseMatrix)
        viewRect.set(0f, 0f, width.toFloat(), height.toFloat())
        inverseMatrix.mapRect(viewRect) // الآن viewRect يحتوي على الإحداثيات الموجودة داخل الشاشة فقط

        for (castle in castles) {
            val bmp = getCastleBitmap(castle.resId)
            
            // تحديد مساحة القلعة
            castleRect.set(castle.x, castle.y, castle.x + bmp.width, castle.y + bmp.height)
            
            // 💡 نظام كشف الكاميرا (View Frustum Culling): تجاوز رسم القلعة إذا كانت خارج الشاشة
            if (!RectF.intersects(viewRect, castleRect)) {
                continue 
            }

            // رسم القلعة (لأنها داخل الشاشة)
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
