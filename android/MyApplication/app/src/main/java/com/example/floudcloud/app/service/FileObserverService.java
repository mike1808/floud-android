package com.example.floudcloud.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.example.floudcloud.app.model.File;
import com.example.floudcloud.app.model.FileUpload;
import com.example.floudcloud.app.utility.FileUtils;
import com.example.floudcloud.app.utility.RecursiveFileObserver;

import java.util.ArrayList;

public class FileObserverService extends Service {
    public final static String EXTRA_PATH = "PATH";
    public final static String EXTRA_API_KEY = "API_KEY";

    private final String LOG_TAG = FileObserverService.class.getSimpleName();
    private LocalFileObserver mFileObserver;
    private String mApiKey;
    private ArrayList<File> mFileArrayList;

    private String lastMovedFrom;
    private boolean waitForMove = false;

    public FileObserverService() {
    }

    @Override
    public void onDestroy() {
        mFileObserver.stopWatching();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!intent.hasExtra(EXTRA_PATH) && !intent.hasExtra(EXTRA_API_KEY)) {
            Log.e(LOG_TAG, "No PATH and API_KEY argument given");
            return START_STICKY;
        }


        String path = intent.getStringExtra(EXTRA_PATH);
        mApiKey = intent.getStringExtra(EXTRA_API_KEY);

        mFileObserver = new LocalFileObserver(path, LocalFileObserver.CHANGES_ONLY);
        mFileObserver.startWatching();

        return START_STICKY;
    }

    private File findFileByHash(String hash) {
        for (File file : mFileArrayList) {
            if (file.getHash().equals(hash)) {
                return file;
            }
        }

        return null;
    }

    private void doDelete(String path) {
        Intent delete = new Intent(FileObserverService.this, CloudOperationsService.class);
        delete.putExtra(CloudOperationsService.EXTRA_OPERATION, CloudOperationsService.OPERATION_DELETE);
        delete.putExtra(CloudOperationsService.EXTRA_API_KEY, mApiKey);
        delete.putExtra(CloudOperationsService.EXTRA_PATH, path);

        startService(delete);
    }

    private class LocalFileObserver extends RecursiveFileObserver {
        public LocalFileObserver(String path) {
            super(path);
        }
        public LocalFileObserver(String path, int mask) {
            super(path, mask);
        }

        @Override
        public void onEvent(int event, String path) {
            if (waitForMove && event != RecursiveFileObserver.MOVED_TO) {
                doDelete(path);
            }
            switch (event) {
                case RecursiveFileObserver.MOVED_FROM:
                    lastMovedFrom = path;
                    waitForMove = true;
                    break;
                case RecursiveFileObserver.MOVED_TO:
                    if (waitForMove) {
                        waitForMove = false;

                        Intent moveIntent = new Intent(FileObserverService.this, CloudOperationsService.class);
                        moveIntent.putExtra(CloudOperationsService.EXTRA_OPERATION, CloudOperationsService.OPERATION_MOVE);
                        moveIntent.putExtra(CloudOperationsService.EXTRA_API_KEY, mApiKey);
                        moveIntent.putExtra(CloudOperationsService.EXTRA_PATH, FileUtils.getFilePath(null, path));
                        moveIntent.putExtra(CloudOperationsService.EXTRA_OLD_PATH, FileUtils.getFilePath(null, lastMovedFrom));

                        startService(moveIntent);
                        break;
                    }
                    // continue and upload file
                case RecursiveFileObserver.CREATE:
                case RecursiveFileObserver.CLOSE_WRITE:
                    String hash;
                    try {
                        hash = FileUtils.getChecksum(new java.io.File(path));
                    } catch (Throwable throwable) {
                        Log.e(LOG_TAG, "Could not compute checksum of the file " + path);
                        return;
                    }

                    FileUpload fileUpload = new FileUpload(FileUtils.getFilePath(null, path), FileUtils.getFileSize(path), hash);

                    Intent uploadIntent = new Intent(FileObserverService.this, CloudOperationsService.class);
                    uploadIntent.putExtra(CloudOperationsService.EXTRA_OPERATION, CloudOperationsService.OPERATION_UPLOAD);
                    uploadIntent.putExtra(CloudOperationsService.EXTRA_API_KEY, mApiKey);
                    uploadIntent.putExtra(CloudOperationsService.EXTRA_FILE, fileUpload);

                    startService(uploadIntent);

                    break;
                case RecursiveFileObserver.DELETE:
                    doDelete(FileUtils.getFilePath(null, path));
                    break;
            }
        }
    }


}
