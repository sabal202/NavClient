package sabal.navclient.persistance;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by sabal202 sabal2000@mail.ru on 19.09.2017.
 */

public class PreferenceManager {
    private static final String LAST_UPDATE_DATE = "LAST_UPDATE_DATE";
    private static SharedPreferences preferences;

    public static void with(Context context) {
        preferences = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void saveLastUpdate(String date) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LAST_UPDATE_DATE, date);
        editor.apply();
    }

    public static String getLastUpdateDate() {
        return preferences.getString(LAST_UPDATE_DATE, "none");
    }
}
