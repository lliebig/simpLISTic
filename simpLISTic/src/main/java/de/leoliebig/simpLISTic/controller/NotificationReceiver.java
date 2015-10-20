package de.leoliebig.simpLISTic.controller;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.leoliebig.simpLISTic.Global;

/**
 * Receives an intent by the {@link android.app.AlarmManager} whenever a task reminder notification
 * should be shown.
 *
 * @author info@leoliebig.de
 */
public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = NotificationReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        //create and show the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = intent.getParcelableExtra(Global.INTENT_EXTRA_NOTIFICATION);
        long id = intent.getLongExtra(Global.INTENT_EXTRA_TASK_ID, -1);
        notificationManager.notify((int) id, notification);

    }
}
