package com.example.floudcloud.app.utility;

import android.util.Log;

import com.example.floudcloud.app.model.FileUpload;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;

public abstract class FileUtils {
    private static final String LOG_TAG = FileUtils.class.getSimpleName();
    private static final int BUFFER_SIZE = 2048;
    private static final String CHECKSUM_ALGORITHM = "SHA-1";
    private static String mFileBase;

    public static String getFileBase() {
        return mFileBase;
    }

    public static void setFilePathBase(String fileBase) {
        if (fileBase.endsWith("/")) {
            mFileBase = fileBase.replaceFirst("//$/", "");
        } else {
            mFileBase = fileBase;
        }
    }

    public static String getFilePath(File file, String path) {
        if (path != null) {
            return path.replace(mFileBase, "");
        }

        return file.getAbsoluteFile().toString().replace(mFileBase, "");
    }

    public static long getFileSize(String path) {
        return new File(path).length();
    }

    public static String getChecksum(File file) throws Throwable {
        return bytesToHex(getDigest(new FileInputStream(file)));
    }

    public static void saveFile(String relativePath, InputStream in) throws IOException {
        String path = resolvePath(relativePath);
        File file = new File(path);
        OutputStream out = null;

        if (file.exists() && !file.isDirectory()) {
            if (!file.delete()) {
                throw new IOException("Could delete the old file");
            }
        }

        out = new FileOutputStream(file);
        int bytesRead = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }

        in.close();
        out.flush();
        out.close();
    }

    public static ArrayList<File> getDirectoryContent(File dir) {
        if (!dir.isDirectory())
            return null;

        ArrayList<File> files = new ArrayList<File>();

        File[] dirContent = dir.listFiles();

        if (dirContent == null) {
            return null;
        }

        for(File file : dirContent) {
            if (file.isDirectory()) {
                files.addAll(getDirectoryContent(file));
            } else {
                files.add(file);
            }
        }

        return files;
    }

    public static ArrayList<com.example.floudcloud.app.model.File> generateFilesList(ArrayList<File> files) {
        if (files == null) {
            Log.e(LOG_TAG, "Files list is null");
            return null;
        }

        ArrayList<com.example.floudcloud.app.model.File> resultFiles = new ArrayList<com.example.floudcloud.app.model.File>();

        for (File file : files) {
            try {
                com.example.floudcloud.app.model.File fi  = new com.example.floudcloud.app.model.File(FileUtils.getFilePath(file, null), file.length(), FileUtils.getChecksum(file));

                resultFiles.add(fi);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            } catch (Throwable throwable) {
                Log.e(LOG_TAG, "Could not compute file checksum");
            }
        }

        return resultFiles;
    }

    public static String resolvePath(String relativePath) {
        String path;
        if (relativePath.startsWith("/")) {
            path = mFileBase + relativePath;
        } else {
            path = mFileBase + "/" + relativePath;
        }

        return path;
    }

    public static boolean mkDir(String path, File destDir) {
        File dir = null;
        if (path == null && destDir != null) {
            dir = destDir;
        } else {
            dir = new File(path);
        }

        if (dir.exists() && !dir.isDirectory()) {
            return false;
        }

        if (dir.exists() && dir.isDirectory()) {
            return true;
        }

        return dir.mkdir();
    }

    private static byte[] getDigest(InputStream in) throws Throwable {
        MessageDigest md = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
        try {
            DigestInputStream dis = new DigestInputStream(in, md);
            byte[] buffer = new byte[BUFFER_SIZE];
            while (dis.read(buffer) != -1) {
            }
            dis.close();
        } finally {
            in.close();
        }

        return md.digest();
    }

    private static String bytesToHex(byte[] b) {
        char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        StringBuffer buf = new StringBuffer();
        for (int j=0; j<b.length; j++) {
            buf.append(hexDigit[(b[j] >> 4) & 0x0f]);
            buf.append(hexDigit[b[j] & 0x0f]);
        }
        return buf.toString();
    }



}
