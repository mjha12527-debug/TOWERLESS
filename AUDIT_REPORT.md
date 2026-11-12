# TOWERLESS v0.3 — तकनीकी ऑडिट रिपोर्ट

**ऑडिट का दायरा:** उपलब्ध Java स्रोत, `AndroidManifest.xml`, Gradle फाइलें, UI संसाधन और README। यह स्थिर स्रोत-ऑडिट है; APK build, emulator/device परीक्षण या runtime permission परीक्षण नहीं चलाए गए।

## सारांश

प्रोजेक्ट छोटा और केवल-पठन वाला सिग्नल मॉनिटर है। मौजूदा स्रोत में कोई स्पष्ट build-blocking त्रुटि नहीं दिखी, लेकिन सेल जानकारी के API स्तर, dual-SIM चयन और Wi‑Fi स्कैन की freshness/permission निर्भरता के कारण फीचर सभी समर्थित फोन पर समान रूप से काम नहीं करेंगे। `minSdk 26` Android 8.0 घोषित करता है; इसलिए Android 10–16 तक संगतता का दावा केवल source inspection से प्रमाणित नहीं है।

## गंभीर त्रुटियाँ (Critical errors)

**इस निरीक्षण में कोई निश्चित गंभीर त्रुटि नहीं मिली।** नीचे दर्ज Major मुद्दे वास्तविक compatibility/feature सीमाएँ हैं, पर उपलब्ध फाइलों से हर डिवाइस पर ऐप के crash या APK के न बनने की पुष्टि नहीं होती।

## प्रमुख मुद्दे (Major issues)

1. **Android 10 से पुराने घोषित समर्थित संस्करणों पर 5G सेल API का जोखिम — `MainActivity.java`**
   - ऐप `minSdk 26` रखता है, पर `CellInfoNr` और `TelephonyManager.NETWORK_TYPE_NR` का उपयोग API 29+ की शर्त जाँचे बिना होता है। Android 8/9 पर class/field resolution का runtime जोखिम है।
   - यदि minSdk 26 रखना है, तो API 29+ को runtime guard और उपयुक्त API annotation से सीमित करना चाहिए। अन्यथा न्यूनतम SDK बढ़ाने पर विचार करें।

2. **डुअल-SIM में केवल डिफ़ॉल्ट subscription की जानकारी — `MainActivity.java`**
   - `getSystemService(TELEPHONY_SERVICE)` से प्राप्त एक `TelephonyManager` उपयोग होता है। ऐप सक्रिय SIM की सूची नहीं दिखाता, `createForSubscriptionId()` से प्रत्येक subscription नहीं चुनता और SIM slot/subscription की पहचान भी नहीं देता।
   - परिणामस्वरूप दूसरी SIM का ऑपरेटर/सेल सिग्नल छूट सकता है; “SIM सिग्नल” UI बहु-SIM मॉनिटरिंग का संकेत देता है, जिसे कोड लागू नहीं करता।

3. **सेल जानकारी को live/निरंतर मॉनिटरिंग नहीं कहा जा सकता — `MainActivity.java`**
   - `getAllCellInfo()` एक बार बटन दबाने पर पढ़ा जाता है। API 29+ पर परिणाम cached हो सकता है; `CellInfo` timestamp प्रदर्शित या freshness जाँची नहीं जाती। कोई `PhoneStateListener`/`TelephonyCallback` निरंतर अपडेट नहीं करता।
   - खाली/पुराना परिणाम नेटवर्क न होने, निर्माता/कैरियर रोक, location state या Android throttling के कारण भी हो सकता है। README इस cache सीमा को बताता है, पर UI में परिणाम का समय नहीं दिखता।

4. **Wi‑Fi स्कैन का अनुरोध और परिणाम अलग-अलग हो सकते हैं — `MainActivity.java`**
   - `startScan()` deprecated API है और Android 9+ पर सिस्टम स्कैन आवृत्ति सीमित करता है। `false` मिलने पर भी तुरंत `getScanResults()` चलाया जाता है, जो पुराने परिणाम दिखा सकता है। परिणामों के timestamp (`ScanResult.timestamp`) की जाँच नहीं है।
   - `EXTRA_RESULTS_UPDATED` receiver से आता है, पर `showWifi()` केवल ताज़ा/पुराना boolean दिखाता है; `fresh=false` वाले callback को भी “नए परिणाम” के रूप में प्रस्तुत किया जा सकता है।
   - स्कैन केवल Activity के जीवनकाल में प्राप्त होंगे; प्रक्रिया बंद/Activity नष्ट होने पर receiver unregister हो जाता है।

