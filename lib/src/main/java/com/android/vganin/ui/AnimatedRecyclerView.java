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

package com.android.vganin.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

public class AnimatedRecyclerView extends RecyclerView {

    private static final String BOUNDS_PROP_NAME = "bounds";

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

    private int mSelectionDuration = 0;

    private Drawable mBackgroundSelector;

    private Drawable mForegroundSelector;

    private int mNavKeyPressedEventCount = 0;

    private WeakHashMap<Drawable, ObjectAnimator> mSelectorAnimators =
            new WeakHashMap<Drawable, ObjectAnimator>();

    public AnimatedRecyclerView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public AnimatedRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public AnimatedRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public void setSelectionDuration(int duration) {
        mSelectionDuration = duration;
    }

    public int getSelectionDuration() {
        return mSelectionDuration;
    }

    public void setBackgroundSelector(Drawable drawable) {
        mBackgroundSelector = drawable;
    }

    public void setBackgroundSelector(int resId) {
        mBackgroundSelector = getResources().getDrawable(resId, getContext().getTheme());
    }

    public Drawable getBackgroundSelector() {
        return mBackgroundSelector;
    }

    public void setForegroundSelector(Drawable drawable) {
        mForegroundSelector = drawable;
    }

    public void setForegroundSelector(int resId) {
        mForegroundSelector = getResources().getDrawable(resId, getContext().getTheme());
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
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                switch (event.getAction()) {
                    case KeyEvent.ACTION_UP:
                        mNavKeyPressedEventCount = 0;
                        break;
                    case KeyEvent.ACTION_DOWN:
                        mNavKeyPressedEventCount++;
                }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);

        int duration = mNavKeyPressedEventCount < 2
                ? mSelectionDuration : 0;

        Rect selectorRect = new Rect();
        child.getHitRect(selectorRect);
        if (mForegroundSelector != null) {
            animateSelectorChange(mForegroundSelector, selectorRect, duration);
        }
        if (mBackgroundSelector != null) {
            animateSelectorChange(mBackgroundSelector, selectorRect, duration);
        }
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (mBackgroundSelector != null) {
            mBackgroundSelector.draw(canvas);
        }

        super.dispatchDraw(canvas);

        if (mForegroundSelector != null) {
            mForegroundSelector.draw(canvas);
        }
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate) {
        return super.requestChildRectangleOnScreen(child, rect, true); // Always immediate
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
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Failed to apply hack in setLayoutManager(). Perhaps this" +
                    " happened due to RecyclerView has changed or something. Please contact author" +
                    " and report this issue if you want to help.", e);
        }
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AnimatedRecyclerView,
                    defStyle, 0);

            if (ta.hasValue(R.styleable.AnimatedRecyclerView_scrollOffsetX)) {
                mScrollOffsetFractionX = ta.getFraction(
                        R.styleable.AnimatedRecyclerView_scrollOffsetX, 1, 1, 0f);
            }

            if (ta.hasValue(R.styleable.AnimatedRecyclerView_scrollOffsetY)) {
                mScrollOffsetFractionY = ta.getFraction(
                        R.styleable.AnimatedRecyclerView_scrollOffsetY, 1, 1, 0f);
            }

            if (ta.hasValue(R.styleable.AnimatedRecyclerView_backgroundSelector)) {
                setBackgroundSelector(ta.getDrawable(
                        R.styleable.AnimatedRecyclerView_backgroundSelector));
            }

            if (ta.hasValue(R.styleable.AnimatedRecyclerView_foregroundSelector)) {
                setForegroundSelector(ta.getDrawable(
                        R.styleable.AnimatedRecyclerView_foregroundSelector));
            }

            if (ta.hasValue(R.styleable.AnimatedRecyclerView_selectionDuration)) {
                setSelectionDuration(ta.getInt(
                        R.styleable.AnimatedRecyclerView_selectionDuration, 0));
            }

            ta.recycle();
        }
    }

    /**
     * Animates selector {@link Drawable} when changes happen.
     *
     * @param selectorDrawable {@link Drawable} instance represents selector
     * @param dest             destination
     * @param duration         animation duration in ms
     */
    protected void animateSelectorChange(Drawable selectorDrawable, Rect dest, long duration) {
        Rect source = new Rect(selectorDrawable.getBounds());

        ObjectAnimator selectorAnimator;
        if ((selectorAnimator = mSelectorAnimators.get(selectorDrawable)) == null) {
            selectorAnimator = ObjectAnimator.ofObject(
                    selectorDrawable, BOUNDS_PROP_NAME, new RectEvaluator(), source, dest);

            selectorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    invalidate();
                }
            });

            mSelectorAnimators.put(selectorDrawable, selectorAnimator);
        } else {
            selectorAnimator.getValues()[0].setObjectValues(source, dest);
        }

        selectorAnimator.setDuration(duration);
        selectorAnimator.start();
    }
}
