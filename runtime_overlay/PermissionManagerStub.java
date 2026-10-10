package com.lody.virtual.client.hook.proxies.permission;

import android.os.IInterface;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.os.VUserHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Android 12+ package permission checks moved from IPackageManager to this service. */
public final class PermissionManagerStub extends BinderInvocationProxy {
    public PermissionManagerStub() {
        super(stubClass(), "permissionmgr");
    }

    private static Class<?> stubClass() {
        try { return Class.forName("android.permission.IPermissionManager$Stub"); }
        catch (ClassNotFoundException e) { throw new IllegalStateException(e); }
    }

    @Override protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new MethodProxy() {
            @Override public String getMethodName() { return "checkPermission"; }
            @Override public Object call(Object who, Method method, Object... args) throws Throwable {
                String pkg = (String) args[0];
                String permission = (String) args[1];
                if (VirtualCore.get().isAppInstalled(pkg)) {
                    return VPackageManager.get().checkPermission(permission, pkg, VUserHandle.myUserId());
                }
                return method.invoke(who, args);
            }
        });
    }

    @Override public void inject() throws Throwable {
        super.inject();
        IInterface proxy = getInvocationStub().getProxyInterface();
        Class<?> thread = Class.forName("android.app.ActivityThread");
        Field service = thread.getDeclaredField("sPermissionManager");
        service.setAccessible(true);
        service.set(null, proxy);
        Object manager = VirtualCore.get().getContext().getSystemService("permission");
        Class<?> type = Class.forName("android.permission.PermissionManager");
        if (manager != null) {
            Field field = type.getDeclaredField("mPermissionManager");
            field.setAccessible(true);
            field.set(manager, proxy);
        }
        // Per-guest policies can change independently of Android's global cache invalidation.
        for (String name : new String[]{"disablePermissionCache", "disablePackageNamePermissionCache"}) {
            Method disable = type.getDeclaredMethod(name);
            disable.setAccessible(true);
            disable.invoke(null);
        }
    }
}
