package com.towerless.app;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.*;
import android.view.View;
import android.widget.*;
import android.view.ViewGroup;
import java.util.*;
import com.towerless.app.ui.*;
import com.towerless.app.smartconnect.SmartConnectAnalyzer;
import com.towerless.app.smartconnect.InternetStatusReader;

public class MainActivity extends Activity {
    private WifiManager wifi;
    private TelephonyManager telephony;
    private LinearLayout wifiList, cellList;
    private TextView wifiStatus, cellStatus;
    private BroadcastReceiver receiver;
    private boolean receiverRegistered = false;
    private boolean wifiScanPending = false;
    private boolean ignoreWifiScanBroadcasts = false;
    private long wifiScanGeneration = 0L;
    private long wifiPermissionRequestGeneration = -1L;
    private long wifiScanStartedAtUs = 0L;
    private long wifiScanDeadlineElapsedMs = 0L;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable wifiScanTimeout;
    private FrameLayout screenHost;
    private UiState uiState = new UiState();
    private Screen currentScreen = Screen.HOME;
    private final Deque<Screen> screenHistory = new ArrayDeque<>();
    private HomeDashboardScreen homeDashboardScreen;
    private View homeDashboardView;
    private List<ScanResult> wifiAnalysisSnapshot = Collections.emptyList();
    private List<Boolean> wifiAnalysisFreshness = Collections.emptyList();
    private List<CellInfo> cellAnalysisSnapshot = Collections.emptyList();
    private String cellAnalysisOperator = "";
    private String cellAnalysisNetworkType = "";
    private String lastWifiShareValue = "Unknown";
    private String lastMobileShareValue = "Unknown";
    private String lastInternetShareValue = "Wi-Fi: Unknown · Mobile: Unknown";
    private String lastRecommendationShareValue = "Unknown";
    private static final String PREF_DASHBOARD_MODE = "dashboard_mode";
    private static final int REQ_WIFI = 41, REQ_CELL = 42;
    private static final long WIFI_FRESHNESS_WINDOW_US = 30_000_000L;
    private static final long CELL_FRESHNESS_WINDOW_NS = 30_000_000_000L;
    private static final long WIFI_SCAN_TIMEOUT_MS = 30_000L;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        wifi = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        telephony = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        restoreDashboardMode();
        buildUi();
        receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                if (ignoreWifiScanBroadcasts) return;
                boolean updated = i.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                long nowElapsedMs = SystemClock.elapsedRealtime();
                boolean timedOut = wifiScanPending && nowElapsedMs >= wifiScanDeadlineElapsedMs;
                boolean matchingRequest = wifiScanPending && !timedOut;
                if (matchingRequest || timedOut) {
                    wifiScanPending = false;
                    clearWifiScanTimeout();
                }
                showWifi(updated, matchingRequest);
                if (timedOut && wifiStatus != null) wifiStatus.setText("स्कैन समयसीमा समाप्त; दिखाए गए परिणाम पुराने/असत्यापित हैं।");
            }
        };
        IntentFilter f = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, f);
        receiverRegistered = true;
    }

    private void buildUi() {
        screenHost = new FrameLayout(this);
        screenHost.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(screenHost);
        showScreen(Screen.HOME);
    }

    /** Opens an in-app destination and records the current screen for Android Back. */
    public void navigateTo(Screen screen) {
        if (screenHost == null || screen == null || screen == Screen.HOME || screen == currentScreen) return;
        screenHistory.push(currentScreen);
        showScreen(screen);
    }

    private void showScreen(Screen screen) {
        currentScreen = screen;

        ScreenRenderer renderer;
        switch (screen) {
            case INSTALL_APP:
                renderer = new InstallAppScreen();
                break;
            case SHARE_APP:
                renderer = new ShareAppScreen();
                break;
            case SHARE_RESULTS_PREVIEW:
                renderer = new ShareResultsPreviewScreen(lastWifiShareValue,
                        lastMobileShareValue, lastInternetShareValue,
                        lastRecommendationShareValue);
                break;
            case ADVANCED_SETTINGS:
                renderer = new AdvancedSettingsScreen();
                break;
            case LANGUAGE_SELECTION:
                renderer = new LanguageSelectionScreen();
                break;
            case RESET_SETTINGS:
                renderer = new ResetSettingsScreen(this::resetPrivacyState);
                break;
            case PRIVACY_POLICY:
                renderer = new PrivacyPolicyScreen();
                break;
            case HOME:
            default:
                if (homeDashboardScreen == null) {
                    homeDashboardScreen = new HomeDashboardScreen(
                            this::requestCellPermission, this::startWifiScan,
                            this::navigateTo, this::setDashboardMode,
                            this::analyzeAvailableNetworks);
                    homeDashboardView = homeDashboardScreen.render(this, uiState);
                    wifiStatus = homeDashboardScreen.getWifiStatus();
                    cellStatus = homeDashboardScreen.getCellStatus();
                    wifiList = homeDashboardScreen.getWifiList();
                    cellList = homeDashboardScreen.getCellList();
                }
                displayScreen(homeDashboardView);
                return;
        }
        displayScreen(renderer.render(this, uiState));
    }

    private void displayScreen(View screenView) {
        screenHost.removeAllViews();
        screenHost.addView(screenView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void restoreDashboardMode() {
        String savedMode = getPreferences(MODE_PRIVATE).getString(
                PREF_DASHBOARD_MODE, UiState.Mode.BEGINNER.name());
        try {
            uiState.setMode(UiState.Mode.valueOf(savedMode));
        } catch (IllegalArgumentException ignored) {
            uiState.setMode(UiState.Mode.BEGINNER);
        }
    }

    private void setDashboardMode(UiState.Mode mode) {
        uiState.setMode(mode);
        getPreferences(MODE_PRIVATE).edit()
                .putString(PREF_DASHBOARD_MODE, uiState.getMode().name())
                .apply();
        if (homeDashboardScreen != null) homeDashboardScreen.setMode(uiState.getMode());
    }

    private void goBack() {
        if (!screenHistory.isEmpty()) showScreen(screenHistory.pop());
        else if (currentScreen != Screen.HOME) showScreen(Screen.HOME);
        else super.onBackPressed();
    }

    @Override public void onBackPressed() {
        goBack();
    }

    private void requestCellPermission() {
        ArrayList<String> req=new ArrayList<>();
        if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED) req.add(Manifest.permission.READ_PHONE_STATE);
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) req.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if(!req.isEmpty()) requestPermissions(req.toArray(new String[0]),REQ_CELL); else showCellInfo();
    }
    @Override public void onRequestPermissionsResult(int code,String[] perms,int[] grants) {
        super.onRequestPermissionsResult(code,perms,grants);
        if(code==REQ_CELL || code==REQ_WIFI) {
            if (code == REQ_WIFI) {
                if (wifiPermissionRequestGeneration != wifiScanGeneration) return;
                wifiPermissionRequestGeneration = -1L;
            }
            boolean ok=true; for(int g:grants) if(g!=PackageManager.PERMISSION_GRANTED) ok=false;
            if(ok) { if(code==REQ_CELL) showCellInfo(); else startWifiScan(); }
            else { if(code==REQ_CELL) cellStatus.setText("SIM विवरण के लिए आवश्यक अनुमति नहीं मिली।"); else wifiStatus.setText("Wi-Fi स्कैन अनुमति नहीं मिली।"); }
        }
    }
    private void showCellInfo() {
        cellList.removeAllViews();
        cellAnalysisSnapshot = Collections.emptyList();
        cellAnalysisOperator = "";
        cellAnalysisNetworkType = "";
        try {
            if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED ||
               checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
                cellStatus.setText("READ_PHONE_STATE और Location अनुमति आवश्यक है।"); return;
            }
            String carrier=telephony.getNetworkOperatorName();
            String type=networkType(telephony.getDataNetworkType());
            cellAnalysisOperator = carrier == null ? "" : carrier;
            cellAnalysisNetworkType = type;
            cellStatus.setText("ऑपरेटर: "+(carrier==null||carrier.isEmpty()?"अज्ञात":carrier)+" · डेटा नेटवर्क: "+type+" · CellInfo snapshot (Live नहीं)");
            List<CellInfo> infos=telephony.getAllCellInfo();
            if (infos != null) cellAnalysisSnapshot = new ArrayList<>(infos);
            if(infos==null || infos.isEmpty()) { cellList.addView(txt("फोन ने सेल विवरण उपलब्ध नहीं कराया।",13,0xFFB9C6D8,false)); return; }
            int n=0;
            for(CellInfo ci:infos) {
                if(n++>=12) break;
                int dbm=ci.getCellSignalStrength().getDbm();
                String tech=cellTechnology(ci);
                String registered=ci.isRegistered()?" · REGISTERED":"";
                cellList.addView(txt(tech+"  |  "+dbm+" dBm"+registered+"  |  "+cellTimestampStatus(ci.getTimeStamp()),14,ci.isRegistered()?0xFF48D6B5:0xFFB9C6D8,true));
            }
        } catch(SecurityException e) { cellStatus.setText("Android ने सेल जानकारी की अनुमति सीमित की।"); }
          catch(Exception e) { cellStatus.setText("सेल जानकारी उपलब्ध नहीं: "+e.getClass().getSimpleName()); }
    }
    private String networkType(int t) {
        if (Build.VERSION.SDK_INT >= 29 && t == TelephonyManager.NETWORK_TYPE_NR) return "5G NR";
        switch(t) {
            case TelephonyManager.NETWORK_TYPE_LTE:return "4G LTE";
            case TelephonyManager.NETWORK_TYPE_HSPA: case TelephonyManager.NETWORK_TYPE_HSPAP:return "3G HSPA";
            case TelephonyManager.NETWORK_TYPE_UMTS:return "3G";
            case TelephonyManager.NETWORK_TYPE_EDGE: case TelephonyManager.NETWORK_TYPE_GPRS:return "2G";
            default:return "अज्ञात / उपलब्ध नहीं";
        }
    }
    private String cellTechnology(CellInfo info) {
        if (info instanceof CellInfoLte) return "LTE/4G";
        if (info instanceof CellInfoWcdma) return "WCDMA/3G";
        if (info instanceof CellInfoGsm) return "GSM/2G";
        if (info instanceof CellInfoCdma) return "CDMA";
        if (Build.VERSION.SDK_INT >= 29 && info instanceof CellInfoNr) return "NR/5G";
        return "Cell";
    }
    private String cellTimestampStatus(long timestampNs) {
        if (timestampNs <= 0L) return "समय उपलब्ध नहीं · Live नहीं";
        long nowNs = SystemClock.elapsedRealtimeNanos();
        if (timestampNs > nowNs) return "समय अमान्य/अज्ञात · Live नहीं";
        long ageNs = nowNs - timestampNs;
        long ageSeconds = ageNs / 1_000_000_000L;
        String freshness = ageNs <= CELL_FRESHNESS_WINDOW_NS ? "टाइमस्टैम्प के अनुसार हालिया" : "पुराना";
        return freshness+" · "+ageSeconds+" सेकंड पुराना · Live नहीं";
    }
    /** Reads only snapshots populated by the existing v0.4 scan/check actions. */
    private void analyzeAvailableNetworks() {
        ArrayList<SmartConnectAnalyzer.Candidate> candidates = new ArrayList<>();
        long nowElapsedUs = SystemClock.elapsedRealtimeNanos() / 1_000L;
        for (int i = 0; i < wifiAnalysisSnapshot.size(); i++) {
            ScanResult result = wifiAnalysisSnapshot.get(i);
            if (result.level > 0 || result.level < -127) continue;
            boolean wasVerified = i < wifiAnalysisFreshness.size() && wifiAnalysisFreshness.get(i);
            boolean fresh = wasVerified && result.timestamp > 0L
                    && result.timestamp >= wifiScanStartedAtUs
                    && nowElapsedUs >= result.timestamp
                    && nowElapsedUs - result.timestamp <= WIFI_FRESHNESS_WINDOW_US;
            candidates.add(SmartConnectAnalyzer.wifi(result.SSID, result.level,
                    result.frequency, result.capabilities, fresh,
                    wifiBandCompatibility(result.frequency)));
        }

        CellInfo bestRegisteredCell = null;
        for (CellInfo info : cellAnalysisSnapshot) {
            if (info == null || !info.isRegistered()) continue;
            int signalDbm = info.getCellSignalStrength().getDbm();
            if (signalDbm < -150 || signalDbm >= 0) continue;
            if (bestRegisteredCell == null || signalDbm
                    > bestRegisteredCell.getCellSignalStrength().getDbm()) {
                bestRegisteredCell = info;
            }
        }
        Integer cellSignalDbm = null;
        boolean cellFresh = false;
        if (bestRegisteredCell != null) {
            int dbm = bestRegisteredCell.getCellSignalStrength().getDbm();
            if (dbm >= -150 && dbm < 0) cellSignalDbm = dbm;
            cellFresh = isCellSnapshotFresh(bestRegisteredCell);
        }
        if (cellSignalDbm != null) {
            candidates.add(SmartConnectAnalyzer.mobile(cellAnalysisOperator,
                    cellAnalysisNetworkType, cellSignalDbm, cellFresh));
        }

        SmartConnectAnalyzer.Analysis analysis = SmartConnectAnalyzer.compare(candidates);
        SmartConnectAnalyzer.Candidate bestWifi = null;
        SmartConnectAnalyzer.Candidate bestMobile = null;
        for (SmartConnectAnalyzer.Candidate candidate : analysis.candidates) {
            if (candidate.kind == SmartConnectAnalyzer.NetworkKind.WIFI && bestWifi == null) bestWifi = candidate;
            if (candidate.kind == SmartConnectAnalyzer.NetworkKind.MOBILE && bestMobile == null) bestMobile = candidate;
        }
        StringBuilder summary = new StringBuilder();
        summary.append("Wi-Fi: ").append(bestWifi == null
                ? "Unknown · signal/band अज्ञात" : bestWifi.details);
        summary.append("\nMobile: ").append(bestMobile == null
                ? "Unknown · signal/type/band अज्ञात"
                : bestMobile.details + " · Band: Unknown");
        InternetStatusReader.Snapshot internet = InternetStatusReader.read(this);
        String wifiInternet = internetStatusLabel(internet.wifi);
        String mobileInternet = internetStatusLabel(internet.mobile);
        summary.append("\nWi-Fi Internet: ").append(wifiInternet);
        summary.append("\nMobile Internet: ").append(mobileInternet);
        summary.append("\n\n").append(analysis.explanation);
        if (analysis.confidence > 0) {
            summary.append("\nविश्वास स्तर: सीमित · ").append(analysis.confidence).append("/100");
        }
        lastWifiShareValue = bestWifi == null ? "Unknown" : bestWifi.details;
        lastMobileShareValue = bestMobile == null ? "Unknown" : bestMobile.details + " · Band: Unknown";
        lastInternetShareValue = "Wi-Fi: " + wifiInternet + " · Mobile: " + mobileInternet;
        lastRecommendationShareValue = recommendationShareSummary(analysis);
        homeDashboardScreen.setSmartConnectStatus(summary.toString());
    }

    private String recommendationShareSummary(SmartConnectAnalyzer.Analysis analysis) {
        switch (analysis.recommendation) {
            case WIFI:
                return "सुझाव: Wi-Fi · संकेत और उपलब्ध band/security जानकारी के आधार पर। "
                        + "SSID या नेटवर्क का नाम शामिल नहीं है।";
            case MOBILE:
                return "सुझाव: Mobile Network · संकेत और उपलब्ध network type के आधार पर। "
                        + "ऑपरेटर का नाम या cell पहचान शामिल नहीं है।";
            case INSUFFICIENT_DATA:
            default:
                return "सुझाव: पर्याप्त जानकारी नहीं · कोई नेटवर्क पहचान विवरण शामिल नहीं है।";
        }
    }

    private void resetPrivacyState() {
        wifiScanGeneration++;
        wifiPermissionRequestGeneration = -1L;
        ignoreWifiScanBroadcasts = true;
        wifiScanPending = false;
        clearWifiScanTimeout();
        getPreferences(MODE_PRIVATE).edit().clear().apply();
        uiState.setMode(UiState.Mode.BEGINNER);
        screenHistory.clear();
        wifiAnalysisSnapshot = Collections.emptyList();
        wifiAnalysisFreshness = Collections.emptyList();
        cellAnalysisSnapshot = Collections.emptyList();
        cellAnalysisOperator = "";
        cellAnalysisNetworkType = "";
        lastWifiShareValue = "Unknown";
        lastMobileShareValue = "Unknown";
        lastInternetShareValue = "Wi-Fi: Unknown · Mobile: Unknown";
        lastRecommendationShareValue = "Unknown";
        if (homeDashboardScreen != null) {
            homeDashboardScreen.setMode(UiState.Mode.BEGINNER);
            homeDashboardScreen.clearDisplayedNetworkData();
        }
        showScreen(Screen.HOME);
        Toast.makeText(this, "Privacy Reset पूरा हुआ।", Toast.LENGTH_SHORT).show();
    }

    private String internetStatusLabel(InternetStatusReader.Status status) {
        switch (status) {
            case VALIDATED_INTERNET: return "उपलब्ध (Android ने पुष्टि की)";
            case CAPTIVE_PORTAL: return "Captive Portal · login जरूरी";
            case NO_INTERNET: return "Internet नहीं";
            case UNKNOWN:
            default: return "Unknown";
        }
    }

    private Boolean wifiBandCompatibility(int frequencyMhz) {
        try {
            if (frequencyMhz >= 2400 && frequencyMhz < 2500) {
                return Build.VERSION.SDK_INT >= 31 ? wifi.is24GHzBandSupported() : null;
            }
            if (frequencyMhz >= 4900 && frequencyMhz < 5925) return wifi.is5GHzBandSupported();
            if (frequencyMhz >= 5925 && frequencyMhz <= 7125) {
                return Build.VERSION.SDK_INT >= 30 ? wifi.is6GHzBandSupported() : null;
            }
        } catch (SecurityException ignored) {
            return null;
        }
        return null;
    }

    private boolean isCellSnapshotFresh(CellInfo info) {
        long timestampNs = info.getTimeStamp();
        if (timestampNs <= 0L) return false;
        long nowNs = SystemClock.elapsedRealtimeNanos();
        return timestampNs <= nowNs && nowNs - timestampNs <= CELL_FRESHNESS_WINDOW_NS;
    }

    private String frequencyBand(int frequencyMhz) {
        if (frequencyMhz >= 2400 && frequencyMhz < 2500) return "2.4 GHz";
        if (frequencyMhz >= 4900 && frequencyMhz < 5925) return "5 GHz";
        if (frequencyMhz >= 5925 && frequencyMhz <= 7125) return "6 GHz";
        return "Unknown";
    }
    private void startWifiScan() {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            wifiPermissionRequestGeneration = wifiScanGeneration;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQ_WIFI); return;
        }
        if(!wifi.isWifiEnabled()) { wifiStatus.setText("Wi-Fi बंद है; सेटिंग खोल रहा है।"); startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); return; }
        ignoreWifiScanBroadcasts = false;
        clearWifiScanTimeout();
        wifiScanPending = false;
        wifiScanStartedAtUs = SystemClock.elapsedRealtimeNanos() / 1_000L;
        wifiScanDeadlineElapsedMs = SystemClock.elapsedRealtime() + WIFI_SCAN_TIMEOUT_MS;
        boolean started=wifi.startScan();
        wifiScanPending=started;
        if (started) {
            final long requestStartedAtUs = wifiScanStartedAtUs;
            wifiScanTimeout = () -> {
                if (wifiScanPending && wifiScanStartedAtUs == requestStartedAtUs) {
                    wifiScanPending = false;
                    wifiScanTimeout = null;
                    showWifi(false, false);
                    if (wifiStatus != null) wifiStatus.setText("स्कैन समयसीमा समाप्त; दिखाए गए परिणाम पुराने/असत्यापित हैं।");
                }
            };
            mainHandler.postDelayed(wifiScanTimeout, WIFI_SCAN_TIMEOUT_MS);
        }
        wifiStatus.setText(started?"स्कैन अनुरोध स्वीकार हुआ; परिणाम की ताज़गी की पुष्टि बाकी है।":"स्कैन अनुरोध स्वीकार नहीं हुआ; केवल पुराने परिणाम उपलब्ध हो सकते हैं।");
        showWifi(false, false);
    }
    private void showWifi(boolean resultsUpdated, boolean matchingRequest) {
        if(wifiList==null) return; wifiList.removeAllViews();
        try {
            List<ScanResult> found=wifi.getScanResults();
            if(found==null||found.isEmpty()) {
                wifiAnalysisSnapshot = Collections.emptyList();
                wifiAnalysisFreshness = Collections.emptyList();
                wifiList.addView(txt("अभी कोई परिणाम नहीं।",13,0xFFB9C6D8,false));
                if (matchingRequest) wifiStatus.setText(resultsUpdated?"अपडेट broadcast मिला, लेकिन कोई scan result उपलब्ध नहीं।":"स्कैन अपडेट उपलब्ध नहीं; कोई नया परिणाम नहीं।");
                return;
            }
            Collections.sort(found,(a,b)->Integer.compare(b.level,a.level));
            ArrayList<ScanResult> analysisResults = new ArrayList<>();
            ArrayList<Boolean> analysisFreshness = new ArrayList<>();
            int n=0, freshCount=0;
            for(ScanResult r:found) {
                if(n++>=25) break;
                long ageUs = r.timestamp > 0L ? Math.max(0L, SystemClock.elapsedRealtimeNanos()/1_000L-r.timestamp) : -1L;
                boolean fresh = matchingRequest && resultsUpdated && r.timestamp > 0L && r.timestamp >= wifiScanStartedAtUs && ageUs <= WIFI_FRESHNESS_WINDOW_US;
                analysisResults.add(r);
                analysisFreshness.add(fresh);
                if (fresh) freshCount++;
                String ssid=(r.SSID==null||r.SSID.isEmpty())?"(छिपा हुआ नेटवर्क)":r.SSID;
                String sec=r.capabilities==null?"सुरक्षा अज्ञात":r.capabilities.contains("WPA3")?"WPA3":r.capabilities.contains("WPA2")?"WPA2":r.capabilities.contains("WEP")?"WEP":"सुरक्षा/ओपन विवरण";
                String freshness = fresh ? "ताज़ा स्कैन" : "पुराना/असत्यापित";
                String ageText = ageUs < 0L ? "समय उपलब्ध नहीं" : (ageUs/1_000_000L)+" ms पुराना";
                wifiList.addView(txt(ssid+"  |  "+r.level+" dBm  |  "+sec+"  |  "+frequencyBand(r.frequency)+"  |  "+freshness+", "+ageText,13,0xFFB9C6D8,false));
            }
            wifiAnalysisSnapshot = analysisResults;
            wifiAnalysisFreshness = analysisFreshness;
            if (matchingRequest && resultsUpdated && freshCount > 0) {
                wifiStatus.setText("ताज़ा परिणाम: "+freshCount+" · अन्य परिणाम पुराने/असत्यापित हो सकते हैं।");
            } else if (matchingRequest && !resultsUpdated) {
                wifiStatus.setText("Android ने नए scan results की पुष्टि नहीं की; दिखाए गए परिणाम पुराने/असत्यापित हैं।");
            } else if (matchingRequest) {
                wifiStatus.setText("अपडेट broadcast मिला, लेकिन कोई परिणाम नया और 30 सेकंड की freshness सीमा में नहीं है।");
            } else {
                wifiStatus.setText("उपलब्ध परिणाम: "+Math.min(found.size(),25)+" · ताज़गी की पुष्टि नहीं।");
            }
        } catch(SecurityException e) { wifiStatus.setText("Wi-Fi स्कैन अनुमति उपलब्ध नहीं।"); }
    }
    private LinearLayout panel(){LinearLayout x=new LinearLayout(this);x.setOrientation(1);x.setPadding(dp(14),dp(14),dp(14),dp(14));x.setBackgroundColor(0xFF151F30);return x;}
    private LinearLayout.LayoutParams params(){return new LinearLayout.LayoutParams(-1,-2);}
    private Button button(String s){Button b=new Button(this);b.setText(s);return b;}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,1);t.setPadding(0,dp(5),0,dp(5));return t;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private void clearWifiScanTimeout() {
        if (wifiScanTimeout != null) {
            mainHandler.removeCallbacks(wifiScanTimeout);
            wifiScanTimeout = null;
        }
    }
    @Override protected void onDestroy(){
        wifiScanPending = false;
        clearWifiScanTimeout();
        if(receiverRegistered && receiver != null){try{unregisterReceiver(receiver);}catch(Exception ignored){}receiverRegistered=false;}
        super.onDestroy();
    }
}
