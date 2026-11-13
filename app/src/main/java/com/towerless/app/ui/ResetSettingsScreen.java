package com.towerless.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class ResetSettingsScreen implements ScreenRenderer {
    private final Runnable onConfirmReset;

    public ResetSettingsScreen(Runnable onConfirmReset) {
        this.onConfirmReset = onConfirmReset;
    }

    @Override public View render(Context context, UiState state) {
        ScrollView scroll = new ScrollView(context);
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        int side = dp(context, 18);
        page.setPadding(side, dp(context, 24), side, dp(context, 28));
        page.setBackgroundColor(Color.rgb(11, 18, 32));
        scroll.addView(page);

        TextView title = text(context, "Privacy Reset", 24, Color.WHITE, true);
        page.addView(title);
        page.addView(text(context,
                "यह Dashboard की सहेजी हुई पसंद मिटाएगा और इस session में दिख रहे Wi-Fi तथा Mobile परिणाम साफ करेगा। Android permissions नहीं बदलेंगी।",
                15, Color.rgb(220, 228, 239), false));

        Button reset = new Button(context);
        reset.setText("Preferences और परिणाम मिटाएँ");
        reset.setMinHeight(dp(context, 56));
        page.addView(reset);
        reset.setOnClickListener(view -> new AlertDialog.Builder(context)
                .setTitle("Privacy Reset की पुष्टि")
                .setMessage("सहेजी हुई Dashboard पसंद और अभी दिख रहे network परिणाम मिटाएँ?")
                .setNegativeButton("रद्द करें", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("मिटाएँ", (dialog, which) -> onConfirmReset.run())
                .show());
        return scroll;
    }

    private static TextView text(Context context, String value, int size, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setPadding(0, dp(context, 8), 0, dp(context, 8));
        if (bold) text.setTypeface(null, android.graphics.Typeface.BOLD);
        return text;
    }

    private static int dp(Context context, int value) {
        return (int)(value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
