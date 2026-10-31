package com.lody.virtual.helper.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Explicit per-package, per-slot device metadata; never changes system properties. */
public final class GuestDeviceProfile {
    public static final String DEFAULT = "default";
    public static final String IQOO13 = "iqoo13";
    private GuestDeviceProfile() {}

    private static File file(File root, String pkg, int user) {
        if (pkg == null || !pkg.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+") || user < 0)
            throw new IllegalArgumentException("Invalid guest identity");
        return new File(new File(root, "phantom_device_profiles"), pkg + "_u" + user + ".properties");
    }

    public static String get(File root, String pkg, int user) {
        try (FileInputStream in = new FileInputStream(file(root, pkg, user))) {
            Properties p = new Properties(); p.load(in);
            return IQOO13.equals(p.getProperty("profile")) ? IQOO13 : DEFAULT;
        } catch (IOException | IllegalArgumentException e) { return DEFAULT; }
    }

    public static synchronized boolean set(File root, String pkg, int user, String profile) {
        if (!DEFAULT.equals(profile) && !IQOO13.equals(profile)) return false;
        File destination;
        try { destination = file(root, pkg, user); }
        catch (IllegalArgumentException e) { return false; }
        if (DEFAULT.equals(profile)) return !destination.exists() || destination.delete();
        File dir = destination.getParentFile();
        if (!dir.isDirectory() && !dir.mkdirs()) return false;
        File temp = null;
        try {
            temp = File.createTempFile("profile-", ".tmp", dir);
            Properties p = new Properties(); p.setProperty("profile", profile);
            try (FileOutputStream out = new FileOutputStream(temp)) {
                p.store(out, "PHANToM guest device profile"); out.getFD().sync();
            }
            if (!temp.renameTo(destination)) return false;
            return true;
        } catch (IOException e) { return false; }
        finally { if (temp != null && temp.exists()) temp.delete(); }
    }

    /** Called only inside the guest process before guest providers/application load. */
    public static void apply(File root, String pkg, int user) {
        if (!IQOO13.equals(get(root, pkg, user))) return;
        String[][] fields = {{"MODEL", "V2408A"}, {"BRAND", "iQOO"},
                {"MANUFACTURER", "iQOO"}, {"PRODUCT", "PD2408"}, {"DEVICE", "PD2408"}};
        try {
            Class<?> build = Class.forName("android.os.Build");
            for (String[] pair : fields) {
                java.lang.reflect.Field field = build.getDeclaredField(pair[0]);
                field.setAccessible(true); field.set(null, pair[1]);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot apply guest device profile", e);
        }
    }
}
