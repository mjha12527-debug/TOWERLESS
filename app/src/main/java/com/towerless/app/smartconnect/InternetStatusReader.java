package com.towerless.app.smartconnect;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

/** Reads the current Android connectivity snapshot; it never requests or switches networks. */
public final class InternetStatusReader {
    public enum Status { VALIDATED_INTERNET, CAPTIVE_PORTAL, NO_INTERNET, UNKNOWN }

    public static final class Snapshot {
        public final Status wifi;
        public final Status mobile;

        private Snapshot(Status wifi, Status mobile) {
            this.wifi = wifi;
            this.mobile = mobile;
        }
    }

    private InternetStatusReader() { }

    public static Snapshot read(Context context) {
        if (Build.VERSION.SDK_INT >= 23
                && context.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            return unknownSnapshot();
        }

        try {
            ConnectivityManager manager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) return unknownSnapshot();

            Status wifiStatus = null;
            Status mobileStatus = null;
            Network[] networks = manager.getAllNetworks();
            if (networks == null) return unknownSnapshot();

            for (Network network : networks) {
                NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
                if (capabilities == null) continue;
                Status status = classify(capabilities);
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    wifiStatus = preferKnown(wifiStatus, status);
                }
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    mobileStatus = preferKnown(mobileStatus, status);
                }
            }
            return new Snapshot(wifiStatus == null ? Status.UNKNOWN : wifiStatus,
                    mobileStatus == null ? Status.UNKNOWN : mobileStatus);
        } catch (RuntimeException ignored) {
            return unknownSnapshot();
        }
    }

    private static Status classify(NetworkCapabilities capabilities) {
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)) {
            return Status.CAPTIVE_PORTAL;
        }
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            return Status.VALIDATED_INTERNET;
        }
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return Status.NO_INTERNET;
        }
        // Android knows this network is intended for Internet, but has not validated it yet.
        return Status.UNKNOWN;
    }

    private static Status preferKnown(Status current, Status candidate) {
        if (current == null || priority(candidate) > priority(current)) return candidate;
        return current;
    }

    private static int priority(Status status) {
        switch (status) {
            case VALIDATED_INTERNET: return 4;
            case CAPTIVE_PORTAL: return 3;
            case UNKNOWN: return 2;
            case NO_INTERNET:
            default: return 1;
        }
    }

    private static Snapshot unknownSnapshot() {
        return new Snapshot(Status.UNKNOWN, Status.UNKNOWN);
    }
}
