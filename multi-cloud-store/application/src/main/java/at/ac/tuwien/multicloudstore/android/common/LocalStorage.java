package at.ac.tuwien.multicloudstore.android.common;

import android.os.Environment;

import java.io.File;

public class LocalStorage {

    private static String TAG = LocalStorage.class.getName();

    private static final String ROOT_PATH = "mcs";

    private File root = null;

    // private BroadcastReceiver mExternalStorageReceiver;
    private boolean isExternalStorageAvailable = false;
    private boolean isExternalStorageWriteable = false;

    public LocalStorage() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            isExternalStorageAvailable = isExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            isExternalStorageAvailable = true;
            isExternalStorageWriteable = false;
        } else {
            isExternalStorageAvailable = isExternalStorageWriteable = false;
        }
        root = new File(Environment.getExternalStorageDirectory(), ROOT_PATH);
        root.mkdirs();
    }

    public String getAppRootPath() throws StorageNotAvailableException {
        if (!(isExternalStorageAvailable && isExternalStorageWriteable)) {
            throw new StorageNotAvailableException();
        }
        return root.getAbsolutePath();
    }

    public String getRootPath() throws StorageNotAvailableException {
        if (!(isExternalStorageAvailable && isExternalStorageWriteable)) {
            throw new StorageNotAvailableException();
        }
        return root.getParentFile().getAbsolutePath();
    }

    /*
    public void startWatchingExternalStorage() {
        mExternalStorageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("test", "Storage: " + intent.getData());
                updateExternalStorageState();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        registerReceiver(mExternalStorageReceiver, filter);
        updateExternalStorageState();
    }

    private void stopWatchingExternalStorage() {
        unregisterReceiver(mExternalStorageReceiver);
    }
    */
}
