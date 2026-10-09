package io.virtualapp.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class CameraProviderPicker {

    interface Callback {
        void onSelected(String packageName);
    }

    private static final class Entry {
        final String label;
        final String packageName;
        final Drawable icon;
        final boolean captureHandler;

        Entry(String label, String packageName, Drawable icon, boolean captureHandler) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
            this.captureHandler = captureHandler;
        }
    }

    static void show(Activity activity, String currentPackage, Callback callback) {
        PackageManager pm = activity.getPackageManager();
        Set<String> capturePackages = new HashSet<>();
        collectCaptureHandlers(pm, new Intent(MediaStore.ACTION_IMAGE_CAPTURE), capturePackages);
        collectCaptureHandlers(pm, new Intent(MediaStore.ACTION_VIDEO_CAPTURE), capturePackages);
        collectCaptureHandlers(pm, new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), capturePackages);

        Map<String, Entry> unique = new LinkedHashMap<>();
        Intent launcherQuery = new Intent(Intent.ACTION_MAIN);
        launcherQuery.addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo info : pm.queryIntentActivities(launcherQuery, 0)) {
            if (info.activityInfo == null || info.activityInfo.packageName == null) continue;
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(activity.getPackageName()) || unique.containsKey(pkg)) continue;
            CharSequence label = info.loadLabel(pm);
            Drawable icon;
            try { icon = info.loadIcon(pm); }
            catch (Throwable ignored) { icon = pm.getDefaultActivityIcon(); }
            unique.put(pkg, new Entry(
                    label == null ? pkg : label.toString(),
                    pkg,
                    icon,
                    capturePackages.contains(pkg)));
        }

        // Include capture handlers even if they do not expose a launcher.
        for (String pkg : capturePackages) {
            if (pkg.equals(activity.getPackageName()) || unique.containsKey(pkg)) continue;
            try {
                android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                CharSequence label = pm.getApplicationLabel(ai);
                unique.put(pkg, new Entry(
                        label == null ? pkg : label.toString(),
                        pkg,
                        pm.getApplicationIcon(ai),
                        true));
            } catch (Throwable ignored) {
            }
        }

        List<Entry> all = new ArrayList<>(unique.values());
        Collections.sort(all, (a, b) -> {
            if (a.captureHandler != b.captureHandler) return a.captureHandler ? -1 : 1;
            return a.label.compareToIgnoreCase(b.label);
        });

        Entry system = new Entry(
                "System default",
                "",
                activity.getDrawable(android.R.drawable.ic_menu_camera),
                true);
        all.add(0, system);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 8);
        content.setPadding(pad, pad, pad, 0);

        EditText search = new EditText(activity);
        search.setHint("Search app name or package");
        search.setSingleLine(true);
        content.addView(search, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ListView list = new ListView(activity);
        list.setDividerHeight(1);
        content.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 430)));

        final List<Entry> shown = new ArrayList<>(all);
        BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return shown.size(); }
            @Override public Entry getItem(int position) { return shown.get(position); }
            @Override public long getItemId(int position) { return position; }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Entry entry = getItem(position);
                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                int rowPad = dp(activity, 12);
                row.setPadding(rowPad, dp(activity, 8), rowPad, dp(activity, 8));

                ImageView icon = new ImageView(activity);
                icon.setImageDrawable(entry.icon);
                row.addView(icon, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));

                LinearLayout texts = new LinearLayout(activity);
                texts.setOrientation(LinearLayout.VERTICAL);
                texts.setPadding(dp(activity, 12), 0, 0, 0);

                TextView title = new TextView(activity);
                boolean selected = entry.packageName.equals(currentPackage == null ? "" : currentPackage);
                title.setText(entry.label + (selected ? "  ✓" : ""));
                title.setTextSize(17);
                texts.addView(title);

                TextView pkg = new TextView(activity);
                if (entry.packageName.isEmpty()) {
                    pkg.setText("Use Android default camera");
                } else {
                    String state = entry.captureHandler ? "Capture intent supported" : "Launcher fallback";
                    pkg.setText(entry.packageName + "  •  " + state);
                }
                pkg.setTextSize(12);
                pkg.setAlpha(0.65f);
                texts.addView(pkg);
                row.addView(texts, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                return row;
            }
        };
        list.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                String q = text == null ? "" : text.toString().trim().toLowerCase(Locale.ROOT);
                shown.clear();
                if (q.isEmpty()) {
                    shown.addAll(all);
                } else {
                    for (Entry e : all) {
                        if (e.label.toLowerCase(Locale.ROOT).contains(q)
                                || e.packageName.toLowerCase(Locale.ROOT).contains(q)) {
                            shown.add(e);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Camera Provider")
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        list.setOnItemClickListener((parent, view, position, id) -> {
            callback.onSelected(shown.get(position).packageName);
            dialog.dismiss();
        });
        dialog.show();
    }

    private static void collectCaptureHandlers(
            PackageManager pm, Intent intent, Set<String> out) {
        for (ResolveInfo info : pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)) {
            if (info.activityInfo != null && info.activityInfo.packageName != null) {
                out.add(info.activityInfo.packageName);
            }
        }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
