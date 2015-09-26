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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AnimatedRecyclerView extends RecyclerView {

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

    /**
     * Sets scroll offset from left border.
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

    /**
     * Sets scroll offset from top border.
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

            ta.recycle();
        }
    }
}
