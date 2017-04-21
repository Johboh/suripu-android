package is.hello.sense.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class InternalPrefManager {
    private static final String EMPTY = "";

    public static SharedPreferences getInternalPrefs(@NonNull final Context context) {
        return context.getSharedPreferences(Constants.INTERNAL_PREFS, Context.MODE_PRIVATE);
    }

    public static void clearPrefs(@NonNull final Context context) {
        getInternalPrefs(context)
                .edit()
                .clear()
                .apply();
    }

    public static void setAccountId(@NonNull final Context context, @Nullable final String id) {
        getInternalPrefs(context)
                .edit()
                .putString(Constants.INTERNAL_PREF_ACCOUNT_ID, id == null ? EMPTY : id)
                .apply();
    }

    public static String getAccountId(@NonNull final Context context) {
        return getInternalPrefs(context)
                .getString(Constants.INTERNAL_PREF_ACCOUNT_ID, EMPTY);
    }


}
