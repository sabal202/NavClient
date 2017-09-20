package sabal.navclient;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static android.text.format.DateUtils.getRelativeTimeSpanString;

public class Utils {
    public static final String DATE_FORMAT = "%02d.%02d.%d %02d:%02d";
    private static final String INPUT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static void log(String message) {
        if (BuildConfig.DEBUG) {
            if (message != null) Log.i(Const.TAG, message);
        }
    }

    public static String getReadableDate(String input) {
        try {
            Date date = StringToDate(input);
            return getRelativeTimeSpanString(MyApplication.getContext(), date.getTime(), true)
                    .toString();
        } catch (Exception e) {
            e.printStackTrace();
            return input;
        }
    }

    public static String getAbsoluteDate(String input) {
        try {
            Date date = StringToDate(input);
            Calendar calendar = Calendar.getInstance();
            TimeZone tz = TimeZone.getDefault();
            calendar.setTimeZone(tz);
            calendar.setTime(date);

            return getAbsoluteDate(calendar);
        } catch (Exception e) {
            e.printStackTrace();
            return input;
        }
    }

    private static String getAbsoluteDate(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        int offset = calendar.get(Calendar.ZONE_OFFSET) / 60 / 1000; //in minutes
        int hours = (calendar.get(Calendar.HOUR_OF_DAY) + offset / 60) % 24;
        int minutes = calendar.get(Calendar.MINUTE) + offset % 60;
        if (minutes >= 60)
            hours++;
        minutes %= 60;
        return String.format(Locale.getDefault(), DATE_FORMAT,
                day, month, year, hours, minutes);
    }

    private static Date StringToDate(String time) throws ParseException {
        SimpleDateFormat inputFormat = new SimpleDateFormat(INPUT_PATTERN, Locale.getDefault());

        return inputFormat.parse(time);
    }
    public static String printHex(String hex) {
        StringBuilder sb = new StringBuilder();
        int len = hex.length();
        try {
            for (int i = 0; i < len; i += 2) {
                sb.append("0x").append(hex.substring(i, i + 2)).append(" ");
            }
        } catch (NumberFormatException e) {
            log("printHex NumberFormatException: " + e.getMessage());

        } catch (StringIndexOutOfBoundsException e) {
            log("printHex StringIndexOutOfBoundsException: " + e.getMessage());
        }
        return sb.toString();
    }

    public static byte[] toHex(String hex) {
        int len = hex.length();
        byte[] result = new byte[len];
        try {
            int index = 0;
            for (int i = 0; i < len; i += 2) {
                result[index] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
                index++;
            }
        } catch (NumberFormatException e) {
            log("toHex NumberFormatException: " + e.getMessage());

        } catch (StringIndexOutOfBoundsException e) {
            log("toHex StringIndexOutOfBoundsException: " + e.getMessage());
        }
        return result;
    }

    public static byte[] concat(byte[] A, byte[] B) {
        byte[] C = new byte[A.length + B.length];
        System.arraycopy(A, 0, C, 0, A.length);
        System.arraycopy(B, 0, C, A.length, B.length);
        return C;
    }

    public static String getPrefence(Context context, String item) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(item, Const.TAG);
    }

    public static boolean getBooleanPrefence(Context context, String tag) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(tag, true);
    }

    public static class InputFilterHex implements InputFilter {

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; i++) {
                if (!Character.isDigit(source.charAt(i))
                        && source.charAt(i) != 'A' && source.charAt(i) != 'D'
                        && source.charAt(i) != 'B' && source.charAt(i) != 'E'
                        && source.charAt(i) != 'C' && source.charAt(i) != 'F'
                        ) {
                    return "";
                }
            }
            return null;
        }
    }
}
