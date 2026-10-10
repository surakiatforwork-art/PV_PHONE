package io.virtualapp.settings;

import android.app.Activity;
import io.virtualapp.BuildConfig;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.system.Os;
import android.system.OsConstants;
import android.content.pm.PackageInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Host observations only. No guest probes, hook injection or policy writes. */
public final class EngineDiagnosticsActivity extends Activity {
    private static final String[] SUBSYSTEMS = {
        "package_manager", "activity_manager", "activity_task_manager", "instrumentation",
        "binder", "file_io", "native_io", "guest_permissions", "location", "camera_provider",
        "browser_routing", "webview", "notifications", "clipboard", "gms", "native_loading",
        "pending_intent", "broadcast", "service", "content_provider", "role_manager",
        "storage", "android16_transactions"
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        body.setPadding(pad, pad, pad, pad);
        scroll.addView(body);
        text(body, "Engine Diagnostics", 24);
        text(body, "Read-only host observations. Guest runtime evidence is not collected by this screen. "
                + "Host permission grants and installed hooks do not prove guest compatibility.", 16);
        text(body, hostFacts(), 16);
        Button refresh = new Button(this);
        refresh.setText("Refresh host observations");
        refresh.setOnClickListener(view -> render());
        body.addView(refresh);
        text(body, "Guest runtime capabilities: 23 UNKNOWN / NOT_TESTED", 18);
        for (String subsystem : SUBSYSTEMS) {
            Button row = new Button(this);
            row.setAllCaps(false);
            row.setText(subsystem + "\nUNKNOWN · NOT_TESTED");
            row.setOnClickListener(view -> new AlertDialog.Builder(this)
                    .setTitle(subsystem)
                    .setMessage("State: UNKNOWN\nResult: NOT_TESTED\n"
                            + "Scope: no guest package, app version or virtual user measured\n"
                            + "Hook presence: not inspected in a guest process\n"
                            + "Last runtime test: none recorded\nEvidence: none attached\n"
                            + "Reason: this screen observes the host only.\n"
                            + "Fallback: existing runtime policy is unchanged.")
                    .setPositiveButton(android.R.string.ok, null).show());
            body.addView(row);
        }
        Button back = new Button(this);
        back.setText("Back to Settings");
        back.setOnClickListener(view -> finish());
        body.addView(back);
        setContentView(scroll);
    }

    private String hostFacts() {
        String version = "unavailable";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = info.versionName + " (" + info.versionCode + ")";
        } catch (Exception ignored) { }
        String pageSize = "unavailable";
        try { pageSize = Long.toString(Os.sysconf(OsConstants._SC_PAGESIZE)); }
        catch (Exception ignored) { }
        String storage = "not applicable before API 30";
        if (Build.VERSION.SDK_INT >= 30) {
            try { storage = Boolean.toString(Environment.isExternalStorageManager()); }
            catch (RuntimeException ignored) { storage = "unavailable"; }
        }
        String camera = getSharedPreferences("phantom_settings", MODE_PRIVATE)
                .getString("camera_package", "");
        return "Observation scope: HOST ONLY\nHost build: " + version
                + "\nSource commit: " + BuildConfig.DIAGNOSTICS_SOURCE
                + "\nAndroid: " + Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT
                + "\nDevice: " + Build.MANUFACTURER + " " + Build.MODEL
                + "\nDevice ABI: " + Build.SUPPORTED_ABIS[0]
                + "\nProcess: " + (Build.VERSION.SDK_INT >= 23 ? (Process.is64Bit() ? "64-bit" : "32-bit") : "unavailable before API 23")
                + "\nHost Linux UID: " + Process.myUid()
                + "\nPage size (bytes): " + pageSize
                + "\nHost all-files grant: " + storage
                + "\nConfigured camera package: " + (camera.isEmpty() ? "system default" : camera)
                + "\nCamera capture result: NOT_TESTED";
    }

    private void text(LinearLayout parent, String value, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setPadding(0, 8, 0, 16);
        parent.addView(text);
    }
}
