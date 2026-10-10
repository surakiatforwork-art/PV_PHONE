package io.virtualapp.home;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.ipc.VPackageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Bridges guest permission requests to the real PHANToM VPhone host process.
 *
 * Virtual apps execute under the host UID, so Android runtime/app-op permissions
 * must be granted to the host. The virtual PackageManager already mirrors host
 * permission state back to guest apps.
 */
final class GuestPermissionBroker {

    private static final String TAG = "PHANToM.Permission";

    static final String SPECIAL_ALL_FILES = "all_files";
    static final String SPECIAL_OVERLAY = "overlay";
    static final String SPECIAL_WRITE_SETTINGS = "write_settings";
    static final String SPECIAL_INSTALL_PACKAGES = "install_packages";
    static final String SPECIAL_EXACT_ALARM = "exact_alarm";

    static final class Plan {
        final List<String> runtimePermissions = new ArrayList<>();
        String specialKey;
        Intent specialIntent;

        boolean isReady() {
            return runtimePermissions.isEmpty() && specialIntent == null;
        }
    }

    private GuestPermissionBroker() {
    }

    static Plan buildPlan(Activity activity, String guestPackage,
                          boolean runtimeAlreadyAttempted,
                          Set<String> attemptedSpecial) {
        Plan plan = new Plan();
        if (activity == null || TextUtils.isEmpty(guestPackage)) {
            return plan;
        }

        PackageInfo guestInfo;
        try {
            guestInfo = VPackageManager.get().getPackageInfo(
                    guestPackage, PackageManager.GET_PERMISSIONS, 0);
        } catch (Throwable e) {
            Log.w(TAG, "Unable to inspect guest permissions: " + guestPackage, e);
            return plan;
        }

        String[] requested = guestInfo == null ? null : guestInfo.requestedPermissions;
        if (requested == null || requested.length == 0) {
            return plan;
        }

        Set<String> requestedSet = new HashSet<>(Arrays.asList(requested));

        if (!runtimeAlreadyAttempted) {
            Set<String> hostDeclared = getHostDeclaredPermissions(activity);
            LinkedHashSet<String> missing = new LinkedHashSet<>();
            for (String permission : requested) {
                if (TextUtils.isEmpty(permission) || isSpecialPermission(permission)) {
                    continue;
                }
                if (!hostDeclared.contains(permission)) {
                    Log.i(TAG, "Guest requested permission not declared by host: " + permission);
                    continue;
                }
                if (!isDangerousPermission(activity, permission)) {
                    continue;
                }
                if (android.support.v4.content.ContextCompat.checkSelfPermission(
                        activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    missing.add(permission);
                }
            }
            plan.runtimePermissions.addAll(missing);
            if (!plan.runtimePermissions.isEmpty()) {
                return plan;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && requestedSet.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                && !Environment.isExternalStorageManager()
                && !attemptedSpecial.contains(SPECIAL_ALL_FILES)) {
            plan.specialKey = SPECIAL_ALL_FILES;
            plan.specialIntent = new Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName()));
            return plan;
        }

        if (requestedSet.contains(Manifest.permission.SYSTEM_ALERT_WINDOW)
                && !Settings.canDrawOverlays(activity)
                && !attemptedSpecial.contains(SPECIAL_OVERLAY)) {
            plan.specialKey = SPECIAL_OVERLAY;
            plan.specialIntent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName()));
            return plan;
        }

        if (requestedSet.contains(Manifest.permission.WRITE_SETTINGS)
                && !Settings.System.canWrite(activity)
                && !attemptedSpecial.contains(SPECIAL_WRITE_SETTINGS)) {
            plan.specialKey = SPECIAL_WRITE_SETTINGS;
            plan.specialIntent = new Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + activity.getPackageName()));
            return plan;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && requestedSet.contains(Manifest.permission.REQUEST_INSTALL_PACKAGES)
                && !activity.getPackageManager().canRequestPackageInstalls()
                && !attemptedSpecial.contains(SPECIAL_INSTALL_PACKAGES)) {
            plan.specialKey = SPECIAL_INSTALL_PACKAGES;
            plan.specialIntent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()));
            return plan;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && requestedSet.contains(Manifest.permission.SCHEDULE_EXACT_ALARM)
                && !attemptedSpecial.contains(SPECIAL_EXACT_ALARM)) {
            AlarmManager alarmManager =
                    (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                plan.specialKey = SPECIAL_EXACT_ALARM;
                plan.specialIntent = new Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + activity.getPackageName()));
                return plan;
            }
        }

        return plan;
    }

    static String describeSpecial(String key) {
        if (SPECIAL_ALL_FILES.equals(key)) {
            return "แอปนี้ต้องการสิทธิ์จัดการไฟล์ทั้งหมด ระบบจะเปิดหน้า All files access ของ PHANToM VPhone";
        }
        if (SPECIAL_OVERLAY.equals(key)) {
            return "แอปนี้ต้องการสิทธิ์แสดงทับแอปอื่น ระบบจะเปิดหน้า Display over other apps ของ PHANToM VPhone";
        }
        if (SPECIAL_WRITE_SETTINGS.equals(key)) {
            return "แอปนี้ต้องการสิทธิ์แก้ไขการตั้งค่าระบบ ระบบจะเปิดหน้า Modify system settings ของ PHANToM VPhone";
        }
        if (SPECIAL_INSTALL_PACKAGES.equals(key)) {
            return "แอปนี้ต้องการติดตั้งแพ็กเกจ ระบบจะเปิดหน้า Install unknown apps ของ PHANToM VPhone";
        }
        if (SPECIAL_EXACT_ALARM.equals(key)) {
            return "แอปนี้ต้องการสิทธิ์ตั้งเวลาปลุกแบบแม่นยำ ระบบจะเปิดหน้า Alarms & reminders ของ PHANToM VPhone";
        }
        return "แอปนี้ต้องการสิทธิ์พิเศษจาก Android ในนาม PHANToM VPhone";
    }

    private static boolean isSpecialPermission(String permission) {
        return Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(permission)
                || Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)
                || Manifest.permission.WRITE_SETTINGS.equals(permission)
                || Manifest.permission.REQUEST_INSTALL_PACKAGES.equals(permission)
                || Manifest.permission.SCHEDULE_EXACT_ALARM.equals(permission);
    }

    private static Set<String> getHostDeclaredPermissions(Context context) {
        Set<String> out = new HashSet<>();
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null) {
                out.addAll(Arrays.asList(info.requestedPermissions));
            }
        } catch (Throwable e) {
            Log.w(TAG, "Unable to read host manifest permissions", e);
        }
        return out;
    }

    private static boolean isDangerousPermission(Context context, String permission) {
        try {
            PermissionInfo info = context.getPackageManager().getPermissionInfo(permission, 0);
            int base = info.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE;
            return base == PermissionInfo.PROTECTION_DANGEROUS;
        } catch (Throwable e) {
            return false;
        }
    }
}
