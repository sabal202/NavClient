package sabal.navclient.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;

import io.realm.Realm;
import sabal.navclient.R;
import sabal.navclient.Utils;


public abstract class BaseActivity extends AppCompatActivity {

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    static final int REQUEST_CONNECT_DEVICE = 1;
    static final int REQUEST_ENABLE_BT = 2;
    private static final String SAVED_PENDING_REQUEST_ENABLE_BT = "PENDING_REQUEST_ENABLE_BT";
    BluetoothAdapter btAdapter;
    boolean pendingRequestEnableBt = false;
    private ProgressDialog progressDialog;
    protected final Realm.Transaction.OnError errorCallback = new Realm.Transaction.OnError() {
        @Override
        public void onError(Throwable error) {
            hideProgress();
        }
    };

    protected Realm.Transaction.OnSuccess successCallback = new Realm.Transaction.OnSuccess() {
        @Override
        public void onSuccess() {
            hideProgress();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getSupportActionBar().setHomeButtonEnabled(false);

        if (state != null) {
            pendingRequestEnableBt = state.getBoolean(SAVED_PENDING_REQUEST_ENABLE_BT);
        }
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            final String no_bluetooth = getString(R.string.no_bt_support);
            showAlertDialog(no_bluetooth);
            Utils.log(no_bluetooth);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        if (btAdapter == null) return;

        if (!btAdapter.isEnabled() && !pendingRequestEnableBt) {
            pendingRequestEnableBt = true;
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    protected void showProgress(@StringRes int titleRes) {

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setTitle(titleRes);
        try {
            progressDialog.show();
        } catch (Exception ignored) {
        }
    }

    protected void hideProgress() {
        if (progressDialog != null) {
            if (progressDialog.isShowing()) {
                progressDialog.hide();
            }
        }
    }


    @Override
    public synchronized void onResume() {
        super.onResume();
    }


    @Override
    public synchronized void onPause() {
        super.onPause();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_PENDING_REQUEST_ENABLE_BT, pendingRequestEnableBt);
    }

    boolean isAdapterReady() {
        return (btAdapter != null) && (btAdapter.isEnabled());
    }

    void showAlertDialog(String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.app_name));
        alertDialogBuilder.setMessage(message);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