5. **Wi‑Fi scan के लिए Location services चालू होने की जाँच/मार्गदर्शन नहीं है — Manifest और `MainActivity.java`**
   - ऐप `ACCESS_FINE_LOCATION` माँगता है, पर Location services/GPS चालू हैं या नहीं, यह जाँचता नहीं। कई Android संस्करण/निर्माता Location बंद होने पर scan results खाली या सीमित रखते हैं।
   - Android 12+ में उपयोगकर्ता approximate location चुन सकता है; fine-location आधारित प्रवाह को permission अस्वीकृति/सीमित grant के लिए स्पष्ट रूप से संभालना चाहिए। अभी केवल सामान्य त्रुटि संदेश आता है।

6. **Target SDK 35 के Android 15+ व्यवहार की पुष्टि आवश्यक — Gradle/UI**
   - `targetSdk 35` के साथ Android 15 पर edge-to-edge UI लागू होता है। UI में status/navigation bar inset या edge-to-edge layout handling नहीं है; कुछ डिवाइस पर शीर्ष/निचला कंटेंट system bars के नीचे जा सकता है।
   - ऐप Android 16 पर SDK 35 target के साथ चल सकता है, लेकिन Android 16/API 36 target व्यवहार और नवीनतम platform संगतता इस सेटअप से प्रमाणित नहीं होती।

## छोटे मुद्दे (Minor issues)

1. **नेटवर्क प्रकार का सारांश अपूर्ण — `networkType()`**: LTE, NR, HSPA, UMTS, EDGE और GPRS के कुछ मान ही map किए गए हैं। HSDPA/HSUPA, TD-SCDMA, 1xRTT, IWLAN तथा अन्य मान “अज्ञात” दिखेंगे। 5G NSA में डेटा नेटवर्क LTE भी दिख सकता है; यह जरूरी नहीं कि 5G कवरेज न हो।
2. **Wi‑Fi आवृत्ति वर्गीकरण मोटा है — `showWifi()`**: `frequency >= 4900` को “5 GHz” कहा गया है। 6 GHz नेटवर्क (Wi‑Fi 6E/7) भी इसी लेबल में आएँगे।
3. **सुरक्षा लेबल heuristic है — `showWifi()`**: capability string में `WPA3`, फिर `WPA2`, फिर `WEP` खोजा जाता है। मिश्रित/transition mode, WPA enterprise या भविष्य के security modes गलत/सामान्य लेबल पा सकते हैं।
4. **सेल सूची की पंक्ति में पहचान विवरण कम हैं**: केवल radio family, dBm और registered flag हैं; cell timestamp, SIM/subscription, signal-strength subtype (RSRP/RSRQ/SINR आदि) या cell identity नहीं।
5. **permission denial संदेश आंशिक है**: “स्थायी रूप से अस्वीकार” स्थिति के लिए Settings तक जाने का मार्ग नहीं है। Permission-result callback में खाली `grantResults`/अप्रत्याशित परिणाम का अलग प्रबंधन भी नहीं है।
6. **रिलीज़ पहचान/वितरण metadata नहीं दिखता**: Manifest में `versionCode`/`versionName` नहीं हैं; वे Gradle में परिभाषित हैं, जो वैध है। कोई release signing configuration या वितरण निर्देश मौजूद नहीं।

## अनुशंसित सुधार (Recommended improvements)

