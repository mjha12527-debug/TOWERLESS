TOWERLESS Android v0.3 — Wi-Fi + Cellular Signal Monitor

यह Android Studio source project है, compiled APK नहीं।

नई सुविधा:
- READ_PHONE_STATE और ACCESS_FINE_LOCATION अनुमति मिलने पर ऑपरेटर नाम, डेटा नेटवर्क प्रकार (जहाँ Android देता है) और उपलब्ध cell signal dBm दिखाने का प्रयास।
- Wi-Fi scan results और signal dBm सूची।

Build:
1. ZIP extract करें।
2. Android Studio > Open > इस folder को चुनें।
3. Gradle Sync करें (Android Gradle Plugin 8.7.3, SDK 35)।
4. Build > Build Bundle(s) / APK(s) > Build APK(s)।
5. फोन में APK install करें। पहली बार SIM Signal और Wi-Fi scan के लिए मांगी गई अनुमति दें।
6. फोन में Wi-Fi तथा Location services चालू रखें।

सीमाएँ:
- Android version, निर्माता, carrier, dual-SIM configuration और permissions के कारण cell info छिपा/अधूरा हो सकता है।
- getAllCellInfo का परिणाम cached हो सकता है; इसे लगातार live tower tracking न समझें।
- ऐप tower coverage नहीं बढ़ाता, network switching नहीं करता, और बिना अनुमति hotspot में प्रवेश नहीं करता।
- इस वातावरण में Android build चलाकर APK compile/verify नहीं किया गया है।
