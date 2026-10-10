package com.lody.virtual.helper.utils;

import android.content.Context;

import com.lody.virtual.client.core.VirtualCore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Per-guest permission policy persisted in the PHANToM host sandbox.
 *
 * DEFAULT: inherit the real host permission state.
 * ALLOW:   explicitly allow, still limited by the real host permission state.
 * DENY:    deny to this guest even when the host owns the permission.
 */
public final class GuestPermissionPolicy {

    public static final int MODE_DEFAULT = 0;
    public static final int MODE_ALLOW = 1;
    public static final int MODE_DENY = 2;

    private static final Object LOCK = new Object();
    private static final String DIR_NAME = "phantom_guest_permissions";

    private GuestPermissionPolicy() {
    }

    public static int getMode(String packageName, int userId, String permission) {
        if (packageName == null || permission == null) {
            return MODE_DEFAULT;
        }
        synchronized (LOCK) {
            Properties properties = load(packageName, userId);
            try {
                return Integer.parseInt(properties.getProperty(permission, "0"));
            } catch (Throwable ignored) {
                return MODE_DEFAULT;
            }
        }
    }

    public static boolean isDenied(String packageName, int userId, String permission) {
        return getMode(packageName, userId, permission) == MODE_DENY;
    }

    public static boolean isExplicitlyAllowed(String packageName, int userId, String permission) {
        return getMode(packageName, userId, permission) == MODE_ALLOW;
    }

    public static boolean setMode(String packageName, int userId, String permission, int mode) {
        if (packageName == null || permission == null) {
            return false;
        }
        if (mode != MODE_DEFAULT && mode != MODE_ALLOW && mode != MODE_DENY) {
            return false;
        }
        synchronized (LOCK) {
            Properties properties = load(packageName, userId);
            if (mode == MODE_DEFAULT) {
                properties.remove(permission);
            } else {
                properties.setProperty(permission, String.valueOf(mode));
            }
            return save(packageName, userId, properties);
        }
    }

    public static boolean clear(String packageName, int userId) {
        synchronized (LOCK) {
            File file = policyFile(packageName, userId);
            return !file.exists() || file.delete();
        }
    }

    private static Properties load(String packageName, int userId) {
        Properties properties = new Properties();
        File file = policyFile(packageName, userId);
        if (!file.isFile()) {
            return properties;
        }
        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        } catch (Throwable ignored) {
        }
        return properties;
    }

    private static boolean save(String packageName, int userId, Properties properties) {
        File file = policyFile(packageName, userId);
        File dir = file.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            return false;
        }

        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temp)) {
            properties.store(output, "PHANToM VPhone guest permission policy");
            output.flush();
        } catch (Throwable e) {
            temp.delete();
            return false;
        }

        if (file.exists() && !file.delete()) {
            temp.delete();
            return false;
        }
        if (!temp.renameTo(file)) {
            temp.delete();
            return false;
        }
        return true;
    }

    private static File policyFile(String packageName, int userId) {
        Context context = VirtualCore.get().getContext();
        File dir = new File(context.getFilesDir(), DIR_NAME);
        String safePackage = packageName.replaceAll("[^A-Za-z0-9._-]", "_");
        return new File(dir, safePackage + "_u" + userId + ".properties");
    }
}
