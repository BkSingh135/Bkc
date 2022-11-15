package com.bkc.zoom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Build
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.OverScroller
import androidx.annotation.IntDef
import com.bkc.zoom.ZoomApi.Companion.TRANSFORMATION_CENTER_CROP
import com.bkc.zoom.ZoomApi.Companion.TRANSFORMATION_CENTER_INSIDE
import com.bkc.zoom.ZoomApi.Companion.TRANSFORMATION_NONE
import com.bkc.zoom.ZoomApi.Companion.TYPE_REAL_ZOOM
import com.bkc.zoom.ZoomApi.Companion.TYPE_ZOOM

/**
 * A low level class that listens to touch events and posts zoom and pan updates.
 * The most useful output is a [Matrix] that can be used to do pretty much everything,
 * from canvas drawing to View hierarchies translations.
 *
 *
 * Users are required to:
 * - Pass the container view in the constructor
 * - Notify the helper of the content size, using [.setContentSize]
 * - Pass touch events to [.onInterceptTouchEvent] and [.onTouchEvent]
 *
 *
 * This class will apply a base transformation to the content, see [.setTransformation],
 * so that it is laid out initially as we wish.
 *
 *
 * When the scaling makes the content smaller than our viewport, the engine will always try
 * to keep the content centered.
 */
