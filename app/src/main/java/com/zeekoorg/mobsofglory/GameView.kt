package com.zeekoorg.mobsofglory

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    private var isRunning = false
    private val paint = Paint()
    
    // إحداثيات المنجنيق (المدفع)
    private var catapultX = 0f
    private var catapultY = 0f
    private var catapultWidth = 200f

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // تحديد موقع المنجنيق الافتراضي في أسفل الشاشة
        catapultX = width / 2f - catapultWidth / 2
        catapultY = height - 300f
        
        isRunning = true
        thread = Thread(this)
        thread?.start()
    }

    override fun run() {
        while (isRunning) {
            if (!holder.surface.isValid) continue
            
            val canvas = holder.lockCanvas()
            update() // تحديث منطق اللعبة
            drawGame(canvas) // رسم العناصر
            holder.unlockCanvasAndPost(canvas)
            
            Thread.sleep(16) // للحفاظ على 60 إطار في الثانية
        }
    }

    private fun update() {
        // هنا سنضيف لاحقاً منطق إطلاق الفرسان وحركتهم
    }

    private fun drawGame(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#2C1E11")) // لون أرضية المعركة مؤقتاً

        // رسم المنجنيق (حالياً مربع ذهبي، وسنستبدله بصورتك لاحقاً)
        paint.color = Color.parseColor("#FFD700")
        canvas.drawRect(catapultX, catapultY, catapultX + catapultWidth, catapultY + 100f, paint)
    }

    // التحكم باللمس لتحريك المنجنيق يميناً ويساراً
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                catapultX = event.x - catapultWidth / 2
                
                // منع المنجنيق من الخروج خارج حواف الشاشة
                if (catapultX < 0) catapultX = 0f
                if (catapultX > width - catapultWidth) catapultX = width - catapultWidth
            }
        }
        return true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        thread?.join()
    }
}
