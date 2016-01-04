/*
 * Copyright 2016 Vsevolod Ganin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ganin.darv;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.DimenRes;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * Extended {@link GridLayoutManager}.
 *
 * <p>New features are:
 * <ul>
 * <li>Selection camera offset.</li>
 * <li>Circular navigation.</li>
 * <li>AUTO_FIT support similar to GridView.</li>
 * <li>Controlling 'arrow' views (any views really) indicating begin or end is reached.</li>
 * </ul>
 */
public class ExtGridLayoutManager extends GridLayoutManager {

    /**
     * Builder for {@link ExtGridLayoutManager}.
     */
    public static final class Builder {

        private final Context mCtx;

        private float mOffsetFraction = 0.f;
        private boolean mOffsetEnabled = false;
        private boolean mCircular = false;
        private int mSpanCount = AUTO_FIT;
        private int mOrientation = GridLayoutManager.VERTICAL;
        private boolean mReverseOrder = false;
        private View mArrowTowardBegin;
        private View mArrowTowardEnd;
        private int mSpanSize = 0;

        public Builder(Context ctx) {
            mCtx = ctx;
        }

        /**
         * Selection offset fraction. E.g. if passed 0.5 selected view will be centered always.
         *
         * @param offsetFraction Offset fraction.
         * @return This builder instance for chaining.
         */
        public Builder offsetFraction(@FloatRange(from = 0.f, to = 1.f) float offsetFraction) {
            mOffsetFraction = offsetFraction;
            mOffsetEnabled = true;
            return this;
        }

        /**
         * Make navigation circular (or not).
         *
         * @param circular if true, make navigation circular.
         * @return This builder instance for chaining.
         */
        public Builder circular(boolean circular) {
            mCircular = circular;
            return this;
        }

        /**
         * Set the number of spans to be laid out. Set {@link ExtGridLayoutManager#AUTO_FIT} to
         * automatically determine span count that fits into available space. Use
         * {@link #spanSizePx(int)} or {@link #spanSizeRes(int)} to designate size of a span.
         *
         * @param spanCount The total number of spans in the grid
         * @return This builder instance for chaining.
         *
         * @see GridLayoutManager#setSpanCount(int)
         */
        public Builder spanCount(int spanCount) {
            if (spanCount < 1 && spanCount != AUTO_FIT) {
                throw new IllegalArgumentException("Span count must be > 0 or AUTO_FIT");
            }

            mSpanCount = spanCount;
            return this;
        }

        /**
         * Set the orientation of the layout.
         *
         * @param orientation orientation
         * @return This builder instance for chaining.
         *
         * @see GridLayoutManager#setOrientation(int)
         */
        public Builder orientation(int orientation) {
            mOrientation = orientation;
            return this;
        }

        /**
         * Used to reverse item traversal and layout order.
         *
         * @param reverseOrder reverse order
         * @return This builder instance for chaining.
         *
         * @see GridLayoutManager#setReverseLayout(boolean)
         */
        public Builder reverseOrder(boolean reverseOrder) {
            mReverseOrder = reverseOrder;
            return this;
        }

        /**
         * Set navigation arrows (or anything that represents them) to control. In particular,
         * if user sees the beginning of adapter data, {@code arrowTowardBegin} will be hidden.
         * If user sees the end of adapter data, {@code arrowTowardEnd} will be hidden.
         *
         * @param arrowTowardBegin View to hide when user see adapter beginning data.
         * @param arrowTowardEnd View to hide when user see end data.
         * @return This builder instance for chaining.
         */
        public Builder navigationArrows(@Nullable View arrowTowardBegin,
                @Nullable View arrowTowardEnd) {
            mArrowTowardBegin = arrowTowardBegin;
            mArrowTowardEnd = arrowTowardEnd;
            return this;
        }

        /**
         * Set size of a span along main axis in px. Can be 0 or less. In that case span size
         * is determined at first measure pass. Note that {@link ExtGridLayoutManager#AUTO_FIT}
         * is resolved only once, so subsequent orientation changes will not trigger recalculation.
         * Also note that this value is used only if span count is set to
         * {@link ExtGridLayoutManager#AUTO_FIT}.
         *
         * @param spanSizePx span size in px.
         * @return This builder instance for chaining.
         */
        public Builder spanSizePx(int spanSizePx) {
            mSpanSize = spanSizePx;
            return this;
        }

