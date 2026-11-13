package com.towerless.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Plain-language summary of the privacy behavior currently implemented in TOWERLESS. */
public final class PrivacyPolicyScreen implements ScreenRenderer {
    @Override public View render(Context context, UiState state) {
        ScrollView scroll = new ScrollView(context);
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        int side = dp(context, 18);
        page.setPadding(side, dp(context, 24), side, dp(context, 28));
        page.setBackgroundColor(Color.rgb(11, 18, 32));
        scroll.addView(page);

        add(context, page, "गोपनीयता नीति", 25, Color.WHITE, true);
        add(context, page, "TOWERLESS उपलब्ध नेटवर्क जानकारी को आपके फोन पर दिखाने के लिए पढ़ता है।", 16,
                Color.rgb(205, 215, 229), false);
        section(context, page, "कौन-सी जानकारी पढ़ी जाती है?",
                "आपके बटन दबाने पर आसपास के Wi-Fi परिणाम और फोन द्वारा उपलब्ध कराई गई SIM/Cell जानकारी पढ़ी जा सकती है। Android अनुमति न दे तो जानकारी Unknown रह सकती है।");
        section(context, page, "जानकारी कहाँ रहती है?",
                "नेटवर्क परिणाम ऐप में दिखाने के लिए फोन की मेमोरी में रहते हैं। इस ऐप में अभी server पर परिणाम upload करने की सुविधा नहीं है।");
        section(context, page, "क्या निजी जानकारी ली जाती है?",
                "मौजूदा कोड IMEI या device ID नहीं पढ़ता। Voice Input की सुविधा अभी उपलब्ध नहीं है, इसलिए आवाज़ रिकॉर्ड या भेजी नहीं जाती।");
        section(context, page, "Share Results कैसे काम करता है?",
                "साझा करने से पहले आप हर जानकारी चुनते हैं और अलग से सहमति देते हैं। केवल चुनी हुई जानकारी Android Sharesheet से आपके चुने ऐप को दी जाती है। उस ऐप का अपना privacy नियम लागू हो सकता है।");
        section(context, page, "Network permission क्यों माँगी जाती है?",
                "Wi-Fi या SIM परिणाम पढ़ने के लिए Android की अनुमति लग सकती है। आप अनुमति न दें तो संबंधित जानकारी उपलब्ध नहीं होगी।");
        section(context, page, "Preferences और Privacy Reset",
                "ऐप Dashboard Mode की पसंद सहेजता है। Privacy Reset इस पसंद को मिटाता है और दिखाई दे रहे network results को साफ करता है।");
        add(context, page, "यह नीति मौजूदा ऐप व्यवहार का सरल सार है। ऐप के व्यवहार में बदलाव होने पर इसे भी अपडेट करना होगा।",
                13, 0xFFFFDFA8, false);
        return scroll;
    }

    private static void section(Context context, LinearLayout page, String title, String body) {
        add(context, page, title, 18, 0xFF48D6B5, true);
        add(context, page, body, 15, Color.rgb(220, 228, 239), false);
    }

    private static void add(Context context, LinearLayout page, String value,
                            int size, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setPadding(0, dp(context, 7), 0, dp(context, 7));
        if (bold) text.setTypeface(null, android.graphics.Typeface.BOLD);
        page.addView(text);
    }

    private static int dp(Context context, int value) {
        return (int)(value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
