# AnimatedRecyclerView
Animated extension of RecyclerView widget. Intended for usage with D-pad. To navigate with D-pad, your adapter items must be focusable.

### Newly included features
 - Sticky scrolling which means that 'camera' will be locked on some offset (user provided) from some border.

### Widget attributes
 - `scrollOffsetX` (fraction) - offset from left border as width fraction.
 - `scrollOffsetY` (fraction) - offset from top border as height fraction.

### Example
````
<com.android.vganin.ui.AnimatedRecyclerView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:scrollOffsetX="50%"
        app:scrollOffsetY="50%"/>
 ````
