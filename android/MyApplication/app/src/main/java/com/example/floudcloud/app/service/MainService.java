package com.example.floudcloud.app.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.example.floudcloud.app.model.FileUpload;
import com.example.floudcloud.app.network.FloudService;
import com.example.floudcloud.app.network.exception.InternalServerError;
import com.example.floudcloud.app.network.exception.NotFoundError;
import com.example.floudcloud.app.network.exception.UnauthorizedError;
import com.example.floudcloud.app.utility.FileUtils;

import java.io.File;
import java.util.ArrayList;

import retrofit.RetrofitError;


public class MainService extends Service {
    private static String LOG_TAG = MainService.class.getSimpleName();

    public static String EXTRA_PATH = "PATH";
    public static String EXTRA_API_KEY = "API_KEY";
    public static String EXTRA_REG_ID = "REG_ID";

    public static String RUN_DIGEST = "RUN_DIGEST";

    public static final String EXTRA_MEGA_KASTIL = "bilo 3 chasa nochi i nuzhno bilo pisat diplom";

    private ServiceHandler mServiceHandler;
    private String mApiKey;
    private String mRegId;
    private String mPath;
    private FloudService floudService;
    private DigestRunner digestRunner;


    private FileObserverService mFileObserverService;
    private boolean mFileObserverServiceBound = false;

