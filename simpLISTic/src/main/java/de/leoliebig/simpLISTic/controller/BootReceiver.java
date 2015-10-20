package de.leoliebig.simpLISTic.controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

import de.leoliebig.simpLISTic.db.DatabaseHelper;
import de.leoliebig.simpLISTic.model.Task;

/**
 * Receives an intent after the device booted and reschedules all task reminders.
 *
 * @author info@leoliebig.de
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = BroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        //get all tasks and reschedule the notification alarms
        List<Task> tasks = DatabaseHelper.getInstance(context).getAll();
        for(Task task: tasks){
            NotificationHelper.updateNotification(task, task.getId(), context);
        }

    }
}
