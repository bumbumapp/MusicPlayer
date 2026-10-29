package org.bumbumapps.musicplayer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.bumbumapps.musicplayer.R

/**
 * Bento "grouped card": draws one white rounded card behind all rows of a linear list,
 * inset from the screen edges, with the corners rounded only on the first and last row.
 */
class GroupedCardDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val density = context.resources.displayMetrics.density
    private val sideInset = (16 * density).toInt()
    private val edgeGap = (6 * density).toInt()
    private val radius = 20 * density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.aurora_card)
    }
    private val path = Path()
    private val rect = RectF()

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val pos = parent.getChildAdapterPosition(view)
        val last = (parent.adapter?.itemCount ?: 0) - 1
        outRect.left = sideInset
        outRect.right = sideInset
        if (pos == 0) outRect.top = edgeGap
        if (pos == last) outRect.bottom = edgeGap
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val last = (parent.adapter?.itemCount ?: 0) - 1
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val pos = parent.getChildAdapterPosition(child)
            if (pos == RecyclerView.NO_POSITION) continue
            val top = if (pos == 0) radius else 0f
            val bottom = if (pos == last) radius else 0f
            rect.set(
                child.left.toFloat(), child.top + child.translationY,
                child.right.toFloat(), child.bottom + child.translationY
            )
            path.reset()
            path.addRoundRect(
                rect,
                floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom),
                Path.Direction.CW
            )
            c.drawPath(path, paint)
        }
    }
}