    private class DigestRunner extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(RUN_DIGEST)) {
                Message msg = mServiceHandler.obtainMessage();
                msg.obj = mPath;
                mServiceHandler.sendMessage(msg);
            }
        }
    }

    private ServiceConnection mFileObserverConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileObserverService.LocalBinder binder = (FileObserverService.LocalBinder) service;
            mFileObserverService = binder.getService();
            mFileObserverServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mFileObserverServiceBound = false;
        }
    };

    public MainService() {
        super();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("Main service thread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Looper mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);

        if (digestRunner == null) digestRunner = new DigestRunner();
        IntentFilter intentFilter = new IntentFilter(RUN_DIGEST);
        registerReceiver(digestRunner, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mFileObserverServiceBound) {
            unbindService(mFileObserverConnection);
        }

        if (digestRunner != null) unregisterReceiver(digestRunner);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!intent.hasExtra(EXTRA_PATH) && !intent.hasExtra(EXTRA_API_KEY) && !intent.hasExtra(EXTRA_REG_ID)) {
            Log.e(LOG_TAG, "Not enough information provided in intent");
            return START_NOT_STICKY;
        }

        mPath = Environment.getExternalStorageDirectory().getAbsolutePath() + intent.getStringExtra(CloudOperationsService.EXTRA_PATH);
        mApiKey = intent.getStringExtra(EXTRA_API_KEY);
        mRegId = intent.getStringExtra(EXTRA_REG_ID);

        floudService = new FloudService(mApiKey, mRegId);

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = mPath;
        // FIXME :(
        msg.arg2 = intent.getIntExtra(EXTRA_MEGA_KASTIL, 0);
        mServiceHandler.sendMessage(msg);

        Intent fileObserverIntent = new Intent(this, FileObserverService.class);
        fileObserverIntent.putExtra(FileObserverService.EXTRA_API_KEY, mApiKey);
        fileObserverIntent.putExtra(FileObserverService.EXTRA_PATH, mPath);
        fileObserverIntent.putExtra(FileObserverService.EXTRA_REG_ID, mRegId);
        bindService(fileObserverIntent, mFileObserverConnection, Context.BIND_AUTO_CREATE);

        return START_NOT_STICKY;
    }


    public boolean getStatus() {
        return mFileObserverService.getWatcherStatus();
    }

    private ArrayList<File> getLocaleFiles(String path) {
        File dir = new File(path);
        File[] files = dir.listFiles();
        ArrayList<File> resultFiles = new ArrayList<File>();

        if (files == null) {
            Log.e(LOG_TAG, "List of files of provided directory is null");
            return null;
        }

        for (File file : files) {
            if (file.isFile()) {
                resultFiles.add(file);
            }
        }

        return resultFiles;
    }


    private com.example.floudcloud.app.model.File.List fetchFiles(boolean withTimestamp) {
        com.example.floudcloud.app.model.File.List cloudFiles = null;

        try {
            cloudFiles = floudService.getFileService().getFilesList(withTimestamp ? floudService.getTimestamp() : 0);
        } catch (UnauthorizedError cause) {
            Log.e(LOG_TAG, "Not Authorized");
            return null;
        } catch (InternalServerError cause) {
            Log.e(LOG_TAG, "Internal Server Error");
            return null;
        } catch (NotFoundError cause) {
            Log.e(LOG_TAG, "Not Found Error");
            return null;
        } catch (RetrofitError cause) {
            Log.e(LOG_TAG, "Unknown Error");
            return null;
        }

        floudService.setTimestamp(System.currentTimeMillis());

        return cloudFiles;
    }

    private ArrayList<String> getFilesToBeDownloaded(com.example.floudcloud.app.model.File.List cloudFiles, ArrayList<com.example.floudcloud.app.model.File> localFiles) {
        if (cloudFiles == null) {
            return null;
        }

        ArrayList<com.example.floudcloud.app.model.File> filesDiff = (com.example.floudcloud.app.model.File.List) cloudFiles.clone();

        if (localFiles != null) {
            filesDiff.removeAll(localFiles);
        }

        if (filesDiff == null) {
            return null;
        }

        ArrayList<String> filesToBeDownloaded = new ArrayList<String>();

        for (com.example.floudcloud.app.model.File file : filesDiff) {
            if (!file.isDeleted()) {
                filesToBeDownloaded.add(file.getPath());
            }
        }

        return filesToBeDownloaded;
    }

    private ArrayList<String> getFileToBeDeleted(com.example.floudcloud.app.model.File.List cloudFiles, ArrayList<com.example.floudcloud.app.model.File> localFiles) {
        if (localFiles == null && cloudFiles == null) {
            return null;
        }

        ArrayList<String> fileToBeDeleted = new ArrayList<String>();


        for (com.example.floudcloud.app.model.File cloudFile : cloudFiles) {
            if (!cloudFile.isDeleted()) continue;
            for (com.example.floudcloud.app.model.File localFile : localFiles) {
                if (cloudFile.isSame(localFile)) {
                    fileToBeDeleted.add(localFile.getPath());
                    break;
                }
            }
        }

        return fileToBeDeleted;
    }

    private ArrayList<FileUpload> getFilesToBeUploaded(com.example.floudcloud.app.model.File.List cloudFiles, ArrayList<com.example.floudcloud.app.model.File> localFiles) {
        if (localFiles == null && cloudFiles == null) {
            return null;
        }

        ArrayList<FileUpload> filesToBeUploaded = new ArrayList<FileUpload>();

        for (com.example.floudcloud.app.model.File localFile : localFiles) {
            boolean inCloud = false;
            for (com.example.floudcloud.app.model.File cloudFile : cloudFiles) {
                if (localFile.isSame(cloudFile)) {
                    inCloud = true;
                    break;
                }
            }

            if (!inCloud) {
                filesToBeUploaded.add(new FileUpload(localFile.getPath(), localFile.getSize(), localFile.getHash(), mRegId));
            }
        }

        return filesToBeUploaded;
    }

    private void downloadFiles(ArrayList<String> files) {
        for (String filePath : files) {
            Intent downloadIntent = new Intent(MainService.this, CloudOperationsService.class);
            downloadIntent.putExtra(CloudOperationsService.EXTRA_API_KEY, mApiKey);
            downloadIntent.putExtra(CloudOperationsService.EXTRA_PATH, filePath);
            downloadIntent.putExtra(CloudOperationsService.EXTRA_REG_ID, mRegId);
            downloadIntent.putExtra(CloudOperationsService.EXTRA_OPERATION, CloudOperationsService.OPERATION_DOWNLOAD);

            startService(downloadIntent);
        }
    }

    private void uploadFiles(ArrayList<FileUpload> files) {
        for (FileUpload file : files) {
            Intent uploadIntent = new Intent(MainService.this, CloudOperationsService.class);
            uploadIntent.putExtra(CloudOperationsService.EXTRA_API_KEY, mApiKey);
            uploadIntent.putExtra(CloudOperationsService.EXTRA_FILE, file);
            uploadIntent.putExtra(CloudOperationsService.EXTRA_OPERATION, CloudOperationsService.OPERATION_UPLOAD);
            uploadIntent.putExtra(CloudOperationsService.EXTRA_REG_ID, mRegId);

            startService(uploadIntent);
        }
    }

    private void deleteLocaleFiles(ArrayList<String> files) {
        if (!FileUtils.deleteFiles(files)) {
            Log.e(LOG_TAG, "Could not delete locale files!");
        }
    }

    private void addIgnoredFiles(ArrayList<String> files) {
        for(String file : files) {
            mFileObserverService.addIgnored(file);
        }
    }


    private static class ServiceHandler extends Handler {
        private MainService mService;

        public ServiceHandler(Looper looper, MainService service) {
            super(looper);
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            String path = (String) msg.obj;

            if (path != null) {
                digest(path);

                //mService.mFileObserverService.startWatcher();
            }
        }

        private void digest(String path) {
            ArrayList<File> pathContent = FileUtils.getDirectoryContent(new File(path));
            ArrayList<com.example.floudcloud.app.model.File> localeFiles = FileUtils.generateFilesList(pathContent);
            com.example.floudcloud.app.model.File.List cloudFiles = mService.fetchFiles(true);

            ArrayList<String> filesToBeDownloaded = mService.getFilesToBeDownloaded(cloudFiles, localeFiles);
            ArrayList<String> fileToBeDeleted = mService.getFileToBeDeleted(cloudFiles, localeFiles);
            ArrayList<FileUpload> filesToBeUploaded = mService.getFilesToBeUploaded(mService.fetchFiles(false), localeFiles);

            ArrayList<String> ignoredFiles = (ArrayList<String>) filesToBeDownloaded.clone();
            ignoredFiles.addAll(fileToBeDeleted);
            mService.addIgnoredFiles(ignoredFiles);

            if (fileToBeDeleted != null && !fileToBeDeleted.isEmpty()) {
                mService.deleteLocaleFiles(fileToBeDeleted);
            }

            if (filesToBeDownloaded != null && !filesToBeDownloaded.isEmpty()) {
                mService.downloadFiles(filesToBeDownloaded);
            }

            if (filesToBeUploaded != null && !filesToBeUploaded.isEmpty()) {
                mService.uploadFiles(filesToBeUploaded);
            }

        }
    }
}
