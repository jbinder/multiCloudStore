package at.ac.tuwien.multicloudstore.android.activities;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;
import at.ac.tuwien.multicloudstore.android.R;
import at.ac.tuwien.multicloudstore.android.common.UserAccountStorage;

public class MainActivity extends TabActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.main);

        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;
        Intent intent;

        intent = new Intent().setClass(this, FileBrowserActivity.class);
        intent.putExtra("localMode", false);
        spec = tabHost.newTabSpec("remote").setIndicator(getString(R.string.remote)).setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, FileBrowserActivity.class);
        intent.putExtra("localMode", true);
        spec = tabHost.newTabSpec("local").setIndicator(getString(R.string.local)).setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, AccountListActivity.class);
        spec = tabHost.newTabSpec("accounts").setIndicator(getString(R.string.accounts)).setContent(intent);
        tabHost.addTab(spec);

        UserAccountStorage userAccountStorage = new UserAccountStorage(getSharedPreferences(UserAccountStorage.PREFS_NAME, 0));
        tabHost.setCurrentTab((userAccountStorage.getUserAccounts().size() > 0) ? 0 : 2);
    }

}