package com.example.floudcloud.app.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.floudcloud.app.R;
import com.example.floudcloud.app.model.FileUpload;
import com.example.floudcloud.app.network.FloudFile;
import com.example.floudcloud.app.network.FloudService;
import com.example.floudcloud.app.utility.FileUtils;
import com.example.floudcloud.app.utility.ProgressNotify;
import com.example.floudcloud.app.utility.SharedPrefs;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;

import retrofit.mime.TypedFile;

public class CloudOperationsService extends Service {
    public static final String EXTRA_OPERATION = "OPERATION";
    public static final String EXTRA_PATH = "PATH";
    public static final String EXTRA_API_KEY = "API_KEY";
    public static final String EXTRA_FILE = "FILE";

    public static final int OPERATION_NOP = -1;
    public static final int OPERATION_DOWNLOAD = 0;
    public static final int OPERATION_UPLOAD = 1;
    public static final int OPERATION_REMOVE = 2;
    public static final int OPERATION_RESTORE = 3;

    private static final String LOG_TAG = CloudOperationsService.class.getSimpleName();

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;

    private class Operation {
        private int mOperation;
        private String mPath;
        private FileUpload mFileUpload;

        Operation(int operation, String path, FileUpload fileUpload) {
            mOperation = operation;
            mPath = path;
            mFileUpload = fileUpload;
        }

        public int getOperation() {
            return mOperation;
        }

        public String getPath() {
            return mPath;
        }

        public FileUpload getFileUpload() {
            return mFileUpload;
        }
    }

    private ServiceHandler mServiceHandler;
    private IBinder mBinder;
    private FloudService mFloudService;
    private Stack<Operation> mOperations = new Stack<Operation>();

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        HandlerThread thread =  new HandlerThread("Operations thread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Looper mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!intent.hasExtra(EXTRA_OPERATION)  && !intent.hasExtra(EXTRA_API_KEY)) {
            Log.e(LOG_TAG, "Not enough information provided in intent");
            return START_NOT_STICKY;
        }

        int operation = intent.getIntExtra(EXTRA_OPERATION, OPERATION_NOP);

        if (operation == OPERATION_NOP) {
            return START_NOT_STICKY;
        }

        String apiKey = intent.getStringExtra(EXTRA_API_KEY);
        String path = intent.getStringExtra(EXTRA_PATH);
        FileUpload fileUpload = intent.getParcelableExtra(EXTRA_FILE);

        mFloudService = new FloudService(apiKey);

        mOperations.push(new Operation(operation, path, fileUpload));

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return START_NOT_STICKY;
    }

    private void notifyDownload(String path) {
        String title = String.format("%s %s", path, getResources().getString(R.string.download));
        String text = String.format("%s %s", path, getResources().getString(R.string.dw_in_progress));
        String ticker = getResources().getString(R.string.dw_started);


        mNotificationBuilder = new NotificationCompat.Builder(this);
        mNotificationBuilder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setTicker(ticker)
                .setOngoing(true)
                .setProgress(100, 0, false);

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Uri uri = Uri.parse(FileUtils.getFileBase());
        intent.setDataAndType(uri, "*/*");

        mNotificationBuilder.setContentIntent(
                PendingIntent.getActivity(
                        this,
                        (int) System.currentTimeMillis(),
                        Intent.createChooser(intent, "Open a folder"),
                        0
                )
        );

        mNotificationManager.notify(R.string.download, mNotificationBuilder.build());
    }

    private void notifyDownloadProgress(int progress) {
        mNotificationManager.cancel(R.string.download);
        mNotificationBuilder
                .setProgress(100, progress, false);

        mNotificationManager.notify(R.string.download, mNotificationBuilder.build());
    }

    private void notifyDownloadResult(int result) {
        mNotificationManager.cancel(R.string.download);
        mNotificationBuilder
                .setProgress(0, 0, false)
                .setContentTitle(getResources().getString(result))
                .setContentText("")
                .setAutoCancel(true)
                .setOngoing(false);

        mNotificationManager.notify(R.string.download, mNotificationBuilder.build());
    }

    private void notifyUpload(String path) {
        String title = String.format("%s %s", path, getResources().getString(R.string.upload));
        String text = String.format("%s %s", path, getResources().getString(R.string.up_in_progress));
        String ticker = getResources().getString(R.string.up_started);


        mNotificationBuilder = new NotificationCompat.Builder(this);
        mNotificationBuilder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setTicker(ticker)
                .setOngoing(true)
                .setProgress(100, 0, false);

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Uri uri = Uri.parse(FileUtils.getFileBase());
        intent.setDataAndType(uri, "*/*");

        mNotificationBuilder.setContentIntent(
                PendingIntent.getActivity(
                        this,
                        (int) System.currentTimeMillis(),
                        Intent.createChooser(intent, "Open a folder"),
                        0
                )
        );

        mNotificationManager.notify(R.string.upload, mNotificationBuilder.build());
    }

    private void notifyUploadProgress(int progress) {
        mNotificationManager.cancel(R.string.upload);
        mNotificationBuilder
                .setProgress(100, progress, false);

        mNotificationManager.notify(R.string.upload, mNotificationBuilder.build());
    }

    private void notifyUploadResult(int result) {
        mNotificationManager.cancel(R.string.upload);
        mNotificationBuilder
                .setProgress(0, 0, false)
                .setContentTitle(getResources().getString(result))
                .setContentText("")
                .setAutoCancel(true)
                .setOngoing(false);

        mNotificationManager.notify(R.string.upload, mNotificationBuilder.build());
    }

    private static class ServiceHandler extends Handler {
        private CloudOperationsService mService;

        public ServiceHandler(Looper looper, CloudOperationsService service) {
            super(looper);
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            mService.nextOperation();
            mService.stopSelf(msg.arg1);
        }
    }

    private void nextOperation() {
        Operation operation = null;

        synchronized (mOperations) {
            operation = mOperations.pop();
        }

        if (operation != null) {
            switch (operation.getOperation()) {
                case OPERATION_DOWNLOAD:
                    performDownload(mFloudService.getApiKey(), operation.getPath());
                    break;
                case OPERATION_UPLOAD:
                    performUpload(mFloudService.getApiKey(), operation.getFileUpload());
                    break;
            }
        }
    }

    private boolean performDownload(String apiKey, String path) {
        notifyDownload(path);

        File saveDir = new File(FileUtils.getFileBase() + path).getParentFile();

        String error = FileUtils.downloadFile(FloudService.FILE_BASE_URL + "?path=" + path, apiKey, saveDir, new ProgressNotify() {
            @Override
            public void notifyProgress(int progress) {
                notifyDownloadProgress(progress);
            }
        });

        if (error != null) {
            notifyDownloadResult(R.string.dw_error);
        } else {
            notifyDownloadResult(R.string.dw_finished);
        }


        return true;
    }


    private boolean performUpload(String apiKey, FileUpload fileUpload) {
        notifyUpload(fileUpload.path);

        int resultStatus = FileUtils.uploadFile(FloudService.FILE_BASE_URL + "?path=" + fileUpload.path, apiKey, fileUpload, FileUtils.getFileBase() + fileUpload.path, new ProgressNotify() {
            @Override
            public void notifyProgress(int progress) {
                notifyUploadProgress(progress);
            }
        });

        if (resultStatus != 200) {
            notifyUploadResult(R.string.up_error);
        } else {
            notifyUploadResult(R.string.up_finished);
        }

        return true;
    }
}
