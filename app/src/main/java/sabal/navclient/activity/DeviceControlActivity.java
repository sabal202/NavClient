package sabal.navclient.activity;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sabal.navclient.DeviceData;
import sabal.navclient.MyApplication;
import sabal.navclient.R;
import sabal.navclient.Utils;
import sabal.navclient.bluetooth.DeviceConnector;
import sabal.navclient.bluetooth.DeviceListActivity;
import sabal.navclient.server_api.models.CityBeacon;
import sabal.navclient.server_api.models.LastUpdateModel;

import static sabal.navclient.persistance.PreferenceManager.getLastUpdateDate;


public final class DeviceControlActivity extends BaseActivity implements TextToSpeech.OnInitListener {
    public static final int CITY_ID = 2;
    private static final String DEVICE_NAME = "DEVICE_NAME";
    private static final String LOG = "LOG";
    private static final SimpleDateFormat timeformat = new SimpleDateFormat("HH:mm:ss.SSS");
    private static String MSG_NOT_CONNECTED;
    private static String MSG_CONNECTING;
    private static String MSG_CONNECTED;
    private static DeviceConnector connector;
    private static BluetoothResponseHandler mHandler;
    DialogInterface.OnClickListener myClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    dialog.cancel();
                    break;
            }
        }

    };
    ArrayList<String> log = new ArrayList<>();
    List<CityBeacon> beaconList;
    private Realm realm;
    private TextToSpeech mTTS;
    private TextView logTextView;
    private boolean hexMode, needClean;
    private boolean show_timings = true, show_direction = false;
    private String deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
        PreferenceManager.setDefaultValues(this, R.xml.settings_activity, false);
        if (mHandler == null) mHandler = new BluetoothResponseHandler(this);
        else mHandler.setTarget(this);
        MSG_NOT_CONNECTED = getString(R.string.msg_not_connected);
        MSG_CONNECTING = getString(R.string.msg_connecting);
        MSG_CONNECTED = getString(R.string.msg_connected);
        mTTS = new TextToSpeech(this, this);
        setContentView(R.layout.activity_terminal);
        if (isConnected() && (savedInstanceState != null)) {
            setDeviceName(savedInstanceState.getString(DEVICE_NAME));
        } else getSupportActionBar().setSubtitle(MSG_NOT_CONNECTED);
        showProgress(R.string.loading);
        MyApplication.getApi().getLastUpdateDate(CITY_ID).enqueue(new Callback<LastUpdateModel>() {
            @Override
            public void onResponse(Call<LastUpdateModel> call, Response<LastUpdateModel> response) {
                hideProgress();
                if (response.body() != null) {
                    String date = response.body().getLastUpdate();
                    if (!getLastUpdateDate().equals(date)) {
                        sabal.navclient.persistance.PreferenceManager.saveLastUpdate(date);
                        getBeacons();
                    } else {
                        getSavedBeacons();
                    }
                    //beaconList.addAll(response.body());
                }
            }

            @Override
            public void onFailure(Call<LastUpdateModel> call, Throwable t) {
                //Toast.makeText(, "Network problems, try later", Toast.LENGTH_SHORT);
                hideProgress();
            }
        });
        this.logTextView = (TextView) findViewById(R.id.log_textview);
        this.logTextView.setMovementMethod(new ScrollingMovementMethod());
        if (savedInstanceState != null)
            logTextView.setText(savedInstanceState.getString(LOG));

    }

    private void getSavedBeacons() {
        showProgress(R.string.loading);
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                List<CityBeacon> result = realm.copyFromRealm(realm.where(CityBeacon.class).findAll());

                beaconList = result;
            }
        }, successCallback, errorCallback);
    }

    private void getBeacons() {
        showProgress(R.string.loading);
        MyApplication.getApi().getAllBeaconsFromCity(CITY_ID).enqueue(new Callback<List<CityBeacon>>() {
            @Override
            public void onResponse(Call<List<CityBeacon>> call, Response<List<CityBeacon>> response) {
                if (response.body() != null) {
                    saveBeacons(response.body());
                }
                hideProgress();
            }

            @Override
            public void onFailure(Call<List<CityBeacon>> call, Throwable t) {
                hideProgress();
                Toast.makeText(DeviceControlActivity.this, "hgbjfhgnjbnvotj", Toast.LENGTH_SHORT);
            }
        });
    }

    private void saveBeacons(final List<CityBeacon> body) {
        showProgress(R.string.loading);
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.delete(CityBeacon.class);
                beaconList = realm.copyToRealm(body);
            }
        }, successCallback, errorCallback);
    }

    @Override
    public void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }
        realm.close();
        hideProgress();
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            Locale locale = new Locale("ru");

            //int result = mTTS.setLanguage(locale);
            int result = mTTS.setLanguage(Locale.getDefault());

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Извините, этот язык не поддерживается");
            } else {
                // mButton.setEnabled(true);
            }

        } else {
            Log.e("TTS", "Ошибка!");
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DEVICE_NAME, deviceName);
        if (logTextView != null) {
            final String log = logTextView.getText().toString();
            outState.putString(LOG, log);
        }
    }

    private boolean isConnected() {
        return (connector != null) && (connector.getState() == DeviceConnector.STATE_CONNECTED);
    }

    private void stopConnection() {
        if (connector != null) {
            connector.stop();
            connector = null;
            deviceName = null;
        }
    }

    private void startDeviceListActivity() {
        stopConnection();
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    @Override
    public boolean onSearchRequested() {
        if (super.isAdapterReady()) startDeviceListActivity();
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_control_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_search:
                if (super.isAdapterReady()) {
                    if (isConnected()) stopConnection();
                    else startDeviceListActivity();
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

                }
                return true;

            case R.id.menu_clear:
                if (logTextView != null) logTextView.setText("");
                return true;

            case R.id.menu_send:
                if (logTextView != null) {
                    final String msg = logTextView.getText().toString();
                    final Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, msg);
                    startActivity(Intent.createChooser(intent, getString(R.string.menu_send)));
                }
                return true;

            case R.id.menu_settings:
                final Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.show_timings = Utils.getBooleanPrefence(this, getString(R.string.pref_log_timing));
        this.show_direction = Utils.getBooleanPrefence(this, getString(R.string.pref_log_direction));
        this.needClean = Utils.getBooleanPrefence(this, getString(R.string.pref_need_clean));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = btAdapter.getRemoteDevice(address);
                    if (super.isAdapterReady() && (connector == null)) setupConnector(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                super.pendingRequestEnableBt = false;
                if (resultCode != Activity.RESULT_OK) {
                    Utils.log("BT not enabled");
                }
                break;

            default:
                break;
        }
    }

    private void setupConnector(BluetoothDevice connectedDevice) {
        stopConnection();
        try {
            String emptyName = getString(R.string.empty_device_name);
            DeviceData data = new DeviceData(connectedDevice, emptyName);
            connector = new DeviceConnector(data, mHandler);
            connector.connect();
        } catch (IllegalArgumentException e) {
            Utils.log("setupConnector failed: " + e.getMessage());
        }
    }

    void appendLog(String message, boolean hexMode, boolean outgoing, boolean clean) {
        if (log.size() == 0) {
            log.add("0");
        }
        StringBuilder msg = new StringBuilder();



        //if (outgoing) msg.append('\n');
        //String[] numbers = msg.toString().split("\n");
        String gettedID = hexMode ? Utils.printHex(message) : message;
        int beaconID = Integer.parseInt(gettedID.split("\r\n")[0]);
        if (show_timings) msg.append('[').append(timeformat.format(new Date())).append(']');
        if (show_direction) {
            final String arrow = (outgoing ? " << " : " >> ");
            msg.append(arrow);
        } else msg.append(' ');
        msg.append(hexMode ? Utils.printHex(message) : message);
        logTextView.append("\n" + String.valueOf(msg));
        if (realm.isClosed()) realm = Realm.getDefaultInstance();
        for (CityBeacon item : beaconList) {
            if (item.getId() == beaconID) {
                mTTS.speak(item.getDescription(), TextToSpeech.QUEUE_FLUSH, null);
            }
        }

        /*if (msg.toString().equals(log.get(log.size() - 1))) {

        } else {
            log.add(msg.toString());
            logTextView.setText(log.get(log.size() - 1));
            int IDbe = Integer.parseInt(numbers[0]);
            /*switch (IDbe) {
                case 1235:

                    break;
                case 1236:
                    mTTS.speak(getString(R.string.beacon2), TextToSpeech.QUEUE_FLUSH, null);
                    break;
                case 1234:
                    mTTS.speak(getString(R.string.beacon3), TextToSpeech.QUEUE_FLUSH, null);
                    break;
                case 1237:

                    mTTS.speak(getString(R.string.beacon4), TextToSpeech.QUEUE_FLUSH, null);
                    break;
                default:
                    Toast.makeText(this, "null" + numbers[0].charAt(0) + "  " + numbers[0].charAt(1) + "  " + numbers[0].charAt(2) + "  " + numbers[0].charAt(3) + IDbe + "  " + msg.toString() + "  " + numbers[0].length() + "  " + numbers[0], Toast.LENGTH_LONG).show();
                    break;
            }
        }*/


        final int scrollAmount = logTextView.getLayout().getLineTop(logTextView.getLineCount()) - logTextView.getHeight();
        if (scrollAmount > 0)
            logTextView.scrollTo(0, scrollAmount);
        else logTextView.scrollTo(0, 0);
    }


    void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        getSupportActionBar().setSubtitle(deviceName);
    }

    private static class BluetoothResponseHandler extends Handler {
        private WeakReference<DeviceControlActivity> mActivity;

        public BluetoothResponseHandler(DeviceControlActivity activity) {
            mActivity = new WeakReference<DeviceControlActivity>(activity);
        }

        public void setTarget(DeviceControlActivity target) {
            mActivity.clear();
            mActivity = new WeakReference<DeviceControlActivity>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            DeviceControlActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:

                        Utils.log("MESSAGE_STATE_CHANGE: " + msg.arg1);
                        final ActionBar bar = activity.getSupportActionBar();
                        switch (msg.arg1) {
                            case DeviceConnector.STATE_CONNECTED:
                                bar.setSubtitle(MSG_CONNECTED);
                                break;
                            case DeviceConnector.STATE_CONNECTING:
                                bar.setSubtitle(MSG_CONNECTING);
                                break;
                            case DeviceConnector.STATE_NONE:
                                bar.setSubtitle(MSG_NOT_CONNECTED);
                                break;
                            default:
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        final String readMessage = (String) msg.obj;
                        if (readMessage != null) {
                            activity.appendLog(readMessage, false, false, activity.needClean);
                        }
                        break;

                    case MESSAGE_DEVICE_NAME:
                        activity.setDeviceName((String) msg.obj);
                        break;

                    case MESSAGE_WRITE:
                        // stub
                        break;

                    case MESSAGE_TOAST:
                        // stub
                        break;

                    default:
                        break;
                }
            }
        }
    }
}