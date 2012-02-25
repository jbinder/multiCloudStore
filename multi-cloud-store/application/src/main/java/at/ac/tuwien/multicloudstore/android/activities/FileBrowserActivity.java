package at.ac.tuwien.multicloudstore.android.activities;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;
import at.ac.tuwien.multicloudstore.android.R;
import at.ac.tuwien.multicloudstore.android.common.*;
import at.ac.tuwien.multicloudstore.lib.common.FileSystemNode;
import at.ac.tuwien.multicloudstore.lib.transfer.InvalidServiceTypeException;
import at.ac.tuwien.multicloudstore.lib.transfer.TransferManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class
        FileBrowserActivity extends ListActivity {

    public static final String BROADCAST_REFRESH = "at.ac.tuwien.multicloudstore.android.activities.FileBrowserActivity.REFRESH";

    private static final String TAG = FileBrowserActivity.class.getSimpleName();
    private static final Map<String, Integer> accountTypeMapping = new HashMap<String, Integer>() {{
        put("FTP", 1);
        put("Amazon S3", 2);
    }};

    private ProgressBar progressBar = null;
    private TextView errorText = null;
    private FileSystemNode curFileList = null;
    private UserAccountStorage userAccountStorage;

    private LocalStorage localStorage = new LocalStorage();
    private TransferManager transferManager = new TransferManager();

    private boolean localMode = false;
    private boolean refreshListInProgress = false;

    private class ListTask extends AsyncTask<String, Void, FileSystemNode> {
        private Exception exception = null;

        @Override
        protected synchronized FileSystemNode doInBackground(String... params) {
            Log.i(TAG, "Getting file list (" + ((!localMode) ? "remote" : "local") + ").");
            if (refreshListInProgress) return null;
            refreshListInProgress = true;
            try {
                if (localMode) return transferManager.listLocal();
                else return transferManager.listRemote();
            } catch (Exception e) {
                exception = e;
            }
            return new FileSystemNode();
        }

        @Override
        protected void onPostExecute(FileSystemNode result) {
            if ((exception == null) && (result == null)) {
                Log.i(TAG, "Getting the file list was skipped because refresh is already in progress.");
                return;
            }
            if (exception != null) {
                Log.e(TAG, "Getting the file list failed (" + ((!localMode) ? "remote" : "local") + ").", exception);
                showToast("Unable to get file list.");
                errorText.setVisibility(View.VISIBLE);
            } else {
                Log.i(TAG, "Done getting list (" + ((!localMode) ? "remote" : "local") + ").");
                errorText.setVisibility(View.GONE);
            }

            progressBar.setVisibility(View.GONE);
            curFileList = result;
            drawList();
            refreshListInProgress = false;
        }
    }

    private class DownloadTask extends AsyncTask<String, Void, Boolean> {
        private Exception exception = null;

        @Override
        protected Boolean doInBackground(String... params) {
            Log.i(TAG, "Downloading file: " + params[0] + " -> " + params[1]);
            try {
                transferManager.download(params[1], params[0]);
                return true;
            } catch (Exception e) {
                exception = e;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (exception != null) {
                Log.e(TAG, "Downloading file failed.", exception);
                showToast("Unable to download file.");
                progressBar.setVisibility(View.GONE);
            } else {
                Log.i(TAG, "Done downloading file.");
                sendBroadcast(BroadcastService.generateBroadcast(BROADCAST_REFRESH));
                showToast("File successfully downloaded.");
            }
        }
    }

    private class UploadTask extends AsyncTask<String, Void, Boolean> {
        private Exception exception = null;

        @Override
        protected Boolean doInBackground(String... params) {
            Log.i(TAG, "Uploading file: " + params[1] + " -> " + params[0]);
            try {
                transferManager.upload(params[1], params[0]);
                return true;
            } catch (Exception e) {
                exception = e;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (exception != null) {
                Log.e(TAG, "Uploading file failed.", exception);
                showToast("Unable to upload file.");
                progressBar.setVisibility(View.GONE);
            } else {
                Log.i(TAG, "Done uploading file.");
                sendBroadcast(BroadcastService.generateBroadcast(BROADCAST_REFRESH));
                showToast("File successfully uploaded.");
            }

        }
    }

    private class DeleteTask extends AsyncTask<String, Void, Boolean> {
        private Exception exception = null;

        @Override
        protected Boolean doInBackground(String... params) {
            Log.i(TAG, "Deleting file: " + params[0]);
            try {
                if (localMode) transferManager.deleteLocal(params[0]);
                else transferManager.deleteRemote(params[0]);
                return true;
            } catch (Exception e) {
                exception = e;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (exception != null || !result) {
                Log.e(TAG, "Deleting file failed.", exception);
                showToast("Unable to delete file/directory.");
                progressBar.setVisibility(View.GONE);
            } else {
                Log.i(TAG, "File/directory successfully deleted.");
                sendBroadcast(BroadcastService.generateBroadcast(BROADCAST_REFRESH));
                showToast("File/directory successfully deleted.");
            }

        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        handleFile(position);
    }

    private void showFileOperationsDialog(final int pos) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Item selected");
        alertDialog.setMessage("Following operations are available for '" + curFileList.getChilds().get(pos).getName() + "':");

        if (localMode) {
            alertDialog.setButton("Upload", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    uploadFile(pos);
                }
            });
        } else {
            alertDialog.setButton("Download", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    downloadFile(pos);
                }
            });
        }
        alertDialog.setButton3("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                deleteFileConfirmDialog(pos);
            }
        });
        alertDialog.setButton2("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
    }

    private void handleFile(int pos) {
        FileSystemNode node = curFileList.getChilds().get(pos);
        switch (node.getType()) {
            case FileSystemNode.TYPE_FILE: showFileOperationsDialog(pos); break;
            case FileSystemNode.TYPE_DIRECTORY: changeDirectory(pos); break;
            default: Log.e(TAG, "Invalid file type.");
        }
    }

    private void changeDirectory(int index) {
        Log.i(TAG, "Change directory " + curFileList.getChilds().get(index));
        curFileList = curFileList.getChilds().get(index);
        if (curFileList.getName().equals(FileSystemNode.PARENT_FOLDER_NAME)) curFileList = curFileList.getParent().getParent();
        drawList();
    }

    private void downloadFile(int index) {
        Log.i(TAG, "Downloading " + curFileList.getChilds().get(index));
        progressBar.setVisibility(View.VISIBLE);
        String path;
        try {
            path = localStorage.getAppRootPath();
        } catch (StorageNotAvailableException e) {
            showToast(getString(R.string.no_memory_card));
            return;
        }
        DownloadTask task = new DownloadTask();
        task.execute(curFileList.getChilds().get(index).getFullPath(),
            new File(path, curFileList.getChilds().get(index).getFullPath()).getAbsolutePath());
    }

    private void uploadFile(int index) {
        Log.i(TAG, "Uploading " + curFileList.getChilds().get(index));
        progressBar.setVisibility(View.VISIBLE);
        String path;
        try {
            path = localStorage.getRootPath();
        } catch (StorageNotAvailableException e) {
            showToast(getString(R.string.no_memory_card));
            return;
        }
        UploadTask task = new UploadTask();
        task.execute(curFileList.getChilds().get(index).getFullPath(),
                new File(path, curFileList.getChilds().get(index).getFullPath()).getAbsolutePath());
    }

    private void deleteFileConfirmDialog(final int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_delete))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteFile(index);
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // nothing to be done
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void deleteFile(int index) {
        Log.i(TAG, "Deleting " + curFileList.getChilds().get(index));
        progressBar.setVisibility(View.VISIBLE);
        new DeleteTask().execute(curFileList.getChilds().get(index).getFullPath());
    }

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        localMode = (Boolean) getIntent().getExtras().get("localMode");
        userAccountStorage = new UserAccountStorage(getSharedPreferences(UserAccountStorage.PREFS_NAME, 0));

        setContentView(R.layout.file_browser);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        errorText = (TextView) findViewById(R.id.error_text);

        updateAccountsAndRefreshList();
        initBroadcastReceivers();
    }

    private void initBroadcastReceivers() {
        IntentFilter filter = new IntentFilter(FileBrowserActivity.BROADCAST_REFRESH);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getList();
            }
        }, filter);

        filter = new IntentFilter(BroadcastService.BROADCAST_ACCOUNT_UPDATE);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateAccountsAndRefreshList();
            }
        }, filter);
    }

    private void updateAccountsAndRefreshList() {
        try {
            userAccountStorage.reload();
            initTransferManager();
        } catch (InvalidServiceTypeException e) {
            showToast(getString(R.string.server_not_supported));
        } catch (StorageNotAvailableException e) {
            showToast(getString(R.string.no_memory_card));
        }
        getList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (localMode) inflater.inflate(R.layout.file_list_menu_local, menu);
        else inflater.inflate(R.layout.file_list_menu_remote, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.delete:
            deleteFiles(getListView().getCheckedItemPositions());
            return true;
        case R.id.download:
            downloadFiles(getListView().getCheckedItemPositions());
            return true;
        case R.id.upload:
            uploadFiles(getListView().getCheckedItemPositions());
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void uploadFiles(SparseBooleanArray checkedItemPositions) {
        for (int i = 0; i < curFileList.getChilds().size(); ++i) {
            if (checkedItemPositions.get(i)) {
                uploadFile(i);
            }
        }
    }

    private void downloadFiles(SparseBooleanArray checkedItemPositions) {
        for (int i = 0; i < curFileList.getChilds().size(); ++i) {
            if (checkedItemPositions.get(i)) {
                downloadFile(i);
            }
        }
    }

    private void deleteFiles(SparseBooleanArray checkedItemPositions) {
        for (int i = 0; i < curFileList.getChilds().size(); ++i) {
            if (checkedItemPositions.get(i)) {
                deleteFileConfirmDialog(i);
            }
        }
    }

    private void initTransferManager() throws InvalidServiceTypeException, StorageNotAvailableException {
        transferManager.clearRemoteStorageServices();
        for (UserAccount account : userAccountStorage.getEnabledUserAccounts()) {
            transferManager.addRemoteStorageService(
                    accountTypeMapping.get(account.getType()), account.getServerAddress(), account.getUserName(), account.getPassword());
        }
        transferManager.setLocalRootDirectory(localStorage.getRootPath());
    }

    private void getList() {
        progressBar.setVisibility(View.VISIBLE);
        ListTask task = new ListTask();
        task.execute();
    }

    private void drawList() {
        setListAdapter(new SimpleAdapter(
            this,
            curFileList.getChildsWithInfo(),
            R.layout.file_list_adapter_item,
            new String[] {"name"},
            new int[] {android.R.id.text1}));
    }

    private void showToast(String msg) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        Toast.makeText(context, msg, duration).show();
    }
}
