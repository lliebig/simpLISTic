package de.leoliebig.simpLISTic;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import de.leoliebig.simpLISTic.controller.NotificationHelper;
import de.leoliebig.simpLISTic.db.DatabaseHelper;
import de.leoliebig.simpLISTic.model.Task;
import de.leoliebig.simpLISTic.view.EditTaskFragment;
import de.leoliebig.wundertest.R;

/**
 * Holds and controls the {@link EditTaskFragment}, only used on smartphones.
 *
 * @author info@leoliebig.de
 */
public class EditTaskActivity extends AppCompatActivity implements EditTaskFragment.OnFragmentInteraction {

    private static final String TAG = EditTaskActivity.class.getSimpleName();
    private EditTaskFragment fragment;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        fragment = (EditTaskFragment) getSupportFragmentManager().findFragmentById(R.id.edit_task_fragment);
        if (fragment == null) throw new AssertionError("ProfileConfigFragment not found in ConfigActivity");

        dbHelper = DatabaseHelper.getInstance(getApplicationContext());
        Task taskToEdit;

        //get the TagProfile
        Intent intent = getIntent();
        long id = intent.getLongExtra(Global.INTENT_EXTRA_TASK_ID, -1);
        ActionBar actionBar = getSupportActionBar();

        if (id != -1) {
            taskToEdit = dbHelper.getTask(id);
            if (taskToEdit == null) throw new AssertionError("No task with id " + id + " found.");
            if(actionBar != null) actionBar.setTitle("Edit task");
        } else {
            int taskPosition = intent.getIntExtra(Global.INTENT_EXTRA_TASK_LIST_POSITION, Integer.MAX_VALUE);
            taskToEdit = new Task("");
            taskToEdit.setListPosition(taskPosition);
            if(actionBar != null) actionBar.setTitle("New task");
        }
        fragment.setTask(taskToEdit);

    }

    @Override
    public void onBackPressed() {
        fragment.onBackPressed();
    }

    @Override
    public void onTaskChanged(@NonNull final Task task) {

        long id = dbHelper.save(task);
        NotificationHelper.updateNotification(task, id, this);
        if (Global.DEBUG) Log.d(TAG, "Saved / updated task: " + task.getTitle());
        finish();
    }

    @Override
    public void onTaskDeleted(@NonNull final Task task) {
        int result = dbHelper.deleteTask(task.getId());

        task.setReminder(null); //notifications for tasks without reminder dates will be deleted in the next line
        NotificationHelper.updateNotification(task, task.getId(), this);

        if (result != 1) Log.e(TAG, "Could not delete task: " + task.getTitle());
        finish();

    }

    @Override
    public void onTaskUnchanged() {
        if(Global.DEBUG) Log.d(TAG, "No changes on task");
        finish();
    }
}
