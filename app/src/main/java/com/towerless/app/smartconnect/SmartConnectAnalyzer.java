package com.towerless.app.smartconnect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Read-only comparison of observations already collected by the v0.4 engine.
 * This class deliberately has no Android service access and cannot start scans
 * or request a network connection.
 */
public final class SmartConnectAnalyzer {
    private static final int WIFI_SIGNAL_WEIGHT = 60;
    private static final int WIFI_BAND_WEIGHT = 15;
    private static final int WIFI_SECURITY_WEIGHT = 15;
    private static final int WIFI_FRESHNESS_WEIGHT = 10;

    private static final int CELL_SIGNAL_WEIGHT = 60;
    private static final int CELL_TYPE_WEIGHT = 25;
    private static final int CELL_FRESHNESS_WEIGHT = 15;

    public enum Band { GHZ_2_4, GHZ_5, GHZ_6, UNKNOWN }
    public enum NetworkKind { WIFI, MOBILE }
    public enum RecommendationKind { WIFI, MOBILE, INSUFFICIENT_DATA }

    private SmartConnectAnalyzer() { }

    /** Creates a Wi-Fi observation from one result already read by v0.4. */
    public static Candidate wifi(String ssid, int rssiDbm, int frequencyMhz,
                                 String capabilities, boolean freshnessVerified,
                                 Boolean deviceBandCompatible) {
        Band band = bandForFrequency(frequencyMhz);
        String security = classifySecurity(capabilities);
        int bandScore = bandScore(band, deviceBandCompatible);
        int securityScore = securityScore(security);
        int score = weightedScore(
                signalScore(rssiDbm), WIFI_SIGNAL_WEIGHT,
                bandScore, WIFI_BAND_WEIGHT,
                securityScore, WIFI_SECURITY_WEIGHT,
                freshnessVerified ? 100 : 25, WIFI_FRESHNESS_WEIGHT);
        int confidence = confidence(4, freshnessVerified,
                security.equals("Unknown") || band == Band.UNKNOWN || deviceBandCompatible == null);
        String label = ssid == null || ssid.trim().isEmpty() ? "Wi-Fi hotspot" : ssid.trim();
        String details = label + " · " + bandLabel(band) + " · " + rssiDbm + " dBm · " + security;
        return new Candidate(NetworkKind.WIFI, details, score, confidence,
                freshnessVerified, band == Band.UNKNOWN || deviceBandCompatible == null
                ? "Band/device compatibility पूरी तरह उपलब्ध नहीं" : "Band compatibility जाँची गई",
                !Boolean.FALSE.equals(deviceBandCompatible));
    }

    /** Creates a cellular observation from CellInfo and TelephonyManager data already read by v0.4. */
    public static Candidate mobile(String operatorName, String networkType,
                                   Integer signalDbm, boolean freshnessVerified) {
        String type = normalizeNetworkType(networkType);
        int score = signalDbm == null
                ? weightedScore(networkTypeScore(type), 65,
                        freshnessVerified ? 100 : 25, 35)
                : weightedScore(cellularSignalScore(signalDbm, type), CELL_SIGNAL_WEIGHT,
                        networkTypeScore(type), CELL_TYPE_WEIGHT,
                        freshnessVerified ? 100 : 25, CELL_FRESHNESS_WEIGHT);
        int confidence = confidence(signalDbm == null ? 2 : 3, freshnessVerified,
                type.equals("Unknown") || signalDbm == null);
        String carrier = operatorName == null || operatorName.trim().isEmpty()
                ? "Operator अज्ञात" : operatorName.trim();
        String signal = signalDbm == null ? "signal Unknown" : signalDbm + " dBm";
        String details = carrier + " · " + type + " · " + signal;
        return new Candidate(NetworkKind.MOBILE, details, score, confidence,
                freshnessVerified, "Carrier/network data Android से उपलब्ध सीमा तक", true);
    }

    /** Compares supplied observations only; it does not collect or refresh data. */
    public static Analysis compare(List<Candidate> observations) {
        ArrayList<Candidate> candidates = new ArrayList<>();
        if (observations != null) {
            for (Candidate candidate : observations) {
                if (candidate != null && candidate.eligible) candidates.add(candidate);
            }
        }
        if (candidates.isEmpty()) {
            return new Analysis(Collections.<Candidate>emptyList(), RecommendationKind.INSUFFICIENT_DATA,
                    "तुलना के लिए कोई उपलब्ध network data नहीं।", 0);
        }
        Collections.sort(candidates, (left, right) -> Integer.compare(right.score, left.score));
        Candidate best = candidates.get(0);
        if (candidates.size() > 1 && best.kind != candidates.get(1).kind
                && best.score - candidates.get(1).score < 5) {
            return new Analysis(candidates, RecommendationKind.INSUFFICIENT_DATA,
                    "उपलब्ध माप लगभग बराबर हैं; निर्णायक recommendation के लिए और data चाहिए।",
                    Math.min(best.confidence, candidates.get(1).confidence));
        }
        RecommendationKind kind = best.kind == NetworkKind.WIFI
                ? RecommendationKind.WIFI : RecommendationKind.MOBILE;
        return new Analysis(candidates, kind,
                "उपलब्ध signal, band/security या network type के आधार पर बेहतर observed विकल्प: "
                        + best.details + ". यह Internet speed/availability की पुष्टि नहीं है.",
                best.confidence);
    }

    public static Band bandForFrequency(int frequencyMhz) {
        if (frequencyMhz >= 2400 && frequencyMhz < 2500) return Band.GHZ_2_4;
        if (frequencyMhz >= 4900 && frequencyMhz < 5925) return Band.GHZ_5;
        if (frequencyMhz >= 5925 && frequencyMhz <= 7125) return Band.GHZ_6;
        return Band.UNKNOWN;
    }

