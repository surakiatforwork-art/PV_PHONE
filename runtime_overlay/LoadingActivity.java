package io.virtualapp.home;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.server.pm.parser.VPackage;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.repo.PackageAppDataStorage;
import io.virtualapp.widgets.EatBeansView;
import jonathanfinerty.once.Once;

/**
 * @author Lody
 */

public class LoadingActivity extends VActivity {

    private static final String TAG = "LoadingActivity";

    private PackageAppData appModel;
    private EatBeansView loadingView;

    private static final int REQUEST_PERMISSION_CODE = 100;
    private static final int REQUEST_SPECIAL_PERMISSION_CODE = 101;

    private Intent intentToLaunch;
    private int userToLaunch;

    private long start;
    private boolean runtimePermissionsAttempted;
    private final java.util.Set<String> attemptedSpecialPermissions = new java.util.HashSet<>();

    public static boolean launch(Context context, String packageName, int userId) {
        Intent intent = VirtualCore.get().getLaunchIntent(packageName, userId);
        if (intent != null) {
            Intent loadingPageIntent = new Intent(context, LoadingActivity.class);
            loadingPageIntent.putExtra(Constants.PASS_PKG_NAME_ARGUMENT, packageName);
            loadingPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            loadingPageIntent.putExtra(Constants.PASS_KEY_INTENT, intent);
            loadingPageIntent.putExtra(Constants.PASS_KEY_USER, userId);
            context.startActivity(loadingPageIntent);
            return true;
        } else {
            return false;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        start = SystemClock.elapsedRealtime();

        setContentView(R.layout.activity_loading);
        loadingView = (EatBeansView) findViewById(R.id.loading_anim);
        int userId = getIntent().getIntExtra(Constants.PASS_KEY_USER, -1);
        String pkg = getIntent().getStringExtra(Constants.PASS_PKG_NAME_ARGUMENT);
        appModel = PackageAppDataStorage.get().acquire(pkg);
        if (appModel == null) {
            Toast.makeText(getApplicationContext(), "Open App:" + pkg + " failed.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView iconView = (ImageView) findViewById(R.id.app_icon);
        iconView.setImageDrawable(appModel.icon);
        TextView nameView = (TextView) findViewById(R.id.app_name);
        nameView.setText(String.format(Locale.ENGLISH, "Opening %s...", appModel.name));
        Intent intent = getIntent().getParcelableExtra(Constants.PASS_KEY_INTENT);
        if (intent == null) {
            finish();
            return;
        }
        VirtualCore.get().setUiCallback(intent, mUiCallback);

        try {
            // 如果已经在运行了，那么直接拉起，不做任何检测。
            boolean uiRunning = false;
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
                    String appProcessName = VActivityManager.get().getAppProcessName(runningAppProcess.pid);
                    if (TextUtils.equals(appProcessName, pkg)) {
                        uiRunning = true;
                        break;
                    }
                }
            }

            VLog.i(TAG, pkg + "is running: " + uiRunning);
            if (uiRunning) {
                launchActivity(intent, userId);
                return;
            }
        } catch (Throwable ignored) {
            ignored.printStackTrace();
        }

        checkAndLaunch(intent, userId);
    }

    private void checkAndLaunch(Intent intent, int userId) {
        intentToLaunch = intent;
        userToLaunch = userId;

        // PHANToM location policy: Guests use the device LocationManager.
        // MODE_CLOSE disables VirtualApp's synthetic location layer.
        try {
            com.lody.virtual.client.ipc.VirtualLocationManager.get().setMode(
                    userId,
                    appModel.packageName,
                    com.lody.virtual.client.ipc.VirtualLocationManager.MODE_CLOSE);
        } catch (Throwable e) {
            Log.w(TAG, "Unable to enforce device location passthrough", e);
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            launchActivityWithDelay(intent, userId);
            return;
        }

        try {
            GuestPermissionBroker.Plan plan = GuestPermissionBroker.buildPlan(
                    this,
                    appModel.packageName,
                    runtimePermissionsAttempted,
                    attemptedSpecialPermissions);

            if (!plan.runtimePermissions.isEmpty()) {
                runtimePermissionsAttempted = true;
                String[] permissions = plan.runtimePermissions.toArray(new String[0]);
                Log.i(TAG, "Requesting host runtime permissions for guest "
                        + appModel.packageName + ": " + plan.runtimePermissions);

                new AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog_Alert)
                        .setTitle(R.string.permission_tip_title)
                        .setMessage("แอป " + appModel.name
                                + " ต้องการสิทธิ์จาก Android สิทธิ์ที่อนุญาตจะถูกมอบให้ PHANToM VPhone "
                                + "และใช้กับ Guest App นี้")
                        .setPositiveButton(R.string.permission_tips_confirm, (dialog, which) -> {
                            try {
                                ActivityCompat.requestPermissions(
                                        this, permissions, REQUEST_PERMISSION_CODE);
                            } catch (Throwable e) {
                                Log.w(TAG, "Runtime permission request failed", e);
                                continuePermissionFlow();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) ->
                                continuePermissionFlow())
                        .setOnCancelListener(dialog -> continuePermissionFlow())
                        .show();
                return;
            }

            if (plan.specialIntent != null && plan.specialKey != null) {
                attemptedSpecialPermissions.add(plan.specialKey);
                new AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog_Alert)
                        .setTitle("Special access")
                        .setMessage(GuestPermissionBroker.describeSpecial(plan.specialKey))
                        .setPositiveButton(R.string.permission_tips_confirm, (dialog, which) -> {
                            try {
                                startActivityForResult(
                                        plan.specialIntent, REQUEST_SPECIAL_PERMISSION_CODE);
                            } catch (Throwable e) {
                                Log.w(TAG, "Special permission page failed", e);
                                continuePermissionFlow();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) ->
                                continuePermissionFlow())
                        .setOnCancelListener(dialog -> continuePermissionFlow())
                        .show();
                return;
            }

            launchActivityWithDelay(intent, userId);
        } catch (Throwable e) {
            Log.e(TAG, "Permission broker failed", e);
            launchActivityWithDelay(intent, userId);
        }
    }

    private void continuePermissionFlow() {
        if (intentToLaunch != null) {
            checkAndLaunch(intentToLaunch, userToLaunch);
        } else {
            finish();
        }
    }

    private void launchActivityWithDelay(Intent intent, int userId) {
        final int MAX_WAIT = 1000;
        long delta = SystemClock.elapsedRealtime() - start;
        long waitTime = MAX_WAIT - delta;

        if (waitTime <= 0) {
            launchActivity(intent, userId);
        } else {
            loadingView.postDelayed(() -> launchActivity(intent, userId), waitTime);
        }
    }

    private void launchActivity(Intent intent, int userId) {
        try {
            VActivityManager.get().startActivity(intent, userId);
        } catch (Throwable e) {
            VLog.e(TAG, "start activity failed:", e);
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.start_app_failed, appModel.name), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this,
                        "บางสิทธิ์ไม่ได้รับอนุญาต Guest App อาจใช้งานฟังก์ชันบางส่วนไม่ได้",
                        Toast.LENGTH_LONG).show();
            }
            continuePermissionFlow();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SPECIAL_PERMISSION_CODE) {
            continuePermissionFlow();
        }
    }

    private final VirtualCore.UiCallback mUiCallback = new VirtualCore.UiCallback() {

        @Override
        public void onAppOpened(String packageName, int userId) throws RemoteException {
            finish();
        }

        @Override
        public void onOpenFailed(String packageName, int userId) throws RemoteException {
            VUiKit.defer().when(() -> {
            }).done((v) -> {
                if (!isFinishing()) {
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.start_app_failed, packageName),
                            Toast.LENGTH_SHORT).show();
                }
            });
            finish();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        startAnim();
    }

    private void startAnim() {
        if (loadingView != null) {
            loadingView.startAnim();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (loadingView != null) {
            loadingView.stopAnim();
        }
    }
}

