package com.towerless.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Shows cached results and shares only fields explicitly selected with consent. */
public final class ShareResultsPreviewScreen implements ScreenRenderer {
    private final String wifiResult;
    private final String mobileResult;
    private final String internetResult;
    private final String recommendation;

    public ShareResultsPreviewScreen(String wifiResult, String mobileResult,
                                     String internetResult, String recommendation) {
        this.wifiResult = safe(wifiResult);
        this.mobileResult = safe(mobileResult);
        this.internetResult = safe(internetResult);
        this.recommendation = safe(recommendation);
    }

    @Override public View render(Context context, UiState state) {
        ScrollView scroll = new ScrollView(context);
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        int side = dp(context, 18);
        page.setPadding(side, dp(context, 24), side, dp(context, 24));
        page.setBackgroundColor(Color.rgb(11, 18, 32));
        scroll.addView(page);

        addText(context, page, "साझा करने से पहले परिणाम देखें", 24, Color.WHITE, true);
        addText(context, page,
                "केवल चुनी हुई जानकारी साझा होगी। सुझाव में नेटवर्क का प्रकार और तुलना का कारण होगा; SSID, ऑपरेटर का नाम या cell पहचान नहीं। यहाँ पुराना उपलब्ध data दिखता है; कोई नया scan या network check नहीं चलता।",
                15, Color.rgb(205, 215, 229), false);

        List<CheckBox> selectedFields = new ArrayList<>();
        addField(context, page, selectedFields, "Wi-Fi परिणाम", wifiResult);
        addField(context, page, selectedFields, "Mobile परिणाम", mobileResult);
        addField(context, page, selectedFields, "Internet स्थिति", internetResult);
        addField(context, page, selectedFields, "सुझाव", recommendation);

        CheckBox consent = new CheckBox(context);
        consent.setText("मैंने ऊपर चुनी हुई जानकारी देखी है और इसे साझा करने की सहमति देता/देती हूँ।");
        consent.setTextColor(Color.WHITE);
        consent.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF48D6B5));
        page.addView(consent);

        TextView status = addText(context, page, "", 14, 0xFFFFDFA8, false);
        Button share = new Button(context);
        share.setText("चुनी हुई जानकारी साझा करें");
        share.setMinHeight(dp(context, 56));
        share.setEnabled(false);
        page.addView(share);

        Runnable updateEnabled = () -> {
            boolean anySelected = false;
            for (CheckBox field : selectedFields) anySelected |= field.isChecked();
            share.setEnabled(consent.isChecked() && anySelected);
        };
        consent.setOnCheckedChangeListener((button, checked) -> updateEnabled.run());
        for (CheckBox field : selectedFields) {
            field.setOnCheckedChangeListener((button, checked) -> {
                if (consent.isChecked()) consent.setChecked(false);
                updateEnabled.run();
            });
        }
        share.setOnClickListener(view -> shareSelected(context, selectedFields, consent, status));
        return scroll;
    }

    private static void addField(Context context, LinearLayout page, List<CheckBox> fields,
                                 String label, String value) {
        CheckBox field = new CheckBox(context);
        field.setText(label + "\n" + value);
        field.setTextColor(Color.rgb(220, 228, 239));
        field.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF48D6B5));
        field.setPadding(0, dp(context, 6), 0, dp(context, 6));
        field.setMinHeight(dp(context, 56));
        page.addView(field);
        fields.add(field);
    }

    private static void shareSelected(Context context, List<CheckBox> fields,
                                      CheckBox consent, TextView status) {
        if (!consent.isChecked()) {
            status.setText("साझा करने से पहले सहमति दें।");
            return;
        }
        StringBuilder payload = new StringBuilder();
        for (CheckBox field : fields) {
            if (!field.isChecked()) continue;
            if (payload.length() > 0) payload.append("\n\n");
            payload.append(field.getText());
        }
        if (payload.length() == 0) {
            status.setText("साझा करने के लिए कम से कम एक जानकारी चुनें।");
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, payload.toString());
        try {
            context.startActivity(Intent.createChooser(send, "चुने हुए TOWERLESS परिणाम साझा करें"));
        } catch (ActivityNotFoundException exception) {
            status.setText("साझा करने के लिए कोई compatible ऐप उपलब्ध नहीं है।");
        }
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Unknown" : value;
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

    private static int dp(Context context, int value) {
        return (int)(value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
