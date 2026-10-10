package com.lody.virtual.client.hook.proxies.wifi_scanner;

import android.os.Build;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.BinderInvocationStub;
import com.lody.virtual.client.hook.base.MethodProxy;
import java.lang.reflect.Method;
import mirror.android.os.ServiceManager;

/** Modern scanners use the platform Binder and its real permission/failure callbacks. */
public class WifiScannerStub extends BinderInvocationProxy {
    public WifiScannerStub() { super(scanner(), "wifiscanner"); }

    private static BinderInvocationStub scanner() {
        if (Build.VERSION.SDK_INT < 34) return new BinderInvocationStub(new GhostWifiScannerImpl());
        try {
            return new BinderInvocationStub(Class.forName("android.net.wifi.IWifiScanner$Stub"),
                    ServiceManager.getService.call("wifiscanner"));
        } catch (ClassNotFoundException e) { throw new IllegalStateException(e); }
    }

    @Override protected void onBindMethods() {
        super.onBindMethods();
        if (Build.VERSION.SDK_INT < 34) return;
        bind("getAvailableChannels", 1);
        bind("setScanningEnabled", 2);
        bind("registerScanListener", 1);
        bind("unregisterScanListener", 1);
        bind("startBackgroundScan", 3);
        bind("stopBackgroundScan", 1);
        bind("getScanResults", 0);
        bind("startScan", 3);
        bind("stopScan", 1);
        bind("getSingleScanResults", 0);
        bind("getCachedScanData", 0);
        bind("startPnoScan", 2);
        bind("stopPnoScan", 1);
    }

    private void bind(final String name, final int packageIndex) {
        addMethodProxy(new MethodProxy() {
            @Override public String getMethodName() { return name; }
            @Override public boolean isEnable() { return isAppProcess(); }
            @Override public Object call(Object who, Method method, Object... args) throws Throwable {
                if (args == null || args.length <= packageIndex || !(args[packageIndex] instanceof String)) {
                    throw new IllegalArgumentException("Unsupported Wi-Fi scanner signature: " + name);
                }
                args[packageIndex] = VirtualCore.get().getHostPkg();
                if (args.length > packageIndex + 1 && method.getParameterTypes()[packageIndex + 1] == String.class) {
                    args[packageIndex + 1] = null;
                }
                return method.invoke(who, args);
            }
        });
    }
}