1. **API सीमा साफ करें:** `CellInfoNr` और NR constants को `SDK_INT >= 29` शाखा में रखें; Android 10+ और पुराने संस्करणों पर अलग व्यवहार दें। Android 10–16 के वास्तविक उपकरण/API स्तरों पर smoke test करें।
2. **Dual-SIM स्पष्ट रूप से लागू करें:** `SubscriptionManager` से सक्रिय subscriptions लें, अनुमति/उपलब्धता जाँचें, प्रत्येक subscription के लिए `createForSubscriptionId()` उपयोग करें और UI में SIM slot/carrier दिखाएँ। एक SIM पर fallback भी दें।
3. **ताज़गी और असफलता स्पष्ट करें:** सेल जानकारी के लिए उपयुक्त callback/update API पर विचार करें; Wi‑Fi के लिए `startScan()` के boolean और broadcast के `EXTRA_RESULTS_UPDATED` को अलग-अलग संभालें। cell/scan timestamps दिखाएँ और पुराने परिणामों को स्पष्ट चिह्नित करें।
4. **Permission UX बेहतर करें:** फीचर-आधारित permission rationale दें, denial/permanent denial समझाएँ और आवश्यक होने पर Settings का बटन दें। Wi‑Fi scan से पहले Location services की स्थिति जाँचकर उपयोगकर्ता को कारण बताएँ। नए Android संस्करणों पर संबंधित Wi‑Fi permission और उसके वास्तविक API उपयोग का परीक्षण करें; अनावश्यक permission न माँगें।
5. **Android 15+ layout जाँचें:** edge-to-edge insets लागू करें और Android 15/API 35 पर status bar, navigation bar तथा gesture navigation के साथ दृश्य परीक्षण करें। Android 16/API 36 पर भी रनटाइम परीक्षण करें।
6. **नेटवर्क लेबलिंग मजबूत करें:** `ScanResult` की आवृत्ति से 2.4/5/6 GHz अलग करें; सुरक्षा और cellular signal मापों के लिए उपलब्ध संरचित API/fields लें तथा अज्ञात मानों को स्पष्ट रखें।
7. **APK build/release प्रक्रिया दर्ज करें:** Gradle wrapper जोड़ें/सुनिश्चित करें (वर्तमान checkout में `gradlew` और `gradle/wrapper` नहीं मिले), JDK 17, Android SDK Platform 35 और उपयुक्त Build Tools उपलब्ध कराएँ। Release APK के लिए signing config/keystore को सुरक्षित रूप से configure करें; debug APK को release वितरण न मानें। CI में assemble और कम-से-कम Android 10, 15 तथा 16 पर परीक्षण रखें।

## Android 10–16 और APK निर्माण स्थिति

| विषय | निरीक्षण से निष्कर्ष |
|---|---|
| Android 10/API 29 | `targetSdk 35` और `compileSdk 35` से build target संभव; API 29+ cell API branch का ध्यान आवश्यक। वास्तविक runtime सत्यापन नहीं हुआ। |
| Android 11–14/API 30–34 | सामान्य source उपयोग संगत दिखता है, पर Wi‑Fi throttling, location policy, निर्माता और permission व्यवहार के कारण device testing चाहिए। |
| Android 15/API 35 | ऐप target 35 करता है; edge-to-edge insets न होने से UI सत्यापन आवश्यक। |
| Android 16/API 36 | compile/target SDK 35 है। स्थापित/चलने की क्षमता का दावा build/device परीक्षण के बिना नहीं किया जा सकता; API 36 target व्यवहार कॉन्फ़िगर नहीं है। |
| APK build | Android Gradle Plugin 8.7.3, `compileSdk 35`, `minSdk 26`, `targetSdk 35`, Java source code। AGP 8.7 के लिए JDK 17 आवश्यक है। SDK/Build Tools installed होने चाहिए। |
| Build tooling | Repository में Gradle wrapper (`gradlew`, `gradle/wrapper`) नहीं मिला; स्थानीय Gradle/Android Studio पर निर्भरता है। कोई compiled APK मौजूद नहीं। |
| Signing | स्पष्ट release signing configuration नहीं मिली। स्थानीय debug build और signed release artifact अलग आवश्यकताएँ हैं। |

**सीमा:** इस रिपोर्ट के निष्कर्ष उपलब्ध source snapshot पर आधारित हैं। कोई फ़ाइल स्रोत/कॉन्फ़िगरेशन में नहीं बदली गई और कोई package install या build/test command नहीं चलाया गया।
