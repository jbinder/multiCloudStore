package at.ac.tuwien.multicloudstore.android.common;

import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UserAccountStorage {

    public static final String PREFS_NAME = "account";

    private static String TAG = UserAccountStorage.class.getName();

    private SharedPreferences prefs;
    private List<UserAccount> userAccounts;

    public UserAccountStorage(SharedPreferences prefs) {
        this.prefs = prefs;
        userAccounts = new ArrayList<UserAccount>();
        reload();
    }

    private String createPrefName(int i, String name) {
        return i + "_" + name;
    }

    public void addAccount(UserAccount userAccount) {
        Log.i(TAG, "Adding: " + userAccount);
        writeAccount(userAccounts.size(), userAccount);
        userAccounts.add(userAccount);
        writeCount();
    }

    private void writeAccount(int index, UserAccount userAccount) {
        Log.i(TAG, "Writing: " + userAccount);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(createPrefName(index, "type"), userAccount.getType());
        editor.putString(createPrefName(index, "host"), userAccount.getServerAddress());
        editor.putString(createPrefName(index, "userName"), userAccount.getUserName());
        editor.putString(createPrefName(index, "password"), userAccount.getPassword());
        editor.putBoolean(createPrefName(index, "enabled"), userAccount.isEnabled());
        editor.commit();
    }

    public List<UserAccount> getUserAccounts() {
        return userAccounts;
    }

    public ArrayList<HashMap<String,String>> buildAccountList() {
        ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
        for (UserAccount child : userAccounts) {
            list.add(createAccountElement(child));
        }
        return list;
    }

    private HashMap<String, String> createAccountElement(UserAccount child) {
        HashMap<String,String> temp = new HashMap<String,String>();
        temp.put("name", child.getUserName());
        return temp;
    }

    public void remove(int index) {
        Log.i(TAG, "Removing: " + userAccounts.get(index));
        userAccounts.remove(index);
        write();
    }

    public void write() {
        Log.i(TAG, "Writing all...");
        for (int i = 0; i < userAccounts.size(); ++i) {
            writeAccount(i, userAccounts.get(i));
        }
        writeCount();
    }

    private void writeCount() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("count", userAccounts.size());
        editor.commit();
    }

    public UserAccount get(int index) {
        return userAccounts.get(index);
    }

    public void reload() {
        Log.i(TAG, "Reloading...");
        userAccounts.clear();
        int count = prefs.getInt("count", -1);
        for (int i = 0; i < count; ++i) {
            String type = prefs.getString(createPrefName(i, "type"), "");
            String host = prefs.getString(createPrefName(i, "host"), "");
            String userName = prefs.getString(createPrefName(i, "userName"), "");
            String password = prefs.getString(createPrefName(i, "password"), "");
            Boolean enabled = prefs.getBoolean(createPrefName(i, "enabled"), true);

            UserAccount userAccount = new UserAccount(type, host, userName, password) ;
            userAccount.setEnabled(enabled);
            userAccounts.add(userAccount);
            Log.i(TAG, "Loaded #" + i + ": " + userAccount);
        }
    }

    public void edit(Integer i, UserAccount account) {
        UserAccount userAccount = userAccounts.get(i);
        userAccount.setType(account.getType());
        userAccount.setServerAddress(account.getServerAddress());
        userAccount.setUserName(account.getUserName());
        userAccount.setPassword(account.getPassword());
        userAccount.setEnabled(account.isEnabled());
        writeAccount(i, userAccount);
    }

    public List<UserAccount> getEnabledUserAccounts() {
        List<UserAccount> enabledAccounts = new ArrayList<UserAccount>();
        for (UserAccount account : userAccounts) {
            if (!account.isEnabled()) continue;
            enabledAccounts.add(account);
        }
        return enabledAccounts;
    }
}
