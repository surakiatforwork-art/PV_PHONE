package com.lody.virtual.client.hook.proxies.connectivity;

import android.content.Context;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.MethodProxy;
import java.lang.reflect.Method;
import mirror.android.net.IConnectivityManager;

/** Translate only this guest's network UID to the actual Binder caller. */
public class ConnectivityStub extends BinderInvocationProxy {
    public ConnectivityStub() {
        super(IConnectivityManager.Stub.asInterface, Context.CONNECTIVITY_SERVICE);
    }

    @Override protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new NetworkUid("getNetworkInfoForUid", 1));
        addMethodProxy(new NetworkUid("getActiveNetworkInfoForUid", 0));
        addMethodProxy(new NetworkUid("getActiveNetworkForUid", 0));
    }

    private static final class NetworkUid extends MethodProxy {
        private final String name;
        private final int index;
        NetworkUid(String name, int index) { this.name = name; this.index = index; }
        @Override public String getMethodName() { return name; }
        @Override public boolean isEnable() { return isAppProcess(); }
        @Override public Object call(Object who, Method method, Object... args) throws Throwable {
            if (args != null && index < args.length && args[index] instanceof Integer) {
                int uid = (Integer) args[index];
                if (uid == getVUid() || uid == getBaseVUid()) args[index] = getRealUid();
            }
            // Keep Android's real network result, block status and permission enforcement.
            return method.invoke(who, args);
        }
    }
}
