package io.virtualapp.home;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.TextView;

import com.lody.virtual.client.core.VirtualCore;

import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.home.adapters.HomeAppAdapter;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.repo.AppRepository;
import io.virtualapp.settings.SettingsActivity;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class NewHomeActivity extends AppCompatActivity {

    private static final String SHOW_DOZE_ALERT_KEY = "SHOW_DOZE_ALERT_KEY";
    private static final String TAG = NewHomeActivity.class.getSimpleName();
    private static final int REQUEST_CAMERA_PERMISSION = 0x5049;
    private Handler mUiHandler;
    private Toolbar mToolbar;

    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private View mEmptyView;
    private View mAddAction;
    private View mCameraAction;
    private TextView mCameraValue;
    private TextView mGuestCount;
    private HomeAppAdapter mAdapter;
    private AppRepository mRepository;

    public static void goHome(Context context) {
        Intent intent = new Intent(context, NewHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mUiHandler = new Handler(getMainLooper());

        // Toolbar — dark gray with white text
        mToolbar = findViewById(R.id.home_toolbar);
        setSupportActionBar(mToolbar);

        // Views
        mRecyclerView = findViewById(R.id.home_recycler_view);
        mProgressBar = findViewById(R.id.home_progress_bar);
        mEmptyView = findViewById(R.id.home_empty_view);
        mAddAction = findViewById(R.id.home_action_add);
        mCameraAction = findViewById(R.id.home_action_camera);
        mCameraValue = findViewById(R.id.home_camera_value);
        mGuestCount = findViewById(R.id.home_guest_count);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        layoutManager.setItemPrefetchEnabled(true);
        layoutManager.setInitialPrefetchItemCount(8);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemViewCacheSize(20);
        mRecyclerView.setHasFixedSize(true);

        mAdapter = new HomeAppAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnAppClickListener(new HomeAppAdapter.OnAppClickListener() {
            @Override
            public void onAppClick(AppData data, int position) {
                if (data instanceof PackageAppData) {
                    PackageAppData appData = (PackageAppData) data;
                    if (!appData.isLoading) {
                        appData.isLoading = true;
                        mAdapter.notifyItemChanged(position);
                        try {
                            LoadingActivity.launch(NewHomeActivity.this, appData.packageName, 0);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        mUiHandler.postDelayed(() -> {
                            appData.isLoading = false;
                            mAdapter.notifyItemChanged(position);
                        }, 3000);
                    }
                }
            }

            @Override
            public void onAppLongClick(AppData data, int position) {
                if (data instanceof PackageAppData) {
                    showAppPopupMenu((PackageAppData) data, position);
                }
            }
        });

        // Soft dashboard actions.
        mAddAction.setOnClickListener(v -> ListAppActivity.gotoListApp(this));
        mCameraAction.setOnClickListener(v -> chooseCameraProvider());
        mCameraAction.setOnLongClickListener(v -> {
            testSelectedCamera();
            return true;
        });

        mRepository = new AppRepository(this);
        ensureCameraPermission();
    }


    private void showAppPopupMenu(PackageAppData appData, int position) {
        View itemView = mRecyclerView.findViewHolderForAdapterPosition(position) != null
                ? mRecyclerView.findViewHolderForAdapterPosition(position).itemView
                : mRecyclerView;
        PopupMenu popup = new PopupMenu(this, itemView);
        popup.getMenuInflater().inflate(R.menu.app_popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.app_clear) {
                confirmClearData(appData);
            } else if (id == R.id.app_stop) {
                confirmStopApp(appData);
            } else if (id == R.id.app_remove) {
                confirmUninstall(appData, position);
            } else if (id == R.id.app_shortcut) {
                Toast.makeText(this, R.string.create_shortcut_success, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        popup.show();
    }

    private void confirmClearData(PackageAppData appData) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.home_menu_clear_title)
                .setMessage(String.format(getString(R.string.home_menu_clear_content), appData.name))
                .setPositiveButton(android.R.string.yes, (d, w) -> {
                    try {
                        VirtualCore.get().clearPackage(appData.packageName);
                        Toast.makeText(this, R.string.reboot_tips_1, Toast.LENGTH_SHORT).show();
                    } catch (Throwable e) {
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void confirmStopApp(PackageAppData appData) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.home_menu_kill_title)
                .setMessage(String.format(getString(R.string.home_menu_kill_content), appData.name))
                .setPositiveButton(android.R.string.yes, (d, w) -> {
                    try {
                        VirtualCore.get().killApp(appData.packageName, 0);
                        Toast.makeText(this, R.string.reboot_tips_1, Toast.LENGTH_SHORT).show();
                    } catch (Throwable e) {
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void confirmUninstall(PackageAppData appData, int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.home_menu_delete_title)
                .setMessage(String.format(getString(R.string.home_menu_delete_content), appData.name))
                .setPositiveButton(android.R.string.yes, (d, w) -> {
                    try {
                        VirtualCore.get().uninstallPackage(appData.packageName);
                        mAdapter.getData().remove(position);
                        mAdapter.notifyItemRemoved(position);
                        updateEmptyView();
                    } catch (Throwable e) {
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void loadApps() {
        mProgressBar.setVisibility(View.VISIBLE);
        mEmptyView.setVisibility(View.GONE);
        mRepository.getVirtualApps().done(apps -> runOnUiThread(() -> {
            mProgressBar.setVisibility(View.GONE);
            mAdapter.setData(apps);
            updateEmptyView();
        })).fail(err -> runOnUiThread(() -> {
            mProgressBar.setVisibility(View.GONE);
            Log.e(TAG, "Failed to load virtual apps", (Throwable) err);
            updateEmptyView();
        }));
    }

    private void updateEmptyView() {
        int count = mAdapter.getItemCount();
        if (mGuestCount != null) {
            mGuestCount.setText(count == 1 ? "1 app" : count + " apps");
        }
        if (count == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        refreshCameraSubtitle();
        alertForDoze();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.main_camera_provider) {
            chooseCameraProvider();
            return true;
        } else if (id == R.id.main_camera_test) {
            testSelectedCamera();
            return true;
        } else if (id == R.id.main_setting) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.main_reboot) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.settings_reboot_title)
                    .setMessage(R.string.settings_reboot_content)
                    .setPositiveButton(android.R.string.yes, (d, w) -> {
                        VirtualCore.get().killAllApps();
                        Toast.makeText(this, R.string.reboot_tips_1, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final String CAMERA_PREFS = "phantom_settings";
    private static final String CAMERA_PACKAGE_KEY = "camera_package";

    private boolean hasCameraPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || checkSelfPermission(android.Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void ensureCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasCameraPermission()) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void chooseCameraProvider() {
        SharedPreferences prefs = getSharedPreferences(CAMERA_PREFS, MODE_PRIVATE);
        String current = prefs.getString(CAMERA_PACKAGE_KEY, "");
        CameraProviderPicker.show(this, current, packageName -> {
            saveCameraProvider(packageName == null ? "" : packageName);
            refreshCameraSubtitle();
            String label = getCameraLabel(packageName);
            Toast.makeText(this, "Camera Provider: " + label, Toast.LENGTH_SHORT).show();
        });
    }

    private void saveCameraProvider(String packageName) {
        String value = packageName == null ? "" : packageName;
        getSharedPreferences(CAMERA_PREFS, MODE_PRIVATE)
                .edit().putString(CAMERA_PACKAGE_KEY, value).apply();
        try (FileOutputStream out = openFileOutput("phantom_camera_provider.txt", MODE_PRIVATE)) {
            out.write(value.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Throwable e) {
            Log.w(TAG, "Unable to persist camera provider file", e);
        }
    }

    private void testSelectedCamera() {
        if (!hasCameraPermission()) {
            ensureCameraPermission();
            Toast.makeText(this, "Allow Camera permission, then try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        String pkg = getSharedPreferences(CAMERA_PREFS, MODE_PRIVATE)
                .getString(CAMERA_PACKAGE_KEY, "");
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (!TextUtils.isEmpty(pkg)) {
            intent.setPackage(pkg);
        }
        try {
            startActivityForResult(intent, 0x5048);
        } catch (Throwable e) {
            Toast.makeText(this, "Camera unavailable: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshCameraSubtitle() {
        String pkg = getSharedPreferences(CAMERA_PREFS, MODE_PRIVATE)
                .getString(CAMERA_PACKAGE_KEY, "");
        if (!TextUtils.isEmpty(pkg)) {
            try {
                getPackageManager().getApplicationInfo(pkg, 0);
            } catch (Throwable e) {
                saveCameraProvider("");
                pkg = "";
            }
        }
        String label = getCameraLabel(pkg);
        if (mCameraValue != null) {
            mCameraValue.setText(label);
        }
        if (mToolbar != null) {
            mToolbar.setSubtitle(null);
        }
    }

    private String getCameraLabel(String pkg) {
        if (TextUtils.isEmpty(pkg)) return "System default";
        try {
            android.content.pm.ApplicationInfo ai = getPackageManager().getApplicationInfo(pkg, 0);
            CharSequence label = getPackageManager().getApplicationLabel(ai);
            return label == null ? pkg : label.toString();
        } catch (Throwable e) {
            return pkg;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VCommends.REQUEST_SELECT_APP) {
            loadApps();
        }
    }

    public void startVirtualActivity(Intent intent, Bundle options, int usedId) {
        String packageName = intent.getPackage();
        if (TextUtils.isEmpty(packageName)) {
            ComponentName component = intent.getComponent();
            if (component != null) {
                packageName = component.getPackageName();
            }
        }
        if (packageName == null) {
            try {
                startActivity(intent);
                return;
            } catch (Throwable ignored) {
            }
        }
        boolean result = LoadingActivity.launch(this, packageName, usedId);
        if (!result) {
            throw new ActivityNotFoundException("can not launch activity for: " + intent);
        }
    }

    private void alertForDoze() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager == null) {
            return;
        }
        boolean showAlert = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SHOW_DOZE_ALERT_KEY, true);
        if (!showAlert) {
            return;
        }
        if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
            mUiHandler.postDelayed(() -> {
                try {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.alert_for_doze_mode_title)
                            .setMessage(R.string.alert_for_doze_mode_content)
                            .setPositiveButton(R.string.alert_for_doze_mode_yes, (dialog, which) -> {
                                try {
                                    startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                            Uri.parse("package:" + getPackageName())));
                                } catch (Throwable e) {
                                    try {
                                        startActivity(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                                    } catch (Throwable ex) {
                                        PreferenceManager.getDefaultSharedPreferences(this)
                                                .edit().putBoolean(SHOW_DOZE_ALERT_KEY, false).apply();
                                    }
                                }
                            })
                            .setNegativeButton(R.string.alert_for_doze_mode_no, (dialog, which) ->
                                    PreferenceManager.getDefaultSharedPreferences(this)
                                            .edit().putBoolean(SHOW_DOZE_ALERT_KEY, false).apply())
                            .show();
                } catch (Throwable ignored) {
                }
            }, 1500);
        }
    }
}
