package com.lody.virtual.client.hook.proxies.location;

import android.content.pm.PackageManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.ipc.VPackageManager;
import java.lang.reflect.Method;

/** Device-location boundary shared by every guest package and virtual user. */
public final class GuestLocationHooks {
    private GuestLocationHooks() {}

    public static void bind(LocationManagerStub stub) {
        bind(stub, "getLastLocation", 2, false);
        bind(stub, "getCurrentLocation", 3, false);
        bind(stub, "registerLocationListener", 3, false);
        bind(stub, "registerLocationPendingIntent", 3, false);
        bind(stub, "registerGnssStatusCallback", 1, true);
        bind(stub, "registerGnssNmeaCallback", 1, true);
        bind(stub, "addGnssMeasurementsListener", 2, true);
        bind(stub, "addGnssNavigationMessageListener", 1, true);
        bind(stub, "addGnssAntennaInfoListener", 1, true);
    }

    private static void bind(LocationManagerStub stub, String name, int packageIndex, boolean fineOnly) {
        stub.getInvocationStub().removeMethodProxy(name);
        stub.addMethodProxy(new DeviceLocation(name, packageIndex, fineOnly));
    }

    static final class DeviceLocation extends MethodProxy {
        private final String name;
        private final int packageIndex;
        private final boolean fineOnly;
        DeviceLocation(String name, int index, boolean fineOnly) {
            this.name = name; this.packageIndex = index; this.fineOnly = fineOnly;
        }
        @Override public String getMethodName() { return name; }
        @Override public boolean isEnable() { return isAppProcess(); }
        @Override public Object call(Object who, Method method, Object... args) throws Throwable {
            String guest = getAppPkg();
            boolean fine = VPackageManager.get().checkPermission(
                    "android.permission.ACCESS_FINE_LOCATION", guest, getAppUserId()) == PackageManager.PERMISSION_GRANTED;
            boolean coarse = !fineOnly && VPackageManager.get().checkPermission(
                    "android.permission.ACCESS_COARSE_LOCATION", guest, getAppUserId()) == PackageManager.PERMISSION_GRANTED;
            if (!fine && !coarse) throw new SecurityException("Guest location permission denied");
            if (!fine && VirtualCore.get().getPackageManager().checkPermission(
                    "android.permission.ACCESS_FINE_LOCATION", VirtualCore.get().getHostPkg()) == PackageManager.PERMISSION_GRANTED) {
                // Android filters precision by the shared host UID. Do not expose
                // the host's precise fixes to a coarse-only or fine-denied guest.
                throw new SecurityException("Coarse-only guest needs a precision-filtered location bridge");
            }
            if (args == null || args.length <= packageIndex || !(args[packageIndex] instanceof String)) {
                throw new IllegalArgumentException("Unsupported location signature: " + name);
            }
            args[packageIndex] = VirtualCore.get().getHostPkg();
            // Guest attribution tags are not declared by the host. Keep provider,
            // request, callback, listener ID and Android's actual result intact.
            if (args.length > packageIndex + 1) args[packageIndex + 1] = null;
            return method.invoke(who, args);
        }
    }
}
