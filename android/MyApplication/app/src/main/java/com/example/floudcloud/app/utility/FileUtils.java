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

    public static String getFilePath(File file) throws Exception {
        if (mFileBase == null) {
            throw new Exception("Base of file path isn't set");
        }

        return file.getAbsoluteFile().toString().replace(mFileBase, "");
    }

    public static String getChecksum(File file) throws Throwable {
        byte[] digest = getDigest(new FileInputStream(file));
        StringBuilder sb = new StringBuilder();
        for (byte aDigest : digest) {
            sb.append(String.format("%x", aDigest));
        }

        return sb.toString();
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

    public static String downloadFile(String uri, String apiKey, File saveDir, ProgressNotify progressNotify) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(uri);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", apiKey);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();

            // download the file
            input = connection.getInputStream();
            output = new FileOutputStream(saveDir);

            byte data[] = new byte[BUFFER_SIZE];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                // publishing the progress....
                if (fileLength > 0) // only if total length is known
                    progressNotify.notifyProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }
        } catch (MalformedURLException ex) {
            Log.e(LOG_TAG, "error: " + ex.getMessage(), ex);
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }

        return null;
    }

    public static int uploadFile(String upLoadServerUri, String apiKey, FileUpload fileUpload, String sourceFileUri, ProgressNotify progressNotify) {
        String fileName = sourceFileUri;
        int serverResponseCode = 0;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "-BOUNDARY---BOUNDARY---BOUNDARY---";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {
            Log.e(LOG_TAG, "Source File not exist :" + sourceFileUri);
            return 0;
        } else {
            try {
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", apiKey);
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + fileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];


                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                long total = bytesRead;
                while (bytesRead > 0) {
                    progressNotify.notifyProgress((int) (total / fileUpload.size));
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }


                // File path
                createFormData(lineEnd, twoHyphens, boundary, "path", fileUpload.path);
                createFormData(lineEnd, twoHyphens, boundary, "hash", fileUpload.hash);
                createFormData(lineEnd, twoHyphens, boundary, "size", Long.toString(fileUpload.size));

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i(LOG_TAG, "HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);

                fileInputStream.close();
                dos.flush();
                dos.close();
            } catch (MalformedURLException ex) {
                Log.e(LOG_TAG, "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception : " + e.getMessage(), e);
            }

            return serverResponseCode;
        }
    }

    private static String createFormData(String lineEnd, String twoHyphens, String boundary, String key, String value) {
        return twoHyphens + boundary + twoHyphens + lineEnd +
               "Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd +
               lineEnd +
               value + lineEnd;
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


    private static String resolvePath(String relativePath) {
        String path;
        if (relativePath.startsWith("/")) {
            path = mFileBase + relativePath;
        } else {
            path = mFileBase + "/" + relativePath;
        }

        return path;
    }
}
