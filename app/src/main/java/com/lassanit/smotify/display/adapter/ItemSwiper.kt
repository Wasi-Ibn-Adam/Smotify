package com.lassanit.smotify.display.adapter

import android.content.Context
import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class ItemSwiper {

    interface CallBacks {
        fun onSwipeStart(holder: RecyclerView.ViewHolder)
        fun onSwipeComplete(holder: RecyclerView.ViewHolder)
    }

    companion object {
        fun dpToPix(context: Context, valueInDp: Float): Int {
            return (valueInDp * context.resources.displayMetrics.density).toInt()
        }

        fun onlySwipe(
            direction: Int = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            callBacks: CallBacks? = null
        ): ItemTouchHelper {
            val swiper = Swiper(direction)
            callBacks?.let { cb ->
                swiper.onSwipeCompleted = { cb.onSwipeComplete(it) }
                swiper.onSwipeStarted = { cb.onSwipeStart(it) }
            }

            return ItemTouchHelper(swiper)
        }

        fun actionSwipe(
            actionViewSizeInPx: Int,
            direction: Int = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            callBacks: CallBacks? = null
        ): ItemTouchHelper {
            val swiper = SwiperAction(actionViewSizeInPx, direction)
            callBacks?.let { cb ->
                swiper.onSwipeCompleted = { cb.onSwipeComplete(it) }
                swiper.onSwipeStarted = { cb.onSwipeStart(it) }
            }
            return ItemTouchHelper(swiper)
        }
    }

    private open class Swiper(private val directions: Int) :
        ItemTouchHelper.Callback() {

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeFlag(ItemTouchHelper.ACTION_STATE_SWIPE, directions)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            runCatching { onSwipeCompleted?.let { it(viewHolder) } }.onFailure { it.printStackTrace() }
        }

        private var swipeStarted = false
        var onSwipeStarted: ((RecyclerView.ViewHolder) -> Unit)? = null
        var onSwipeCompleted: ((RecyclerView.ViewHolder) -> Unit)? = null

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && swipeStarted.not()) {
                swipeStarted = true
                onSwipeStarted?.let { it(viewHolder) }
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    private class SwiperAction(childWidthInPx: Int, private val directions: Int) :
        Swiper(directions) {
        private val limitScrollX = childWidthInPx
        private var currentScrollX = 0
        private var currentScrollXWhenInActive = 0
        private var initXWhenInActive = 0f
        private var firstInActive = false
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeMovementFlags(ItemTouchHelper.ACTION_STATE_IDLE, directions)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return true
        }

        override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
            return Integer.MAX_VALUE.toFloat()
        }

        override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
            return Integer.MAX_VALUE.toFloat()
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                if (dX == 0F) {
                    currentScrollX = viewHolder.itemView.scrollX
                    firstInActive = true
                }
                if (isCurrentlyActive) {
                    // finger swipe
                    var scrollOffset = currentScrollX + (-dX).toInt()
                    if (scrollOffset > limitScrollX) {
                        scrollOffset = limitScrollX
                    } else if (scrollOffset < 0) {
                        scrollOffset = 0
                    }
                    viewHolder.itemView.scrollTo(scrollOffset, 0)
                } else {
                    // auto swipe
                    if (firstInActive) {
                        firstInActive = false
                        currentScrollXWhenInActive = viewHolder.itemView.scrollX
                        initXWhenInActive = dX
                    }

                    if (viewHolder.itemView.scrollX < limitScrollX) {
                        viewHolder.itemView.scrollTo(
                            (currentScrollXWhenInActive * dX / initXWhenInActive).toInt(),
                            0
                        )
                    }

                }
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            if (viewHolder.itemView.scrollX > limitScrollX) {
                viewHolder.itemView.scrollTo(limitScrollX, 0)
            } else if (viewHolder.itemView.scrollX < 0) {
                viewHolder.itemView.scrollTo(0, 0)
            }
        }

    }

}