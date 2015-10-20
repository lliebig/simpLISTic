package de.leoliebig.simpLISTic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.List;

import de.leoliebig.simpLISTic.view.TasksFragment;
import de.leoliebig.wundertest.R;
import de.leoliebig.simpLISTic.controller.NotificationHelper;
import de.leoliebig.simpLISTic.controller.Preferences;
import de.leoliebig.simpLISTic.db.DatabaseHelper;
import de.leoliebig.simpLISTic.model.Task;
import de.leoliebig.simpLISTic.view.EditTaskFragment;
import rx.Observer;
import rx.Subscription;

/**
 * Holds and controls the {@link TasksFragment} on smartphones. On tablets
 * it also displays and manages the {@link EditTaskFragment}.
 *
 * @author info@leoliebig.de
 */
public class MainActivity extends AppCompatActivity implements TasksFragment.OnFragmentInteraction, EditTaskFragment.OnFragmentInteraction{

    private static final String TAG = MainActivity.class.getSimpleName();

    //gui
    private TasksFragment tasksFragment;
    private EditTaskFragment editFragment;

    //misc
    private DatabaseHelper dbHelper;
    private List<Task> tasks;
    private boolean hideDoneTasks = false;
    //indicates whether the tasks were reordered and need to be updated
    private boolean updateTasks = false;

    //concurrency
    private Subscription subAllTasks;
    private Subscription subUpdateTasks;
    private FetchTasksObserver fetchTasksObserver;
    private UpdateTasksObserver updateTasksObserver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tasksFragment = (TasksFragment) getSupportFragmentManager().findFragmentById(R.id.tasks_fragment);
        editFragment = (EditTaskFragment) getSupportFragmentManager().findFragmentById(R.id.edit_task_fragment);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createTask();
            }
        });

        dbHelper = DatabaseHelper.getInstance(getApplicationContext());
        fetchTasksObserver = new FetchTasksObserver();
        updateTasksObserver = new UpdateTasksObserver();

        hideDoneTasks = Preferences.getDoneTasksHidden(getApplicationContext());
        tasksFragment.setDoneTasksHidden(hideDoneTasks);
    }


    @Override
    protected void onResume() {
        super.onResume();
        subAllTasks = dbHelper.getAllAsync(fetchTasksObserver);
    }

    @Override
    protected void onPause() {
        if(updateTasks) {
            if(Global.DEBUG) Log.d(TAG, "Updating all tasks");
            subUpdateTasks = dbHelper.saveAllAsync(updateTasksObserver, tasks);
        }
        if(subAllTasks != null) subAllTasks.unsubscribe();
        super.onPause();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_hide_done_tasks);
        if(item!=null) item.setChecked(hideDoneTasks);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_delete_all) {

            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_delete_all_tasks)
                    .setNegativeButton(getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //do nothing
                        }
                    })
                    .setPositiveButton(getString(R.string.btn_okay), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            dbHelper.deleteAll();
                            tasks.clear();
                            tasksFragment.updateContent(tasks);
                        }
                    })
                    .create()
                    .show();

            return true;
        }
        else if(id == R.id.action_hide_done_tasks){
            item.setChecked(!item.isChecked());
            hideDoneTasks = item.isChecked();
            tasksFragment.setDoneTasksHidden(hideDoneTasks);
            Preferences.setDoneTasksHidden(getApplicationContext(), hideDoneTasks);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Lets the user create a new task and updates the GUI
     */
    private void createTask() {

        if (editFragment == null) {
            //smartphone layout, start a new activity for editing a task
            Intent intent = new Intent(MainActivity.this, EditTaskActivity.class);
            intent.putExtra(Global.INTENT_EXTRA_TASK_LIST_POSITION, tasks.size());
            startActivity(intent);
        }else{
            //tablet layout, update the editFragment
            Task newTask = new Task("");
            newTask.setListPosition(tasks.size());
            editFragment.setTask(newTask);
            ActionBar actionBar = getSupportActionBar();
            if(actionBar != null) actionBar.setTitle("New task");
        }
    }

    /**
     * Updates the the content of the editFragment if it is available
     * in the current layout (tablets only) by creating a new task.
     */
    private void updateEditFragmentContent() {
        //update the editFragment if it is visible
        if(editFragment != null){
            createTask();
        }
    }

    //TaskFragment callbacks

    @Override
    public void onTaskEdit(Task task) {

        if (editFragment == null) {
            //smartphone layout, start a new activity for editing a task
            Intent intent = new Intent(MainActivity.this, EditTaskActivity.class);
            intent.putExtra(Global.INTENT_EXTRA_TASK_ID, task.getId());
            startActivity(intent);
        }else{
            //tablet layout, update the editFragment
            ActionBar actionBar = getSupportActionBar();
            if(actionBar != null) actionBar.setTitle("Edit task");
            editFragment.setTask(task);
        }

    }

    @Override
    public void onTaskDone(Task task) {
        dbHelper.save(task);
        NotificationHelper.updateNotification(task, task.getId(), this);
    }

    @Override
    public void onTaskMoved(List<Task> tasksWithNewPositions) {
        tasks = tasksWithNewPositions;
        updateTasks = true;
    }

    //EditTaskFragment callbacks

    @Override
    public void onTaskChanged(@NonNull final Task task) {

        if(task.getId() == Task.TRANSIENT){
            Toast.makeText(this, getString(R.string.toast_created_task) + task.getTitle(), Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this, getString(R.string.toast_updated_task) + task.getTitle(), Toast.LENGTH_LONG).show();
        }
        long id = dbHelper.save(task);
        NotificationHelper.updateNotification(task, id, this);

        subAllTasks = dbHelper.getAllAsync(fetchTasksObserver);
    }

    @Override
    public void onTaskDeleted(@NonNull final Task task) {
        int result = dbHelper.deleteTask(task.getId());

        task.setReminder(null); //notifications for tasks without reminder dates will be deleted in the next line
        NotificationHelper.updateNotification(task, task.getId(), this);

        if (result != 1) Log.e(TAG, "Could not delete task: " + task.getTitle());

        subAllTasks = dbHelper.getAllAsync(fetchTasksObserver);
    }

    @Override
    public void onTaskUnchanged() {
        if(Global.DEBUG) Log.d(TAG, "No changes on task");
    }

    /**
     * Observer for handling the result of an async database query for all
     * tasks performed via RxAndroid.
     */
    class FetchTasksObserver implements Observer<List<Task>>{

        @Override
        public void onCompleted() {
            tasksFragment.updateContent(tasks);
            if(Global.DEBUG) Log.d(TAG, "Loaded " + tasks.size() + " task(s) from db");
            updateEditFragmentContent();
        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
            if(Global.DEBUG) Log.e(TAG, "Error fetching all tasks from db: " + e.getLocalizedMessage());
        }

        @Override
        public void onNext(List<Task> tasks) {
            MainActivity.this.tasks = tasks;
        }
    }

    /**
     * Observer for handling the result of an async database update query for all
     * tasks performed via RxAndroid.
     */
    class UpdateTasksObserver implements Observer<Boolean>{

        @Override
        public void onCompleted() {
            subUpdateTasks.unsubscribe();
            //reset the update flag
            updateTasks = false;
        }

        @Override
        public void onError(Throwable e) {
            if(Global.DEBUG) Log.e(TAG, "Error updating all tasks from db: " + e.getLocalizedMessage());
        }

        @Override
        public void onNext(Boolean successful) {
            if(Global.DEBUG){
                if(successful) Log.d(TAG, "Updated all tasks successfully");
                else Log.e(TAG, "Could not update tasks");
            }
        }
    }

}
