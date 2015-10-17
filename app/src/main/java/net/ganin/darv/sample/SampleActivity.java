package net.ganin.darv.sample;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.ganin.darv.DpadAwareRecyclerView;
import net.ganin.darv.GridLayoutManager;

public class SampleActivity extends Activity {

    private static final String[] SAMPLE_DATA = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
            .split(" ");

    private static final int COLUMNS_NUM = 3;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        DpadAwareRecyclerView list = (DpadAwareRecyclerView) findViewById(R.id.list);

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
