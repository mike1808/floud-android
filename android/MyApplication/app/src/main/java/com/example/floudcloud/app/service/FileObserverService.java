package com.example.floudcloud.app.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.floudcloud.app.model.File;
import com.example.floudcloud.app.model.FileUpload;
import com.example.floudcloud.app.utility.FileUtils;
import com.example.floudcloud.app.utility.RecursiveFileObserver;

import java.util.ArrayList;
import java.util.List;

public class FileObserverService extends Service {
    public final static String EXTRA_PATH = "PATH";
    public final static String EXTRA_API_KEY = "API_KEY";
    public final static String EXTRA_REG_ID = "REG_ID";

    public final static String IGNORE_FILE = "IGNORE_FILE";
    public static final String EXTRA_ADD_IGNORE = "EXTRA_ADD_IGNORE";
    public static final String EXTRA_REMOVE_IGNORE = "EXTRA_REMOVE_IGNORE";

    private final String LOG_TAG = FileObserverService.class.getSimpleName();
    private LocalFileObserver mFileObserver;
    private String mApiKey;
    private String mRegId;
    private boolean mWatching = false;

    private ArrayList<String> ignoredFiles;

    private IBinder mBinder = new LocalBinder();

    private String lastMovedFrom;
    private boolean waitForMove = false;

    private IgnoreFilesReceiver ignoreFilesReceiver;

    public class LocalBinder extends Binder {
        FileObserverService getService() {
            return FileObserverService.this;
        }
    }

    public FileObserverService() {
        super();
    }

    @Override
    public void onDestroy() {
        stopWatcher();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (!intent.hasExtra(EXTRA_PATH) && !intent.hasExtra(EXTRA_API_KEY) && !intent.hasExtra(EXTRA_REG_ID)) {
            Log.e(LOG_TAG, "No PATH and API_KEY argument given");
            return null;
        }

        String path = intent.getStringExtra(EXTRA_PATH);
        mApiKey = intent.getStringExtra(EXTRA_API_KEY);
        mRegId = intent.getStringExtra(EXTRA_REG_ID);

        ignoredFiles = new ArrayList<String>();

        mFileObserver = new LocalFileObserver(path, LocalFileObserver.CHANGES_ONLY);
        startWatcher();

        if (ignoreFilesReceiver == null) ignoreFilesReceiver = new IgnoreFilesReceiver();
        IntentFilter intentFilter = new IntentFilter(IGNORE_FILE);
        registerReceiver(ignoreFilesReceiver, intentFilter);

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopWatcher();
        if (ignoreFilesReceiver != null) unregisterReceiver(ignoreFilesReceiver);
        return true;
    }

    public void startWatcher() {
        mFileObserver.startWatching();
        mWatching = true;
    }

    public void stopWatcher() {
        mFileObserver.stopWatching();
        mWatching = false;
    }

    public boolean getWatcherStatus() {
        return mWatching;
    }

    public void addIgnored(String filePath) {
        ignoredFiles.add(FileUtils.resolvePath(filePath));

    }

    public void removeIgnored(String filePath) {
        ignoredFiles.remove(FileUtils.resolvePath(filePath));
    }

    private class IgnoreFilesReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(FileObserverService.IGNORE_FILE)) {
                String ignored = intent.getStringExtra(EXTRA_ADD_IGNORE);
                String unignored = intent.getStringExtra(EXTRA_REMOVE_IGNORE);

                if (ignored != null) {
                    addIgnored(ignored);
                }

                if (unignored != null) {
                    removeIgnored(unignored);
                }
            }
        }
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
            if (ignoredFiles.contains(path)) return;

            // The file was moved from but we didn't got MOVED_TO event, so delete this file
            if (waitForMove && (RecursiveFileObserver.MOVED_TO & event) == 0) {
                doDelete(path);
            }

            // The file was moved from our watching directory
            // remember it and continue
            if ((RecursiveFileObserver.MOVED_FROM & event) != 0) {
                lastMovedFrom = path;
                waitForMove = true;
            }

            // The file was moved to our watching directory
            // If before there wasn't any MOVED_FROM event then we'd upload
            // otherwise move this file
            if ((RecursiveFileObserver.MOVED_TO & event) != 0) {
                if (waitForMove) {
                    waitForMove = false;
                    doMove(path);
                } else {
                    doUpload(path);
                }
            }

            // A new file was created in our directory
            if ((RecursiveFileObserver.CLOSE_WRITE & event) != 0) {
                doUpload(path);
            }

            if ((RecursiveFileObserver.DELETE & event) != 0) {
                doDelete(path);
            }
        }

        private void doMove(String path) {
            addIgnored(path);

            Intent moveIntent = new Intent(FileObserverService.this, CloudOperationsService.class);
            moveIntent.putExtra(CloudOperationsService.EXTRA_OPERATION, CloudOperationsService.OPERATION_MOVE);
            moveIntent.putExtra(CloudOperationsService.EXTRA_API_KEY, mApiKey);
            moveIntent.putExtra(CloudOperationsService.EXTRA_REG_ID, mRegId);
            moveIntent.putExtra(CloudOperationsService.EXTRA_PATH, FileUtils.getFilePath(null, path));
            moveIntent.putExtra(CloudOperationsService.EXTRA_OLD_PATH, FileUtils.getFilePath(null, lastMovedFrom));

            startService(moveIntent);
        }

        private void doUpload(String path) {
            String hash;
            try {
                hash = FileUtils.getChecksum(new java.io.File(path));
            } catch (Throwable throwable) {
                Log.e(LOG_TAG, "Could not compute checksum of the file " + path);
                return;
            }

            FileUpload fileUpload = new FileUpload(FileUtils.getFilePath(null, path), FileUtils.getFileSize(path), hash, mRegId);

            Intent uploadIntent = new Intent(FileObserverService.this, CloudOperationsService.class);
            uploadIntent.putExtra(CloudOperationsService.EXTRA_OPERATION, CloudOperationsService.OPERATION_UPLOAD);
            uploadIntent.putExtra(CloudOperationsService.EXTRA_API_KEY, mApiKey);
            uploadIntent.putExtra(CloudOperationsService.EXTRA_REG_ID, mRegId);
            uploadIntent.putExtra(CloudOperationsService.EXTRA_FILE, fileUpload);

            startService(uploadIntent);
        }

        private void doDelete(String path) {
            addIgnored(path);

            Intent delete = new Intent(FileObserverService.this, CloudOperationsService.class);
            delete.putExtra(CloudOperationsService.EXTRA_OPERATION, CloudOperationsService.OPERATION_DELETE);
            delete.putExtra(CloudOperationsService.EXTRA_API_KEY, mApiKey);
            delete.putExtra(CloudOperationsService.EXTRA_REG_ID, mRegId);
            delete.putExtra(CloudOperationsService.EXTRA_PATH, FileUtils.getFilePath(null, path));

            startService(delete);
        }
    }

}
