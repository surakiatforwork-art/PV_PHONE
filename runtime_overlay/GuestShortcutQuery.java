package com.lody.virtual.client.hook.proxies.shortcut;

import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.os.PersistableBundle;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.helper.compat.ParceledListSliceCompat;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Modern query: authorize the host caller, then expose only this guest's routes. */
public final class GuestShortcutQuery extends MethodProxy {
    @Override public String getMethodName() { return "getShortcuts"; }
    @Override public boolean isEnable() { return isAppProcess(); }
    @Override public Object call(Object who, Method method, Object... args) throws Throwable {
        Class<?>[] types = method.getParameterTypes();
        if (types.length != 3 || types[0] != String.class || types[1] != int.class || types[2] != int.class)
            throw new IllegalArgumentException("Unsupported shortcut query signature");
        if (!getAppPkg().equals(args[0])) throw new SecurityException("Shortcut caller mismatch");
        args[0] = getHostPkg();
        args[2] = VirtualCore.get().myUid() / 100000;
        Object result = method.invoke(who, args); // Android errors remain authoritative.
        if (result == null) return null;
        List visible = new ArrayList();
        for (Object item : ParceledListSliceCompat.getList(result)) {
            if (item instanceof ShortcutInfo && owned((ShortcutInfo)item)) visible.add(item);
        }
        return ParceledListSliceCompat.create(visible);
    }
    private boolean owned(ShortcutInfo shortcut) {
        PersistableBundle[] routes = mirror.android.content.pm.ShortcutInfo.mIntentPersistableExtrases.get(shortcut);
        if (routes == null || routes.length == 0) return false;
        for (PersistableBundle route : routes) {
            if (route == null || !route.containsKey("_VA_|_user_id_")
                    || route.getInt("_VA_|_user_id_") != getAppUserId()) return false;
            String uri = route.getString("_VA_|_uri_");
            if (uri == null) return false;
            try {
                Intent intent = Intent.parseUri(uri, 0);
                String pkg = intent.getComponent() != null ? intent.getComponent().getPackageName() : intent.getPackage();
                if (!getAppPkg().equals(pkg)) return false;
            } catch (Exception e) { return false; }
        }
        return true;
    }
}
