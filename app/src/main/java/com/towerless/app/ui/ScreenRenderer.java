package com.towerless.app.ui;

import android.content.Context;
import android.view.View;

/** Creates a screen view from presentation state; renderers do not access network APIs. */
public interface ScreenRenderer {
    View render(Context context, UiState state);
}
