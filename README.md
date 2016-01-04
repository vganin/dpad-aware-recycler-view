# DpadAwareRecyclerView
Collection of useful RecyclerView functionality for D-pad environments.

 - `DpadAwareRecyclerView`, D-pad friendly extension of RecyclerView.
   - Many bugfixes and workarounds for common RecyclerView issues in D-pad environment.
   - Floating `Drawable` selectors with tunable velocity.
   - Classic `OnItemClickListener` and `OnItemSelectedListener` emulations.
   - Classic `setSelection()` and `getSelectedItemPosition()` emulations.
   - Classic `setEnabled()` emulation.
   - Choose between remembering focus and focus natural flow via `setRememberLastFocus()` method.
 - `ExtGridLayoutManager`, extension of `GridLayoutManager`.
   - Beautiful `Builder` for construction.
   - Camera offset for selection. E.g. for `0.5` selected view will be centered.
   - Circular navigation.
   - `AUTO_FIT` support similar to GridView.
   - 'Arrow' views (any views really) indicating begin or end is reached.

### `DpadAwareRecyclerView` attributes
 - `foregroundSelector` (reference) - drawable resource for foreground floating selector.
 - `backgroundSelector` (reference) - drawable resource for background floating selector.
 - `selectorVelocity` (integer) - selector transition velocity in px/sec. When less or equals to 0, transition duration will always be immediate. Default: 0 px/sec.
 - `smoothScrolling` (boolean) - sets smooth scrolling on or off.

Note: you can leave any of these attributes unspecified if you don't want this functionality. Without any of these you should get vanilla RecyclerView behavior (almost).

### Example
````
<net.ganin.darv.DpadAwareRecyclerView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:foregroundSelector="@drawable/selector_item_foreground"
        app:backgroundSelector="@drawable/selector_item_background"
        app:selectorVelocity="1000"
        app:smoothScrolling="true">
 ````

### Note to contributors
  Feel free to propose additional functionality, bugfixes, documentation enhancements, etc. through pull requests or issues.

  General long-term purpose of this library is to become the largest collection of useful functionality and workarounds for TV and other D-pad related devices. So it is appreciated to contribute not only to existing components but to add a new ones that are highly demanded in D-pad environments.
