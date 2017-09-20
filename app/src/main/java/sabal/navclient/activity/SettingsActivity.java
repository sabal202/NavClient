package sabal.navclient.activity;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sabal.navclient.MyApplication;
import sabal.navclient.R;
import sabal.navclient.Utils;
import sabal.navclient.server_api.models.CityBeacon;
import sabal.navclient.server_api.models.LastUpdateModel;

import static sabal.navclient.activity.DeviceControlActivity.CITY_ID;
import static sabal.navclient.persistance.PreferenceManager.getLastUpdateDate;


@SuppressWarnings("deprecation")
public final class SettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
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
    private Realm realm;
    private Preference lastUpdate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_activity);

        realm = Realm.getDefaultInstance();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        setPrefenceTitle(getString(R.string.pref_commands_mode));
        setPrefenceTitle(getString(R.string.pref_commands_ending));

        lastUpdate = findPreference("edit_text_preference_2");
        setLustUpdateText();
        lastUpdate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                update();
                return true;
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Toolbar bar;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
            bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar, root, false);
            root.addView(bar, 0); // insert at top
        } else {
            ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
            ListView content = (ListView) root.getChildAt(0);

            root.removeAllViews();

            bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar, root, false);


            int height;
            TypedValue tv = new TypedValue();
            if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            } else {
                height = bar.getHeight();
            }

            content.setPadding(0, height, 0, 0);

            root.addView(content);
            root.addView(bar);
        }

        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String value) {
        setPrefenceTitle(value);
    }

    private void setPrefenceTitle(String TAG) {
        final Preference preference = findPreference(TAG);
        if (preference == null) return;
        if (preference instanceof ListPreference) {
            if (((ListPreference) preference).getEntry() == null) return;
            final String title = ((ListPreference) preference).getEntry().toString();
            preference.setTitle(title);
        }
    }

    private void update() {
        showProgress(R.string.loading);
        MyApplication.getApi().getLastUpdateDate(CITY_ID).enqueue(new Callback<LastUpdateModel>() {
            @Override
            public void onResponse(Call<LastUpdateModel> call, Response<LastUpdateModel> response) {
                hideProgress();
                if (response.body() != null) {
                    String date = response.body().getLastUpdate();
                    if (!getLastUpdateDate().equals(date)) {
                        sabal.navclient.persistance.PreferenceManager.saveLastUpdate(date);
                        setLustUpdateText();
                        getBeacons();
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
    }

    private void setLustUpdateText() {
        String date = sabal.navclient.persistance.PreferenceManager.getLastUpdateDate();
        lastUpdate.setSummary(Utils.getAbsoluteDate(date));
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
                Toast.makeText(SettingsActivity.this, "Проблемы с сетью", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveBeacons(final List<CityBeacon> body) {
        showProgress(R.string.loading);
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.delete(CityBeacon.class);
                realm.copyToRealm(body);
            }
        }, successCallback, errorCallback);
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
}
