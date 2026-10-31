package com.lody.virtual.helper.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/** Private APK copies must be read-only before Android loads executable content. */
public final class GuestApkFiles {
    private GuestApkFiles() {}
    public static void copy(File source, File destination) throws IOException {
        if (source.getCanonicalFile().equals(destination.getCanonicalFile()))
            throw new IOException("APK source and destination coincide");
        if (destination.exists() && !destination.delete())
            throw new IOException("Cannot replace private APK");
        boolean complete = false;
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(destination)) {
            // Set mode before writing; the already-open descriptor remains writable.
            if (!destination.setReadOnly()) throw new IOException("Cannot protect private APK");
            byte[] buffer = new byte[65536]; int size;
            while ((size = in.read(buffer)) != -1) out.write(buffer, 0, size);
            out.flush(); out.getFD().sync(); complete = true;
        } finally { if (!complete) destination.delete(); }
    }
}
