package com.lody.virtual.client.hook.proxies.locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.LocaleList;
import com.lody.virtual.client.VClientImpl;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.MethodProxy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** App-specific language state belongs to the guest, never to the shared host package. */
public final class LocaleManagerStub extends BinderInvocationProxy {
    public LocaleManagerStub() { super(stubClass(), "locale"); }
    private static Class<?> stubClass() {
        try { return Class.forName("android.app.ILocaleManager$Stub"); }
        catch (ClassNotFoundException e) { throw new IllegalStateException(e); }
    }
    private static SharedPreferences preferences() {
        Context app = VClientImpl.get().getCurrentApplication();
        if (app == null) throw new IllegalStateException("Guest application is not initialized");
        return app.getSharedPreferences("phantom_app_locales", Context.MODE_PRIVATE);
    }
    private static void ownPackage(Object[] args) {
        if (!getAppPkg().equals(args[0])) throw new SecurityException("Cross-guest locale access is unsupported");
    }
    @Override protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new MethodProxy() {
            @Override public String getMethodName() { return "getApplicationLocales"; }
            @Override public Object call(Object who, Method method, Object... args) throws Throwable {
                ownPackage(args);
                return LocaleList.forLanguageTags(preferences().getString("tags", ""));
            }
        });
        addMethodProxy(new MethodProxy() {
            @Override public String getMethodName() { return "setApplicationLocales"; }
            @Override public Object call(Object who, Method method, Object... args) throws Throwable {
                ownPackage(args);
                LocaleList locales = (LocaleList) args[2];
                if (locales == null) throw new IllegalArgumentException("locales == null");
                if (!preferences().edit().putString("tags", locales.toLanguageTags()).commit()) {
                    throw new IllegalStateException("Unable to persist guest locales");
                }
                Context app = VClientImpl.get().getCurrentApplication();
                Configuration config = new Configuration(app.getResources().getConfiguration());
                config.setLocales(locales.isEmpty() ? LocaleList.getDefault() : locales);
                app.getResources().updateConfiguration(config, app.getResources().getDisplayMetrics());
                // Guests may recreate their activities, or restart, to apply their own language change.
                return null;
            }
        });
    }
    @Override public void inject() throws Throwable {
        super.inject();
        Object manager = VirtualCore.get().getContext().getSystemService("locale");
        if (manager != null) {
            Field service = manager.getClass().getDeclaredField("mService");
            service.setAccessible(true);
            service.set(manager, getInvocationStub().getProxyInterface());
        }
    }
}
