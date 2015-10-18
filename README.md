# DpadAwareRecyclerView
D-pad friendly extension of RecyclerView widget. In order to enable D-pad navigation you must make your items focusable.

### Newly included features
 - Floating `Drawable` selectors: background and foreground.
 - Sticky scrolling which means that 'camera' will be locked on some offset (user provided) from some border.

### Widget attributes
 - `foregroundSelector` (reference) - drawable resource for foreground floating selector.
 - `backgroundSelector` (reference) - drawable resource for background floating selector.
 - `selectorVelocity` (integer) - selector transition velocity in px/sec. When less or equals to 0, transition duration will always be immediate. Default: 0 px/sec.
 - `smoothScrolling` (boolean) - sets smooth scrolling on or off.
 - `scrollOffsetX` (fraction) - offset from left border as width fraction.
 - `scrollOffsetY` (fraction) - offset from top border as height fraction.

Note: you can leave any of these attributes unspecified if you don't want this functionality. Without any of these you should get vanilla RecyclerView behavior (almost).

### Example
````
<net.ganin.darv.DpadAwareRecyclerView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:foregroundSelector="@drawable/selector_item_foreground"
        app:backgroundSelector="@drawable/selector_item_background"
        app:selectorVelocity="1000"
        app:smoothScrolling="true"
        app:scrollOffsetX="50%"
        app:scrollOffsetY="50%"/>
 ````
