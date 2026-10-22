package de.hamid.lumatv.platform;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

/**
 * Current connection type for the status icon. Listens only while the dashboard is visible;
 * no Wi-Fi scan, no location, no network traffic of its own.
 */
public final class NetworkStatus {
    public enum Kind { WIFI, ETHERNET, OTHER, OFFLINE }

    public interface Listener { void onNetworkChanged(Kind kind); }

    private final ConnectivityManager manager;
    private ConnectivityManager.NetworkCallback callback;

    public NetworkStatus(Context context) {
        manager = context.getSystemService(ConnectivityManager.class);
    }

    public void start(Listener listener) {
        stop();
        listener.onNetworkChanged(current());
        if (manager == null) return;
        callback = new ConnectivityManager.NetworkCallback() {
            @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                listener.onNetworkChanged(kindOf(caps));
            }
            @Override public void onLost(Network network) {
                listener.onNetworkChanged(Kind.OFFLINE);
            }
        };
        try {
            manager.registerDefaultNetworkCallback(callback, new Handler(Looper.getMainLooper()));
        } catch (RuntimeException unavailable) {
            callback = null;
        }
    }

    public void stop() {
        if (manager != null && callback != null) {
            try {
                manager.unregisterNetworkCallback(callback);
            } catch (RuntimeException alreadyGone) {
                // Nothing left to release.
            }
        }
        callback = null;
    }

    public Kind current() {
        if (manager == null) return Kind.OFFLINE;
        try {
            Network active = manager.getActiveNetwork();
            return kindOf(active == null ? null : manager.getNetworkCapabilities(active));
        } catch (RuntimeException unavailable) {
            return Kind.OFFLINE;
        }
    }

    private static Kind kindOf(NetworkCapabilities caps) {
        if (caps == null) return Kind.OFFLINE;
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return Kind.ETHERNET;
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return Kind.WIFI;
        return Kind.OTHER;
    }
}
