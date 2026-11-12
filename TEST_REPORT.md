# TOWERLESS v0.4 — परीक्षण रिपोर्ट

## किए गए सत्यापन

- बदले गए स्रोत का static review किया: `CellInfoNr` तथा `NETWORK_TYPE_NR` references API 29+ runtime checks के भीतर हैं।
- Wi‑Fi freshness का निर्णय scan request स्वीकार होने, संबंधित broadcast के `EXTRA_RESULTS_UPDATED`, `ScanResult.timestamp` के request से नया होने और 30 सेकंड की freshness window—इन सभी शर्तों पर निर्भर है।
- पुराने, unverifiable या timestamp-विहीन Wi‑Fi परिणाम “ताज़ा स्कैन” नहीं दिखते।
- Cellular timestamp age उपलब्ध होने पर प्रदर्शित होती है; `getAllCellInfo()` परिणाम को Live नहीं कहा जाता।
- `git diff --check` सफल रहा; whitespace/error output नहीं मिला।

## Build और runtime परीक्षण

- APK build नहीं चलाया जा सका: वातावरण में `java`, `gradle`, Gradle wrapper (`gradlew`) और Android SDK (`ANDROID_HOME`/`ANDROID_SDK_ROOT`) उपलब्ध नहीं हैं।
- Android API 26–28, 29+ या वास्तविक Wi‑Fi/cellular hardware पर runtime परीक्षण नहीं किया गया।
- कोई package install नहीं किया गया।

## निष्कर्ष

उपलब्ध static review में अनुरोधित बदलाव मौजूद हैं। Build और device-level व्यवहार की पुष्टि Android SDK/JDK/Gradle वाले वातावरण में अभी आवश्यक है।
