package com.example.floudcloud.app.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.floudcloud.app.model.FileUpload;
import com.example.floudcloud.app.network.FloudService;
import com.example.floudcloud.app.network.exception.InternalServerError;
import com.example.floudcloud.app.network.exception.NotFoundError;
import com.example.floudcloud.app.network.exception.UnauthorizedError;
import com.example.floudcloud.app.utility.FileUtils;
import com.example.floudcloud.app.utility.SharedPrefs;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import retrofit.RetrofitError;


public class MainService extends Service {
    private static String LOG_TAG = MainService.class.getSimpleName();

    public static String EXTRA_PATH = "PATH";
    public static String EXTRA_API_KEY = "API_KEY";
    public static final String EXTRA_MEGA_KASTIL = "bilo 3 chasa nochi i nuzhno bilo pisat diplom";

    private ServiceHandler mServiceHandler;
    private IBinder mBinder;
    private String mApiKey;
    private FloudService floudService;

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
        HandlerThread thread =  new HandlerThread("Main thread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Looper mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!intent.hasExtra(EXTRA_PATH) && !intent.hasExtra(EXTRA_API_KEY)) {
            Log.e(LOG_TAG, "Not enough information provided in intent");
            return START_NOT_STICKY;
        }

        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + intent.getStringExtra(CloudOperationsService.EXTRA_PATH);
        mApiKey = intent.getStringExtra(CloudOperationsService.EXTRA_API_KEY);

        floudService = new FloudService(mApiKey);

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = path;
        // FIXME :(
        msg.arg2 = intent.getIntExtra(EXTRA_MEGA_KASTIL, 0);
        mServiceHandler.sendMessage(msg);

        return START_NOT_STICKY;
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

        ArrayList<com.example.floudcloud.app.model.File> filesDiff = (com.example.floudcloud.app.model.File.List)cloudFiles.clone();

        if(localFiles != null) {
            filesDiff.removeAll(localFiles);
        }

        if (filesDiff == null) {
            return null;
        }

        ArrayList<String> filesToBeDownloaded = new ArrayList<String>();

        for (com.example.floudcloud.app.model.File file : filesDiff) {
            filesToBeDownloaded.add(file.getPath());
        }

        return filesToBeDownloaded;
    }

    private ArrayList<FileUpload> getFilesToBeUploaded(com.example.floudcloud.app.model.File.List cloudFiles, ArrayList<com.example.floudcloud.app.model.File> localFiles) {
        if (localFiles == null) {
            return null;
        }

        ArrayList<com.example.floudcloud.app.model.File> filesDiff = (ArrayList<com.example.floudcloud.app.model.File>)localFiles.clone();

        if (cloudFiles != null) {
            filesDiff.removeAll(cloudFiles);
        }

        if (filesDiff == null) {
            return null;
        }

        ArrayList<FileUpload> filesToBeUploaded = new ArrayList<FileUpload>();

        for (com.example.floudcloud.app.model.File file : filesDiff) {
            filesToBeUploaded.add(new FileUpload(file.getPath(), file.getSize(), file.getHash()));
        }

        return filesToBeUploaded;
    }

    private void downloadFiles(ArrayList<String> files) {
        for (String filePath : files) {
            Intent downloadIntent = new Intent(MainService.this, CloudOperationsService.class);
            downloadIntent.putExtra(CloudOperationsService.EXTRA_API_KEY, mApiKey);
            downloadIntent.putExtra(CloudOperationsService.EXTRA_PATH, filePath);
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

            startService(uploadIntent);
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

            if (msg.arg2 == 1) {
                Intent fileObserverIntent = new Intent(mService, FileObserverService.class);
                fileObserverIntent.putExtra(FileObserverService.EXTRA_API_KEY, mService.mApiKey);
                fileObserverIntent.putExtra(FileObserverService.EXTRA_PATH, path);
                mService.startService(fileObserverIntent);
                return;
            }

            if (path != null) {
                ArrayList<File> pathContent = FileUtils.getDirectoryContent(new File(path));
                ArrayList<com.example.floudcloud.app.model.File> localeFiles = FileUtils.generateFilesList(pathContent);
                com.example.floudcloud.app.model.File.List cloudFiles = mService.fetchFiles(false);

                ArrayList<String> filesToBeDownloaded = mService.getFilesToBeDownloaded(cloudFiles, localeFiles);
                ArrayList<FileUpload> filesToBeUploaded = mService.getFilesToBeUploaded(mService.fetchFiles(false), localeFiles);

                if (filesToBeDownloaded != null) {
                    mService.downloadFiles(filesToBeDownloaded);
                }

                if (filesToBeUploaded != null) {
                    mService.uploadFiles(filesToBeUploaded);
                }


            }

            mService.stopSelf(msg.arg1);
        }
    }
}