        /**
         * Set size of a span along main axis as dimension resource. Could be 0 or less. In that
         * case span size is determined at first measure pass. Note that {@link
         * ExtGridLayoutManager#AUTO_FIT} is resolved only once, so subsequent orientation
         * changes will not trigger recalculation. Also note that this value is used only if
         * span count is set to {@link ExtGridLayoutManager#AUTO_FIT}.
         *
         * @param spanSizeRes span size resource.
         * @return This builder instance for chaining.
         */
        public Builder spanSizeRes(@DimenRes int spanSizeRes) {
            return spanSizePx(mCtx.getResources().getDimensionPixelSize(spanSizeRes));
        }

        /**
         * Build this adapter into {@link ExtGridLayoutManager} instance.
         *
         * @return New {@link ExtGridLayoutManager} instance.
         */
        public ExtGridLayoutManager build() {
            ExtGridLayoutManager nemoLM = new ExtGridLayoutManager(
                    mCtx, mSpanCount, mOrientation, mReverseOrder);
            return configure(nemoLM);
        }

        private ExtGridLayoutManager configure(ExtGridLayoutManager inst) {
            inst.setCircular(mCircular);
            if (mOffsetEnabled) inst.setOffset(mOffsetFraction);
            inst.setArrowTowardBegin(mArrowTowardBegin);
            inst.setArrowTowardEnd(mArrowTowardEnd);
            inst.setSpanSizePx(mSpanSize);
            return inst;
        }
    }

    public static final int AUTO_FIT = 0;

    private static final int DO_NOT_FOCUS = -1;
    private static final int FIRST = -2;
    private static final int LAST = -3;

    private float mOffsetFraction = 0.f;
    private boolean mOffsetEnabled = false;
    private boolean mCircular = false;
    private WeakReference<View> mArrowTowardBeginRef;
    private WeakReference<View> mArrowTowardEndRef;
    private int mSpanCount = AUTO_FIT;

    /**
     * Px size of one span. Only used if {@link #mSpanSize} equals to {@link #AUTO_FIT}.
     * If size <= 0 then one tries to determine size itself.
     * Actual resolution of auto fit happens in {@link #resolveAutoFit(RecyclerView.Recycler)}.
     */
    private int mSpanSize;

    private int mPendingChildPositionToFocus = DO_NOT_FOCUS;

    public ExtGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ExtGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    public ExtGridLayoutManager(Context context, int spanCount, int orientation,
            boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
    }

    public void disableOffset() {
        mOffsetEnabled = false;
    }

    public void setOffset(@FloatRange(from = 0.f, to = 1.f) float offsetFraction) {
        mOffsetFraction = offsetFraction;
        mOffsetEnabled = true;
    }

    public float getOffsetFraction() {
        return mOffsetFraction;
    }

    public boolean isOffsetEnabled() {
        return mOffsetEnabled;
    }

    public void setCircular(boolean circular) {
        mCircular = circular;
    }

    public boolean isCircular() {
        return mCircular;
    }

    public void setArrowTowardBegin(View arrowTowardBegin) {
        mArrowTowardBeginRef = new WeakReference<View>(arrowTowardBegin);
    }

    public void setArrowTowardEnd(View arrowTowardEnd) {
        mArrowTowardEndRef = new WeakReference<View>(arrowTowardEnd);
    }

    public View getArrowTowardBegin() {
        return mArrowTowardBeginRef == null ? null : mArrowTowardBeginRef.get();
    }

    public View getArrowTowardEnd() {
        return mArrowTowardEndRef == null ? null : mArrowTowardEndRef.get();
    }

    public void setSpanSizePx(int spanSizePx) {
        mSpanSize = spanSizePx;
    }

