package com.lassanit.smotify.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager2.widget.ViewPager2
import com.lassanit.smotify.R


class MaskedLayout(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    class RoundedDrawable(private val cornerRadius: Float) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun draw(canvas: Canvas) {
            paint.strokeWidth = 3f
            canvas.drawRect(130f, 130f, 180f, 180f, paint);
            paint.setStrokeWidth(0f);
            paint.setColor(Color.CYAN);
            canvas.drawRect(133f, 160f, 177f, 177f, paint);
            paint.setColor(Color.YELLOW);
            canvas.drawRect(133f, 133f, 177f, 160f, paint);
        }

        override fun setAlpha(alpha: Int) {
            // No action required
        }

        override fun getAlpha(): Int = 255

        override fun setColorFilter(p0: ColorFilter?) {}

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.OPAQUE

        override fun isStateful(): Boolean = false
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val path = Path()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)


        // Draw the transparent area behind the ViewPager
        val viewPager = findViewById<ViewPager2>(R.id.app_viewPager)
        path.addRoundRect(
            viewPager.left.toFloat(),
            viewPager.top.toFloat(),
            viewPager.right.toFloat(),
            viewPager.bottom.toFloat(), 20f, 20f, Path.Direction.CW
        )
        paint.color = Color.TRANSPARENT
        canvas.drawPath(path, paint)
       //paint.color = Color.RED
       //canvas.drawRect(
       //    10f,
       //    viewPager.height.plus(viewPager.top).toFloat(),
       //    10f,
       //    20f,
       //    paint
       //)

        // Draw the remaining area with the activity's background color (if applicable)
       // if (background != null) {
       //     //   canvas.drawBitmap(Resource.drawableToBitmap(background.current), 0f, 0f, null)
       //     paint.color = Color.RED
       //     canvas.drawRect(0f, viewPagerHeight.toFloat(), width.toFloat(), height.toFloat(), paint)
       // }
    }


}