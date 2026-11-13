package com.towerless.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Minimal rendering scaffold used until each destination is implemented in its planned phase. */
abstract class PlaceholderScreen implements ScreenRenderer {
    private final String title;

    PlaceholderScreen(String title) { this.title = title; }

    @Override public View render(Context context, UiState state) {
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(20 * context.getResources().getDisplayMetrics().density + 0.5f);
        page.setPadding(pad, pad, pad, pad);
        page.setBackgroundColor(Color.rgb(11, 18, 32));
        TextView heading = new TextView(context);
        heading.setText(title);
        heading.setTextSize(22);
        heading.setTextColor(Color.WHITE);
        page.addView(heading);
        TextView status = new TextView(context);
        status.setText("इस स्क्रीन का UI अगले implementation चरण में जोड़ा जाएगा।");
        status.setTextSize(15);
        status.setTextColor(Color.rgb(185, 198, 216));
        page.addView(status);
        return page;
    }
}
