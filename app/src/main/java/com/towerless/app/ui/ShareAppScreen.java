package com.towerless.app.ui;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class ShareAppScreen implements ScreenRenderer {
    private static final String APP_NAME = "TOWERLESS";
    private static final String PROJECT_DESCRIPTION =
            "Wi-Fi और Cellular signal की उपलब्ध जानकारी दिखाने वाला Android ऐप।";

    @Override public View render(Context context, UiState state) {
        ScrollView scroll = new ScrollView(context);
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        int side = dp(context, 18);
        page.setPadding(side, dp(context, 24), side, dp(context, 24));
        page.setBackgroundColor(Color.rgb(11, 18, 32));
        scroll.addView(page);

        addText(context, page, "TOWERLESS ऐप साझा करें", 24, Color.WHITE, true);
        addText(context, page,
                "Android Sharesheet से WhatsApp, Telegram या आपके फोन में उपलब्ध किसी अन्य ऐप को चुनें।",
                16, Color.rgb(205, 215, 229), false);

        String projectUrl = OfficialLinks.PROJECT_URL;
        boolean available = OfficialLinks.isValidProjectUrl(projectUrl);
        TextView preview = addText(context, page,
                APP_NAME + "\n" + PROJECT_DESCRIPTION + (available ? "\n" + projectUrl : ""),
                15, available ? Color.rgb(185, 198, 216) : 0xFFFFDFA8, false);
        preview.setTextIsSelectable(available);

        TextView status = addText(context, page,
                available ? "" : "आधिकारिक Project URL उपलब्ध नहीं है; केवल नाम और विवरण साझा होंगे।",
                14, available ? Color.rgb(72, 214, 181) : 0xFFFFDFA8, false);
        Button shareButton = actionButton(context, "ऐप की जानकारी साझा करें");
        shareButton.setContentDescription(available
                ? "Android Sharesheet खोलकर TOWERLESS, विवरण और Project URL साझा करें"
                : "Android Sharesheet पर TOWERLESS का नाम और विवरण साझा करें");
        shareButton.setOnClickListener(view -> shareProject(context, projectUrl, status));
        page.addView(shareButton);

        Button copyButton = actionButton(context, "Project Link कॉपी करें");
        copyButton.setEnabled(available);
        copyButton.setOnClickListener(view -> copyProjectUrl(context, projectUrl, status));
        page.addView(copyButton);
        return scroll;
    }

    private static void shareProject(Context context, String projectUrl, TextView status) {
        boolean includeUrl = OfficialLinks.isValidProjectUrl(projectUrl);
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        String sharedContent = APP_NAME + "\n" + PROJECT_DESCRIPTION;
        if (includeUrl) sharedContent += "\n" + projectUrl;
        sendIntent.putExtra(Intent.EXTRA_TEXT, sharedContent);
        if (!includeUrl) status.setText("आधिकारिक Project URL उपलब्ध नहीं है; केवल नाम और विवरण साझा किए गए।");
        try {
            context.startActivity(Intent.createChooser(sendIntent, "TOWERLESS साझा करें"));
        } catch (ActivityNotFoundException e) {
            status.setText("साझा करने के लिए कोई compatible ऐप उपलब्ध नहीं है।");
        }
    }

    private static void copyProjectUrl(Context context, String projectUrl, TextView status) {
        if (!OfficialLinks.isValidProjectUrl(projectUrl)) {
            status.setText("आधिकारिक Project URL उपलब्ध नहीं है।");
            return;
        }
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            status.setText("लिंक कॉपी नहीं हो सका।");
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("TOWERLESS Project Link", projectUrl));
        status.setText("Project Link कॉपी हो गया।");
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
