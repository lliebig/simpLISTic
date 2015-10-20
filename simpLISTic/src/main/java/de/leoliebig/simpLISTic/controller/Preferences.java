package de.leoliebig.simpLISTic.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

/**
 * Static helper class for persisting application preferences on the device.
 *
 * @author info@leoliebig.de
 */
public class Preferences {

    private static final String KEY_HIDE_DONE_TASKS="hideDoneTasks";

    /**
     * Returns the default {@link SharedPreferences} of the application
     * @param applicationContext The application context.
     * @return A SharedPreferences instance that can be used to retrieve and listen to values of the preferences.
     */
    public static SharedPreferences getPreferences(@NonNull final Context applicationContext){
        if(applicationContext == null) throw new IllegalArgumentException("Parameters must not be null");
        return PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    /**
     * Sets whether done tasks should be hidden or not.
     * @param applicationContext The application context.
     * @param hideDoneTasks <code>true</code> if done tasks should be hidden, otherwise <code>false</code>.
     */
    public static void setDoneTasksHidden(@NonNull final Context applicationContext, final boolean hideDoneTasks){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_HIDE_DONE_TASKS, hideDoneTasks);
        editor.commit();
    }

    /**
     * Returns whether done tasks should be hidden or not.
     * @param applicationContext The application context.
     * @return <code>true</code> if done tasks should be hidden, otherwise <code>false</code>.
     */
    public static boolean getDoneTasksHidden(@NonNull final Context applicationContext){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        return prefs.getBoolean(KEY_HIDE_DONE_TASKS, false);
    }

}
