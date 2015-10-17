/*
 * Copyright 2015 Vsevolod Ganin
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
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;

/**
 * {@link android.support.v7.widget.RecyclerView.LayoutManager} wrapper which overrides following
 * general behaviors:
 * <ul>
 * <li>{@link #requestChildRectangleOnScreen(RecyclerView, View, Rect, boolean)} contains
 * sticky scrolling behavior when focused item always offset by some fraction from some border.</li>
 * </ul>
 */
class LayoutManagerDecorator extends RecyclerView.LayoutManager {

    /**
     * Wrapped {@link android.support.v7.widget.RecyclerView.LayoutManager} instance.
     */
    private final RecyclerView.LayoutManager mLayoutManager;

    /** @see DpadAwareRecyclerView#mScrollOffsetFractionX */
    private Float mScrollOffsetFractionX;

    /** @see DpadAwareRecyclerView#mScrollOffsetFractionY */
    private Float mScrollOffsetFractionY;

    public LayoutManagerDecorator(RecyclerView.LayoutManager lm) {
        mLayoutManager = lm;
    }

    /**
     * This constructor enables sticky scrolling behavior with supplied value as offset fraction.
     *
     * @param lm {@link android.support.v7.widget.RecyclerView.LayoutManager} instance to be wrapped
     * @param scrollOffsetFractionX offset fraction from left border which will hold while scrolling
     *                              with dpad
     * @param scrollOffsetFractionY offset fraction from top border which will hold while scrolling
     *                              with dpad
     */
    public LayoutManagerDecorator(RecyclerView.LayoutManager lm,
            Float scrollOffsetFractionX, Float scrollOffsetFractionY) {
        mLayoutManager = lm;

        mScrollOffsetFractionX = scrollOffsetFractionX;
        mScrollOffsetFractionY = scrollOffsetFractionY;
    }

    /** @see DpadAwareRecyclerView#setScrollOffsetFractionX(Float) */
    public void setScrollOffsetFractionX(Float scrollOffsetFraction) {
        mScrollOffsetFractionX = scrollOffsetFraction;
    }

    /** @see DpadAwareRecyclerView#setScrollOffsetFractionY(Float) */
    public void setScrollOffsetFractionY(Float scrollOffsetFraction) {
        mScrollOffsetFractionY = scrollOffsetFraction;
    }

    @Override
    public boolean requestChildRectangleOnScreen(RecyclerView parent, View child, Rect rect,
            boolean immediate) {
        if (mScrollOffsetFractionX == null && mScrollOffsetFractionY == null) {
            return mLayoutManager.requestChildRectangleOnScreen(parent, child, rect, immediate);
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

        if (mScrollOffsetFractionX == null) {
            cameraLeft = parentLeft;
            cameraRight = parentRight;
        } else {
            final int cameraCenterX = (int) ((parentRight + parentLeft) * mScrollOffsetFractionX);
            final int childHalfWidth = (int) Math.ceil((childRight - childLeft) * 0.5);
            cameraLeft = cameraCenterX - childHalfWidth;
            cameraRight = cameraCenterX + childHalfWidth;
        }

        if (mScrollOffsetFractionY == null) {
            cameraTop = parentTop;
            cameraBottom = parentBottom;
        } else {
            final int cameraCenterY = (int) ((parentBottom + parentTop) * mScrollOffsetFractionY);
            final int childHalfHeight = (int) Math.ceil((childBottom - childTop) * 0.5);
            cameraTop = cameraCenterY - childHalfHeight;
            cameraBottom = cameraCenterY + childHalfHeight;
        }

        final int offScreenLeft = Math.min(0, childLeft - cameraLeft);
        final int offScreenTop = Math.min(0, childTop - cameraTop);
        final int offScreenRight = Math.max(0, childRight - cameraRight);
        final int offScreenBottom = Math.max(0, childBottom - cameraBottom);

        // Favor the "start" layout direction over the end when bringing one side or the other
        // of a large rect into view. If we decide to bring in end because start is already
        // visible, limit the scroll such that start won't go out of bounds.
        final int dx;
        if (getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL) {
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
            if (immediate) {
                parent.scrollBy(dx, dy);
            } else {
                parent.smoothScrollBy(dx, dy);
            }
            return true;
        }
        return false;
    }

    @Override
    public void requestLayout() {
        mLayoutManager.requestLayout();
    }

    @Override
    public void assertInLayoutOrScroll(String message) {
        mLayoutManager.assertInLayoutOrScroll(message);
    }

    @Override
    public void assertNotInLayoutOrScroll(String message) {
        mLayoutManager.assertNotInLayoutOrScroll(message);
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return mLayoutManager.supportsPredictiveItemAnimations();
    }

    @Override
    public boolean isAttachedToWindow() {
        return mLayoutManager.isAttachedToWindow();
    }

    @Override
    public void postOnAnimation(Runnable action) {
        mLayoutManager.postOnAnimation(action);
    }

    @Override
    public boolean removeCallbacks(Runnable action) {
        return mLayoutManager.removeCallbacks(action);
    }

    @Override
    @CallSuper
    public void onAttachedToWindow(RecyclerView view) {
        mLayoutManager.onAttachedToWindow(view);
    }

    @Override
    @Deprecated
    public void onDetachedFromWindow(RecyclerView view) {
        mLayoutManager.onDetachedFromWindow(view);
    }

    @Override
    @CallSuper
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        mLayoutManager.onDetachedFromWindow(view, recycler);
    }

