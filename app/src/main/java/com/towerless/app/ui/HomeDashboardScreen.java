package com.towerless.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class HomeDashboardScreen implements ScreenRenderer {
    public interface NavigationActions {
        void open(Screen destination);
    }

    public interface ModeActions {
        void select(UiState.Mode mode);
    }

    private final Runnable onCellCheck;
    private final Runnable onWifiScan;
    private final NavigationActions navigationActions;
    private final ModeActions modeActions;
    private final Runnable onOptimize;
    private TextView wifiStatus;
    private TextView cellStatus;
    private LinearLayout wifiList;
    private LinearLayout cellList;
    private LinearLayout cellDetailsPanel;
    private TextView modeDescription;
    private Button beginnerModeButton;
    private Button advancedModeButton;
    private TextView smartConnectStatus;

    public HomeDashboardScreen(Runnable onCellCheck, Runnable onWifiScan,
                               NavigationActions navigationActions, ModeActions modeActions,
                               Runnable onOptimize) {
        this.onCellCheck = onCellCheck;
        this.onWifiScan = onWifiScan;
        this.navigationActions = navigationActions;
        this.modeActions = modeActions;
        this.onOptimize = onOptimize;
    }

    @Override public View render(Context context, UiState state) {
        ScrollView scroll = new ScrollView(context);
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        int horizontal = dp(context, 18);
        page.setPadding(horizontal, dp(context, 25), horizontal, dp(context, 25));
        page.setBackgroundColor(Color.rgb(11, 18, 32));
        scroll.addView(page);

        page.addView(text(context, "TOWERLESS", 27, Color.WHITE, true));
        page.addView(text(context, "नेटवर्क जानकारी · Android v0.5.0", 14,
                Color.rgb(158, 172, 192), false));

        LinearLayout modePanel = panel(context);
        addPanel(page, modePanel, context, 12);
        modePanel.addView(text(context, "देखने का तरीका चुनें", 18, Color.WHITE, true));
        modeDescription = text(context, "शुरुआती मोड: केवल मुख्य स्थिति और आसान नियंत्रण।",
                14, Color.rgb(185, 198, 216), false);
        modePanel.addView(modeDescription);
        beginnerModeButton = modeButton(context, modePanel, "शुरुआती मोड",
                () -> modeActions.select(UiState.Mode.BEGINNER));
        advancedModeButton = modeButton(context, modePanel, "उन्नत मोड",
                () -> modeActions.select(UiState.Mode.ADVANCED));

        LinearLayout smartConnect = panel(context);
        addPanel(page, smartConnect, context, 12);
        smartConnect.addView(text(context, "Smart Connect सुझाव", 18, Color.WHITE, true));
        Button optimizeButton = new Button(context);
        optimizeButton.setText("OPTIMIZE MY CONNECTION");
        configureActionButton(context, optimizeButton);
        smartConnect.addView(optimizeButton);
        optimizeButton.setOnClickListener(view -> onOptimize.run());
        smartConnectStatus = text(context,
                "अभी विश्लेषण नहीं हुआ। केवल पहले से उपलब्ध परिणाम देखे जाएँगे; नया scan नहीं चलेगा।",
                14, Color.rgb(185, 198, 216), false);
        smartConnect.addView(smartConnectStatus);

        LinearLayout appActions = panel(context);
        addPanel(page, appActions, context, 12);
        appActions.addView(text(context, "ऐप के विकल्प", 17, Color.WHITE, true));
        addNavigationButton(context, appActions, "ऐप इंस्टॉल करें", Screen.INSTALL_APP);
        addNavigationButton(context, appActions, "ऐप साझा करें", Screen.SHARE_APP);
        addNavigationButton(context, appActions, "परिणाम साझा करने से पहले देखें", Screen.SHARE_RESULTS_PREVIEW);
        addNavigationButton(context, appActions, "उन्नत सेटिंग्स", Screen.ADVANCED_SETTINGS);
        addNavigationButton(context, appActions, "Privacy Policy", Screen.PRIVACY_POLICY);
        addNavigationButton(context, appActions, "Privacy Reset", Screen.RESET_SETTINGS);

        LinearLayout cell = panel(context);
        addPanel(page, cell, context, 15);
        cell.addView(text(context, "मोबाइल नेटवर्क / SIM सिग्नल", 18, Color.WHITE, true));
        cellStatus = text(context, "Jio/Airtel सिग्नल विवरण देखने के लिए अनुमति दें।", 13,
                Color.rgb(185, 198, 216), false);
        cell.addView(cellStatus);
        Button cellButton = new Button(context);
        cellButton.setText("SIM सिग्नल जाँचें");
        configureActionButton(context, cellButton);
        cell.addView(cellButton);
        cellButton.setOnClickListener(view -> onCellCheck.run());

        LinearLayout cellResults = panel(context);
        addPanel(page, cellResults, context, 12);
        cellResults.addView(text(context, "सेल जानकारी", 17, Color.WHITE, true));
        cellList = new LinearLayout(context);
        cellList.setOrientation(LinearLayout.VERTICAL);
        cellResults.addView(cellList);
        cellDetailsPanel = cellResults;

        LinearLayout wifi = panel(context);
        addPanel(page, wifi, context, 12);
        wifi.addView(text(context, "आसपास के Wi-Fi नेटवर्क", 18, Color.WHITE, true));
        wifiStatus = text(context,
                "स्कैन करने के लिए Location अनुमति और फोन में Wi-Fi/Location चालू होना चाहिए।",
                13, Color.rgb(185, 198, 216), false);
        wifi.addView(wifiStatus);
        Button wifiButton = new Button(context);
        wifiButton.setText("Wi-Fi स्कैन करें");
        configureActionButton(context, wifiButton);
        wifi.addView(wifiButton);
        wifiButton.setOnClickListener(view -> onWifiScan.run());
        wifiList = new LinearLayout(context);
        wifiList.setOrientation(LinearLayout.VERTICAL);
        wifi.addView(wifiList);

        LinearLayout note = panel(context);
        addPanel(page, note, context, 12);
        note.addView(text(context, "महत्वपूर्ण सीमाएँ", 17, Color.WHITE, true));
        note.addView(text(context,
                "यह ऐप टावर की भौतिक कवरेज नहीं बढ़ाता। Android/फोन निर्माता SIM और सेल विवरण सीमित कर सकते हैं। यह केवल पढ़ता और दिखाता है; नेटवर्क को अपने आप नहीं बदलता। किसी दूसरे व्यक्ति के Wi-Fi/Hotspot का उपयोग केवल उसकी अनुमति से करें।",
                13, 0xFFFFDFA8, false));
        page.addView(text(context,
                "यह ऐप केवल उपलब्ध नेटवर्क जानकारी दिखाता है। परिणाम फोन और अनुमति पर निर्भर करते हैं।",
                12, Color.rgb(158, 172, 192), false));
        setMode(state.getMode());
        return scroll;
    }

    public void setMode(UiState.Mode mode) {
        boolean advanced = mode == UiState.Mode.ADVANCED;
        if (modeDescription != null) {
            modeDescription.setText(advanced
                    ? "उन्नत मोड: Wi-Fi और सेल के उपलब्ध विस्तृत परिणाम दिखते हैं।"
                    : "शुरुआती मोड: केवल मुख्य स्थिति और आसान नियंत्रण।");
        }
        if (beginnerModeButton != null) {
            beginnerModeButton.setText(advanced ? "शुरुआती मोड चुनें" : "शुरुआती मोड · चुना गया");
        }
        if (advancedModeButton != null) {
            advancedModeButton.setText(advanced ? "उन्नत मोड · चुना गया" : "उन्नत मोड चुनें");
        }
        int detailVisibility = advanced ? View.VISIBLE : View.GONE;
        if (wifiList != null) wifiList.setVisibility(detailVisibility);
        if (cellDetailsPanel != null) cellDetailsPanel.setVisibility(detailVisibility);
    }

    public TextView getWifiStatus() { return wifiStatus; }
    public TextView getCellStatus() { return cellStatus; }
    public LinearLayout getWifiList() { return wifiList; }
    public LinearLayout getCellList() { return cellList; }
    public void setSmartConnectStatus(String status) {
        if (smartConnectStatus != null) smartConnectStatus.setText(status);
    }

    public void clearDisplayedNetworkData() {
        if (wifiList != null) wifiList.removeAllViews();
        if (cellList != null) cellList.removeAllViews();
        if (wifiStatus != null) wifiStatus.setText("Wi-Fi परिणाम उपलब्ध नहीं। Scan केवल आपके बटन दबाने पर चलेगा।");
        if (cellStatus != null) cellStatus.setText("मोबाइल नेटवर्क परिणाम उपलब्ध नहीं।");
        if (smartConnectStatus != null) smartConnectStatus.setText(
                "Privacy Reset पूरा हुआ। अभी कोई network परिणाम उपलब्ध नहीं है।");
    }

    private static LinearLayout panel(Context context) {
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 14);
        panel.setPadding(padding, padding, padding, padding);
        panel.setBackgroundColor(0xFF151F30);
        return panel;
    }

    private static void addPanel(LinearLayout parent, View panel, Context context, int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(context, topMarginDp);
        parent.addView(panel, params);
    }

    private void addNavigationButton(Context context, LinearLayout parent, String label, Screen destination) {
        Button button = new Button(context);
        button.setText(label);
        configureActionButton(context, button);
        button.setOnClickListener(view -> navigationActions.open(destination));
        parent.addView(button);
    }

    private Button modeButton(Context context, LinearLayout parent, String label, Runnable action) {
        Button button = new Button(context);
        button.setText(label);
        configureActionButton(context, button);
        button.setOnClickListener(view -> action.run());
        parent.addView(button, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return button;
    }

    private static void configureActionButton(Context context, Button button) {
        button.setMinHeight(dp(context, 56));
        button.setSingleLine(false);
        button.setMaxLines(2);
    }

    private static TextView text(Context context, String value, int size, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) text.setTypeface(null, android.graphics.Typeface.BOLD);
        int verticalPadding = dp(context, 5);
        text.setPadding(0, verticalPadding, 0, verticalPadding);
        return text;
    }

    private static int dp(Context context, int value) {
        return (int)(value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
