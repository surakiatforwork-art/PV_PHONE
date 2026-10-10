package io.virtualapp.settings;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceCategory;
import android.preference.SwitchPreference;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.ipc.VActivityManager;

import java.io.File;
import java.io.IOException;

import io.virtualapp.R;
import io.virtualapp.VCommends;
import com.lody.virtual.GmsSupport;
import io.virtualapp.home.ListAppActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String ADVANCE_SETTINGS_KEY = "settings_advance";
    private static final String ADD_APP_KEY = "settings_add_app";
    private static final String MODULE_MANAGE_KEY = "settings_module_manage";
    private static final String APP_MANAGE_KEY = "settings_app_manage";
    private static final String TASK_MANAGE_KEY = "settings_task_manage";
    private static final String REBOOT_KEY = "settings_reboot";
    private static final String HIDE_SETTINGS_KEY = "advance_settings_hide_settings";
    private static final String DISABLE_INSTALLER_KEY = "advance_settings_disable_installer";
    private static final String INSTALL_GMS_KEY = "advance_settings_install_gms";
    private static final String RECOMMEND_PLUGIN = "settings_plugin_recommend";
    private static final String DISABLE_RESIDENT_NOTIFICATION = "advance_settings_disable_resident_notification";
    private static final String ALLOW_FAKE_SIGNATURE = "advance_settings_allow_fake_signature";
    private static final String DISABLE_XPOSED = "advance_settings_disable_xposed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class SettingsFragment extends PreferenceFragment {


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_preferences);

            // Setup allow rotation preference

            Preference addApp = findPreference(ADD_APP_KEY);
            Preference moduleManage = findPreference(MODULE_MANAGE_KEY);
            Preference recommend = findPreference(RECOMMEND_PLUGIN);
            Preference appManage = findPreference(APP_MANAGE_KEY);
            Preference taskManage = findPreference(TASK_MANAGE_KEY);
            Preference reboot = findPreference(REBOOT_KEY);

            SwitchPreference disableInstaller = (SwitchPreference) findPreference(DISABLE_INSTALLER_KEY);
            SwitchPreference disableResidentNotification = (SwitchPreference) findPreference(DISABLE_RESIDENT_NOTIFICATION);
            SwitchPreference allowFakeSignature = (SwitchPreference) findPreference(ALLOW_FAKE_SIGNATURE);
            SwitchPreference disableXposed = (SwitchPreference) findPreference(DISABLE_XPOSED);

            addApp.setOnPreferenceClickListener(preference -> {
                ListAppActivity.gotoListApp(getActivity());
                return false;
            });

            appManage.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), AppManageActivity.class));
                return false;
            });

            taskManage.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), TaskManageActivity.class));
                return false;
            });

            reboot.setOnPreferenceClickListener(preference -> {
                android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(getActivity())
                        .setTitle(R.string.settings_reboot_title)
                        .setMessage(getResources().getString(R.string.settings_reboot_content))
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            VirtualCore.get().killAllApps();
                            Toast.makeText(getActivity(), R.string.reboot_tips_1, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();
                try {
                    alertDialog.show();
                } catch (Throwable ignored) {
                }
                return false;
            });

            disableInstaller.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!(newValue instanceof Boolean)) {
                    return false;
                }
                try {
                    boolean disable = (boolean) newValue;
                    PackageManager packageManager = getActivity().getPackageManager();
                    packageManager.setComponentEnabledSetting(new ComponentName(getActivity().getPackageName(), "vxp.installer"),
                            !disable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                    return true;
                } catch (Throwable ignored) {
                    return false;
                }
            });

            Preference installGms = findPreference(INSTALL_GMS_KEY);
            installGms.setTitle("Google Services (experimental)");
            installGms.setSummary("Import Google Play services / GSF / Play Store from this device into PHANToM VPhone");
            installGms.setOnPreferenceClickListener(preference -> {
                if (!GmsSupport.isOutsideGoogleFrameworkExist()) {
                    Toast.makeText(getActivity(),
                            "Google Play services is not installed on this device",
                            Toast.LENGTH_LONG).show();
                    return true;
                }

                ProgressDialog dialog = new ProgressDialog(getActivity());
                dialog.setTitle("Google Services");
                dialog.setMessage("Importing Google services from device...");
                dialog.setCancelable(false);
                dialog.show();

                io.virtualapp.abs.ui.VUiKit.defer()
                        .when(() -> GmsSupport.installGApps(0))
                        .done((v) -> {
                            dismiss(dialog);
                            boolean ok = GmsSupport.isGoogleFrameworkInstalled();
                            Toast.makeText(getActivity(),
                                    ok ? "Google Services imported. Reboot PHANToM before testing."
                                            : "Google Services import incomplete",
                                    Toast.LENGTH_LONG).show();
                        })
                        .fail((e) -> {
                            dismiss(dialog);
                            Toast.makeText(getActivity(),
                                    "Google Services import failed",
                                    Toast.LENGTH_LONG).show();
                        });
                return true;
            });

            disableResidentNotification.setOnPreferenceChangeListener(((preference, newValue) -> {

                if (!(newValue instanceof Boolean)) {
                    return false;
                }

                boolean on = (boolean) newValue;

                File flag = getActivity().getFileStreamPath(Constants.NO_NOTIFICATION_FLAG);
                if (on) {
                    boolean success;
                    try {
                        success = flag.createNewFile();
                    } catch (IOException e) {
                        success = false;
                    }
                    return success;
                } else {
                    return !flag.exists() || flag.delete();
                }
            }));

            if (android.os.Build.VERSION.SDK_INT < 25) {
                // Android NR1 below do not need this.
                PreferenceCategory advance = (PreferenceCategory) findPreference(ADVANCE_SETTINGS_KEY);
                advance.removePreference(disableResidentNotification);
            }

            allowFakeSignature.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!(newValue instanceof Boolean)) {
                    return false;
                }

                boolean on = (boolean) newValue;
                File flag = getActivity().getFileStreamPath(Constants.FAKE_SIGNATURE_FLAG);
                if (on) {
                    boolean success;
                    try {
                        success = flag.createNewFile();
                    } catch (IOException e) {
                        success = false;
                    }
                    return success;
                } else {
                    return !flag.exists() || flag.delete();
                }
            });

        }

        private static void dismiss(ProgressDialog dialog) {
            try {
                dialog.dismiss();
            } catch (Throwable ignored) {
            }
        }

        protected int dp2px(float dp) {
            final float scale = getResources().getDisplayMetrics().density;
            return (int) (dp * scale + 0.5f);
        }

        @Override
        public void startActivity(Intent intent) {
            try {
                super.startActivity(intent);
            } catch (Throwable ignored) {
                Toast.makeText(getActivity(), "startActivity failed.", Toast.LENGTH_SHORT).show();
                ignored.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VCommends.REQUEST_SELECT_APP) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        }
    }
}
