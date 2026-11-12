package com.towerless.app;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
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
    private static final int REQ_WIFI = 41, REQ_CELL = 42;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        wifi = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        telephony = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        buildUi();
        receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                showWifi(i.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false));
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
            cellStatus.setText("ऑपरेटर: "+(carrier==null||carrier.isEmpty()?"अज्ञात":carrier)+" · डेटा नेटवर्क: "+type);
            List<CellInfo> infos=telephony.getAllCellInfo();
            if(infos==null || infos.isEmpty()) { cellList.addView(txt("फोन ने सेल विवरण उपलब्ध नहीं कराया।",13,0xFFB9C6D8,false)); return; }
            int n=0;
            for(CellInfo ci:infos) {
                if(n++>=12) break;
                int dbm=ci.getCellSignalStrength().getDbm();
                String tech=ci instanceof CellInfoLte?"LTE/4G":ci instanceof CellInfoWcdma?"WCDMA/3G":ci instanceof CellInfoGsm?"GSM/2G":ci instanceof CellInfoNr?"NR/5G":ci instanceof CellInfoCdma?"CDMA":"Cell";
                String registered=ci.isRegistered()?" · REGISTERED":"";
                cellList.addView(txt(tech+"  |  "+dbm+" dBm"+registered,14,ci.isRegistered()?0xFF48D6B5:0xFFB9C6D8,true));
            }
        } catch(SecurityException e) { cellStatus.setText("Android ने सेल जानकारी की अनुमति सीमित की।"); }
          catch(Exception e) { cellStatus.setText("सेल जानकारी उपलब्ध नहीं: "+e.getClass().getSimpleName()); }
    }
    private String networkType(int t) {
        switch(t) {
            case TelephonyManager.NETWORK_TYPE_LTE:return "4G LTE";
            case TelephonyManager.NETWORK_TYPE_NR:return "5G NR";
            case TelephonyManager.NETWORK_TYPE_HSPA: case TelephonyManager.NETWORK_TYPE_HSPAP:return "3G HSPA";
            case TelephonyManager.NETWORK_TYPE_UMTS:return "3G";
            case TelephonyManager.NETWORK_TYPE_EDGE: case TelephonyManager.NETWORK_TYPE_GPRS:return "2G";
            default:return "अज्ञात / उपलब्ध नहीं";
        }
    }
    private void startWifiScan() {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQ_WIFI); return;
        }
        if(!wifi.isWifiEnabled()) { wifiStatus.setText("Wi-Fi बंद है; सेटिंग खोल रहा है।"); startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); return; }
        boolean started=wifi.startScan();
        wifiStatus.setText(started?"स्कैन अनुरोध भेजा गया। Android स्कैन को सीमित कर सकता है।":"Android ने नया स्कैन सीमित किया; पुराने परिणाम दिख सकते हैं।");
        showWifi(false);
    }
    private void showWifi(boolean fresh) {
        if(wifiList==null) return; wifiList.removeAllViews();
        try {
            List<ScanResult> found=wifi.getScanResults();
            if(found==null||found.isEmpty()) { wifiList.addView(txt("अभी कोई परिणाम नहीं।",13,0xFFB9C6D8,false)); return; }
            Collections.sort(found,(a,b)->Integer.compare(b.level,a.level));
            int n=0;
            for(ScanResult r:found) {
                if(n++>=25) break;
                String ssid=(r.SSID==null||r.SSID.isEmpty())?"(छिपा हुआ नेटवर्क)":r.SSID;
                String sec=r.capabilities==null?"सुरक्षा अज्ञात":r.capabilities.contains("WPA3")?"WPA3":r.capabilities.contains("WPA2")?"WPA2":r.capabilities.contains("WEP")?"WEP":"सुरक्षा/ओपन विवरण";
                wifiList.addView(txt(ssid+"  |  "+r.level+" dBm  |  "+sec+"  |  "+(r.frequency>=4900?"5 GHz":"2.4 GHz"),13,0xFFB9C6D8,false));
            }
            wifiStatus.setText((fresh?"नए":"उपलब्ध/पिछले")+" परिणाम: "+Math.min(found.size(),25));
        } catch(SecurityException e) { wifiStatus.setText("Wi-Fi स्कैन अनुमति उपलब्ध नहीं।"); }
    }
    private LinearLayout panel(){LinearLayout x=new LinearLayout(this);x.setOrientation(1);x.setPadding(dp(14),dp(14),dp(14),dp(14));x.setBackgroundColor(0xFF151F30);return x;}
    private LinearLayout.LayoutParams params(){return new LinearLayout.LayoutParams(-1,-2);}
    private Button button(String s){Button b=new Button(this);b.setText(s);return b;}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,1);t.setPadding(0,dp(5),0,dp(5));return t;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    @Override protected void onDestroy(){if(receiverRegistered){try{unregisterReceiver(receiver);}catch(Exception ignored){}}super.onDestroy();}
}
