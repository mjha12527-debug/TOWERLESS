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
import java.util.*;

public class MainActivity extends Activity {
    private WifiManager wifi;
    private TelephonyManager telephony;
    private LinearLayout wifiList, cellList;
    private TextView wifiStatus, cellStatus;
    private BroadcastReceiver receiver;
    private boolean receiverRegistered = false;
    private boolean wifiScanPending = false;
    private long wifiScanStartedAtUs = 0L;
    private long wifiScanDeadlineElapsedMs = 0L;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable wifiScanTimeout;
    private static final int REQ_WIFI = 41, REQ_CELL = 42;
    private static final long WIFI_FRESHNESS_WINDOW_US = 30_000_000L;
    private static final long CELL_FRESHNESS_WINDOW_NS = 30_000_000_000L;
    private static final long WIFI_SCAN_TIMEOUT_MS = 30_000L;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        wifi = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        telephony = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        buildUi();
        receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
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
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this); page.setOrientation(1);
        page.setPadding(dp(18),dp(25),dp(18),dp(25)); page.setBackgroundColor(0xFF0B1220); scroll.addView(page);
        page.addView(txt("TOWERLESS",27,0xFFFFFFFF,true));
        page.addView(txt("Connectivity Intelligence · Android v0.3",13,0xFF9EACC0,false));

        LinearLayout cell = panel(); LinearLayout.LayoutParams p = params(); p.topMargin=dp(15); page.addView(cell,p);
        cell.addView(txt("मोबाइल नेटवर्क / SIM सिग्नल",18,0xFFFFFFFF,true));
        cellStatus=txt("Jio/Airtel सिग्नल विवरण देखने के लिए अनुमति दें।",13,0xFFB9C6D8,false);
        cell.addView(cellStatus);
        Button cb=button("SIM सिग्नल जाँचें"); cell.addView(cb);
        cb.setOnClickListener(v -> requestCellPermission());

        LinearLayout cresults=panel(); p=params(); p.topMargin=dp(12); page.addView(cresults,p);
        cresults.addView(txt("सेल जानकारी",17,0xFFFFFFFF,true));
        cellList=new LinearLayout(this); cellList.setOrientation(1); cresults.addView(cellList);

        LinearLayout w=panel(); p=params(); p.topMargin=dp(12); page.addView(w,p);
        w.addView(txt("आसपास के Wi-Fi नेटवर्क",18,0xFFFFFFFF,true));
        wifiStatus=txt("स्कैन करने के लिए Location अनुमति और फोन में Wi-Fi/Location चालू होना चाहिए।",13,0xFFB9C6D8,false);
        w.addView(wifiStatus);
        Button wb=button("Wi-Fi स्कैन करें"); w.addView(wb); wb.setOnClickListener(v->startWifiScan());
        wifiList=new LinearLayout(this); wifiList.setOrientation(1); w.addView(wifiList);

        LinearLayout note=panel(); p=params(); p.topMargin=dp(12); page.addView(note,p);
        note.addView(txt("महत्वपूर्ण सीमाएँ",17,0xFFFFFFFF,true));
        note.addView(txt("यह ऐप टावर की भौतिक कवरेज नहीं बढ़ाता। Android/फोन निर्माता SIM और सेल विवरण सीमित कर सकते हैं। यह केवल पढ़ता और दिखाता है; नेटवर्क को अपने आप नहीं बदलता। किसी दूसरे व्यक्ति के Wi-Fi/Hotspot का उपयोग केवल उसकी अनुमति से करें।",13,0xFFFFDFA8,false));
        page.addView(txt("अगला चरण: authorized hotspot workflow और connection quality tests.",12,0xFF9EACC0,false));
        setContentView(scroll);
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
            boolean ok=true; for(int g:grants) if(g!=PackageManager.PERMISSION_GRANTED) ok=false;
            if(ok) { if(code==REQ_CELL) showCellInfo(); else startWifiScan(); }
            else { if(code==REQ_CELL) cellStatus.setText("SIM विवरण के लिए आवश्यक अनुमति नहीं मिली।"); else wifiStatus.setText("Wi-Fi स्कैन अनुमति नहीं मिली।"); }
        }
    }
    private void showCellInfo() {
        cellList.removeAllViews();
        try {
            if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED ||
               checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
                cellStatus.setText("READ_PHONE_STATE और Location अनुमति आवश्यक है।"); return;
            }
            String carrier=telephony.getNetworkOperatorName();
            String type=networkType(telephony.getDataNetworkType());
            cellStatus.setText("ऑपरेटर: "+(carrier==null||carrier.isEmpty()?"अज्ञात":carrier)+" · डेटा नेटवर्क: "+type+" · CellInfo snapshot (Live नहीं)");
            List<CellInfo> infos=telephony.getAllCellInfo();
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
    private String frequencyBand(int frequencyMhz) {
        if (frequencyMhz >= 2400 && frequencyMhz < 2500) return "2.4 GHz";
        if (frequencyMhz >= 4900 && frequencyMhz < 5925) return "5 GHz";
        if (frequencyMhz >= 5925 && frequencyMhz <= 7125) return "6 GHz";
        return "Unknown";
    }
    private void startWifiScan() {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQ_WIFI); return;
        }
        if(!wifi.isWifiEnabled()) { wifiStatus.setText("Wi-Fi बंद है; सेटिंग खोल रहा है।"); startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); return; }
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
                wifiList.addView(txt("अभी कोई परिणाम नहीं।",13,0xFFB9C6D8,false));
                if (matchingRequest) wifiStatus.setText(resultsUpdated?"अपडेट broadcast मिला, लेकिन कोई scan result उपलब्ध नहीं।":"स्कैन अपडेट उपलब्ध नहीं; कोई नया परिणाम नहीं।");
                return;
            }
            Collections.sort(found,(a,b)->Integer.compare(b.level,a.level));
            int n=0, freshCount=0;
            for(ScanResult r:found) {
                if(n++>=25) break;
                long ageUs = r.timestamp > 0L ? Math.max(0L, SystemClock.elapsedRealtimeNanos()/1_000L-r.timestamp) : -1L;
                boolean fresh = matchingRequest && resultsUpdated && r.timestamp > 0L && r.timestamp >= wifiScanStartedAtUs && ageUs <= WIFI_FRESHNESS_WINDOW_US;
                if (fresh) freshCount++;
                String ssid=(r.SSID==null||r.SSID.isEmpty())?"(छिपा हुआ नेटवर्क)":r.SSID;
                String sec=r.capabilities==null?"सुरक्षा अज्ञात":r.capabilities.contains("WPA3")?"WPA3":r.capabilities.contains("WPA2")?"WPA2":r.capabilities.contains("WEP")?"WEP":"सुरक्षा/ओपन विवरण";
                String freshness = fresh ? "ताज़ा स्कैन" : "पुराना/असत्यापित";
                String ageText = ageUs < 0L ? "समय उपलब्ध नहीं" : (ageUs/1_000_000L)+" ms पुराना";
                wifiList.addView(txt(ssid+"  |  "+r.level+" dBm  |  "+sec+"  |  "+frequencyBand(r.frequency)+"  |  "+freshness+", "+ageText,13,0xFFB9C6D8,false));
            }
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
