package at.ac.tuwien.multicloudstore.android.activities;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;
import at.ac.tuwien.multicloudstore.android.R;
import at.ac.tuwien.multicloudstore.android.common.BroadcastService;
import at.ac.tuwien.multicloudstore.android.common.UserAccount;
import at.ac.tuwien.multicloudstore.android.common.UserAccountStorage;

public class AccountListActivity extends ListActivity {

    private static final String TAG = AccountListActivity.class.getSimpleName();

    private SimpleAdapter adapter;
    private UserAccountStorage userAccountStorage;

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

        setContentView(R.layout.account_list);

        userAccountStorage = new UserAccountStorage(getSharedPreferences(UserAccountStorage.PREFS_NAME, 0));
        reCreateAdapter();
    }

    private void reCreateAdapter() {
        adapter = new SimpleAdapter(
            this,
            userAccountStorage.buildAccountList(),
            R.layout.file_list_adapter_item,
            new String[] {"name"},
            new int[] {android.R.id.text1});
        setListAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.account_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.edit:
            editAccounts(getListView().getCheckedItemPositions());
            return true;
        case R.id.delete:
            deleteAccounts(getListView().getCheckedItemPositions());
            return true;
        case R.id.new_account:
            showAccountAlert();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void editAccounts(SparseBooleanArray checkedItemPositions) {
        for (int i = 0; i < adapter.getCount(); ++i) {
            if (checkedItemPositions.get(i)) {
                editAccount(i);
            }
        }
    }

    private void editAccount(int i) {
        showAccountAlert(userAccountStorage.get(i), i);
    }

    private void deleteAccounts(SparseBooleanArray checkedItemPositions) {
        for (int i = 0; i < adapter.getCount(); ++i) {
            if (checkedItemPositions.get(i)) {
                deleteAccountDialog(i);
            }
        }
    }

    private void deleteAccountDialog(final int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_delete))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteAccount(index);
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

    private void deleteAccount(int index) {
        Log.i(TAG, "Deleting " + userAccountStorage.get(index));
        userAccountStorage.remove(index);
        reCreateAdapter();
        sendBroadcast(BroadcastService.generateBroadcast(BroadcastService.BROADCAST_ACCOUNT_UPDATE));
    }

    private void showAccountAlert() {
        showAccountAlert(null, null);
    }

    private void showAccountAlert(final UserAccount userAccount, final Integer i) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        LinearLayout view = (LinearLayout) getLayoutInflater().inflate(
                R.layout.account_settings_dialog, (ViewGroup) findViewById(R.id.layout_root));

        alert.setTitle(getString((userAccount != null) ? R.string.asd_edit : R.string.asd_create));
        // alert.setMessage(""); // TODO: is a message needed?

        final TextView hostLabel = (TextView) view.findViewById(R.id.address_label);
        final EditText hostInput = (EditText) view.findViewById(R.id.address);
        final EditText userNameInput = (EditText) view.findViewById(R.id.username);
        final EditText passwordInput = (EditText) view.findViewById(R.id.password);
        final CheckBox enabledInput = (CheckBox) view.findViewById(R.id.enabled);
        passwordInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setTransformationMethod(new PasswordTransformationMethod());
        final Spinner spinner = (Spinner) view.findViewById(R.id.type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.asd_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "Clicked on: " + i + ".");
                switch (i) {
                    case 1:
                        hostLabel.setText(R.string.asd_address_s3);
                        break;
                    default:
                        hostLabel.setText(R.string.asd_address);
                        break;
                }
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                // nothing to be done
            }
        });

        if (userAccount != null) {
            spinner.setSelection(adapter.getPosition(userAccount.getType()));
            hostInput.setText(userAccount.getServerAddress());
            userNameInput.setText(userAccount.getUserName());
            passwordInput.setText(userAccount.getPassword());
            enabledInput.setChecked(userAccount.isEnabled());
        }
        alert.setView(view);

        alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                UserAccount account = new UserAccount(
                    (String) spinner.getSelectedItem(),
                    hostInput.getText().toString(), userNameInput.getText().toString(), passwordInput.getText().toString());
                account.setEnabled(enabledInput.isChecked());
                if (userAccount != null) {
                    userAccountStorage.edit(i, account);
                } else {
                    userAccountStorage.addAccount(account);
                }
                reCreateAdapter();
                sendBroadcast(BroadcastService.generateBroadcast(BroadcastService.BROADCAST_ACCOUNT_UPDATE));
            }
        });
        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // nothing to be done
            }
        });

        alert.show();
    }

}