class ZoomEngine(context: Context?, private val mView: View, private val mListener: Listener?,
                 override val ZoomApi: Int,

                 ) :
    OnGlobalLayoutListener, ZoomApi {
    /**
     * An interface to listen for updates in the inner matrix. This will be called
     * typically on animation frames.
     */
    interface Listener {
        /**
         * Notifies that the inner matrix was updated. The passed matrix can be changed,
         * but is not guaranteed to be stable. For a long lasting value it is recommended
         * to make a copy of it using [Matrix.set].
         *
         * @param engine the engine hosting the matrix
         * @param matrix a matrix with the given updates
         */
        fun onUpdate(engine: ZoomEngine?, matrix: Matrix?)

        /**
         * Notifies that the engine is in an idle state. This means that (most probably)
         * running animations have completed and there are no touch actions in place.
         *
         * @param engine this engine
         */
        fun onIdle(engine: ZoomEngine?)
    }

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(
        NONE, SCROLLING, PINCHING, ANIMATING, FLINGING
    )
    private annotation class State

    private var mMatrix = Matrix()
    private val mOutMatrix = Matrix()

    @State
    private var mMode = NONE
    private var mViewWidth = 0f
    private var mViewHeight = 0f
    private var mInitialized = false
    private var mContentRect = RectF()
    private var mContentBaseRect = RectF()
    private var mMinZoom = 0.8f
    private var mMinZoomMode: Int = TYPE_ZOOM
    private var mMaxZoom = 2.5f
    private var mMaxZoomMode: Int = TYPE_ZOOM

    @ZoomApi.Zoom
    private var mZoom = 1f // Not necessarily equal to the matrix scale.
    private var mBaseZoom // mZoom * mBaseZoom matches the matrix scale.
            = 0f
    private var mTransformation: Int = TRANSFORMATION_CENTER_INSIDE
    private var mTransformationGravity = Gravity.CENTER
    private var mOverScrollHorizontal = true
    private var mOverScrollVertical = true
    private var mHorizontalPanEnabled = true
    private var mVerticalPanEnabled = true
    private var mOverPinchable = true
    private var mZoomEnabled = true
    private var mClearAnimation = false
    private val mFlingScroller: OverScroller
    private val mTemp = IntArray(3)
    private val mScaleDetector: ScaleGestureDetector
    private val mFlingDragDetector: GestureDetector

    /**
     * Returns the current matrix. This can be changed from the outside, but is not
     * guaranteed to remain stable.
     *
     * @return the current matrix.
     */
    val matrix: Matrix
        get() {
            mOutMatrix.set(mMatrix)
            return mOutMatrix
        }

    // Returns true if we should go to that mode.
    @SuppressLint("SwitchIntDef")
    private fun setState(@State mode: Int): Boolean {
        LOG.v("trySetState:", ms(mode))
        if (!mInitialized) return false
        if (mode == mMode) return true
        val oldMode = mMode
        when (mode) {
            SCROLLING -> if (oldMode == PINCHING || oldMode == ANIMATING) return false
            FLINGING -> if (oldMode == ANIMATING) return false
            PINCHING -> if (oldMode == ANIMATING) return false
            NONE -> dispatchOnIdle()
        }
        when (oldMode) {
            FLINGING -> mFlingScroller.forceFinished(true)
            ANIMATING -> mClearAnimation = true
        }
        LOG.i("setState:", ms(mode))
        mMode = mode
        return true
    }
    //region Overscroll
    /**
     * Controls whether the content should be over-scrollable horizontally.
     * If it is, drag and fling horizontal events can scroll the content outside the safe area,
     * then return to safe values.
     *
     * @param overScroll whether to allow horizontal over scrolling
     */
    override fun setOverScrollHorizontal(overScroll: Boolean) {
        mOverScrollHorizontal = overScroll
    }

    /**
     * Controls whether the content should be over-scrollable vertically.
     * If it is, drag and fling vertical events can scroll the content outside the safe area,
     * then return to safe values.
     *
     * @param overScroll whether to allow vertical over scrolling
     */
    override fun setOverScrollVertical(overScroll: Boolean) {
        mOverScrollVertical = overScroll
    }

    /**
     * Controls whether horizontal panning using gestures is enabled.
     *
     * @param enabled true enables horizontal panning, false disables it
     */
    override fun setHorizontalPanEnabled(enabled: Boolean) {
        mHorizontalPanEnabled = enabled
    }

    /**
     * Controls whether vertical panning using gestures is enabled.
     *
     * @param enabled true enables vertical panning, false disables it
     */
    override fun setVerticalPanEnabled(enabled: Boolean) {
        mVerticalPanEnabled = enabled
    }

    /**
     * Controls whether the content should be overPinchable.
     * If it is, pinch events can change the zoom outside the safe bounds,
     * than return to safe values.
     *
     * @param overPinchable whether to allow over pinching
     */
    override fun setOverPinchable(overPinchable: Boolean) {
        mOverPinchable = overPinchable
    }

    @get:ZoomApi.ScaledPan
    private val currentOverScroll: Int
        private get() {
            val overX = mViewWidth / 20f * mZoom
            val overY = mViewHeight / 20f * mZoom
            return Math.min(overX, overY).toInt()
        }

    @get:ZoomApi.Zoom
    private val currentOverPinch: Float
        private get() = 0.1f * (resolveZoom(mMaxZoom, mMaxZoomMode) - resolveZoom(
            mMinZoom,
            mMinZoomMode
        ))

    /**
     * Controls whether zoom using pinch gesture is enabled or not.
     *
     * @param enabled true enables zooming, false disables it
     */
    override fun setZoomEnabled(enabled: Boolean) {
        mZoomEnabled = enabled
    }
    //endregion
    //region Initialize
    /**
     * Sets the base transformation to be applied to the content.
     * Defaults to [.TRANSFORMATION_CENTER_INSIDE] with [Gravity.CENTER],
     * which means that the content will be zoomed so that it fits completely inside the container.
     *
     * @param transformation the transformation type
     * @param gravity        the transformation gravity. Might be ignored for some transformations
     */
    override fun setTransformation(transformation: Int, gravity: Int) {
        mTransformation = transformation
        mTransformationGravity = gravity
    }

    override fun onGlobalLayout() {
        val width = mView.width
        val height = mView.height
        if (width <= 0 || height <= 0) return
        if (width.toFloat() != mViewWidth || height.toFloat() != mViewHeight) {
            init(width.toFloat(), height.toFloat(), mContentBaseRect)
        }
    }

    /**
     * Notifies the helper of the content size (be it a child View, a Bitmap, or whatever else).
     * This is needed for the helper to start working.
     *
     * @param rect the content rect
     */
    fun setContentSize(rect: RectF) {
        if (rect.width() <= 0 || rect.height() <= 0) return
        if (rect != mContentBaseRect) {
            init(mViewWidth, mViewHeight, rect)
        }
    }

    private fun init(viewWidth: Float, viewHeight: Float, rect: RectF) {
        mViewWidth = viewWidth
        mViewHeight = viewHeight
        mContentBaseRect.set(rect)
        mContentRect.set(rect)
        if (rect.width() <= 0 || rect.height() <= 0 || viewWidth <= 0 || viewHeight <= 0) return
        LOG.i(
            "init:", "viewWdith:", viewWidth, "viewHeight:", viewHeight,
            "rectWidth:", rect.width(), "rectHeight:", rect.height()
        )
        if (mInitialized) {
            // Content dimensions changed.
            setState(NONE)

            // Base zoom makes no sense anymore. We must recompute it.
            // We must also compute a new zoom value so that real zoom (that is, the matrix scale)
            // is kept the same as before. (So, no matrix updates here).
            LOG.i("init:", "wasAlready:", "Trying to keep real zoom to", getRealZoom())
            LOG.i(
                "init:", "wasAlready:", "oldBaseZoom:", mBaseZoom,
                "oldZoom:$mZoom"
            )
            @ZoomApi.RealZoom val realZoom = getRealZoom()
            mBaseZoom = computeBaseZoom()
            mZoom = realZoom / mBaseZoom
            LOG.i("init:", "wasAlready: newBaseZoom:", mBaseZoom, "newZoom:", mZoom)

            // Now sync the content rect with the current matrix since we are trying to keep it.
            // This is to have consistent values for other calls here.
            mMatrix.mapRect(mContentRect, mContentBaseRect)

            // If the new zoom value is invalid, though, we must bring it to the valid place.
            // This is a possible matrix update.
            @ZoomApi.Zoom val newZoom = ensureScaleBounds(mZoom, false)
            LOG.i(
                "init:", "wasAlready:", "scaleBounds:", "we need a zoom correction of",
                newZoom - mZoom
            )
            if (newZoom != mZoom) applyZoom(newZoom, false)

            // If there was any, pan should be kept. I think there's nothing to do here:
            // If the matrix is kept, and real zoom is kept, then also the real pan is kept.
            // I am not 100% sure of this though.
            ensureCurrentTranslationBounds(false)
            dispatchOnMatrix()
        } else {
            // First time. Apply base zoom, dispatch first event and return.
            mBaseZoom = computeBaseZoom()
            mMatrix.setScale(mBaseZoom, mBaseZoom)
            mMatrix.mapRect(mContentRect, mContentBaseRect)
            mZoom = 1f
            LOG.i("init:", "fromScratch:", "newBaseZoom:", mBaseZoom, "newZoom:", mZoom)
            @ZoomApi.Zoom val newZoom = ensureScaleBounds(mZoom, false)
            LOG.i(
                "init:", "fromScratch:", "scaleBounds:", "we need a zoom correction of",
                newZoom - mZoom
            )
            if (newZoom != mZoom) applyZoom(newZoom, false)

            // pan based on transformation gravity.
            @ZoomApi.ScaledPan val newPan = computeBasePan()
            @ZoomApi.ScaledPan val deltaX = newPan[0] - scaledPanX
            @ZoomApi.ScaledPan val deltaY = newPan[1] - scaledPanY
            if (deltaX != 0f || deltaY != 0f) applyScaledPan(deltaX, deltaY, false)
            ensureCurrentTranslationBounds(false)
            dispatchOnMatrix()
            mInitialized = true
        }
    }

    /**
     * Clears the current state, and stops dispatching matrix events
     * until the view is laid out again and [.setContentSize]
     * is called.
     */
    fun clear() {
        mViewHeight = 0f
        mViewWidth = 0f
        mZoom = 1f
        mBaseZoom = 0f
        mContentRect = RectF()
        mContentBaseRect = RectF()
        mMatrix = Matrix()
        mInitialized = false
    }

    private fun computeBaseZoom(): Float {
        return when (mTransformation) {
            TRANSFORMATION_CENTER_INSIDE -> {
                val scaleX = mViewWidth / mContentRect.width()
                val scaleY = mViewHeight / mContentRect.height()
                LOG.v("computeBaseZoom", "centerInside", "scaleX:", scaleX, "scaleY:", scaleY)
                Math.min(scaleX, scaleY)
            }
            TRANSFORMATION_CENTER_CROP -> {
                val scaleX = mViewWidth / mContentRect.width()
                val scaleY = mViewHeight / mContentRect.height()
                LOG.v("computeBaseZoom", "centerCrop", "scaleX:", scaleX, "scaleY:", scaleY)
                Math.max(scaleX, scaleY)
            }
            TRANSFORMATION_NONE -> 1f
            else -> 1f
        }
    }

    @SuppressLint("RtlHardcoded")
    @ZoomApi.ScaledPan
    private fun computeBasePan(): FloatArray {
        val result = floatArrayOf(0f, 0f)
        val extraWidth = mContentRect.width() - mViewWidth
        val extraHeight = mContentRect.height() - mViewHeight
        if (extraWidth > 0) {
            // Honour the horizontal gravity indication.
            when (mTransformationGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                Gravity.LEFT -> result[0] = 0F
                Gravity.CENTER_HORIZONTAL -> result[0] = -0.5f * extraWidth
                Gravity.RIGHT -> result[0] = -extraWidth
            }
        }
        if (extraHeight > 0) {
            // Honour the vertical gravity indication.
            when (mTransformationGravity and Gravity.VERTICAL_GRAVITY_MASK) {
                Gravity.TOP -> result[1] = 0F
                Gravity.CENTER_VERTICAL -> result[1] = -0.5f * extraHeight
                Gravity.BOTTOM -> result[1] = -extraHeight
            }
        }
        return result
    }

    //endregion
    //region Private helpers
    private fun dispatchOnMatrix() {
        mListener?.onUpdate(this, matrix)
    }

    private fun dispatchOnIdle() {
        mListener?.onIdle(this)
    }

    @ZoomApi.Zoom
    private fun ensureScaleBounds(@ZoomApi.Zoom value: Float, allowOverPinch: Boolean): Float {
        var value = value
        var minZoom = resolveZoom(mMinZoom, mMinZoomMode)
        var maxZoom = resolveZoom(mMaxZoom, mMaxZoomMode)
        if (allowOverPinch && mOverPinchable) {
            minZoom -= currentOverPinch
            maxZoom += currentOverPinch
        }
        if (value < minZoom) value = minZoom
        if (value > maxZoom) value = maxZoom
        return value
    }

    private fun ensureCurrentTranslationBounds(allowOverScroll: Boolean) {
        @ZoomApi.ScaledPan val fixX = ensureTranslationBounds(0f, true, allowOverScroll)
        @ZoomApi.ScaledPan val fixY = ensureTranslationBounds(0f, false, allowOverScroll)
        if (fixX != 0f || fixY != 0f) {
            mMatrix.postTranslate(fixX, fixY)
            mMatrix.mapRect(mContentRect, mContentBaseRect)
        }
    }

    // Checks against the translation value to ensure it is inside our acceptable bounds.
    // If allowOverScroll, overScroll value might be considered to allow "invalid" value.
    @ZoomApi.ScaledPan
    private fun ensureTranslationBounds(
        @ZoomApi.ScaledPan delta: Float,
        horizontal: Boolean,
        allowOverScroll: Boolean
    ): Float {
        @ZoomApi.ScaledPan val value = if (horizontal) scaledPanX else scaledPanY
        val viewSize = if (horizontal) mViewWidth else mViewHeight
        @ZoomApi.ScaledPan val contentSize = if (horizontal) mContentRect.width() else mContentRect.height()
        val overScrollable = if (horizontal) mOverScrollHorizontal else mOverScrollVertical
        @ZoomApi.ScaledPan val overScroll =
            if (overScrollable && allowOverScroll) currentOverScroll.toFloat() else 0.toFloat()
        return getTranslationCorrection(value + delta, viewSize, contentSize, overScroll)
    }

    @ZoomApi.ScaledPan
    private fun getTranslationCorrection(
        @ZoomApi.ScaledPan value: Float, viewSize: Float,
        @ZoomApi.ScaledPan contentSize: Float, @ZoomApi.ScaledPan overScroll: Float
    ): Float {
        @ZoomApi.ScaledPan val tolerance = overScroll.toInt()
        var min: Float
        var max: Float
        if (contentSize <= viewSize) {
            // If contentSize <= viewSize, we want to stay centered.
            // Need a positive translation, that shows some background.
            min = (viewSize - contentSize) / 2f
            max = (viewSize - contentSize) / 2f
        } else {
            // If contentSize is bigger, we just don't want to go outside.
            // Need a negative translation, that hides content.
            min = viewSize - contentSize
            max = 0f
        }
        min -= tolerance.toFloat()
        max += tolerance.toFloat()
        var desired = value
        if (desired < min) desired = min
        if (desired > max) desired = max
        return desired - value
    }

    @ZoomApi.Zoom
    private fun resolveZoom(zoom: Float, @ZoomApi.ZoomType mode: Int): Float {
        when (mode) {
            TYPE_ZOOM -> return zoom
            TYPE_REAL_ZOOM -> return zoom / mBaseZoom
        }
        return (-1).toFloat()
    }

    @ZoomApi.ScaledPan
    private fun resolvePan(@ZoomApi.AbsolutePan pan: Float): Float {
        return pan * getRealZoom()
    }

    @ZoomApi.AbsolutePan
    private fun unresolvePan(@ZoomApi.ScaledPan pan: Float): Float {
        return pan / getRealZoom()
    }

    /**
     * This is required when the content is a View that has clickable hierarchies inside.
     * If true is returned, implementors should not pass the call to super.
     *
     * @param ev the motion event
     * @return whether we want to intercept the event
     */
    fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return processTouchEvent(ev) > TOUCH_LISTEN
    }

    /**
     * Process the given touch event.
     * If true is returned, implementors should not pass the call to super.
     *
     * @param ev the motion event
     * @return whether we want to steal the event
     */
    fun onTouchEvent(ev: MotionEvent): Boolean {
        return processTouchEvent(ev) > TOUCH_NO
    }

    private fun processTouchEvent(event: MotionEvent): Int {
        LOG.v("processTouchEvent:", "start.")
        if (mMode == ANIMATING) return TOUCH_STEAL
        var result = mScaleDetector.onTouchEvent(event)
        LOG.v("processTouchEvent:", "scaleResult:", result)

        // Pinch detector always returns true. If we actually started a pinch,
        // Don't pass to fling detector.
        if (mMode != PINCHING) {
            result = result or mFlingDragDetector.onTouchEvent(event)
            LOG.v("processTouchEvent:", "flingResult:", result)
        }

        // Detect scroll ends, this appears to be the only way.
        if (mMode == SCROLLING) {
            val a = event.actionMasked
            if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                LOG.i("processTouchEvent:", "up event while scrolling, dispatching onScrollEnd.")
                onScrollEnd()
            }
        }
        return if (result && mMode != NONE) {
            LOG.v("processTouchEvent:", "returning: TOUCH_STEAL")
            TOUCH_STEAL
        } else if (result) {
            LOG.v("processTouchEvent:", "returning: TOUCH_LISTEN")
            TOUCH_LISTEN
        } else {
            LOG.v("processTouchEvent:", "returning: TOUCH_NO")
            setState(NONE)
            TOUCH_NO
        }
    }

    private inner class PinchListener : SimpleOnScaleGestureListener() {
        @ZoomApi.AbsolutePan
        private var mAbsTargetX = 0f

        @ZoomApi.AbsolutePan
        private var mAbsTargetY = 0f
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (!mZoomEnabled) {
                return false
            }
            if (setState(PINCHING)) {
                val eps = 0.0001f
                if (Math.abs(mAbsTargetX) < eps || Math.abs(mAbsTargetY) < eps) {
                    // We want to interpret this as a scaled value, to work with the *actual* zoom.
                    @ZoomApi.ScaledPan var scaledFocusX = -detector.focusX
                    @ZoomApi.ScaledPan var scaledFocusY = -detector.focusY
                    LOG.i(
                        "onScale:",
                        "Setting focus.",
                        "detectorFocusX:",
                        scaledFocusX,
                        "detectorFocusX:",
                        scaledFocusY
                    )

                    // Account for current pan.
                    scaledFocusX += scaledPanX
                    scaledFocusY += scaledPanY

                    // Transform to an absolute, scale-independent value.
                    mAbsTargetX = unresolvePan(scaledFocusX)
                    mAbsTargetY = unresolvePan(scaledFocusY)
                    LOG.i(
                        "onScale:",
                        "Setting focus.",
                        "absTargetX:",
                        mAbsTargetX,
                        "absTargetY:",
                        mAbsTargetY
                    )
                }

                // Having both overPinch and overScroll is hard to manage, there are lots of bugs if we do.
                val factor = detector.scaleFactor
                val newZoom = mZoom * factor
                applyPinch(newZoom, mAbsTargetX, mAbsTargetY, true)
                return true
            }
            return false
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            LOG.i(
                "onScaleEnd:", "mAbsTargetX:", mAbsTargetX, "mAbsTargetY:",
                mAbsTargetY, "mOverPinchable;", mOverPinchable
            )
            mAbsTargetX = 0f
            mAbsTargetY = 0f
            if (mOverPinchable) {
                // We might have over pinched. Animate back to reasonable value.
                @ZoomApi.Zoom var zoom = 0f
                @ZoomApi.Zoom val maxZoom = resolveZoom(mMaxZoom, mMaxZoomMode)
                @ZoomApi.Zoom val minZoom = resolveZoom(mMinZoom, mMinZoomMode)
                if (getZoom() < minZoom) zoom = minZoom
                if (getZoom() > maxZoom) zoom = maxZoom
                LOG.i(
                    "onScaleEnd:", "zoom:", getZoom(), "max:",
                    maxZoom, "min;", minZoom
                )
                if (zoom > 0) {
                    animateZoom(zoom, true)
                    return
                }
            }
            setState(NONE)
        }
    }

    private inner class FlingScrollListener : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true // We are interested in the gesture.
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val vX = (if (mHorizontalPanEnabled) velocityX else 0).toInt()
            val vY = (if (mVerticalPanEnabled) velocityY else 0).toInt()
            return startFling(vX, vY)
        }

        override fun onScroll(
            e1: MotionEvent, e2: MotionEvent,
            @ZoomApi.AbsolutePan distanceX: Float, @ZoomApi.AbsolutePan distanceY: Float
        ): Boolean {
            var distanceX = distanceX
            var distanceY = distanceY
            if (setState(SCROLLING)) {
                // Allow overScroll. Will be reset in onScrollEnd().
                distanceX = if (mHorizontalPanEnabled) -distanceX else 0F
                distanceY = if (mVerticalPanEnabled) -distanceY else 0F
                applyScaledPan(distanceX, distanceY, true)
                // applyZoomAndAbsolutePan(getZoom(), getPanX() + distanceX, getPanY() + distanceY, true);
                return true
            }
            return false
        }
    }

    private fun onScrollEnd() {
        if (mOverScrollHorizontal || mOverScrollVertical) {
            // We might have over scrolled. Animate back to reasonable value.
            @ZoomApi.ScaledPan val fixX = ensureTranslationBounds(0f, true, false)
            @ZoomApi.ScaledPan val fixY = ensureTranslationBounds(0f, false, false)
            if (fixX != 0f || fixY != 0f) {
                animateScaledPan(fixX, fixY, true)
                return
            }
        }
        setState(NONE)
    }
    //endregion
    //region Position APIs
    /**
     * A low level API that can animate both zoom and pan at the same time.
     * Zoom might not be the actual matrix scale, see [.getZoom] and [.getRealZoom].
     * The coordinates are referred to the content size passed in [.setContentSize]
     * so they do not depend on current zoom.
     *
     * @param zoom    the desired zoom value
     * @param x       the desired left coordinate
     * @param y       the desired top coordinate
     * @param animate whether to animate the transition
     */
    override fun moveTo(
        @ZoomApi.Zoom zoom: Float,
        @ZoomApi.AbsolutePan x: Float,
        @ZoomApi.AbsolutePan y: Float,
        animate: Boolean
    ) {
        if (!mInitialized) return
        if (animate) {
            animateZoomAndAbsolutePan(zoom, x, y, false)
        } else {
            applyZoomAndAbsolutePan(zoom, x, y, false)
        }
    }

    /**
     * Pans the content until the top-left coordinates match the given x-y
     * values. These are referred to the content size passed in [.setContentSize],
     * so they do not depend on current zoom.
     *
     * @param x       the desired left coordinate
     * @param y       the desired top coordinate
     * @param animate whether to animate the transition
     */
    override fun panTo(@ZoomApi.AbsolutePan x: Float, @ZoomApi.AbsolutePan y: Float, animate: Boolean) {
        panBy(x - getPanX(), y - getPanY(), animate)
    }

    /**
     * Pans the content by the given quantity in dx-dy values.
     * These are referred to the content size passed in [.setContentSize],
     * so they do not depend on current zoom.
     *
     *
     * In other words, asking to pan by 1 pixel might result in a bigger pan, if the content
     * was zoomed in.
     *
     * @param dx      the desired delta x
     * @param dy      the desired delta y
     * @param animate whether to animate the transition
     */
    override fun panBy(@ZoomApi.AbsolutePan dx: Float, @ZoomApi.AbsolutePan dy: Float, animate: Boolean) {
        if (!mInitialized) return
        if (animate) {
            animateZoomAndAbsolutePan(mZoom, getPanX() + dx, getPanY() + dy, false)
        } else {
            applyZoomAndAbsolutePan(mZoom, getPanX() + dx, getPanY() + dy, false)
        }
    }

    /**
     * Zooms to the given scale. This might not be the actual matrix zoom,
     * see [.getZoom] and [.getRealZoom].
     *
     * @param zoom    the new scale value
     * @param animate whether to animate the transition
     */
    override fun zoomTo(@ZoomApi.Zoom zoom: Float, animate: Boolean) {
        if (!mInitialized) return
        if (animate) {
            animateZoom(zoom, false)
        } else {
            applyZoom(zoom, false)
        }
    }

    /**
     * Applies the given factor to the current zoom.
     *
     * @param zoomFactor a multiplicative factor
     * @param animate    whether to animate the transition
     */
    override fun zoomBy(zoomFactor: Float, animate: Boolean) {
        @ZoomApi.Zoom val newZoom = mZoom * zoomFactor
        zoomTo(newZoom, animate)
    }

    /**
     * Applies a small, animated zoom-in.
     * Shorthand for [.zoomBy] with factor 1.3.
     */
    override fun zoomIn() {
        zoomBy(1.3f, true)
    }

    /**
     * Applies a small, animated zoom-out.
     * Shorthand for [.zoomBy] with factor 0.7.
     */
    override fun zoomOut() {
        zoomBy(0.7f, true)
    }

    /**
     * Animates the actual matrix zoom to the given value.
     *
     * @param realZoom the new real zoom value
     * @param animate  whether to animate the transition
     */
    override fun realZoomTo(realZoom: Float, animate: Boolean) {
        zoomTo(resolveZoom(realZoom, TYPE_REAL_ZOOM), animate)
    }

    /**
     * Which is the max zoom that should be allowed.
     * If [.setOverPinchable] is set to true, this can be over-pinched
     * for a brief time.
     *
     * @param maxZoom the max zoom
     * @param type    the constraint mode
     * @see .getZoom
     * @see .getRealZoom
     * @see .TYPE_ZOOM
     *
     * @see .TYPE_REAL_ZOOM
     */
    override fun setMaxZoom(maxZoom: Float, @ZoomApi.ZoomType type: Int) {
        require(maxZoom >= 0) { "Max zoom should be >= 0." }
        mMaxZoom = maxZoom
        mMaxZoomMode = type
        if (mZoom > resolveZoom(maxZoom, type)) {
            zoomTo(resolveZoom(maxZoom, type), true)
        }
    }

    /**
     * Which is the min zoom that should be allowed.
     * If [.setOverPinchable] is set to true, this can be over-pinched
     * for a brief time.
     *
     * @param minZoom the min zoom
     * @param type    the constraint mode
     * @see .getZoom
     * @see .getRealZoom
     */
    override fun setMinZoom(minZoom: Float, @ZoomApi.ZoomType type: Int) {
        require(minZoom >= 0) { "Min zoom should be >= 0" }
        mMinZoom = minZoom
        mMinZoomMode = type
        if (mZoom <= resolveZoom(minZoom, type)) {
            zoomTo(resolveZoom(minZoom, type), true)
        }
    }

    /**
     * Gets the current zoom value, which can be used as a reference when calling
     * [.zoomTo] or [.zoomBy].
     *
     *
     * This can be different than the actual scale you get in the matrix, because at startup
     * we apply a base transformation, see [.setTransformation].
     * All zoom calls, including min zoom and max zoom, refer to this axis, where zoom is set to 1
     * right after the initial transformation.
     *
     * @return the current zoom
     * @see .getRealZoom
     */
    @ZoomApi.Zoom
    override fun getZoom(): Float {
        return mZoom
    }

    /**
     * Gets the current zoom value, including the base zoom that was eventually applied during
     * the starting transformation, see [.setTransformation].
     * This value will match the scaleX - scaleY values you get into the [Matrix],
     * and is the actual scale value of the content from its original size.
     *
     * @return the real zoom
     */
    @ZoomApi.RealZoom
    override fun getRealZoom(): Float {
        return mZoom * mBaseZoom
    }

    /**
     * Returns the current horizontal pan value, in content coordinates
     * (that is, as if there was no zoom at all) referring to what was passed
     * to [.setContentSize].
     *
     * @return the current horizontal pan
     */
    @ZoomApi.AbsolutePan
    override fun getPanX(): Float {
        return scaledPanX / getRealZoom()
    }

    /**
     * Returns the current vertical pan value, in content coordinates
     * (that is, as if there was no zoom at all) referring to what was passed
     * to [.setContentSize].
     *
     * @return the current vertical pan
     */
    @ZoomApi.AbsolutePan
    override fun getPanY(): Float {
        return scaledPanY / getRealZoom()
    }

    @get:ZoomApi.ScaledPan
    private val scaledPanX: Float
        get() = mContentRect.left

    @get:ZoomApi.ScaledPan
    private val scaledPanY: Float
        get() = mContentRect.top
    //endregion
    //region Apply values
    /**
     * Calls [.applyZoom] repeatedly
     * until the final zoom is reached, interpolating.
     *
     * @param newZoom        the new zoom
     * @param allowOverPinch whether overpinching is allowed
     */
    private fun animateZoom(@ZoomApi.Zoom newZoom: Float, allowOverPinch: Boolean) {
        var newZoom = newZoom
        newZoom = ensureScaleBounds(newZoom, allowOverPinch)
        if (setState(ANIMATING)) {
            mClearAnimation = false
            val startTime = System.currentTimeMillis()
            @ZoomApi.Zoom val startZoom = mZoom
            @ZoomApi.Zoom val endZoom = newZoom
            mView.post(object : Runnable {
                override fun run() {
                    if (mClearAnimation) return
                    val time = interpolateAnimationTime(System.currentTimeMillis() - startTime)
                    LOG.v("animateZoomAndAbsolutePan:", "animationStep:", time)
                    @ZoomApi.Zoom val zoom = startZoom + time * (endZoom - startZoom)
                    applyZoom(zoom, allowOverPinch)
                    if (time >= 1f) {
                        setState(NONE)
                    } else {
                        mView.postOnAnimation(this)
                    }
                }
            })
        }
    }

    /**
     * Calls [.applyZoomAndAbsolutePan] repeatedly
     * until the final position is reached, interpolating.
     *
     * @param newZoom         new zoom
     * @param x               final abs pan
     * @param y               final abs pan
     * @param allowOverScroll whether to overscroll
     */
    private fun animateZoomAndAbsolutePan(
        @ZoomApi.Zoom newZoom: Float,
        @ZoomApi.AbsolutePan x: Float, @ZoomApi.AbsolutePan y: Float,
        allowOverScroll: Boolean
    ) {
        var newZoom = newZoom
        newZoom = ensureScaleBounds(newZoom, allowOverScroll)
        if (setState(ANIMATING)) {
            mClearAnimation = false
            val startTime = System.currentTimeMillis()
            @ZoomApi.Zoom val startZoom = mZoom
            @ZoomApi.Zoom val endZoom = newZoom
            @ZoomApi.AbsolutePan val startX = getPanX()
            @ZoomApi.AbsolutePan val startY = getPanY()
            LOG.i(
                "animateZoomAndAbsolutePan:",
                "starting.",
                "startX:",
                startX,
                "endX:",
                x,
                "startY:",
                startY,
                "endY:",
                y
            )
            LOG.i(
                "animateZoomAndAbsolutePan:",
                "starting.",
                "startZoom:",
                startZoom,
                "endZoom:",
                endZoom
            )
            mView.post(object : Runnable {
                override fun run() {
                    if (mClearAnimation) return
                    val time = interpolateAnimationTime(System.currentTimeMillis() - startTime)
                    LOG.v("animateZoomAndAbsolutePan:", "animationStep:", time)
                    @ZoomApi.Zoom val zoom = startZoom + time * (endZoom - startZoom)
                    @ZoomApi.AbsolutePan val targetX = startX + time * (x - startX)
                    @ZoomApi.AbsolutePan val targetY = startY + time * (y - startY)
                    applyZoomAndAbsolutePan(zoom, targetX, targetY, allowOverScroll)
                    if (time >= 1f) {
                        setState(NONE)
                    } else {
                        mView.postOnAnimation(this)
                    }
                }
            })
        }
    }

    /**
     * Calls [.animateScaledPan] repeatedly
     * until the final delta is applied, interpolating.
     *
     * @param deltaX          a scaled delta
     * @param deltaY          a scaled delta
     * @param allowOverScroll whether to overscroll
     */
    private fun animateScaledPan(
        @ZoomApi.ScaledPan deltaX: Float, @ZoomApi.ScaledPan deltaY: Float,
        allowOverScroll: Boolean
    ) {
        if (setState(ANIMATING)) {
            mClearAnimation = false
            val startTime = System.currentTimeMillis()
            @ZoomApi.ScaledPan val startX = scaledPanX
            @ZoomApi.ScaledPan val startY = scaledPanY
            @ZoomApi.ScaledPan val endX = startX + deltaX
            @ZoomApi.ScaledPan val endY = startY + deltaY
            mView.post(object : Runnable {
                override fun run() {
                    if (mClearAnimation) return
                    val time = interpolateAnimationTime(System.currentTimeMillis() - startTime)
                    LOG.v("animateScaledPan:", "animationStep:", time)
                    @ZoomApi.ScaledPan val x = startX + time * (endX - startX)
                    @ZoomApi.ScaledPan val y = startY + time * (endY - startY)
                    applyScaledPan(x - scaledPanX, y - scaledPanY, allowOverScroll)
                    if (time >= 1f) {
                        setState(NONE)
                    } else {
                        mView.postOnAnimation(this)
                    }
                }
            })
        }
    }

    private fun interpolateAnimationTime(delta: Long): Float {
        val time = Math.min(1f, delta.toFloat() / ANIMATION_DURATION.toFloat())
        return INTERPOLATOR.getInterpolation(time)
    }

    /**
     * Applies the given zoom value, meant as a [Zoom] value
     * (so not a [RealZoom]).
     * The zoom is applied so that the center point is kept in its place
     *
     * @param newZoom        the new zoom value
     * @param allowOverPinch whether to overpinch
     */
    private fun applyZoom(@ZoomApi.Zoom newZoom: Float, allowOverPinch: Boolean) {
        var newZoom = newZoom
        newZoom = ensureScaleBounds(newZoom, allowOverPinch)
        val scaleFactor = newZoom / mZoom
        mMatrix.postScale(
            scaleFactor, scaleFactor,
            mViewWidth / 2f, mViewHeight / 2f
        )
        mMatrix.mapRect(mContentRect, mContentBaseRect)
        mZoom = newZoom
        ensureCurrentTranslationBounds(false)
        dispatchOnMatrix()
    }

    /**
     * Applies both zoom and absolute pan. This is like specifying a position.
     * The semantics of this are that after the position is applied, the zoom corresponds
     * to the given value, getPanX() returns x, getPanY() returns y.
     *
     *
     * Absolute panning is achieved through [Matrix.preTranslate],
     * which works in the original coordinate system.
     *
     * @param newZoom         the new zoom value
     * @param x               the final left absolute pan
     * @param y               the final top absolute pan
     * @param allowOverScroll whether to overscroll
     */
    private fun applyZoomAndAbsolutePan(
        @ZoomApi.Zoom newZoom: Float,
        @ZoomApi.AbsolutePan x: Float, @ZoomApi.AbsolutePan y: Float,
        allowOverScroll: Boolean
    ) {
        // Translation
        var newZoom = newZoom
        @ZoomApi.AbsolutePan val deltaX = x - getPanX()
        @ZoomApi.AbsolutePan val deltaY = y - getPanY()
        mMatrix.preTranslate(deltaX, deltaY)
        mMatrix.mapRect(mContentRect, mContentBaseRect)

        // Scale
        newZoom = ensureScaleBounds(newZoom, false)
        val scaleFactor = newZoom / mZoom
        // TODO: This used to work but I am not sure about it.
        // mMatrix.postScale(scaleFactor, scaleFactor, getScaledPanX(), getScaledPanY());
        // It keeps the pivot point at the scaled values 0, 0 (see applyPinch).
        // I think we should keep the current top, left.. Let's try:
        mMatrix.postScale(scaleFactor, scaleFactor, 0f, 0f)
        mMatrix.mapRect(mContentRect, mContentBaseRect)
        mZoom = newZoom
        ensureCurrentTranslationBounds(allowOverScroll)
        dispatchOnMatrix()
    }

    /**
     * Applies the given scaled translation.
     *
     *
     * Scaled translation are applied through [Matrix.postTranslate],
     * which acts on the actual dimension of the rect.
     *
     * @param deltaX          the x translation
     * @param deltaY          the y translation
     * @param allowOverScroll whether to overScroll
     */
    private fun applyScaledPan(
        @ZoomApi.ScaledPan deltaX: Float,
        @ZoomApi.ScaledPan deltaY: Float,
        allowOverScroll: Boolean
    ) {
        mMatrix.postTranslate(deltaX, deltaY)
        mMatrix.mapRect(mContentRect, mContentBaseRect)
        ensureCurrentTranslationBounds(allowOverScroll)
        dispatchOnMatrix()
    }

    /**
     * Helper for pinch gestures. In these cases what we know is the detector focus,
     * and we can use it in [Matrix.postScale] to avoid
     * buggy translations.
     *
     * @param newZoom        the new zoom
     * @param targetX        the target X in abs value
     * @param targetY        the target Y in abs value
     * @param allowOverPinch whether to overPinch
     */
    private fun applyPinch(
        @ZoomApi.Zoom newZoom: Float, @ZoomApi.AbsolutePan targetX: Float, @ZoomApi.AbsolutePan targetY: Float,
        allowOverPinch: Boolean
    ) {
        // The pivotX and pivotY options of postScale refer (obviously!) to the visible
        // portion of the screen, since the (0,0) point is remapped to be in top-left of the view.
        // The right coordinates to use are the view coordinates.
        // This means we should use scaled coordinates, but remove the current pan.
        var newZoom = newZoom
        @ZoomApi.ScaledPan val scaledX = resolvePan(targetX)
        @ZoomApi.ScaledPan val scaledY = resolvePan(targetY)
        newZoom = ensureScaleBounds(newZoom, allowOverPinch)
        val scaleFactor = newZoom / mZoom
        mMatrix.postScale(
            scaleFactor, scaleFactor,
            scaledPanX - scaledX,
            scaledPanY - scaledY
        )
        mMatrix.mapRect(mContentRect, mContentBaseRect)
        mZoom = newZoom
        ensureCurrentTranslationBounds(false)
        dispatchOnMatrix()
    }

    //endregion
    //region Fling
    // Puts min, start and max values in the mTemp array.
    // Since axes are shifted (pans are negative), min values are related to bottom-right,
    // while max values are related to top-left.
    private fun computeScrollerValues(horizontal: Boolean): Boolean {
        @ZoomApi.ScaledPan val currentPan = (if (horizontal) scaledPanX else scaledPanY).toInt()
        val viewDim = (if (horizontal) mViewWidth else mViewHeight).toInt()
        @ZoomApi.ScaledPan val contentDim =
            (if (horizontal) mContentRect.width() else mContentRect.height()).toInt()
        val fix = ensureTranslationBounds(0f, horizontal, false).toInt()
        if (viewDim >= contentDim) {
            // Content is smaller, we are showing some boundary.
            // We can't move in any direction (but we can overScroll).
            mTemp[0] = currentPan + fix
            mTemp[1] = currentPan
            mTemp[2] = currentPan + fix
        } else {
            // Content is bigger, we can move.
            // in this case minPan + viewDim = contentDim
            mTemp[0] = -(contentDim - viewDim)
            mTemp[1] = currentPan
            mTemp[2] = 0
        }
        return fix != 0
    }

    private fun startFling(@ZoomApi.ScaledPan velocityX: Int, @ZoomApi.ScaledPan velocityY: Int): Boolean {
        if (!setState(FLINGING)) return false

        // Using actual pan values for the scroller.
        // Note: these won't make sense if zoom changes.
        var overScrolled: Boolean
        overScrolled = computeScrollerValues(true)
        @ZoomApi.ScaledPan val minX = mTemp[0]
        @ZoomApi.ScaledPan val startX = mTemp[1]
        @ZoomApi.ScaledPan val maxX = mTemp[2]
        overScrolled = overScrolled or computeScrollerValues(false)
        @ZoomApi.ScaledPan val minY = mTemp[0]
        @ZoomApi.ScaledPan val startY = mTemp[1]
        @ZoomApi.ScaledPan val maxY = mTemp[2]
        val go =
            overScrolled || mOverScrollHorizontal || mOverScrollVertical || minX < maxX || minY < maxY
        if (!go) {
            setState(NONE)
            return false
        }
        @ZoomApi.ScaledPan val overScrollX = if (mOverScrollHorizontal) currentOverScroll else 0
        @ZoomApi.ScaledPan val overScrollY = if (mOverScrollVertical) currentOverScroll else 0
        LOG.i("startFling", "velocityX:", velocityX, "velocityY:", velocityY)
        LOG.i(
            "startFling",
            "flingX:",
            "min:",
            minX,
            "max:",
            maxX,
            "start:",
            startX,
            "overScroll:",
            overScrollY
        )
        LOG.i(
            "startFling",
            "flingY:",
            "min:",
            minY,
            "max:",
            maxY,
            "start:",
            startY,
            "overScroll:",
            overScrollX
        )
        mFlingScroller.fling(
            startX, startY,
            velocityX, velocityY,
            minX, maxX, minY, maxY,
            overScrollX, overScrollY
        )
        mView.post(object : Runnable {
            override fun run() {
                if (mFlingScroller.isFinished) {
                    setState(NONE)
                } else if (mFlingScroller.computeScrollOffset()) {
                    @ZoomApi.ScaledPan val newPanX = mFlingScroller.currX
                    @ZoomApi.ScaledPan val newPanY = mFlingScroller.currY
                    // OverScroller will eventually go back to our bounds.
                    applyScaledPan(newPanX - scaledPanX, newPanY - scaledPanY, true)
                    mView.postOnAnimation(this)
                }
            }
        })
        return true
    } //endregion

    companion object {
        private val TAG = ZoomEngine::class.java.simpleName
        private val INTERPOLATOR: Interpolator = AccelerateDecelerateInterpolator()
        private const val ANIMATION_DURATION = 280
        private val LOG: ZoomLogger = ZoomLogger.create(TAG)
        private const val NONE = 0
        private const val SCROLLING = 1
        private const val PINCHING = 2
        private const val ANIMATING = 3
        private const val FLINGING = 4
        private fun ms(@State mode: Int): String {
            when (mode) {
                NONE -> return "NONE"
                FLINGING -> return "FLINGING"
                SCROLLING -> return "SCROLLING"
                PINCHING -> return "PINCHING"
                ANIMATING -> return "ANIMATING"
            }
            return ""
        }

        //endregion
        //region Touch events and Gesture Listeners
        // Might make these public some day?
        private const val TOUCH_NO = 0
        private const val TOUCH_LISTEN = 1
        private const val TOUCH_STEAL = 2
    }

    /**
     * Constructs an helper instance.
     *
     * @param context   a valid context
     * @param container the view hosting the zoomable content
     * @param listener  a listener for events
     */
    init {
        mFlingScroller = OverScroller(context)
        mScaleDetector = ScaleGestureDetector(context, PinchListener())
        if (Build.VERSION.SDK_INT >= 19) mScaleDetector.isQuickScaleEnabled = false
        mFlingDragDetector = GestureDetector(context, FlingScrollListener())
        mView.viewTreeObserver.addOnGlobalLayoutListener(this)
    }
}