    @Override
    public boolean getClipToPadding() {
        return mLayoutManager.getClipToPadding();
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        mLayoutManager.onLayoutChildren(recycler, state);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return mLayoutManager.generateDefaultLayoutParams();
    }

    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return mLayoutManager.checkLayoutParams(lp);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return mLayoutManager.generateLayoutParams(lp);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return mLayoutManager.generateLayoutParams(c, attrs);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State
            state) {
        return mLayoutManager.scrollHorizontallyBy(dx, recycler, state);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State
            state) {
        return mLayoutManager.scrollVerticallyBy(dy, recycler, state);
    }

    @Override
    public boolean canScrollHorizontally() {
        return mLayoutManager.canScrollHorizontally();
    }

    @Override
    public boolean canScrollVertically() {
        return mLayoutManager.canScrollVertically();
    }

    @Override
    public void scrollToPosition(int position) {
        mLayoutManager.scrollToPosition(position);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int
            position) {
        mLayoutManager.smoothScrollToPosition(recyclerView, state, position);
    }

    @Override
    public void startSmoothScroll(RecyclerView.SmoothScroller smoothScroller) {
        mLayoutManager.startSmoothScroll(smoothScroller);
    }

    @Override
    public boolean isSmoothScrolling() {
        return mLayoutManager.isSmoothScrolling();
    }

    @Override
    public int getLayoutDirection() {
        return mLayoutManager.getLayoutDirection();
    }

    @Override
    public void endAnimation(View view) {
        mLayoutManager.endAnimation(view);
    }

    @Override
    public void addDisappearingView(View child) {
        mLayoutManager.addDisappearingView(child);
    }

    @Override
    public void addDisappearingView(View child, int index) {
        mLayoutManager.addDisappearingView(child, index);
    }

    @Override
    public void addView(View child) {
        mLayoutManager.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        mLayoutManager.addView(child, index);
    }

    @Override
    public void removeView(View child) {
        mLayoutManager.removeView(child);
    }

    @Override
    public void removeViewAt(int index) {
        mLayoutManager.removeViewAt(index);
    }

    @Override
    public void removeAllViews() {
        mLayoutManager.removeAllViews();
    }

    @Override
    public int getBaseline() {
        return mLayoutManager.getBaseline();
    }

    @Override
    public int getPosition(View view) {
        return mLayoutManager.getPosition(view);
    }

    @Override
    public int getItemViewType(View view) {
        return mLayoutManager.getItemViewType(view);
    }

    @Override
    public View findViewByPosition(int position) {
        return mLayoutManager.findViewByPosition(position);
    }

    @Override
    public void detachView(View child) {
        mLayoutManager.detachView(child);
    }

    @Override
    public void detachViewAt(int index) {
        mLayoutManager.detachViewAt(index);
    }

    @Override
    public void attachView(View child, int index, RecyclerView.LayoutParams lp) {
        mLayoutManager.attachView(child, index, lp);
    }

    @Override
    public void attachView(View child, int index) {
        mLayoutManager.attachView(child, index);
    }

    @Override
    public void attachView(View child) {
        mLayoutManager.attachView(child);
    }

    @Override
    public void removeDetachedView(View child) {
        mLayoutManager.removeDetachedView(child);
    }

    @Override
    public void moveView(int fromIndex, int toIndex) {
        mLayoutManager.moveView(fromIndex, toIndex);
    }

    @Override
    public void detachAndScrapView(View child, RecyclerView.Recycler recycler) {
        mLayoutManager.detachAndScrapView(child, recycler);
    }

    @Override
    public void detachAndScrapViewAt(int index, RecyclerView.Recycler recycler) {
        mLayoutManager.detachAndScrapViewAt(index, recycler);
    }

    @Override
    public void removeAndRecycleView(View child, RecyclerView.Recycler recycler) {
        mLayoutManager.removeAndRecycleView(child, recycler);
    }

    @Override
    public void removeAndRecycleViewAt(int index, RecyclerView.Recycler recycler) {
        mLayoutManager.removeAndRecycleViewAt(index, recycler);
    }

    @Override
    public int getChildCount() {
        return mLayoutManager.getChildCount();
    }

    @Override
    public View getChildAt(int index) {
        return mLayoutManager.getChildAt(index);
    }

    @Override
    public int getWidth() {
        return mLayoutManager.getWidth();
    }

    @Override
    public int getHeight() {
        return mLayoutManager.getHeight();
    }

    @Override
    public int getPaddingLeft() {
        return mLayoutManager.getPaddingLeft();
    }

    @Override
    public int getPaddingTop() {
        return mLayoutManager.getPaddingTop();
    }

    @Override
    public int getPaddingRight() {
        return mLayoutManager.getPaddingRight();
    }

    @Override
    public int getPaddingBottom() {
        return mLayoutManager.getPaddingBottom();
    }

    @Override
    public int getPaddingStart() {
        return mLayoutManager.getPaddingStart();
    }

    @Override
    public int getPaddingEnd() {
        return mLayoutManager.getPaddingEnd();
    }

    @Override
    public boolean isFocused() {
        return mLayoutManager.isFocused();
    }

    @Override
    public boolean hasFocus() {
        return mLayoutManager.hasFocus();
    }

    @Override
    public View getFocusedChild() {
        return mLayoutManager.getFocusedChild();
    }

    @Override
    public int getItemCount() {
        return mLayoutManager.getItemCount();
    }

    @Override
    public void offsetChildrenHorizontal(int dx) {
        mLayoutManager.offsetChildrenHorizontal(dx);
    }

    @Override
    public void offsetChildrenVertical(int dy) {
        mLayoutManager.offsetChildrenVertical(dy);
    }

    @Override
    public void ignoreView(View view) {
        mLayoutManager.ignoreView(view);
    }

    @Override
    public void stopIgnoringView(View view) {
        mLayoutManager.stopIgnoringView(view);
    }

    @Override
    public void detachAndScrapAttachedViews(RecyclerView.Recycler recycler) {
        mLayoutManager.detachAndScrapAttachedViews(recycler);
    }

    @Override
    public void measureChild(View child, int widthUsed, int heightUsed) {
        mLayoutManager.measureChild(child, widthUsed, heightUsed);
    }

    @Override
    public void measureChildWithMargins(View child, int widthUsed, int heightUsed) {
        mLayoutManager.measureChildWithMargins(child, widthUsed, heightUsed);
    }

    @Override
    public int getDecoratedMeasuredWidth(View child) {
        return mLayoutManager.getDecoratedMeasuredWidth(child);
    }

    @Override
    public int getDecoratedMeasuredHeight(View child) {
        return mLayoutManager.getDecoratedMeasuredHeight(child);
    }

    @Override
    public void layoutDecorated(View child, int left, int top, int right, int bottom) {
        mLayoutManager.layoutDecorated(child, left, top, right, bottom);
    }

    @Override
    public int getDecoratedLeft(View child) {
        return mLayoutManager.getDecoratedLeft(child);
    }

    @Override
    public int getDecoratedTop(View child) {
        return mLayoutManager.getDecoratedTop(child);
    }

    @Override
    public int getDecoratedRight(View child) {
        return mLayoutManager.getDecoratedRight(child);
    }

    @Override
    public int getDecoratedBottom(View child) {
        return mLayoutManager.getDecoratedBottom(child);
    }

    @Override
    public void calculateItemDecorationsForChild(View child, Rect outRect) {
        mLayoutManager.calculateItemDecorationsForChild(child, outRect);
    }

    @Override
    public int getTopDecorationHeight(View child) {
        return mLayoutManager.getTopDecorationHeight(child);
    }

    @Override
    public int getBottomDecorationHeight(View child) {
        return mLayoutManager.getBottomDecorationHeight(child);
    }

    @Override
    public int getLeftDecorationWidth(View child) {
        return mLayoutManager.getLeftDecorationWidth(child);
    }

    @Override
    public int getRightDecorationWidth(View child) {
        return mLayoutManager.getRightDecorationWidth(child);
    }

    @Override
    @Nullable
    public View onFocusSearchFailed(View focused, int direction, RecyclerView.Recycler recycler,
                                    RecyclerView.State state) {
        return mLayoutManager.onFocusSearchFailed(focused, direction, recycler, state);
    }

    @Override
    public View onInterceptFocusSearch(View focused, int direction) {
        return mLayoutManager.onInterceptFocusSearch(focused, direction);
    }

    @Override
    @Deprecated
    public boolean onRequestChildFocus(RecyclerView parent, View child, View focused) {
        return mLayoutManager.onRequestChildFocus(parent, child, focused);
    }

    @Override
    public boolean onRequestChildFocus(RecyclerView parent, RecyclerView.State state, View child,
                                       View focused) {
        return mLayoutManager.onRequestChildFocus(parent, state, child, focused);
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        mLayoutManager.onAdapterChanged(oldAdapter, newAdapter);
    }

    @Override
    public boolean onAddFocusables(RecyclerView recyclerView, ArrayList<View> views, int
            direction, int focusableMode) {
        return mLayoutManager.onAddFocusables(recyclerView, views, direction, focusableMode);
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        mLayoutManager.onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        mLayoutManager.onItemsAdded(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        mLayoutManager.onItemsRemoved(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
        mLayoutManager.onItemsUpdated(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount,
                               Object payload) {
        mLayoutManager.onItemsUpdated(recyclerView, positionStart, itemCount, payload);
    }

    @Override
    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        mLayoutManager.onItemsMoved(recyclerView, from, to, itemCount);
    }

    @Override
    public int computeHorizontalScrollExtent(RecyclerView.State state) {
        return mLayoutManager.computeHorizontalScrollExtent(state);
    }

    @Override
    public int computeHorizontalScrollOffset(RecyclerView.State state) {
        return mLayoutManager.computeHorizontalScrollOffset(state);
    }

    @Override
    public int computeHorizontalScrollRange(RecyclerView.State state) {
        return mLayoutManager.computeHorizontalScrollRange(state);
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        return mLayoutManager.computeVerticalScrollExtent(state);
    }

    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        return mLayoutManager.computeVerticalScrollOffset(state);
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        return mLayoutManager.computeVerticalScrollRange(state);
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
            int widthSpec, int heightSpec) {
        mLayoutManager.onMeasure(recycler, state, widthSpec, heightSpec);
    }

    @Override
    public void setMeasuredDimension(int widthSize, int heightSize) {
        mLayoutManager.setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    public int getMinimumWidth() {
        return mLayoutManager.getMinimumWidth();
    }

    @Override
    public int getMinimumHeight() {
        return mLayoutManager.getMinimumHeight();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        return mLayoutManager.onSaveInstanceState();
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        mLayoutManager.onRestoreInstanceState(state);
    }

    @Override
    public void onScrollStateChanged(int state) {
        mLayoutManager.onScrollStateChanged(state);
    }

    @Override
    public void removeAndRecycleAllViews(RecyclerView.Recycler recycler) {
        mLayoutManager.removeAndRecycleAllViews(recycler);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(RecyclerView.Recycler recycler,
            RecyclerView.State state, AccessibilityNodeInfoCompat info) {
        mLayoutManager.onInitializeAccessibilityNodeInfo(recycler, state, info);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        mLayoutManager.onInitializeAccessibilityEvent(event);
    }

    @Override
    public void onInitializeAccessibilityEvent(RecyclerView.Recycler recycler,
            RecyclerView.State state, AccessibilityEvent event) {
        mLayoutManager.onInitializeAccessibilityEvent(recycler, state, event);
    }

    @Override
    public void onInitializeAccessibilityNodeInfoForItem(RecyclerView.Recycler recycler,
            RecyclerView.State state, View host, AccessibilityNodeInfoCompat info) {
        mLayoutManager.onInitializeAccessibilityNodeInfoForItem(recycler, state, host, info);
    }

    @Override
    public void requestSimpleAnimationsInNextLayout() {
        mLayoutManager.requestSimpleAnimationsInNextLayout();
    }

    @Override
    public int getSelectionModeForAccessibility(RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        return mLayoutManager.getSelectionModeForAccessibility(recycler, state);
    }

    @Override
    public int getRowCountForAccessibility(RecyclerView.Recycler recycler,
                                           RecyclerView.State state) {
        return mLayoutManager.getRowCountForAccessibility(recycler, state);
    }

    @Override
    public int getColumnCountForAccessibility(RecyclerView.Recycler recycler,
                                              RecyclerView.State state) {
        return mLayoutManager.getColumnCountForAccessibility(recycler, state);
    }

    @Override
    public boolean isLayoutHierarchical(RecyclerView.Recycler recycler, RecyclerView.State state) {
        return mLayoutManager.isLayoutHierarchical(recycler, state);
    }

    @Override
    public boolean performAccessibilityAction(RecyclerView.Recycler recycler,
            RecyclerView.State state, int action, Bundle args) {
        return mLayoutManager.performAccessibilityAction(recycler, state, action, args);
    }

    @Override
    public boolean performAccessibilityActionForItem(RecyclerView.Recycler recycler,
            RecyclerView.State state, View view, int action, Bundle args) {
        return mLayoutManager.performAccessibilityActionForItem(recycler, state, view, action, args);
    }
}
