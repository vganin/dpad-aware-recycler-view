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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;

/**
 * RecyclerView adaptation for D-pad.
 */
public class DpadAwareRecyclerView extends RecyclerView implements
        ViewTreeObserver.OnGlobalFocusChangeListener {

    /**
     * Interface definition for a callback to be invoked when an item in this
     * DpadAwareRecyclerView has been clicked.
     */
    public interface OnItemClickListener {
        /**
         * Callback method to be invoked when an item in this DpadAwareRecyclerView has
         * been clicked.
         *
         * @param parent The DpadAwareRecyclerView where the click happened.
         * @param view The view within the DpadAwareRecyclerView that was clicked (this
         * will be a view provided by the adapter)
         * @param position The position of the view in the adapter.
         * @param id The row id of the item that was clicked.
         */
        void onItemClick(DpadAwareRecyclerView parent, View view, int position, long id);
    }

    /**
     * <p>Interface definition for a callback to be invoked when
     * an item in this view has been selected.</p>
     *
     * <p>Note that this interface differs from classic
     * {@link android.widget.AdapterView.OnItemSelectedListener}.
     */
    public interface OnItemSelectedListener {
        /**
         * Will be called when selector arrives at place.
         *
         * @param parent The DpadAwareRecyclerView where the selection happened
         * @param view The view within the DpadAwareRecyclerView that was selected
         * @param position The position of the view in the adapter
         * @param id The row id of the item that is selected
         */
        void onItemSelected(DpadAwareRecyclerView parent, View view, int position, long id);

        /**
         * Will be called immediately after user issues controller command.
         *
         * @param parent The DpadAwareRecyclerView where the selection happened
         * @param view The view within the DpadAwareRecyclerView that was selected
         * @param position The position of the view in the adapter
         * @param id The row id of the item that is selected
         */
        void onItemFocused(DpadAwareRecyclerView parent, View view, int position, long id);
    }

    private static final String BOUNDS_PROP_NAME = "bounds";

    private class LocalAdapterDataObserver extends AdapterDataObserver {

        @Override
        public void onChanged() {
            // Case when adapter hasn't stable ids. Other case is handled natively by RecyclerView.
            if (!getAdapter().hasStableIds()) {
                mPendingSelectionInt = getSelectedItemPosition();
                if (mPendingSelectionInt == NO_POSITION) {
                    mPendingSelectionInt = 0;
                }
            }
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            // Case when adapter hasn't stable ids. Other case is handled natively by RecyclerView.
            if (!getAdapter().hasStableIds()) {
                int selectedPos = getSelectedItemPosition();
                if (selectedPos >= positionStart && selectedPos < positionStart + itemCount) {
                    mPendingSelectionInt = getSelectedItemPosition();
                }
                if (mPendingSelectionInt == NO_POSITION) {
                    mPendingSelectionInt = 0;
                }
            }
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            onItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            int selectedPos = getSelectedItemPosition();
            if (selectedPos >= fromPosition && selectedPos < fromPosition + itemCount) {
                setSelection(selectedPos - fromPosition + toPosition);
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            int selectedPos = getSelectedItemPosition();
            if (selectedPos >= positionStart && selectedPos < positionStart + itemCount) {
                setSelection(selectedPos + itemCount);
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            int selectedPos = getSelectedItemPosition();
            if (selectedPos >= positionStart && selectedPos < positionStart + itemCount) {
                setSelection(positionStart);
            }
        }
    }

    private final class SelectAnimatorListener extends AnimatorListenerAdapter {

        @Nullable View mToSelect;
        @Nullable View mToDeselect;

        @Override
        public void onAnimationStart(Animator animation) {
            if (mToDeselect != null) {
                childSetSelected(mToDeselect, false);
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mToSelect != null) {
                childSetSelected(mToSelect, true);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            onAnimationEnd(animation);
        }
    }

    /**
     * Callback for {@link Drawable} selectors. View must keep this reference in order for
     * {@link java.lang.ref.WeakReference} in selectors to survive.
     *
     * @see Drawable#setCallback(Drawable.Callback)
     */
    private final Drawable.Callback mSelectorCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            invalidate(who.getBounds());
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            getHandler().postAtTime(what, who, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            getHandler().removeCallbacks(what, who);
        }
    };

    private OnItemClickListener mOnItemClickListener;
    private OnItemSelectedListener mOnItemSelectedListener;

    private final AdapterDataObserver mDataObserver = new LocalAdapterDataObserver();

    /**
     * Adapter position that will be selected after certain layout pass.
     */
    private int mPendingSelectionInt = NO_POSITION;

    /**
     * Focus helper.
     */
    private final FocusArchivist mFocusArchivist = new FocusArchivist();

    private boolean mRememberLastFocus = true;

    private final Rect mSelectorSourceRect = new Rect();
    private final Rect mSelectorDestRect = new Rect();
    private final Interpolator mTransitionInterpolator = new LinearInterpolator();
    private final Animator mBackgroundSelectorAnimator = createHollowSelectorAnimator();
    private final Animator mForegroundSelectorAnimator = createHollowSelectorAnimator();

    private AnimatorSet mSelectorAnimator; // Unfortunately cannot be reused

    private final SelectAnimatorListener mReusableSelectListener = new SelectAnimatorListener();

    private int mSelectorVelocity = 0;

    private boolean mSmoothScrolling = false;

    private Drawable mBackgroundSelector;

    private Drawable mForegroundSelector;

    public DpadAwareRecyclerView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public DpadAwareRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public DpadAwareRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DpadAwareRecyclerView,
                    defStyle, 0);

            if (ta.hasValue(R.styleable.DpadAwareRecyclerView_backgroundSelector)) {
                setBackgroundSelector(ta.getDrawable(
                        R.styleable.DpadAwareRecyclerView_backgroundSelector));
            }

            if (ta.hasValue(R.styleable.DpadAwareRecyclerView_foregroundSelector)) {
                setForegroundSelector(ta.getDrawable(
                        R.styleable.DpadAwareRecyclerView_foregroundSelector));
            }

            if (ta.hasValue(R.styleable.DpadAwareRecyclerView_selectorVelocity)) {
                setSelectorVelocity(ta.getInt(
                        R.styleable.DpadAwareRecyclerView_selectorVelocity, 0));
            }

            setSmoothScrolling(ta.getBoolean(
                    R.styleable.DpadAwareRecyclerView_smoothScrolling, false));

            ta.recycle();
        }

        setFocusable(true);
        setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);
        setWillNotDraw(false);
    }

    public void setSelectorVelocity(int velocity) {
        mSelectorVelocity = velocity;
    }

    public int getSelectorVelocity() {
        return mSelectorVelocity;
    }

    public void setSmoothScrolling(boolean smoothScrolling) {
        mSmoothScrolling = smoothScrolling;
    }

    public boolean getSmoothScrolling() {
        return mSmoothScrolling;
    }

    public void setBackgroundSelector(Drawable drawable) {
        mBackgroundSelector = drawable;
        mBackgroundSelectorAnimator.setTarget(mBackgroundSelector);
        setSelectorCallback(mForegroundSelector);
    }

    public void setBackgroundSelector(int resId) {
        setBackgroundSelector(getDrawableResource(resId));
    }

    public Drawable getBackgroundSelector() {
        return mBackgroundSelector;
    }

    public void setForegroundSelector(Drawable drawable) {
        mForegroundSelector = drawable;
        mForegroundSelectorAnimator.setTarget(mForegroundSelector);
        setSelectorCallback(mForegroundSelector);
    }

    public void setForegroundSelector(int resId) {
        setForegroundSelector(getDrawableResource(resId));
    }

    public Drawable getForegroundSelector() {
        return mForegroundSelector;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public OnItemClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    public OnItemSelectedListener getOnItemSelectedListener() {
        return mOnItemSelectedListener;
    }

    /**
     * Get adapter position of item that is currently focused/selected.
     *
     * @return Selected item's adapter position.
     */
    public int getSelectedItemPosition() {
        View focusedChild = getFocusedChild();
        return getChildAdapterPosition(focusedChild);
    }

    /**
     * Set adapter position for item to select if RecycleView currently has focus or schedule
     * selection on next focus obtainment.
     *
     * @param adapterPosition Adapter position of item to be selected.
     */
    public void setSelection(int adapterPosition) {
        scrollToPosition(adapterPosition);
        mPendingSelectionInt = adapterPosition;
    }

    /**
     * Get flag indicating that last focused view should be remembered in order to re-focus
     * it in future.
     *
     * @return true if focus remembering is enabled, otherwise disable.
     */
    public boolean isRememberLastFocus() {
        return mRememberLastFocus;
    }

    /**
     * Set flag indicating that last focused view should be remembered in order to re-focus
     * it in future.
     *
     * @param rememberLastFocus true to enable focus remembering, otherwise disable.
     */
    public void setRememberLastFocus(boolean rememberLastFocus) {
        mRememberLastFocus = rememberLastFocus;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setDescendantFocusability(enabled ? FOCUS_BEFORE_DESCENDANTS : FOCUS_BLOCK_DESCENDANTS);
        setFocusable(enabled);
    }

    @Override
    public void setAdapter(Adapter newAdapter) {
        Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(mDataObserver);
        }

        super.setAdapter(newAdapter);

        if (newAdapter != null) {
            newAdapter.registerAdapterDataObserver(mDataObserver);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (mPendingSelectionInt != NO_POSITION) {
            setSelectionOnLayout(mPendingSelectionInt);
            mPendingSelectionInt = NO_POSITION;
        }
    }

    private void setSelectionOnLayout(int position) {
        RecyclerView.ViewHolder holder = findViewHolderForAdapterPosition(position);

        if (holder != null) {
            if (hasFocus()) {
                holder.itemView.requestFocus();
            } else {
                mFocusArchivist.archiveFocus(this, holder.itemView);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        ViewTreeObserver obs = getViewTreeObserver();
        obs.addOnGlobalFocusChangeListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        ViewTreeObserver obs = getViewTreeObserver();
        obs.removeOnGlobalFocusChangeListener(this);
    }

    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
        // FIXME: Parent view will get focus and immediately lose it in favor of some child.
        // So we actually can't enforce selectors visibility solely by placing this
        // in onFocusChanged(). Hence we handle it this way.
        enforceSelectorsVisibility(isInTouchMode(), hasFocus());
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (gainFocus) {
            // We favor natural focus if we don't want to remember focus AND if previously focused
            // rectangle is NOT null. Usually latter condition holds true if simple requestFocus()
            // is called and holds false if focus changed during navigation. Overall this is done
            // because of backward compatibility - some code is dependent on the fact that
            // focused position will be restored if requestFocus() is called and it expects
            // to have natural focus when ordinary navigation happens.
            boolean favorNaturalFocus = !mRememberLastFocus && previouslyFocusedRect != null;
            View lastFocusedView = mFocusArchivist.getLastFocus(this);
            if (favorNaturalFocus || lastFocusedView == null) {
                requestNaturalFocus(direction, previouslyFocusedRect);
            } else {
                lastFocusedView.requestFocus();
            }
        }
    }

    /**
     * Request natural focus.
     *
     * @param direction direction in which focus is changing.
     * @param previouslyFocusedRect previously focus rectangle.
     */
    private void requestNaturalFocus(int direction, Rect previouslyFocusedRect) {
        FocusFinder ff = FocusFinder.getInstance();
        previouslyFocusedRect = previouslyFocusedRect == null
                ? new Rect(0, 0, 0, 0) : previouslyFocusedRect;
        View toFocus = ff.findNextFocusFromRect(this, previouslyFocusedRect, direction);
        toFocus = toFocus == null ? getChildAt(0) : toFocus;
        if (toFocus != null) {
            toFocus.requestFocus();
        }
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);

        requestChildFocusInner(child, focused);
        fireOnItemFocusedEvent(child);
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);

        if (state == SCROLL_STATE_IDLE) {
            View focusedChild = getFocusedChild();
            requestChildFocusInner(focusedChild, focusedChild);
        }
    }

    private void requestChildFocusInner(View child, View focused) {
        Drawable refSelector = mBackgroundSelector != null
                ? mBackgroundSelector : mForegroundSelector;

        int scrollState = getScrollState();

        if (refSelector != null && scrollState == SCROLL_STATE_IDLE) {
            mSelectorSourceRect.set(refSelector.getBounds());

            // Focused cannot be null
            focused.getHitRect(mSelectorDestRect);

            mReusableSelectListener.mToSelect = child;
            mReusableSelectListener.mToDeselect = mFocusArchivist.getLastFocus(this);

            animateSelectorChange(mReusableSelectListener);

            mFocusArchivist.archiveFocus(this, child);
        }
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        if (mBackgroundSelector != null && mBackgroundSelector.isVisible()) {
            mBackgroundSelector.draw(canvas);
        }

        super.onDraw(canvas);

        if (mForegroundSelector != null && mForegroundSelector.isVisible()) {
            mForegroundSelector.draw(canvas);
        }
    }

    /**
     * Animates selector {@link Drawable} when changes happen.
     */
    private void animateSelectorChange(Animator.AnimatorListener listener) {
        if (mSelectorAnimator != null) {
            mSelectorAnimator.cancel();
        }

        mSelectorAnimator = new AnimatorSet();

        if (mForegroundSelector != null) {
            mSelectorAnimator.playTogether(mForegroundSelectorAnimator);
        }

        if (mBackgroundSelector != null) {
            mSelectorAnimator.playTogether(mBackgroundSelectorAnimator);
        }

        mSelectorAnimator.setInterpolator(mTransitionInterpolator);
        mSelectorAnimator.addListener(listener);

        int duration = 0;
        if (mSelectorVelocity > 0) {
            int dx = mSelectorDestRect.centerX() - mSelectorSourceRect.centerX();
            int dy = mSelectorDestRect.centerY() - mSelectorSourceRect.centerY();
            duration = computeTravelDuration(dx, dy, mSelectorVelocity);
        }

        mSelectorAnimator.setDuration(duration);
        mSelectorAnimator.start();
    }

    private Animator createHollowSelectorAnimator() {
        return ObjectAnimator.ofObject(
                null, BOUNDS_PROP_NAME, new RectEvaluator(),
                mSelectorSourceRect, mSelectorDestRect);
    }

    private int computeTravelDuration(int dx, int dy, int velocity) {
        return (int) (Math.sqrt(dx * dx + dy * dy) / velocity * 1000);
    }

    private void enforceSelectorsVisibility(boolean isInTouchMode, boolean hasFocus) {
        boolean visible = !isInTouchMode && hasFocus;

        if (mBackgroundSelector != null) mBackgroundSelector.setVisible(visible, false);
        if (mForegroundSelector != null) mForegroundSelector.setVisible(visible, false);
    }

    private Drawable getDrawableResource(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return getResources().getDrawable(resId, getContext().getTheme());
        } else {
            return getResources().getDrawable(resId);
        }
    }

    private void setSelectorCallback(Drawable selector) {
        if (selector != null) {
            selector.setCallback(mSelectorCallback);
        }
    }

    private void childSetSelected(View child, boolean selected) {
        child.setSelected(selected);

        if (selected) {
            fireOnItemSelectedEvent(child);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean consumed = super.dispatchKeyEvent(event);

        View focusedChild = getFocusedChild();

        if (focusedChild != null
                && mOnItemClickListener != null
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                && event.getRepeatCount() == 0) {
            fireOnItemClickEvent(focusedChild);
        }

        return consumed;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        if (mOnItemClickListener != null) {
            child.setClickable(true);
        }
    }

    @Override
    public void getFocusedRect(Rect r) {
        getDrawingRect(r);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        // Allow focus on children only if focus is already in this view
        if (hasFocus()) {
            super.addFocusables(views, direction, focusableMode);
        } else if (isFocusable()) {
            views.add(this);
        }
    }

    private void fireOnItemClickEvent(View child) {
        if (mOnItemClickListener != null) {
            int position = getChildAdapterPosition(child);
            long id = getChildItemId(child);
            mOnItemClickListener.onItemClick(this, child, position, id);
        }
    }

    private void fireOnItemFocusedEvent(View child) {
        if (mOnItemSelectedListener != null) {
            int position = getChildAdapterPosition(child);
            long id = getChildItemId(child);
            mOnItemSelectedListener.onItemFocused(this, child, position, id);
        }
    }

    private void fireOnItemSelectedEvent(View child) {
        if (mOnItemSelectedListener != null) {
            int position = getChildAdapterPosition(child);
            long id = getChildItemId(child);
            mOnItemSelectedListener.onItemSelected(this, child, position, id);
        }
    }
}
