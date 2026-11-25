package cat.rellotgeparlant.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import cat.rellotgeparlant.R
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** A big round, high-contrast analog clock showing the current time. Used as her button. */
class ClockView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val face = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.big_button)
    }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.big_button_text)
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.big_button_text)
    }

    private val tick = object : Runnable {
        override fun run() {
            invalidate()
            // Redraw at the start of the next minute.
            postDelayed(this, (60 - LocalTime.now().second) * 1000L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(tick)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    /** Always square: as big as the space allows. */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy)

        canvas.drawCircle(cx, cy, r, face)

        // Ring and hour marks (longer at 12, 3, 6 and 9).
        ink.strokeWidth = r * 0.04f
        canvas.drawCircle(cx, cy, r * 0.9f, ink)
        for (h in 0 until 12) {
            val major = h % 3 == 0
            ink.strokeWidth = r * (if (major) 0.07f else 0.04f)
            line(canvas, cx, cy, h * 30f, r * (if (major) 0.62f else 0.70f), r * 0.80f)
        }

        val now = LocalTime.now()
        ink.strokeWidth = r * 0.10f
        line(canvas, cx, cy, ClockHands.hourAngle(now.hour, now.minute), 0f, r * 0.45f)
        ink.strokeWidth = r * 0.06f
        line(canvas, cx, cy, ClockHands.minuteAngle(now.minute), 0f, r * 0.68f)
        canvas.drawCircle(cx, cy, r * 0.08f, dot)
    }

    private fun line(canvas: Canvas, cx: Float, cy: Float, degrees: Float, from: Float, to: Float) {
        val a = Math.toRadians(degrees.toDouble())
        val dx = sin(a).toFloat()
        val dy = -cos(a).toFloat()
        canvas.drawLine(cx + dx * from, cy + dy * from, cx + dx * to, cy + dy * to, ink)
    }
}