    @Override
    public void setSpanCount(int spanCount) {
        if (spanCount < 1 && spanCount != AUTO_FIT) {
            throw new IllegalArgumentException("Span count must be > 0 or AUTO_FIT");
        }

        mSpanCount = spanCount;

        if (mSpanCount == AUTO_FIT) {
            // Provide minimum span count to not break things.
            spanCount = 1;
        }

        super.setSpanCount(spanCount);
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec,
            int heightSpec) {
        boolean wrap;

        int orientation = getOrientation();
        int widthMode = View.MeasureSpec.getMode(widthSpec);
        int heightMode = View.MeasureSpec.getMode(heightSpec);

        if (orientation == VERTICAL) {
            wrap = widthMode == View.MeasureSpec.AT_MOST;
        } else {
            wrap = heightMode == View.MeasureSpec.AT_MOST;
        }

        if (wrap && getItemCount() > 0) {
            int spanCount = getSpanCount();
            View dummyChild = recycler.getViewForPosition(0);
            measureChildWithMargins(dummyChild, 0, 0);

            if (orientation == VERTICAL) {
                int widthSize = View.MeasureSpec.getSize(widthSpec);
                int supposedOccupiedWidth = dummyChild.getMeasuredWidth() * spanCount;

                if (supposedOccupiedWidth <= widthSize) {
                    widthSpec = View.MeasureSpec.makeMeasureSpec(supposedOccupiedWidth, widthMode);
                }
            } else {
                int heightSize = View.MeasureSpec.getSize(heightSpec);
                int supposedOccupiedHeight = dummyChild.getMeasuredHeight() * spanCount;

                if (supposedOccupiedHeight <= heightSize) {
                    heightSpec = View.MeasureSpec.makeMeasureSpec(supposedOccupiedHeight, heightMode);
                }
            }
        }

        super.onMeasure(recycler, state, widthSpec, heightSpec);

        if (getItemCount() > 0 && mSpanCount == AUTO_FIT) {
            resolveAutoFit(recycler);
        }
    }

    private void resolveAutoFit(RecyclerView.Recycler recycler) {
        int orientation = getOrientation();

        // Have no span size data. Trying to determine it from what children want themselves.
        if (mSpanSize <= 0) {
            View dummyChild = recycler.getViewForPosition(0);
            mSpanSize = orientation == VERTICAL
                    ? getDecoratedMeasuredWidth(dummyChild)
                    : getDecoratedMeasuredHeight(dummyChild);
        }

        if (mSpanSize > 0) {
            int size = orientation == VERTICAL ? getWidth() : getHeight();
            int spanCount = Math.max(1, size / mSpanSize);
            setSpanCount(spanCount);
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);

        if (mPendingChildPositionToFocus != DO_NOT_FOCUS) {
            int layoutPosToFocus;

            switch (mPendingChildPositionToFocus) {
                case FIRST:
                    layoutPosToFocus = 0;
                    break;
                case LAST:
                    layoutPosToFocus = getChildCount() - 1;
                    break;
                default:
                    layoutPosToFocus = mPendingChildPositionToFocus;
            }

            if (layoutPosToFocus >= 0 && layoutPosToFocus < getChildCount()) {
                getChildAt(layoutPosToFocus).requestFocus();
            }

            mPendingChildPositionToFocus = DO_NOT_FOCUS;
        }

        updateArrowTowardBeginVisibility();
        updateArrowTowardEndVisibility();
    }

    @Override
    public boolean onRequestChildFocus(RecyclerView parent, RecyclerView.State state, View child,
            View focused) {
        updateArrowTowardBeginVisibility();
        updateArrowTowardEndVisibility();

        return super.onRequestChildFocus(parent, state, child, focused);
    }

