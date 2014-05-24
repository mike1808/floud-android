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
import com.example.floudcloud.app.operation.DeleteOperation;
import com.example.floudcloud.app.operation.DownloadOperation;
import com.example.floudcloud.app.operation.MoveOperation;
import com.example.floudcloud.app.operation.RemoteOperation;
import com.example.floudcloud.app.operation.UploadOperation;
import com.example.floudcloud.app.utility.FileUtils;
import com.example.floudcloud.app.utility.ProgressListener;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CloudOperationsService extends Service {
    public static final String EXTRA_OPERATION = "OPERATION";
    public static final String EXTRA_PATH = "PATH";
    public static final String EXTRA_OLD_PATH = "OLD_PATH";
    public static final String EXTRA_API_KEY = "API_KEY";
    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_REG_ID = "REG_ID";

    public static final int OPERATION_NOP = -1;
    public static final int OPERATION_DOWNLOAD = 0;
    public static final int OPERATION_UPLOAD = 1;
    public static final int OPERATION_MOVE = 2;
    public static final int OPERATION_DELETE = 3;

    private static final String LOG_TAG = CloudOperationsService.class.getSimpleName();

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;

    private ServiceHandler mServiceHandler;
    private IBinder mBinder;
    private ConcurrentLinkedQueue<RemoteOperation> mOperations = new ConcurrentLinkedQueue<RemoteOperation>();

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        HandlerThread thread = new HandlerThread("Operations thread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
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
        if (!intent.hasExtra(EXTRA_OPERATION) && !intent.hasExtra(EXTRA_API_KEY)) {
            Log.e(LOG_TAG, "Not enough information provided in intent");
            return START_NOT_STICKY;
        }

        int operation = intent.getIntExtra(EXTRA_OPERATION, OPERATION_NOP);

        String apiKey = intent.getStringExtra(EXTRA_API_KEY);
        String path = intent.getStringExtra(EXTRA_PATH);
        String regId = intent.getStringExtra(EXTRA_REG_ID);
        FileUpload fileUpload = intent.getParcelableExtra(EXTRA_FILE);

        switch (operation) {
            case OPERATION_DOWNLOAD:
                try {
                    mOperations.add(new DownloadOperation(path, apiKey, FileUtils.getFileBase()));
                } catch (Exception e) {
                    Log.e(LOG_TAG, "DownloadOperation " + e.getMessage());
                }
                break;
            case OPERATION_UPLOAD:
                mOperations.add(new UploadOperation(fileUpload, apiKey, FileUtils.getFileBase(), regId));
                break;
            case OPERATION_MOVE:
                String oldPath = intent.getStringExtra(EXTRA_OLD_PATH);
                mOperations.add(new MoveOperation(apiKey, path, oldPath, regId));
                break;
            case OPERATION_DELETE:
                mOperations.add(new DeleteOperation(apiKey, path, regId));
                break;
        }

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return START_NOT_STICKY;
    }

    private void notifyDownload(String path) {
        String ticker = getResources().getString(R.string.dw_started) + " " + path;

        mNotificationBuilder = new NotificationCompat.Builder(this);
        mNotificationBuilder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(path)
                .setContentText(getResources().getString(R.string.dw_in_progress))
                .setTicker(ticker)
                .setOngoing(true)
                .setProgress(100, 0, false);

        mNotificationBuilder.setContentIntent(
                PendingIntent.getActivity(
                        this,
                        (int) System.currentTimeMillis(),
                        createNotificationIntent(path),
                        0
                )
        );

        mNotificationManager.notify(path.hashCode(), mNotificationBuilder.build());
    }

    private void notifyDownloadProgress(String path, int progress) {
        mNotificationBuilder
                .setProgress(100, progress, false);

        mNotificationManager.notify(path.hashCode(), mNotificationBuilder.build());
    }

    private void notifyDownloadResult(String path, int result) {
        removeIgnoredFile(path);

        mNotificationManager.cancel(R.string.download);
        mNotificationBuilder
                .setProgress(0, 0, false)
                .setContentTitle(path)
                .setContentText(getResources().getString(result))
                .setTicker(getResources().getString(R.string.dw_finished))
                .setAutoCancel(true)
                .setOngoing(false);

        mNotificationManager.notify(path.hashCode(), mNotificationBuilder.build());
    }

    private void notifyUpload(String path) {
        String ticker = getResources().getString(R.string.up_started) + " " + path;

        mNotificationBuilder = new NotificationCompat.Builder(this);
        mNotificationBuilder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(path)
                .setContentText(getResources().getString(R.string.up_in_progress))
                .setTicker(ticker)
                .setOngoing(true)
                .setProgress(100, 0, false);

        mNotificationBuilder.setContentIntent(
                PendingIntent.getActivity(
                        this,
                        (int) System.currentTimeMillis(),
                        createNotificationIntent(path),
                        0
                )
        );

        mNotificationManager.notify(path.hashCode(), mNotificationBuilder.build());
    }

    private void notifyUploadProgress(String path, int progress) {
        mNotificationBuilder
                .setProgress(100, progress, false);

        mNotificationManager.notify(path.hashCode(), mNotificationBuilder.build());
    }

    private void notifyUploadResult(String path, int result) {
        mNotificationManager.cancel(R.string.upload);
        mNotificationBuilder
                .setProgress(0, 0, false)
                .setContentTitle(path)
                .setContentText(getResources().getString(result))
                .setTicker(getResources().getString(R.string.up_finished))
                .setAutoCancel(true)
                .setOngoing(false);

        mNotificationManager.notify(path.hashCode(), mNotificationBuilder.build());
    }

    private void notifyMoveFailed(String path, int result) {
        mNotificationBuilder = new NotificationCompat.Builder(this);
        mNotificationBuilder
                .setSmallIcon(R.drawable.ic_notification)
                .setTicker(getResources().getString(R.string.move_failed))
                .setContentTitle(path)
                .setContentText(getResources().getString(R.string.move_failed));

        mNotificationManager.notify(path.hashCode(), mNotificationBuilder.build());
    }

    private void notifyDeleteFailed(String path, int result) {
        mNotificationBuilder = new NotificationCompat.Builder(this);
        mNotificationBuilder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(path)
                .setContentText(getResources().getString(R.string.delete_failed));

        mNotificationManager.notify(path.hashCode(), mNotificationBuilder.build());
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

    private void removeIgnoredFile(String path) {
        Intent observer = new Intent(FileObserverService.IGNORE_FILE);
        observer.putExtra(FileObserverService.EXTRA_REMOVE_IGNORE, path);
        sendBroadcast(observer);
    }


    private void nextOperation() {
        RemoteOperation operation = null;

        synchronized (mOperations) {
            operation = mOperations.peek();
        }

        if (operation != null) {
            if (operation instanceof DownloadOperation) {
                performDownload((DownloadOperation) operation);
            } else if (operation instanceof UploadOperation) {
                performUpload((UploadOperation) operation);
            } else if (operation instanceof MoveOperation) {
                performMove((MoveOperation) operation);
            } else if (operation instanceof DeleteOperation) {
                performDelete((DeleteOperation) operation);
            }
        }

        synchronized (mOperations) {
            mOperations.poll();
        }
    }

    private boolean performMove(MoveOperation operation) {
        final String path = operation.getPath();
        Log.d(LOG_TAG, "Move for " + path + " has started");

        int statusCode = operation.execute(null);

        if (statusCode != 200) {
            notifyMoveFailed(path, 0);
        }

        removeIgnoredFile(path);

        return true;
    }

    private boolean performDelete(DeleteOperation operation) {
        final String path = operation.getPath();
        Log.d(LOG_TAG, "Delete for " + path + " has started");

        int statusCode = operation.execute(null);

        if (statusCode != 200) {
            notifyDeleteFailed(path, 0);
        }

        removeIgnoredFile(path);

        return true;
    }

    private boolean performDownload(DownloadOperation operation) {
        final String path = operation.getPath();
        Log.d(LOG_TAG, "Download for " + path + " has started");

        notifyDownload(path);

        int statusCode = operation.execute(new ProgressListener() {
            @Override
            public void notifyProgress(int progress) {
                notifyDownloadProgress(path, progress);
            }
        });

        if (statusCode != 200) {
            notifyDownloadResult(path, R.string.dw_error);
        } else {
            notifyDownloadResult(path, R.string.dw_finished);
        }


        return true;
    }


    private boolean performUpload(UploadOperation operation) {
        final String path = operation.getPath();
        Log.d(LOG_TAG, "Upload for " + path + " has started");


        notifyUpload(path);

        int resultStatus = operation.execute(new ProgressListener() {
            @Override
            public void notifyProgress(int progress) {
                notifyUploadProgress(path, progress);
            }
        });

        if (resultStatus != 201 && resultStatus != 200) {
            notifyUploadResult(path, R.string.up_error);
        } else {
            notifyUploadResult(path, R.string.up_finished);
        }

        return true;
    }

    private Intent createNotificationIntent(String path) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(new File(FileUtils.getFileBase(), path)));
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        return intent;
    }
}
