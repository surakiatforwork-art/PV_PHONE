package com.lody.virtual.client.hook.proxies.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.helper.compat.ParceledListSliceCompat;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Translate the public channel API at the host Binder boundary, without mutating guest objects. */
public final class NotificationChannelHooks extends MethodProxy {
    private final String name;
    private NotificationChannelHooks(String name) { this.name = name; }
    @Override public String getMethodName() { return name; }

    public static void bind(NotificationManagerStub stub) {
        for (String name : new String[]{"createNotificationChannels", "createNotificationChannelGroups",
                "getNotificationChannels", "getNotificationChannel", "getConversationNotificationChannel",
                "deleteNotificationChannel", "deleteNotificationChannelGroup", "getNotificationChannelGroup",
                "getNotificationChannelGroups", "getNotificationChannelGroupsWithoutChannels"}) {
            stub.addMethodProxy(new NotificationChannelHooks(name));
        }
    }

    private static String prefix() { return "phantom:" + getAppUserId() + ":" + getAppPkg() + ":"; }
    private static String hostId(String id) {
        if (id == null) return null;
        String result = prefix() + id;
        // Android truncates IDs at 1000 characters. Fail explicitly instead of merging two channels.
        if (result.length() > 1000) throw new IllegalArgumentException("Guest notification ID is too long");
        return result;
    }
    private static String guestId(String id) {
        return id != null && id.startsWith(prefix()) ? id.substring(prefix().length()) : null;
    }
    private static void field(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
    private static <T extends Parcelable> T copy(T object, Parcelable.Creator<T> creator) {
        Parcel parcel = Parcel.obtain();
        try { object.writeToParcel(parcel, 0); parcel.setDataPosition(0); return creator.createFromParcel(parcel); }
        finally { parcel.recycle(); }
    }
    private static NotificationChannel channel(NotificationChannel source, boolean toHost) throws Exception {
        String id = toHost ? hostId(source.getId()) : guestId(source.getId());
        if (id == null) return null;
        NotificationChannel result = copy(source, NotificationChannel.CREATOR);
        field(result, "mId", id);
        result.setGroup(toHost ? hostId(source.getGroup()) : guestId(source.getGroup()));
        if (Build.VERSION.SDK_INT >= 30 && source.getParentChannelId() != null) {
            result.setConversationId(toHost ? hostId(source.getParentChannelId()) : guestId(source.getParentChannelId()),
                    source.getConversationId());
        }
        return result;
    }
    private static NotificationChannelGroup group(NotificationChannelGroup source, boolean toHost) throws Exception {
        String id = toHost ? hostId(source.getId()) : guestId(source.getId());
        if (id == null) return null;
        NotificationChannelGroup result = copy(source, NotificationChannelGroup.CREATOR);
        field(result, "mId", id);
        List<NotificationChannel> channels = new ArrayList<>();
        for (NotificationChannel item : source.getChannels()) {
            NotificationChannel mapped = channel(item, toHost);
            if (mapped != null) channels.add(mapped);
        }
        field(result, "mChannels", channels);
        return result;
    }
    private static Object item(Object source, boolean toHost) throws Exception {
        if (source instanceof NotificationChannel) return channel((NotificationChannel) source, toHost);
        if (source instanceof NotificationChannelGroup) return group((NotificationChannelGroup) source, toHost);
        throw new IllegalArgumentException("Unexpected notification channel payload");
    }
    private static Object list(Object source, boolean toHost) throws Exception {
        List<Object> result = new ArrayList<>();
        for (Object value : ParceledListSliceCompat.getList(source)) {
            Object mapped = item(value, toHost);
            if (mapped != null) result.add(mapped);
        }
        return ParceledListSliceCompat.create(result);
    }
    public static Notification notification(Notification source) throws Exception {
        if (Build.VERSION.SDK_INT < 26 || source.getChannelId() == null) return source;
        Notification result = source.clone();
        field(result, "mChannelId", hostId(source.getChannelId()));
        return result;
    }

    @Override public Object call(Object who, Method method, Object... args) throws Throwable {
        // The proxy is also present in the host. Host channels must remain untouched.
        if (getAppPkg() == null || getHostPkg().equals(getAppPkg())) return method.invoke(who, args);
        args[0] = getHostPkg();
        if (name.equals("createNotificationChannels") || name.equals("createNotificationChannelGroups")) {
            args[1] = list(args[1], true);
            return method.invoke(who, args);
        }
        if (name.equals("getNotificationChannels")) {
            // Android 10+ has callingPkg, targetPkg, Android userId; older releases have pkg only.
            if (args.length >= 3) { args[1] = getHostPkg(); args[2] = getRealUid() / 100000; }
            return list(method.invoke(who, args), false);
        }
        if (name.equals("getNotificationChannel") || name.equals("getConversationNotificationChannel")) {
            int idIndex = 1;
            if (args.length >= 4) {
                args[1] = getRealUid() / 100000;
                args[2] = getHostPkg();
                idIndex = 3;
            }
            args[idIndex] = hostId((String) args[idIndex]);
            NotificationChannel result = (NotificationChannel) method.invoke(who, args);
            return result == null ? null : channel(result, false);
        }
        if (name.equals("deleteNotificationChannel") || name.equals("deleteNotificationChannelGroup")
                || name.equals("getNotificationChannelGroup")) {
            args[1] = hostId((String) args[1]);
            Object result = method.invoke(who, args);
            return result instanceof NotificationChannelGroup ? group((NotificationChannelGroup) result, false) : result;
        }
        return list(method.invoke(who, args), false);
    }
}
