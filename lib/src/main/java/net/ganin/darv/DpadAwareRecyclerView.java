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

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;

import java.lang.reflect.Method;
import java.util.WeakHashMap;

public class DpadAwareRecyclerView extends RecyclerView
        implements ViewTreeObserver.OnTouchModeChangeListener {

    private static final String BOUNDS_PROP_NAME = "bounds";

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

    private final Rect mSelectorDestRect = new Rect();

    /**
     * Fraction of parent size which always will be offset from left border to currently focused
     * item view center.
     */
    private Float mScrollOffsetFractionX;

    /**
     * Fraction of parent size which always will be offset from top border to currently focused
     * item view center.
     */
    private Float mScrollOffsetFractionY;

    private int mSelectorVelocity = 0;

    private Boolean mSmoothScrolling;

    private Drawable mBackgroundSelector;

    private Drawable mForegroundSelector;

    private WeakHashMap<Drawable, ObjectAnimator> mSelectorAnimators =
            new WeakHashMap<Drawable, ObjectAnimator>();

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
        setSelectorCallback(mForegroundSelector);
    }

    public void setForegroundSelector(int resId) {
        setForegroundSelector(getDrawableResource(resId));
    }

    public Drawable getForegroundSelector() {
        return mForegroundSelector;
    }

    /**
     * Sets scroll offset from left border. Pass <code>null</code> to disable feature.
     *
     * @param scrollOffsetFraction scroll offset from left border
     */
    public void setScrollOffsetFractionX(Float scrollOffsetFraction) {
        mScrollOffsetFractionX = scrollOffsetFraction;

        LayoutManagerDecorator layoutWrapper = (LayoutManagerDecorator) getLayoutManager();
        if (layoutWrapper != null) {
            layoutWrapper.setScrollOffsetFractionX(mScrollOffsetFractionX);
        }
    }

    public Float getScrollOffsetFractionX() {
        return mScrollOffsetFractionX;
    }

    /**
     * Sets scroll offset from top border. Pass <code>null</code> to disable feature.
     *
     * @param scrollOffsetFraction scroll offset from top border
     */
    public void setScrollOffsetFractionY(Float scrollOffsetFraction) {
        mScrollOffsetFractionY = scrollOffsetFraction;

        LayoutManagerDecorator layoutWrapper = (LayoutManagerDecorator) getLayoutManager();
        if (layoutWrapper != null) {
            layoutWrapper.setScrollOffsetFractionY(mScrollOffsetFractionY);
        }
    }

    public Float getScrollOffsetFractionY() {
        return mScrollOffsetFractionY;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        ViewTreeObserver obs = getViewTreeObserver();
        if (obs != null) {
            obs.addOnTouchModeChangeListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        ViewTreeObserver obs = getViewTreeObserver();
        if (obs != null) {
            obs.removeOnTouchModeChangeListener(this);
        }
    }

    @Override
    public void onTouchModeChanged(boolean isInTouchMode) {
        enforceSelectorsVisibility(isInTouchMode, hasFocus());
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        enforceSelectorsVisibility(isInTouchMode(), hasFocus());

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);

        child.getHitRect(mSelectorDestRect);

        if (mForegroundSelector != null) {
            animateSelectorChange(mForegroundSelector);
        }
        if (mBackgroundSelector != null) {
            animateSelectorChange(mBackgroundSelector);
        }
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (mBackgroundSelector != null && mBackgroundSelector.isVisible()) {
            mBackgroundSelector.draw(canvas);
        }

        super.dispatchDraw(canvas);

        if (mForegroundSelector != null && mForegroundSelector.isVisible()) {
            mForegroundSelector.draw(canvas);
        }
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate) {
        return super.requestChildRectangleOnScreen(child, rect,
                mSmoothScrolling == null ? immediate : !mSmoothScrolling);
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        super.setLayoutManager(new LayoutManagerDecorator(layout,
                mScrollOffsetFractionX, mScrollOffsetFractionY));

        // Because RecyclerView instance is supplied to LayoutManager in super.setLayoutManager()
        // and we passed our wrapper, original LayoutManager didn't get its reference to
        // RecyclerView. Plus method setRecyclerView() is package-protected so we have to apply
        // this dirty hack.
        try {
            Method method = LayoutManager.class.getDeclaredMethod("setRecyclerView", RecyclerView
                    .class);
            method.setAccessible(true);
            method.invoke(layout, this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply hack in setLayoutManager(). Perhaps this" +
                    " happened due to RecyclerView has changed or something. Please contact author" +
                    " and report this issue if you want to help.", e);
        }
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DpadAwareRecyclerView,
                    defStyle, 0);

            if (ta.hasValue(R.styleable.DpadAwareRecyclerView_scrollOffsetX)) {
                mScrollOffsetFractionX = ta.getFraction(
                        R.styleable.DpadAwareRecyclerView_scrollOffsetX, 1, 1, 0f);
            }

            if (ta.hasValue(R.styleable.DpadAwareRecyclerView_scrollOffsetY)) {
                mScrollOffsetFractionY = ta.getFraction(
                        R.styleable.DpadAwareRecyclerView_scrollOffsetY, 1, 1, 0f);
            }

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

            if (ta.hasValue(R.styleable.DpadAwareRecyclerView_smoothScrolling)) {
                setSmoothScrolling(ta.getBoolean(
                        R.styleable.DpadAwareRecyclerView_smoothScrolling, true));
            }

            ta.recycle();
        }
    }

    /**
     * Animates selector {@link Drawable} when changes happen.
     *
     * @param selectorDrawable {@link Drawable} instance represents selector
     */
    private void animateSelectorChange(final Drawable selectorDrawable) {
        Rect source = new Rect(selectorDrawable.getBounds());

        int duration = 0;
        if (mSelectorVelocity > 0) {
            int dx = mSelectorDestRect.centerX() - source.centerX();
            int dy = mSelectorDestRect.centerY() - source.centerY();
            duration = (int) (Math.sqrt(dx * dx + dy * dy) / mSelectorVelocity * 1000);
        }

        ObjectAnimator selectorAnimator;
        if ((selectorAnimator = mSelectorAnimators.get(selectorDrawable)) == null) {
            selectorAnimator = ObjectAnimator.ofObject(
                    selectorDrawable, BOUNDS_PROP_NAME, new RectEvaluator(), source, mSelectorDestRect);

            selectorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    invalidate(selectorDrawable.getBounds());
                }
            });

            selectorAnimator.setInterpolator(new LinearInterpolator());

            mSelectorAnimators.put(selectorDrawable, selectorAnimator);
        } else {
            selectorAnimator.setObjectValues(source, mSelectorDestRect);
        }

        selectorAnimator.setDuration(duration);
        selectorAnimator.start();
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
}
