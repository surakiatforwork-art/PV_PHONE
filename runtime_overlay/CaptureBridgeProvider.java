package com.lody.virtual.client.stub;

import android.content.ClipData;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IInterface;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import com.lody.virtual.client.VClientImpl;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.os.VUserHandle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.util.Properties;
import java.util.UUID;

/** Grantable host URI that forwards only a guest's explicitly selected capture output. */
public final class CaptureBridgeProvider extends ContentProvider {
    private static final long MAX_AGE_MS = 24L * 60 * 60 * 1000;

    public static void prepareOutput(Context host, Intent intent) throws Exception {
        Uri original = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        if (original == null || !"content".equals(original.getScheme())) return;
        int user = VUserHandle.myUserId();
        ProviderInfo info = VPackageManager.get().resolveContentProvider(original.getAuthority(), 0, user);
        if (info == null) return; // Real Android providers already support Android URI grants.
        if (!info.packageName.equals(VClientImpl.get().getCurrentPackage())) {
            throw new SecurityException("Capture output must belong to the requesting guest");
        }
        File dir = routeDir(host);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new java.io.IOException("Cannot create capture routes");
        File[] stale = dir.listFiles();
        if (stale != null) for (File file : stale) {
            if (System.currentTimeMillis() - file.lastModified() > MAX_AGE_MS) file.delete();
        }
        String token = UUID.randomUUID().toString();
        Properties route = new Properties();
        route.setProperty("uri", original.toString());
        route.setProperty("user", String.valueOf(user));
        route.setProperty("package", info.packageName);
        try (FileOutputStream out = new FileOutputStream(new File(dir, token))) {
            route.store(out, "PHANToM capture output");
        }
        Uri bridge = new Uri.Builder().scheme("content")
                .authority(VirtualCore.get().getHostPkg() + ".capturebridge").appendPath(token).build();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, bridge);
        intent.setClipData(ClipData.newRawUri("capture", bridge));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    private static File routeDir(Context context) {
        return new File(context.getFilesDir(), "phantom_capture_routes");
    }

    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "image/jpeg"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
        MatrixCursor cursor = new MatrixCursor(columns);
        Object[] values = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) values[i] = "capture.jpg";
            if (OpenableColumns.SIZE.equals(columns[i])) values[i] = 0L;
        }
        cursor.addRow(values);
        return cursor;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String token = uri.getLastPathSegment();
        if (token == null || !token.matches("[a-f0-9-]{36}")) throw new FileNotFoundException("Invalid capture token");
        File file = new File(routeDir(getContext()), token);
        if (!file.isFile() || System.currentTimeMillis() - file.lastModified() > MAX_AGE_MS) {
            throw new FileNotFoundException("Capture route expired");
        }
        long identity = Binder.clearCallingIdentity();
        try {
            Properties route = new Properties();
            try (FileInputStream input = new FileInputStream(file)) { route.load(input); }
            Uri original = Uri.parse(route.getProperty("uri"));
            int user = Integer.parseInt(route.getProperty("user"));
            ProviderInfo info = VPackageManager.get().resolveContentProvider(original.getAuthority(), 0, user);
            if (info == null || !info.packageName.equals(route.getProperty("package"))) {
                throw new FileNotFoundException("Guest provider unavailable");
            }
            IInterface provider = VActivityManager.get().acquireProviderClient(user, info);
            if (provider == null) throw new FileNotFoundException("Guest provider unavailable");
            for (Constructor<?> constructor : ContentProviderClient.class.getDeclaredConstructors()) {
                Class<?>[] types = constructor.getParameterTypes();
                if (types.length == 3 && types[2] == boolean.class) {
                    constructor.setAccessible(true);
                    ContentProviderClient client = (ContentProviderClient) constructor.newInstance(
                            getContext().getContentResolver(), provider, true);
                    try { return client.openFile(original, mode); }
                    finally { client.release(); }
                }
            }
            throw new FileNotFoundException("Unsupported content provider client");
        } catch (Exception e) {
            FileNotFoundException failure = new FileNotFoundException("Cannot open guest capture output: " + e);
            failure.initCause(e);
            throw failure;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}
