package com.bkc.zoom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatImageView
import com.bkc.R
import com.bkc.zoom.ZoomApi.Companion.TRANSFORMATION_CENTER_INSIDE
import com.bkc.zoom.ZoomApi.Companion.TYPE_ZOOM
import kotlin.math.roundToInt

/**
 * Uses [ZoomEngine] to allow zooming and pan events to the inner drawable.
 *
 *
 * TODO: support padding (from inside ZoomEngine that gets the view)
 */
@SuppressLint("AppCompatCustomView")
open class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0, override val ZoomApi: Int
) :
    AppCompatImageView(context, attrs, defStyleAttr), ZoomEngine.Listener, ZoomApi {
    private val mEngine: ZoomEngine
    private val mMatrix = Matrix()
    private val mDrawableRect = RectF()

    //region Internal
    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        init()
    }

    private fun init() {
        val drawable = drawable
        if (drawable != null) {
            mDrawableRect[0f, 0f, drawable.intrinsicWidth.toFloat()] =
                drawable.intrinsicHeight.toFloat()
            mEngine.setContentSize(mDrawableRect)
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return mEngine.onTouchEvent(ev) || super.onTouchEvent(ev)
    }

    override fun onUpdate(helper: ZoomEngine?, matrix: Matrix?) {
        mMatrix.set(matrix)
        imageMatrix = mMatrix
        awakenScrollBars()
    }

    override fun onIdle(engine: ZoomEngine?) {}
    override fun computeHorizontalScrollOffset(): Int {
        return ((-1 * mEngine.getPanX() * mEngine.getRealZoom()).roundToInt())
    }

    override fun computeHorizontalScrollRange(): Int {
        return (mDrawableRect.width() * mEngine.getRealZoom()) as Int
    }

    override fun computeVerticalScrollOffset(): Int {
        return ((-1 * mEngine.getPanY() * mEngine.getRealZoom()).roundToInt())
    }

    override fun computeVerticalScrollRange(): Int {
        return (mDrawableRect.height() * mEngine.getRealZoom()) as Int
    }
    //endregion
    //region APIs
    /**
     * Gets the backing [ZoomEngine] so you can access its APIs.
     *
     * @return the backing engine
     */
    val engine: ZoomEngine
        get() = mEngine
    //endregion
    //region ZoomApis
    /**
     * Controls whether the content should be over-scrollable horizontally.
     * If it is, drag and fling horizontal events can scroll the content outside the safe area,
     * then return to safe values.
     *
     * @param overScroll whether to allow horizontal over scrolling
     */
    override fun setOverScrollHorizontal(overScroll: Boolean) {
        engine.setOverScrollHorizontal(overScroll)
    }

    /**
     * Controls whether the content should be over-scrollable vertically.
     * If it is, drag and fling vertical events can scroll the content outside the safe area,
     * then return to safe values.
     *
     * @param overScroll whether to allow vertical over scrolling
     */
    override fun setOverScrollVertical(overScroll: Boolean) {
        engine.setOverScrollVertical(overScroll)
    }

    /**
     * Controls whether horizontal panning using gestures is enabled.
     *
     * @param enabled true enables horizontal panning, false disables it
     */
    override fun setHorizontalPanEnabled(enabled: Boolean) {
        engine.setHorizontalPanEnabled(enabled)
    }

    /**
     * Controls whether vertical panning using gestures is enabled.
     *
     * @param enabled true enables vertical panning, false disables it
     */
    override fun setVerticalPanEnabled(enabled: Boolean) {
        engine.setVerticalPanEnabled(enabled)
    }

    /**
     * Controls whether the content should be overPinchable.
     * If it is, pinch events can change the zoom outside the safe bounds,
     * than return to safe values.
     *
     * @param overPinchable whether to allow over pinching
     */
    override fun setOverPinchable(overPinchable: Boolean) {
        engine.setOverPinchable(overPinchable)
    }

    /**
     * Controls whether zoom using pinch gesture is enabled or not.
     *
     * @param enabled true enables zooming, false disables it
     */
    override fun setZoomEnabled(enabled: Boolean) {
        engine.setZoomEnabled(enabled)
    }

    /**
     * Sets the base transformation to be applied to the content.
     * Defaults to [.TRANSFORMATION_CENTER_INSIDE] with [Gravity.CENTER],
     * which means that the content will be zoomed so that it fits completely inside the container.
     *
     * @param transformation the transformation type
     * @param gravity        the transformation gravity. Might be ignored for some transformations
     */
    override fun setTransformation(transformation: Int, gravity: Int) {
        engine.setTransformation(transformation, gravity)
    }

    /**
     * A low level API that can animate both zoom and pan at the same time.
     * Zoom might not be the actual matrix scale, see [.getZoom] and [.getRealZoom].
     * The coordinates are referred to the content size so they do not depend on current zoom.
     *
     * @param zoom    the desired zoom value
     * @param x       the desired left coordinate
     * @param y       the desired top coordinate
     * @param animate whether to animate the transition
     */
    override fun moveTo(zoom: Float, x: Float, y: Float, animate: Boolean) {
        engine.moveTo(zoom, x, y, animate)
    }

    /**
     * Pans the content until the top-left coordinates match the given x-y
     * values. These are referred to the content size so they do not depend on current zoom.
     *
     * @param x       the desired left coordinate
     * @param y       the desired top coordinate
     * @param animate whether to animate the transition
     */
    override fun panTo(x: Float, y: Float, animate: Boolean) {
        engine.panTo(x, y, animate)
    }

    /**
     * Pans the content by the given quantity in dx-dy values.
     * These are referred to the content size so they do not depend on current zoom.
     *
     *
     * In other words, asking to pan by 1 pixel might result in a bigger pan, if the content
     * was zoomed in.
     *
     * @param dx      the desired delta x
     * @param dy      the desired delta y
     * @param animate whether to animate the transition
     */
    override fun panBy(dx: Float, dy: Float, animate: Boolean) {
        engine.panBy(dx, dy, animate)
    }

    /**
     * Zooms to the given scale. This might not be the actual matrix zoom,
     * see [.getZoom] and [.getRealZoom].
     *
     * @param zoom    the new scale value
     * @param animate whether to animate the transition
     */
    override fun zoomTo(zoom: Float, animate: Boolean) {
        engine.zoomTo(zoom, animate)
    }

    /**
     * Applies the given factor to the current zoom.
     *
     * @param zoomFactor a multiplicative factor
     * @param animate    whether to animate the transition
     */
    override fun zoomBy(zoomFactor: Float, animate: Boolean) {
        engine.zoomBy(zoomFactor, animate)
    }

    /**
     * Applies a small, animated zoom-in.
     */
    override fun zoomIn() {
        engine.zoomIn()
    }

    /**
     * Applies a small, animated zoom-out.
     */
    override fun zoomOut() {
        engine.zoomOut()
    }

    /**
     * Animates the actual matrix zoom to the given value.
     *
     * @param realZoom the new real zoom value
     * @param animate  whether to animate the transition
     */
    override fun realZoomTo(realZoom: Float, animate: Boolean) {
        engine.realZoomTo(realZoom, animate)
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
     */
    override fun setMaxZoom(maxZoom: Float, type: Int) {
        engine.setMaxZoom(maxZoom, type)
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
    override fun setMinZoom(minZoom: Float, type: Int) {
        engine.setMinZoom(minZoom, type)
    }

    /**
     * Gets the current zoom value, which can be used as a reference when calling
     * [.zoomTo] or [.zoomBy].
     *
     *
     * This can be different than the actual scale you get in the matrix, because at startup
     * we apply a base zoom to respect the "center inside" policy.
     * All zoom calls, including min zoom and max zoom, refer to this axis, where zoom is set to 1
     * right after the initial transformation.
     *
     * @return the current zoom
     * @see .getRealZoom
     */
    override fun getZoom(): Float {
        return engine.getZoom()
    }

    /**
     * Gets the current zoom value, including the base zoom that was eventually applied when
     * initializing to respect the "center inside" policy. This will match the scaleX - scaleY
     * values you get into the [Matrix], and is the actual scale value of the content
     * from its original size.
     *
     * @return the real zoom
     */
    override fun getRealZoom(): Float {
        return engine.getRealZoom()
    }

    /**
     * Returns the current horizontal pan value, in content coordinates
     * (that is, as if there was no zoom at all).
     *
     * @return the current horizontal pan
     */
    override fun getPanX() : Float {
        return engine.getPanX()
    }
    /*val panX: Float
        get() = engine.getPanX()*/

    /**
     * Returns the current vertical pan value, in content coordinates
     * (that is, as if there was no zoom at all).
     *
     * @return the current vertical pan
     */ //endregion
    override fun getPanY(): Float {
        return engine.getPanY()
    }
   /* val panY: Float
        get() = engine.getPanY()*/

    companion object {
        private val TAG = ZoomImageView::class.java.simpleName
    }

    init {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.ZoomEngine, defStyleAttr, 0)
        val overScrollHorizontal = a.getBoolean(R.styleable.ZoomEngine_overScrollHorizontal, true)
        val overScrollVertical = a.getBoolean(R.styleable.ZoomEngine_overScrollVertical, true)
        val horizontalPanEnabled = a.getBoolean(R.styleable.ZoomEngine_horizontalPanEnabled, true)
        val verticalPanEnabled = a.getBoolean(R.styleable.ZoomEngine_verticalPanEnabled, true)
        val overPinchable = a.getBoolean(R.styleable.ZoomEngine_overPinchable, true)
        val zoomEnabled = a.getBoolean(R.styleable.ZoomEngine_zoomEnabled, true)
        val minZoom = a.getFloat(R.styleable.ZoomEngine_minZoom, -1f)
        val maxZoom = a.getFloat(R.styleable.ZoomEngine_maxZoom, -1f)
        @ZoomApi.ZoomType val minZoomMode = a.getInteger(R.styleable.ZoomEngine_minZoomType, TYPE_ZOOM)
        @ZoomApi.ZoomType val maxZoomMode = a.getInteger(R.styleable.ZoomEngine_maxZoomType, TYPE_ZOOM)
        val transformation =
            a.getInteger(R.styleable.ZoomEngine_transformation, TRANSFORMATION_CENTER_INSIDE)
        val transformationGravity =
            a.getInt(R.styleable.ZoomEngine_transformationGravity, Gravity.CENTER)
        a.recycle()
        mEngine = ZoomEngine(context, this, this,ZoomApi)
        setTransformation(transformation, transformationGravity)
        setOverScrollHorizontal(overScrollHorizontal)
        setOverScrollVertical(overScrollVertical)
        setHorizontalPanEnabled(horizontalPanEnabled)
        setVerticalPanEnabled(verticalPanEnabled)
        setOverPinchable(overPinchable)
        setZoomEnabled(zoomEnabled)
        if (minZoom > -1) setMinZoom(minZoom, minZoomMode)
        if (maxZoom > -1) setMaxZoom(maxZoom, maxZoomMode)
        imageMatrix = mMatrix
        scaleType = ScaleType.MATRIX
    }
}