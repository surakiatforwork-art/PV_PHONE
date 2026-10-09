package io.virtualapp.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CameraProviderPicker {

    interface Callback {
        void onSelected(String packageName);
    }

    private static final class Entry {
        final String label;
        final String packageName;
        final Drawable icon;

        Entry(String label, String packageName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    static void show(Activity activity, String currentPackage, Callback callback) {
        PackageManager pm = activity.getPackageManager();
        Map<String, Entry> unique = new LinkedHashMap<>();
        collect(pm, new Intent(MediaStore.ACTION_IMAGE_CAPTURE), unique);
        collect(pm, new Intent(MediaStore.ACTION_VIDEO_CAPTURE), unique);

        List<Entry> entries = new ArrayList<>(unique.values());
        Collections.sort(entries, Comparator.comparing(e -> e.label.toLowerCase()));
        entries.add(0, new Entry("System default", "",
                activity.getDrawable(android.R.drawable.ic_menu_camera)));

        ListView list = new ListView(activity);
        list.setDividerHeight(1);
        list.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return entries.size(); }
            @Override public Entry getItem(int position) { return entries.get(position); }
            @Override public long getItemId(int position) { return position; }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Entry entry = getItem(position);
                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                int pad = dp(activity, 14);
                row.setPadding(pad, dp(activity, 9), pad, dp(activity, 9));

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
                pkg.setText(entry.packageName.isEmpty() ? "Use Android resolver/default camera" : entry.packageName);
                pkg.setTextSize(12);
                pkg.setAlpha(0.65f);
                texts.addView(pkg);
                row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                return row;
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Camera Provider")
                .setView(list)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        list.setOnItemClickListener((parent, view, position, id) -> {
            callback.onSelected(entries.get(position).packageName);
            dialog.dismiss();
        });
        dialog.show();
    }

    private static void collect(PackageManager pm, Intent intent, Map<String, Entry> out) {
        List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : list) {
            if (info.activityInfo == null || info.activityInfo.packageName == null) continue;
            String pkg = info.activityInfo.packageName;
            if (out.containsKey(pkg)) continue;
            CharSequence label = info.loadLabel(pm);
            Drawable icon;
            try { icon = info.loadIcon(pm); }
            catch (Throwable ignored) { icon = pm.getDefaultActivityIcon(); }
            out.put(pkg, new Entry(label == null ? pkg : label.toString(), pkg, icon));
        }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
