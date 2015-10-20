package de.leoliebig.simpLISTic;

import android.app.Application;
import android.content.Context;

import com.google.gson.Gson;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import java.util.Calendar;
import java.util.Date;

/**
 * Class holding application constants and helper methods
 * @author info@leoliebig.de
 */
public class Global extends Application {

    public static final boolean DEBUG = true;
    public static final String INTENT_EXTRA_TASK_ID = "extraTaskId";
    public static final String INTENT_EXTRA_TASK_LIST_POSITION = "extraTaskListPosition";
    public static final String INTENT_EXTRA_TASK_TITLE = "extraTaskTitle";
    public static final String INTENT_EXTRA_NOTIFICATION = "extraNotification";

    public static final Gson gson = new Gson();

    private RefWatcher refWatcher;

    public static RefWatcher getRefWatcher(Context context) {
        Global application = (Global) context.getApplicationContext();
        return application.refWatcher;
    }



    /**
     * Returns the default reminder date which is tomorrow at 9 am
     */
    public static Date getDefaultReminderDate() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 9);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        return c.getTime();
    }

    @Override public void onCreate() {
        super.onCreate();
        refWatcher = LeakCanary.install(this);
    }

}
