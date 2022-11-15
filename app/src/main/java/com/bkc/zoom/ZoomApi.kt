package com.bkc.zoom

import android.graphics.Matrix
import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * An interface for zoom controls.
 */
public interface ZoomApi {

    @Retention(RetentionPolicy.SOURCE)
    annotation class RealZoom

    @Retention(RetentionPolicy.SOURCE)
    annotation class Zoom

    @Retention(RetentionPolicy.SOURCE)
    annotation class AbsolutePan

    @Retention(RetentionPolicy.SOURCE)
    annotation class ScaledPan

    /**
     * Flag for zoom constraints and settings.
     * With TYPE_ZOOM the constraint is measured over the zoom in [.getZoom].
     * This is not the actual matrix scale value.
     *
     * @see .getZoom
     * @see .getRealZoom
     */
    val ZoomApi: Int

    companion object {
        const val TYPE_ZOOM = 0
        const val TYPE_REAL_ZOOM = 1
        const val TRANSFORMATION_CENTER_INSIDE = 0
        const val TRANSFORMATION_CENTER_CROP = 1
        const val TRANSFORMATION_NONE = 2
    }
    //var TYPE_ZOOM = 0

    /**
     * Flag for zoom constraints and settings.
     * With TYPE_REAL_ZOOM the constraint is measured over the zoom in [.getRealZoom],
     * which is the actual scale you get in the matrix.
     *
     * @see .getZoom
     * @see .getRealZoom
     */
   // var TYPE_REAL_ZOOM = 1

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(TYPE_ZOOM, TYPE_REAL_ZOOM)
    annotation class ZoomType

    /**
     * Constant for [.setTransformation].
     * The content will be zoomed so that it fits completely inside the container.
     */
   // var TRANSFORMATION_CENTER_INSIDE = 0

    /**
     * Constant for [.setTransformation].
     * The content will be zoomed so that its smaller side fits exactly inside the container.
     * The larger side will be partially cropped.
     */
   // var TRANSFORMATION_CENTER_CROP = 1

    /**
     * Constant for [.setTransformation].
     * No transformation will be applied, which means that both [.getZoom] and
     * [.getRealZoom] will return the same value.
     */
   // var TRANSFORMATION_NONE = 2

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(TRANSFORMATION_CENTER_INSIDE, TRANSFORMATION_CENTER_CROP, TRANSFORMATION_NONE)
    annotation class Transformation

    /**
     * Controls whether the content should be over-scrollable horizontally.
     * If it is, drag and fling horizontal events can scroll the content outside the safe area,
     * then return to safe values.
     *
     * @param overScroll whether to allow horizontal over scrolling
     */
    fun setOverScrollHorizontal(overScroll: Boolean)

    /**
     * Controls whether the content should be over-scrollable vertically.
     * If it is, drag and fling vertical events can scroll the content outside the safe area,
     * then return to safe values.
     *
     * @param overScroll whether to allow vertical over scrolling
     */
    fun setOverScrollVertical(overScroll: Boolean)

    /**
     * Controls whether horizontal panning using gestures is enabled.
     *
     * @param enabled true enables horizontal panning, false disables it
     */
    fun setHorizontalPanEnabled(enabled: Boolean)

    /**
     * Controls whether vertical panning using gestures is enabled.
     *
     * @param enabled true enables vertical panning, false disables it
     */
    fun setVerticalPanEnabled(enabled: Boolean)

    /**
     * Controls whether the content should be overPinchable.
     * If it is, pinch events can change the zoom outside the safe bounds,
     * than return to safe values.
     *
     * @param overPinchable whether to allow over pinching
     */
    fun setOverPinchable(overPinchable: Boolean)

    /**
     * Controls whether zoom using pinch gesture is enabled or not.
     *
     * @param enabled true enables zooming, false disables it
     */
    fun setZoomEnabled(enabled: Boolean)

    /**
     * Sets the base transformation to be applied to the content.
     * Defaults to [.TRANSFORMATION_CENTER_INSIDE] with [android.view.Gravity.CENTER],
     * which means that the content will be zoomed so that it fits completely inside the container.
     *
     * @param transformation the transformation type
     * @param gravity        the transformation gravity. Might be ignored for some transformations
     */
    fun setTransformation(@Transformation transformation: Int, gravity: Int)

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
    fun moveTo(@Zoom zoom: Float, @AbsolutePan x: Float, @AbsolutePan y: Float, animate: Boolean)

    /**
     * Pans the content until the top-left coordinates match the given x-y
     * values. These are referred to the content size so they do not depend on current zoom.
     *
     * @param x       the desired left coordinate
     * @param y       the desired top coordinate
     * @param animate whether to animate the transition
     */
    fun panTo(@AbsolutePan x: Float, @AbsolutePan y: Float, animate: Boolean)

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
    fun panBy(@AbsolutePan dx: Float, @AbsolutePan dy: Float, animate: Boolean)

    /**
     * Zooms to the given scale. This might not be the actual matrix zoom,
     * see [.getZoom] and [.getRealZoom].
     *
     * @param zoom    the new scale value
     * @param animate whether to animate the transition
     */
    fun zoomTo(@Zoom zoom: Float, animate: Boolean)

    /**
     * Applies the given factor to the current zoom.
     *
     * @param zoomFactor a multiplicative factor
     * @param animate    whether to animate the transition
     */
    fun zoomBy(zoomFactor: Float, animate: Boolean)

    /**
     * Applies a small, animated zoom-in.
     */
    fun zoomIn()

    /**
     * Applies a small, animated zoom-out.
     */
    fun zoomOut()

    /**
     * Animates the actual matrix zoom to the given value.
     *
     * @param realZoom the new real zoom value
     * @param animate  whether to animate the transition
     */
    fun realZoomTo(realZoom: Float, animate: Boolean)

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
    fun setMaxZoom(maxZoom: Float, @ZoomType type: Int)

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
    fun setMinZoom(minZoom: Float, @ZoomType type: Int)

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
    @Zoom
    fun getZoom(): Float

    /**
     * Gets the current zoom value, including the base zoom that was eventually applied when
     * initializing to respect the "center inside" policy. This will match the scaleX - scaleY
     * values you get into the [Matrix], and is the actual scale value of the content
     * from its original size.
     *
     * @return the real zoom
     */
    @RealZoom
    fun getRealZoom(): Float

    /**
     * Returns the current horizontal pan value, in content coordinates
     * (that is, as if there was no zoom at all).
     *
     * @return the current horizontal pan
     */
    @AbsolutePan
    fun getPanX(): Float

    /**
     * Returns the current vertical pan value, in content coordinates
     * (that is, as if there was no zoom at all).
     *
     * @return the current vertical pan
     */
    @AbsolutePan
    fun getPanY(): Float

}