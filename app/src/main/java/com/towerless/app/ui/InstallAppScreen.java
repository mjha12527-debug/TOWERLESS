package com.towerless.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class InstallAppScreen implements ScreenRenderer {
    @Override public View render(Context context, UiState state) {
        ScrollView scroll = new ScrollView(context);
        LinearLayout page = page(context);
        scroll.addView(page);

        addText(context, page, "TOWERLESS ऐप इंस्टॉल करें", 24, Color.WHITE, true);
        addText(context, page,
                "TOWERLESS उपलब्ध Wi-Fi और Cellular signal जानकारी दिखाता है। Android और फोन की सीमाओं के कारण कुछ जानकारी उपलब्ध न हो सकती है।",
                16, Color.rgb(205, 215, 229), false);

        String releaseUrl = OfficialLinks.RELEASE_URL;
        boolean available = OfficialLinks.isValidReleaseUrl(releaseUrl);
        TextView releaseStatus = addText(context, page,
                available ? "आधिकारिक Release Page:\n" + releaseUrl
                        : "आधिकारिक GitHub Release URL उपलब्ध नहीं है या मान्य नहीं है। डाउनलोड विकल्प बंद है।",
                15, available ? Color.rgb(185, 198, 216) : 0xFFFFDFA8, false);
        releaseStatus.setTextIsSelectable(available);

        Button downloadButton = actionButton(context, "आधिकारिक Release Page खोलें");
        downloadButton.setEnabled(available);
        downloadButton.setContentDescription(available
                ? "आधिकारिक Release Page ब्राउज़र में खोलें"
                : "आधिकारिक Release URL उपलब्ध नहीं; बटन बंद है");
        downloadButton.setOnClickListener(view -> openOfficialRelease(context, releaseUrl, releaseStatus));
        page.addView(downloadButton);

        addText(context, page,
                "यह बटन केवल आधिकारिक Release Page ब्राउज़र में खोलता है। ऐप APK डाउनलोड या अपने-आप install नहीं करता।",
                14, Color.rgb(158, 172, 192), false);
        return scroll;
    }

    private static void openOfficialRelease(Context context, String releaseUrl, TextView status) {
        if (!OfficialLinks.isValidReleaseUrl(releaseUrl)) {
            status.setText("आधिकारिक Release URL उपलब्ध नहीं है।");
            return;
        }
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl));
            browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
            context.startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            status.setText("इस लिंक को खोलने के लिए कोई ब्राउज़र उपलब्ध नहीं है।");
        }
    }

    private static LinearLayout page(Context context) {
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        int side = dp(context, 18);
        page.setPadding(side, dp(context, 24), side, dp(context, 24));
        page.setBackgroundColor(Color.rgb(11, 18, 32));
        return page;
    }

    private static TextView addText(Context context, LinearLayout parent, String value,
                                    int size, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setPadding(0, dp(context, 8), 0, dp(context, 8));
        if (bold) text.setTypeface(null, android.graphics.Typeface.BOLD);
        parent.addView(text);
        return text;
    }

    private static Button actionButton(Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setMinHeight(dp(context, 56));
        button.setSingleLine(false);
        button.setMaxLines(2);
        return button;
    }

    private static int dp(Context context, int value) {
        return (int)(value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
