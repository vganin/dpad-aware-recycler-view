package com.android.vganin.ui.sample;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.TextView;

import com.android.vganin.ui.AnimatedRecyclerView;

public class SampleActivity extends Activity {

    private static final String[] SAMPLE_DATA = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque elementum, nisl vel ornare gravida, leo odio volutpat ex, ac blandit eros eros id ante."
            .split(" ");

    private static final int COLUMNS_NUM = 2;

    private static class RowController extends RecyclerView.ViewHolder
            implements View.OnFocusChangeListener {

        private static final float SCALE_BY_PX = 100;

        private final View vRoot;
        private final TextView vMainText;
        private final TextView vDebugInfo;

        public RowController(View itemView) {
            super(itemView);

            vRoot = itemView;
            vMainText = (TextView) itemView.findViewById(R.id.text);
            vDebugInfo = (TextView) itemView.findViewById(R.id.debug_info);

            vRoot.setOnFocusChangeListener(this);
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                animateSelection();
            } else {
                animateDeselection();
            }
        }

        public void bind(String model) {
            vMainText.setText(model);
        }

        public void updateDebugInfo() {
            vDebugInfo.setText(String.format("left: %s right: %s top: %s bottom: %s",
                    vRoot.getLeft(), vRoot.getRight(), vRoot.getTop(), vRoot.getBottom()));
        }

        private void animateSelection() {
            float translationX = getAdapterPosition() % COLUMNS_NUM == 0
                    ? SCALE_BY_PX / COLUMNS_NUM : - SCALE_BY_PX / COLUMNS_NUM;

            float translationY = 0f;
            int position = getAdapterPosition();
            if (position < COLUMNS_NUM) {
                translationY = SCALE_BY_PX / COLUMNS_NUM;
            } else if (position > SAMPLE_DATA.length - COLUMNS_NUM - 1) {
                translationY = - SCALE_BY_PX / COLUMNS_NUM;
            }

            scaleByPx(SCALE_BY_PX, vRoot, vRoot.animate())
                    .translationX(translationX).translationY(translationY);

            vRoot.setZ(vRoot.getZ() + 1);
        }

        private void animateDeselection() {
            vRoot.animate().scaleX(1f).scaleY(1f).translationX(0f).translationY(0f);
            vRoot.setZ(vRoot.getZ() - 1);
        }

        private ViewPropertyAnimator scaleByPx(float px, View view, ViewPropertyAnimator animator) {
            int width = view.getMeasuredWidth();
            int height = view.getMeasuredHeight();

            float scaleX = (width + px) / width;
            float scaleY = (height + px) / height;

            return animator.scaleX(scaleX).scaleY(scaleY);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        AnimatedRecyclerView list = (AnimatedRecyclerView) findViewById(R.id.list);

        list.setLayoutManager(new GridLayoutManager(this, COLUMNS_NUM));

        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int childCount = recyclerView.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View child = recyclerView.getChildAt(i);
                    RowController rowController = (RowController) recyclerView.getChildViewHolder
                            (child);
                    if (rowController != null) {
                        rowController.updateDebugInfo();
                    }
                }
            }
        });

        list.setAdapter(new RecyclerView.Adapter<RowController>() {

            @Override
            public RowController onCreateViewHolder(ViewGroup parent, int viewType) {
                return new RowController(getLayoutInflater().inflate(R.layout.item, parent, false));
            }

            @Override
            public void onBindViewHolder(RowController holder, int position) {
                holder.bind(getItem(position));
            }

            @Override
            public int getItemCount() {
                return SAMPLE_DATA.length;
            }

            public String getItem(int position) {
                return SAMPLE_DATA[position];
            }
        });
    }
}
