package com.example.floudcloud.app.operation;


import android.util.Log;

import com.example.floudcloud.app.network.FloudService;
import com.example.floudcloud.app.utility.FileUtils;
import com.example.floudcloud.app.utility.ProgressListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadOperation extends RemoteOperation {
    private final String LOG_TAG = DownloadOperation.class.getSimpleName();
    private static final int BUFFER_SIZE = 16 * 1024 * 1024; // 16 KiB
    private File saveFile;

    public DownloadOperation(String path, String apiKey, String storagePath) {
        super(apiKey, FloudService.FILE_URL + "/file?path=" + path, path);

        this.saveFile = new File(storagePath, path);
    }

    @Override
    public int execute(ProgressListener progressListener) {
        int responseCode;
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(getUri());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", getApiKey());
            connection.connect();



            int fileLength = connection.getContentLength();

            input = new BufferedInputStream(connection.getInputStream());
            FileUtils.mkDir(null, saveFile.getParentFile());
            output = new FileOutputStream(saveFile);

            byte data[] = new byte[BUFFER_SIZE];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                if (fileLength > 0)
                    progressListener.notifyProgress((int) (float) total * 100 / fileLength);
                output.write(data, 0, count);
            }

            responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return responseCode;
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "An exception occurred when downloading " + saveFile.getAbsolutePath());
            Log.e(LOG_TAG, "Exception " + e.getMessage());
            Log.e(LOG_TAG, "Stack trace " + e.getStackTrace());
            return 0;
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

        return responseCode;
    }
}
