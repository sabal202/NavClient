package sabal.navclient.persistance;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by sabal202 sabal2000@mail.ru on 19.09.2017.
 */

public class PreferenceManager {
    private static final String LAST_UPDATE_DATE = "LAST_UPDATE_DATE";
    private static final String LAST_DEVICE = "LAST_DEVICE";
    private static SharedPreferences preferences;

    public static void with(Context context) {
        preferences = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void saveLastUpdate(String date) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LAST_UPDATE_DATE, date);
        editor.apply();
    }

    public static void saveLastDevice(String device) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LAST_DEVICE, device);
        editor.apply();
    }

    public static String getLastDevice() {
        return preferences.getString(LAST_DEVICE, "none");
    }
    public static String getLastUpdateDate() {
        return preferences.getString(LAST_UPDATE_DATE, "none");
    }
}
