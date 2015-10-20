package de.leoliebig.simpLISTic.controller;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.Date;

import de.leoliebig.simpLISTic.Global;
import de.leoliebig.simpLISTic.MainActivity;
import de.leoliebig.simpLISTic.model.Task;
import de.leoliebig.wundertest.R;

/**
 * Provides convenience methods for creating and managing notifications.
 *
 * @author info@leoliebig.de
 */
public class NotificationHelper {

    private static final String TAG = NotificationHelper.class.getSimpleName();

    /**
     * Builds and returns a {@link Notification} for the passed {@link Task}.
     * @param task The {@link Task} to build a notification for.
     * @param context The application context.
     * @return A {@link Notification}  object for the task with default sound, light and vibration.
     */
    private static Notification buildNotification(@NonNull final Task task, @NonNull final Context context) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.menu_okay);
        builder.setContentTitle(task.getTitle());
        builder.setContentText(context.getString(R.string.notification_reminder));
        builder.setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_LIGHTS|Notification.DEFAULT_VIBRATE);
        builder.setAutoCancel(true);
        Intent resultIntent = new Intent(context, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(resultPendingIntent);

        return builder.build();
    }

    /**
     * Updates existing notifications for the passed {@link Task}. If the task has no scheduled notification
     * a new one will be created. If the task's reminder date has changed, the scheduled notification will be
     * deleted and a new one is created. If the task's reminder date was set to null, the scheduled notification
     * for this task will be deleted. Tasks with reminder dates for the past will be ignored.
     *
     * @param task The {@link Task} to schedule or update the notification for.
     * @param taskId The id of the task as used in the database. This is an additional argument to allow
     * @param context The application context.
     */
    public static void updateNotification(@NonNull final Task task, long taskId, @NonNull final Context context){

        Context appContext = context.getApplicationContext();

        Intent notificationIntent = new Intent(appContext, NotificationReceiver.class);
        notificationIntent.putExtra(Global.INTENT_EXTRA_TASK_TITLE, task.getTitle());
        notificationIntent.putExtra(Global.INTENT_EXTRA_NOTIFICATION, buildNotification(task, appContext));
        notificationIntent.putExtra(Global.INTENT_EXTRA_TASK_ID, task.getId());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                appContext,
                (int) taskId, //TODO: this is a problem if there are more tasks than the range of int provides --> hash
                notificationIntent,
                PendingIntent.FLAG_ONE_SHOT
        );

        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        Date now = new Date();
        Date reminderDate = task.getReminder();

        //reschedule if the task is not done, the reminder date is set and not in the past
        if(reminderDate != null && reminderDate.after(now) && !task.isDone()){
            if(Global.DEBUG) Log.d(TAG, "Updated notification for " + task.getTitle());
            alarmManager.set(AlarmManager.RTC_WAKEUP, task.getReminder().getTime(), pendingIntent);
        }
        else{
            if(Global.DEBUG) Log.d(TAG, "Cancelled notification for " + task.getTitle());
        }
    }

}
