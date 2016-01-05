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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import static android.support.v7.widget.RecyclerView.NO_ID;
import static android.support.v7.widget.RecyclerView.NO_POSITION;

/**
 * Helper class that manages focus archiving for some RecyclerView.
 */
class FocusArchivist {

    private int mLastSelectedPos = NO_POSITION;
    private long mLastSelectedId = NO_ID;

    /**
     * Remember currently focused entity of the passed RecyclerView. If RecyclerView doesn't contain
     * focus, ignore archiving and stick to the former remembered entity.
     *
     * @param rv Recycler view.
     */
    void archiveFocus(@NonNull RecyclerView rv) {
        if (rv.hasFocus()) {
            View focusedChild = rv.getFocusedChild();
            archiveFocus(rv, focusedChild);
        }
    }

    /**
     * Remember some child of RecyclerView as was being focused.
     *
     * @param rv Recycler view.
     * @param child child of that recycler view.
     */
    void archiveFocus(@NonNull RecyclerView rv, View child) {
        mLastSelectedPos = rv.getChildAdapterPosition(child);
        mLastSelectedId = rv.getChildItemId(child);
    }

    /**
     * Gets last focused view.
     *
     * @param rv Recycler view.
     * @return Last focused view. Returns null if no appropriate view was found.
     */
    @Nullable
    View getLastFocus(@NonNull RecyclerView rv) {
        View lastFocused = findLastFocusedViewById(rv, mLastSelectedId);

        if (lastFocused != null) {
            return lastFocused;
        }

        return findLastFocusedViewByPos(rv, mLastSelectedPos);
    }

    /**
     * Find focusable view by item id.
     *
     * @param rv Recycler view.
     * @param id Item's id.
     * @return View with such id, if selection can be restored on it. Null otherwise.
     */
    private View findLastFocusedViewById(@NonNull RecyclerView rv, long id) {
        RecyclerView.Adapter adapter = rv.getAdapter();
        if (adapter != null && adapter.hasStableIds() && mLastSelectedId != NO_ID) {
            RecyclerView.ViewHolder viewHolder = rv.findViewHolderForItemId(id);
            if (viewHolder != null) {
                viewHolder.itemView.requestFocus();
                return viewHolder.itemView;
            }
        }

        return null;
    }

    /**
     * Find focusable view by item position.
     *
     * @param rv Recycler view.
     * @param pos Item's pos.
     * @return View with such position, if selection can be restored on it. Null otherwise.
     */
    private View findLastFocusedViewByPos(@NonNull RecyclerView rv, int pos) {
        if (pos != NO_POSITION) {
            RecyclerView.ViewHolder viewHolder = rv.findViewHolderForAdapterPosition(pos);
            if (viewHolder != null) {
                return viewHolder.itemView;
            }
        }

        return null;
    }
}
