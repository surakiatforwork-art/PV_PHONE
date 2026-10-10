package io.virtualapp.settings;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.utils.GuestPermissionPolicy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GuestPermissionSettingsActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE = "guest_package";
    public static final String EXTRA_USER_ID = "guest_user_id";
    public static final String EXTRA_LABEL = "guest_label";

    private static final int REQ_RUNTIME = 5101;
    private static final int REQ_SPECIAL = 5102;

    private String guestPackage;
    private int guestUserId;
    private String guestLabel;
    private String pendingPermission;

    private final List<PermissionRow> rows = new ArrayList<>();
    private PermissionAdapter adapter;

    public static Intent createIntent(Context context, String packageName, int userId, CharSequence label) {
        Intent intent = new Intent(context, GuestPermissionSettingsActivity.class);
        intent.putExtra(EXTRA_PACKAGE, packageName);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_LABEL, label == null ? packageName : label.toString());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        guestPackage = getIntent().getStringExtra(EXTRA_PACKAGE);
        guestUserId = getIntent().getIntExtra(EXTRA_USER_ID, 0);
        guestLabel = getIntent().getStringExtra(EXTRA_LABEL);
        if (TextUtils.isEmpty(guestPackage)) {
            finish();
            return;
        }
        if (TextUtils.isEmpty(guestLabel)) {
            guestLabel = guestPackage;
        }

        setTitle("Guest Permissions");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setSubtitle(guestLabel);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(10), dp(14), dp(8));

        TextView title = new TextView(this);
        title.setText(guestLabel);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView pkg = new TextView(this);
        pkg.setText(guestPackage + "  •  virtual user " + guestUserId);
        pkg.setTextSize(12);
        pkg.setAlpha(0.7f);
        root.addView(pkg);

        TextView note = new TextView(this);
        note.setText("Allow/Deny is applied per Guest inside PHANToM VPhone. "
                + "Android grants the real permission to the PHANToM host UID, so an Allow setting "
                + "still requires Android approval when the host does not already have that access.");
        note.setTextSize(13);
        note.setPadding(0, dp(10), 0, dp(10));
        root.addView(note);

        ListView list = new ListView(this);
        adapter = new PermissionAdapter();
        list.setAdapter(adapter);
        list.setDividerHeight(1);
        list.setOnItemClickListener((parent, view, position, id) ->
                showPermissionModeDialog(rows.get(position)));
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView reset = new TextView(this);
        reset.setText("Reset all to Default");
        reset.setGravity(Gravity.CENTER);
        reset.setTextSize(15);
        reset.setPadding(dp(12), dp(14), dp(12), dp(14));
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Reset permissions")
                .setMessage("Reset all virtual permission overrides for " + guestLabel + "?")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    GuestPermissionPolicy.clear(guestPackage, guestUserId);
                    refreshRows();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show());
        root.addView(reset);

        setContentView(root);
        refreshRows();
    }

    private void refreshRows() {
        rows.clear();
        try {
            PackageInfo info = VPackageManager.get().getPackageInfo(
                    guestPackage, PackageManager.GET_PERMISSIONS, guestUserId);
            String[] requested = info == null ? null : info.requestedPermissions;
            if (requested != null) {
                Set<String> unique = new LinkedHashSet<>();
                for (String permission : requested) {
                    if (!TextUtils.isEmpty(permission)) {
                        unique.add(permission);
                    }
                }
                for (String permission : unique) {
                    rows.add(buildRow(permission));
                }
            }
        } catch (Throwable e) {
            Toast.makeText(this, "Unable to read guest permissions: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private PermissionRow buildRow(String permission) {
        PermissionRow row = new PermissionRow();
        row.permission = permission;
        row.mode = GuestPermissionPolicy.getMode(guestPackage, guestUserId, permission);
        try {
            PermissionInfo pi = getPackageManager().getPermissionInfo(permission, 0);
            CharSequence label = pi.loadLabel(getPackageManager());
            row.label = label == null ? shortName(permission) : label.toString();
        } catch (Throwable ignored) {
            row.label = shortName(permission);
        }
        row.hostStatus = getHostStatus(permission);
        return row;
    }

    private void showPermissionModeDialog(PermissionRow row) {
        String[] choices = new String[] {
                "Default — inherit PHANToM host state",
                "Allow — request/use host access",
                "Deny — block for this Guest"
        };
        int checked = row.mode == GuestPermissionPolicy.MODE_ALLOW ? 1
                : row.mode == GuestPermissionPolicy.MODE_DENY ? 2 : 0;

        new AlertDialog.Builder(this)
                .setTitle(row.label)
                .setSingleChoiceItems(choices, checked, (dialog, which) -> {
                    int mode = which == 1 ? GuestPermissionPolicy.MODE_ALLOW
                            : which == 2 ? GuestPermissionPolicy.MODE_DENY
                            : GuestPermissionPolicy.MODE_DEFAULT;
                    if (!GuestPermissionPolicy.setMode(
                            guestPackage, guestUserId, row.permission, mode)) {
                        Toast.makeText(this, "Unable to save permission policy", Toast.LENGTH_LONG).show();
                        return;
                    }
                    dialog.dismiss();

                    if (mode == GuestPermissionPolicy.MODE_ALLOW) {
                        ensureHostAccess(row.permission);
                    } else {
                        refreshRows();
                    }
                })
                .setNeutralButton("Details", (dialog, which) -> new AlertDialog.Builder(this)
                        .setTitle(row.permission)
                        .setMessage("Virtual policy: " + modeLabel(row.mode)
                                + "\nHost state: " + row.hostStatus)
                        .setPositiveButton(android.R.string.ok, null)
                        .show())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void ensureHostAccess(String permission) {
        pendingPermission = permission;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && (Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(permission)
                || Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)
                || Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission))) {
            if (!Environment.isExternalStorageManager()) {
                openSpecial(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
                return;
            }
            refreshRows();
            return;
        }

        if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
            if (!Settings.canDrawOverlays(this)) {
                openSpecial(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
                return;
            }
            refreshRows();
            return;
        }

        if (Manifest.permission.WRITE_SETTINGS.equals(permission)) {
            if (!Settings.System.canWrite(this)) {
                openSpecial(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
                return;
            }
            refreshRows();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && Manifest.permission.REQUEST_INSTALL_PACKAGES.equals(permission)) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                openSpecial(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName())));
                return;
            }
            refreshRows();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && Manifest.permission.SCHEDULE_EXACT_ALARM.equals(permission)) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                openSpecial(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
                return;
            }
            refreshRows();
            return;
        }

        if (isDangerous(permission)
                && isHostDeclared(permission)
                && ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { permission }, REQ_RUNTIME);
            return;
        }

        refreshRows();
    }

    private void openSpecial(Intent intent) {
        try {
            startActivityForResult(intent, REQ_SPECIAL);
        } catch (Throwable e) {
            Toast.makeText(this, "Android does not expose this special-access page",
                    Toast.LENGTH_LONG).show();
            refreshRows();
        }
    }

    private boolean isDangerous(String permission) {
        try {
            PermissionInfo info = getPackageManager().getPermissionInfo(permission, 0);
            return (info.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE)
                    == PermissionInfo.PROTECTION_DANGEROUS;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isHostDeclared(String permission) {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(), PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null) {
                for (String declared : info.requestedPermissions) {
                    if (permission.equals(declared)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private String getHostStatus(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && (Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(permission)
                || Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)
                || Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission))) {
            return Environment.isExternalStorageManager()
                    ? "PHANToM has All files access"
                    : "PHANToM does not have All files access";
        }

        if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
            return Settings.canDrawOverlays(this) ? "Host granted" : "Host not granted";
        }
        if (Manifest.permission.WRITE_SETTINGS.equals(permission)) {
            return Settings.System.canWrite(this) ? "Host granted" : "Host not granted";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && Manifest.permission.REQUEST_INSTALL_PACKAGES.equals(permission)) {
            return getPackageManager().canRequestPackageInstalls()
                    ? "Host granted" : "Host not granted";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && Manifest.permission.SCHEDULE_EXACT_ALARM.equals(permission)) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms()
                    ? "Host granted" : "Host not granted";
        }

        if (!isHostDeclared(permission)) {
            return "Not declared/available to PHANToM host";
        }
        return ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED ? "Host granted" : "Host not granted";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RUNTIME) {
            pendingPermission = null;
            refreshRows();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SPECIAL) {
            pendingPermission = null;
            refreshRows();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            refreshRows();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private String shortName(String permission) {
        int dot = permission == null ? -1 : permission.lastIndexOf('.');
        return dot >= 0 && dot + 1 < permission.length()
                ? permission.substring(dot + 1) : permission;
    }

    private String modeLabel(int mode) {
        if (mode == GuestPermissionPolicy.MODE_ALLOW) return "Allow";
        if (mode == GuestPermissionPolicy.MODE_DENY) return "Deny";
        return "Default";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class PermissionAdapter extends BaseAdapter {
        @Override public int getCount() { return rows.size(); }
        @Override public PermissionRow getItem(int position) { return rows.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PermissionRow row = getItem(position);

            LinearLayout root = new LinearLayout(GuestPermissionSettingsActivity.this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(12), dp(10), dp(12), dp(10));

            TextView label = new TextView(GuestPermissionSettingsActivity.this);
            label.setText(row.label);
            label.setTextSize(16);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            root.addView(label);

            TextView permission = new TextView(GuestPermissionSettingsActivity.this);
            permission.setText(row.permission);
            permission.setTextSize(11);
            permission.setAlpha(0.65f);
            root.addView(permission);

            TextView status = new TextView(GuestPermissionSettingsActivity.this);
            status.setText("Virtual: " + modeLabel(row.mode) + "  •  " + row.hostStatus);
            status.setTextSize(12);
            status.setPadding(0, dp(4), 0, 0);
            root.addView(status);

            return root;
        }
    }

    private static final class PermissionRow {
        String permission;
        String label;
        int mode;
        String hostStatus;
    }
}