    @Override
    public View onFocusSearchFailed(View focused, int focusDirection,
            RecyclerView.Recycler recycler, RecyclerView.State state) {
        View nextView = super.onFocusSearchFailed(focused, focusDirection, recycler, state);

        if (nextView == null) {
            if (mCircular) {
                final int adapterPositionToJump;
                if ((focusDirection == View.FOCUS_DOWN && getOrientation() == VERTICAL)
                        || (focusDirection == View.FOCUS_RIGHT && getOrientation() == HORIZONTAL)) {
                    adapterPositionToJump = 0;
                    mPendingChildPositionToFocus = FIRST;
                } else if ((focusDirection == View.FOCUS_UP && getOrientation() == VERTICAL)
                        || (focusDirection == View.FOCUS_LEFT && getOrientation() == HORIZONTAL)) {
                    adapterPositionToJump = getItemCount() - 1;
                    mPendingChildPositionToFocus = LAST;
                } else {
                    return null;
                }

                // Can't initiate scrolling because requesting layout is forbidden in this state
                focused.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollToPosition(adapterPositionToJump);
                    }
                });
            }
        }

        return null;
    }

    @Override
    public void scrollToPosition(int position) {
        if (mOffsetEnabled) {
            float offset = getOrientation() == VERTICAL
                    ? getHeight() * mOffsetFraction
                    : getWidth() * mOffsetFraction;

            super.scrollToPositionWithOffset(position, (int) offset);
        } else {
            super.scrollToPosition(position);
        }
    }

    @Override
    public boolean requestChildRectangleOnScreen(RecyclerView parent, View child, Rect rect,
            boolean immediate) {
        if (!mOffsetEnabled) {
            return super.requestChildRectangleOnScreen(parent, child, rect, true);
        }

        final int parentLeft = getPaddingLeft();
        final int parentTop = getPaddingTop();
        final int parentRight = getWidth() - getPaddingRight();
        final int parentBottom = getHeight() - getPaddingBottom();
        final int childLeft = child.getLeft() + rect.left;
        final int childTop = child.getTop() + rect.top;
        final int childRight = childLeft + rect.width();
        final int childBottom = childTop + rect.height();

        int cameraLeft;
        int cameraRight;
        int cameraTop;
        int cameraBottom;

        final int cameraCenterX = (int) ((parentRight + parentLeft) * mOffsetFraction);
        final int childHalfWidth = (int) Math.ceil((childRight - childLeft) * 0.5);
        cameraLeft = cameraCenterX - childHalfWidth;
        cameraRight = cameraCenterX + childHalfWidth;

        final int cameraCenterY = (int) ((parentBottom + parentTop) * mOffsetFraction);
        final int childHalfHeight = (int) Math.ceil((childBottom - childTop) * 0.5);
        cameraTop = cameraCenterY - childHalfHeight;
        cameraBottom = cameraCenterY + childHalfHeight;

        final int offScreenLeft = Math.min(0, childLeft - cameraLeft);
        final int offScreenTop = Math.min(0, childTop - cameraTop);
        final int offScreenRight = Math.max(0, childRight - cameraRight);
        final int offScreenBottom = Math.max(0, childBottom - cameraBottom);

        // Favor the "start" layout direction over the end when bringing one side or the other
        // of a large rect into view. If we decide to bring in end because start is already
        // visible, limit the scroll such that start won't go out of bounds.
        final int dx;
        if (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            dx = offScreenRight != 0 ? offScreenRight
                    : Math.max(offScreenLeft, childRight - parentRight);
        } else {
            dx = offScreenLeft != 0 ? offScreenLeft
                    : Math.min(childLeft - parentLeft, offScreenRight);
        }

        // Favor bringing the top into view over the bottom. If top is already visible and
        // we should scroll to make bottom visible, make sure top does not go out of bounds.
        final int dy = offScreenTop != 0 ? offScreenTop
                : Math.min(childTop - parentTop, offScreenBottom);

        if (dx != 0 || dy != 0) {
            parent.scrollBy(dx, dy);
            return true;
        }

        return false;
    }

    private void updateArrowTowardBeginVisibility() {
        View arrowTowardBegin;
        if (mArrowTowardBeginRef == null || (arrowTowardBegin = mArrowTowardBeginRef.get()) == null) {
            return;
        }

        if (getChildCount() == 0) {
            arrowTowardBegin.setVisibility(View.INVISIBLE);
        } else {
            View firstChild = getChildAt(0);
            int beginTop = firstChild.getTop();
            int beginLeft = firstChild.getLeft();
            int paddingTop = getPaddingTop();
            int paddingLeft = getPaddingLeft();
            int beginAdapterPosition = getPosition(firstChild);

            if (beginAdapterPosition == 0 && beginTop >= paddingTop && beginLeft >= paddingLeft) {
                arrowTowardBegin.setVisibility(View.INVISIBLE);
            } else {
                arrowTowardBegin.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateArrowTowardEndVisibility() {
        View arrowTowardEnd;
        if (mArrowTowardEndRef == null || (arrowTowardEnd = mArrowTowardEndRef.get()) == null) {
            return;
        }

        if (getChildCount() == 0) {
            arrowTowardEnd.setVisibility(View.INVISIBLE);
        } else {
            View lastChild = getChildAt(getChildCount() - 1);
            int endBottom = lastChild.getBottom();
            int endRight = lastChild.getRight();
            int bottomLine = getHeight() - getPaddingBottom();
            int rightLine = getWidth() - getPaddingRight();
            int endAdapterPosition = getPosition(lastChild);
            int itemCount = getItemCount();

            if (endAdapterPosition == itemCount - 1
                    && endBottom <= bottomLine && endRight <= rightLine) {
                arrowTowardEnd.setVisibility(View.INVISIBLE);
            } else {
                arrowTowardEnd.setVisibility(View.VISIBLE);
            }
        }
    }


}
