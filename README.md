# AnimatedRecyclerView
Animated extension of RecyclerView widget. Intended for usage with D-pad. To navigate with D-pad, your adapter items must be focusable.

### Newly included features
 - Floating `Drawable` selectors: background and foreground.
 - Sticky scrolling which means that 'camera' will be locked on some offset (user provided) from some border.

### Widget attributes
 - `foregroundSelector` (reference) - drawable resource for foreground floating selector.
 - `backgroundSelector` (reference) - drawable resource for background floating selector.
 - `selectionDuration` (integer) - selector transition duration in milliseconds. Default: 0 ms.
 - `scrollOffsetX` (fraction) - offset from left border as width fraction.
 - `scrollOffsetY` (fraction) - offset from top border as height fraction.

 Note: you can leave any of these attributes unspecified if you don't want this functionality. Without any of these you should get vanilla RecyclerView behavior.

### Example
````
<com.android.vganin.ui.AnimatedRecyclerView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:foregroundSelector="@drawable/selector_item_foreground"
        app:backgroundSelector="@drawable/selector_item_background"
        app:selectionDuration="200"
        app:scrollOffsetX="50%"
        app:scrollOffsetY="50%"/>
 ````
