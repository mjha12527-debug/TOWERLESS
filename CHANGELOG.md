# Changelog

## TOWERLESS v0.4 — Compatibility & Reliability

- API 26–28 पर NR cell type और NR network type के Android 10+ references को runtime API 29 guard के पीछे रखा।
- Wi‑Fi scan request की स्वीकृति, `EXTRA_RESULTS_UPDATED`, और हर `ScanResult.timestamp` का अलग सत्यापन जोड़ा। केवल स्वीकार की गई pending request, updated broadcast, request के बाद का timestamp और 30 सेकंड के भीतर का परिणाम “ताज़ा स्कैन” कहलाता है।
- Wi‑Fi परिणामों के साथ timestamp age दिखती है; अनुपलब्ध timestamp और पुराने/असत्यापित परिणाम अलग बताए जाते हैं।
- Cellular `CellInfo` timestamp से age/freshness दिखाई जाती है। जानकारी को snapshot और स्पष्ट रूप से “Live नहीं” बताया जाता है।
- Wi‑Fi आवृत्ति को 2.4 GHz, 5 GHz, 6 GHz या Unknown में अलग दिखाया जाता है।
- Cellular timestamp भविष्य का हो तो उसे अमान्य/अज्ञात दिखाया जाता है; उसे हालिया नहीं माना जाता।
- Pending Wi‑Fi scan 30 सेकंड में timeout होता है। Timeout के बाद आने वाला broadcast पुराने request से match नहीं होता और परिणाम Fresh नहीं दिखते।
- Activity नष्ट होने पर pending timeout callback और BroadcastReceiver साफ़ किए जाते हैं।
- Dual SIM और अन्य नए फीचर इस चरण में शामिल नहीं हैं।