    public static String classifySecurity(String capabilities) {
        if (capabilities == null || capabilities.trim().isEmpty()) return "Unknown";
        String value = capabilities.toUpperCase(Locale.ROOT);
        if (value.contains("EAP") || value.contains("IEEE8021X")) return "Enterprise";
        if (value.contains("OWE")) return "Enhanced Open";
        if ((value.contains("SAE") || value.contains("WPA3"))
                && (value.contains("PSK") || value.contains("WPA2"))) return "WPA2/WPA3 transition";
        if (value.contains("SAE") || value.contains("WPA3")) return "WPA3";
        if (value.contains("WPA2") || value.contains("PSK")) return "WPA2/PSK";
        if (value.contains("WEP")) return "WEP";
        if (value.contains("ESS")) return "Open";
        return "Unknown";
    }

    private static int signalScore(int dbm) {
        if (dbm >= -50) return 100;
        if (dbm >= -60) return 85;
        if (dbm >= -67) return 70;
        if (dbm >= -75) return 50;
        if (dbm >= -85) return 30;
        return 10;
    }

    private static int cellularSignalScore(int dbm, String type) {
        // Approximate tiers only: dBm semantics and thresholds vary by radio technology/OEM.
        if ("2G".equals(type)) {
            if (dbm >= -75) return 100;
            if (dbm >= -85) return 80;
            if (dbm >= -95) return 60;
            if (dbm >= -105) return 40;
            return 20;
        }
        if (dbm >= -85) return 100;
        if (dbm >= -95) return 80;
        if (dbm >= -105) return 60;
        if (dbm >= -115) return 40;
        return 20;
    }

    private static int bandScore(Band band, Boolean compatible) {
        if (Boolean.FALSE.equals(compatible)) return 0;
        switch (band) {
            case GHZ_2_4:
            case GHZ_5:
            case GHZ_6: return 75;
            default: return 50;
        }
    }

    private static int securityScore(String security) {
        if ("WPA3".equals(security) || "Enterprise".equals(security)) return 100;
        if ("WPA2/WPA3 transition".equals(security)) return 90;
        if ("WPA2/PSK".equals(security)) return 80;
        if ("Enhanced Open".equals(security)) return 70;
        if ("Open".equals(security)) return 45;
        if ("WEP".equals(security)) return 20;
        return 50;
    }

    private static String normalizeNetworkType(String networkType) {
        if (networkType == null) return "Unknown";
        String value = networkType.trim().toUpperCase(Locale.ROOT);
        if (value.contains("5G") || value.contains("NR")) return "5G";
        if (value.contains("4G") || value.contains("LTE")) return "4G";
        if (value.contains("3G") || value.contains("HSPA") || value.contains("UMTS")) return "3G";
        if (value.contains("2G") || value.contains("GPRS") || value.contains("EDGE")
                || value.contains("GSM")) return "2G";
        return "Unknown";
    }

    private static int networkTypeScore(String type) {
        switch (type) {
            case "5G": return 90;
            case "4G": return 80;
            case "3G": return 60;
            case "2G": return 35;
            default: return 50;
        }
    }

    private static int weightedScore(int first, int firstWeight, int second, int secondWeight,
                                     int third, int thirdWeight) {
        return (first * firstWeight + second * secondWeight + third * thirdWeight)
                / (firstWeight + secondWeight + thirdWeight);
    }

    private static int weightedScore(int first, int firstWeight, int second, int secondWeight) {
        return (first * firstWeight + second * secondWeight) / (firstWeight + secondWeight);
    }

    private static int weightedScore(int first, int firstWeight, int second, int secondWeight,
                                     int third, int thirdWeight, int fourth, int fourthWeight) {
        return (first * firstWeight + second * secondWeight + third * thirdWeight
                + fourth * fourthWeight) / (firstWeight + secondWeight + thirdWeight + fourthWeight);
    }

    private static int confidence(int availableFactors, boolean fresh, boolean unknownTypeOrSecurity) {
        int score = 45 + (availableFactors * 8);
        if (!fresh) score -= 20;
        if (unknownTypeOrSecurity) score -= 10;
        // Internet validation and measured stability are intentionally absent in this phase.
        return Math.max(15, Math.min(65, score));
    }

    private static String bandLabel(Band band) {
        switch (band) {
            case GHZ_2_4: return "2.4 GHz";
            case GHZ_5: return "5 GHz";
            case GHZ_6: return "6 GHz";
            default: return "Band अज्ञात";
        }
    }

    public static final class Candidate {
        public final NetworkKind kind;
        public final String details;
        public final int score;
        public final int confidence;
        public final boolean freshnessVerified;
        public final String limitation;
        public final boolean eligible;

        private Candidate(NetworkKind kind, String details, int score, int confidence,
                          boolean freshnessVerified, String limitation, boolean eligible) {
            this.kind = kind;
            this.details = details;
            this.score = score;
            this.confidence = confidence;
            this.freshnessVerified = freshnessVerified;
            this.limitation = limitation;
            this.eligible = eligible;
        }
    }

    public static final class Analysis {
        public final List<Candidate> candidates;
        public final RecommendationKind recommendation;
        public final String explanation;
        public final int confidence;

        private Analysis(List<Candidate> candidates, RecommendationKind recommendation,
                         String explanation, int confidence) {
            this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates));
            this.recommendation = recommendation;
            this.explanation = explanation;
            this.confidence = confidence;
        }
    }
}
