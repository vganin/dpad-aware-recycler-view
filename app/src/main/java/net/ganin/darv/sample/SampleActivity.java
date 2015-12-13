package net.ganin.darv.sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.ganin.darv.DpadAwareRecyclerView;
import net.ganin.darv.GridLayoutManager;

public class SampleActivity extends Activity {

    private static final String[] SAMPLE_DATA = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
            .split(" ");

    private static class RowController extends RecyclerView.ViewHolder {

        private final View vRoot;
        private final TextView vMainText;
        private final TextView vDebugInfo;

        public RowController(View itemView) {
            super(itemView);

            vRoot = itemView;
            vMainText = (TextView) itemView.findViewById(R.id.text);
            vDebugInfo = (TextView) itemView.findViewById(R.id.debug_info);
        }

        public void bind(String model) {
            vMainText.setText(model);
        }

        public void updateDebugInfo() {
            vDebugInfo.setText(String.format("left: %s right: %s top: %s bottom: %s",
                    vRoot.getLeft(), vRoot.getRight(), vRoot.getTop(), vRoot.getBottom()));
        }
    }

    private TextView mInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        mInfo = (TextView) findViewById(R.id.info);

        prepareFirstList((DpadAwareRecyclerView) findViewById(R.id.list1));
        prepareSecondList((DpadAwareRecyclerView) findViewById(R.id.list2));
    }

    private void prepareFirstList(DpadAwareRecyclerView list) {
        list.setLayoutManager(new GridLayoutManager(this, 3));

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

        final int factor = 1;
        final int sampleLength = SAMPLE_DATA.length;
        final String[] bigSampleData = new String[sampleLength * factor];
        for (int i = 0; i < factor; i++) {
            System.arraycopy(SAMPLE_DATA, 0, bigSampleData, i * sampleLength, sampleLength);
        }

        list.setAdapter(new RecyclerView.Adapter<RowController>() {

            @Override
            public RowController onCreateViewHolder(ViewGroup parent, int viewType) {
                return new RowController(getLayoutInflater().inflate(R.layout.item1, parent, false));
            }

            @Override
            public void onBindViewHolder(RowController holder, int position) {
                holder.bind(getItem(position));
            }

            @Override
            public int getItemCount() {
                return bigSampleData.length;
            }

            public String getItem(int position) {
                return bigSampleData[position];
            }
        });

        list.setOnItemSelectedListener(new DpadAwareRecyclerView.OnItemSelectedListener() {
            @SuppressLint("SetTextI18n") // Allow for testing
            @Override
            public void onItemSelected(DpadAwareRecyclerView parent, View view, int position,
                    long id) {
                mInfo.setText("Item " + position + " with id " + id + " in first list selected");
            }

            @Override
            public void onItemFocused(DpadAwareRecyclerView parent, View view, int position,
                    long id) {
                // Not interested
            }
        });

        list.setOnItemClickListener(new DpadAwareRecyclerView.OnItemClickListener() {
            @SuppressLint("SetTextI18n") // Allow for testing
            @Override
            public void onItemClick(DpadAwareRecyclerView parent, View view, int position,
                    long id) {
                mInfo.setText("Item " + position + " with id " + id + " in first list clicked");
            }
        });
    }

    private void prepareSecondList(DpadAwareRecyclerView list) {
        list.setLayoutManager(new GridLayoutManager(this, 1, LinearLayoutManager.HORIZONTAL, false));

        list.setAdapter(new RecyclerView.Adapter<RowController>() {

            @Override
            public RowController onCreateViewHolder(ViewGroup parent, int viewType) {
                return new RowController(
                        getLayoutInflater().inflate(R.layout.item2, parent, false));
            }

            @Override
            public void onBindViewHolder(RowController holder, int position) {
                // All data is static
            }

            @Override
            public int getItemCount() {
                return 100;
            }
        });

        list.setOnItemSelectedListener(new DpadAwareRecyclerView.OnItemSelectedListener() {
            @SuppressLint("SetTextI18n") // Allow for testing
            @Override
            public void onItemSelected(DpadAwareRecyclerView parent, View view, int position,
                    long id) {
                mInfo.setText("Item " + position + " with id " + id + " in second list selected");
            }

            @Override
            public void onItemFocused(DpadAwareRecyclerView parent, View view, int position,
                    long id) {
                // Not interested
            }
        });

        list.setOnItemClickListener(new DpadAwareRecyclerView.OnItemClickListener() {
            @SuppressLint("SetTextI18n") // Allow for testing
            @Override
            public void onItemClick(DpadAwareRecyclerView parent, View view, int position,
                    long id) {
                mInfo.setText("Item " + position + " with id " + id + " in second list clicked");
            }
        });
    }
}
