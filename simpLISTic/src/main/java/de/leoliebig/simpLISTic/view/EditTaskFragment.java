package de.leoliebig.simpLISTic.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.squareup.leakcanary.RefWatcher;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.leoliebig.simpLISTic.Global;
import de.leoliebig.simpLISTic.model.Task;
import de.leoliebig.wundertest.R;


/**
 * Implements the GUI for editing a task.
 *
 * Activities that contain this fragment must implement {@link OnFragmentInteraction}
 * to handle interaction events.
 *
 * @author info@leoliebig.de
 */
public class EditTaskFragment extends Fragment implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener, TimePickerFragment.OnFragmentInteraction, DatePickerFragment.OnFragmentInteraction{

    private static final String TAG = EditTaskFragment.class.getSimpleName();

    private Context context;
    private OnFragmentInteraction listener;
    private boolean isEdit = true;
    private Task task;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat();

    private Button guiBtnDate;
    private Button guiBtnTime;
    private CheckBox guiCheckboxReminder;
    private EditText guiEditTextTitle;
    private EditText guiEditTextNotes;

    private Menu optionsMenu;

    public EditTaskFragment() {
        // Required empty public constructor
    }

    /**
     * Default factory method to create a new instance
     * @param taskToEdit The task object to edit.
     * @return A new instance of fragment EditTaskFragment.
     */
    public static EditTaskFragment newInstance(@NonNull Task taskToEdit) {
        EditTaskFragment fragment = new EditTaskFragment();
        fragment.setTask(taskToEdit);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_edit_task, container, false);
        initGui(rootView);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_task, menu);

        optionsMenu = menu;

        //remove the delete action if a new task is created
        if(!isEdit){
            MenuItem deleteItem = menu.findItem(R.id.action_task_delete);
            deleteItem.setVisible(false);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;

        try {
            this.listener = (OnFragmentInteraction) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException("Hosting activity must implement a the OnFragmentInteraction interface");
        }
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = Global.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch(id){
            case R.id.action_task_save:
                saveTask();
                break;
            case R.id.action_task_discard:
                listener.onTaskUnchanged();
                break;
            case R.id.action_task_delete:
                listener.onTaskDeleted(task);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;

    }

    /**
     * Validates the input and saves the
     */
    private void saveTask() {

        //read the input from the GUI elements and update the task object accordingly
        Task.Detail details = task.getDetails();
        task.setTitle(guiEditTextTitle.getText().toString());
        details.setNotes(guiEditTextNotes.getText().toString());

        //validate
        if(task.getTitle().isEmpty()){
            //show and highlight the hint
            guiEditTextTitle.setHint(getString(R.string.hint_task_title));
            //have to use the deprecated method because of the API min level
            guiEditTextTitle.setHintTextColor(getResources().getColor(R.color.colorAccent));
            Toast.makeText(context, R.string.toast_enter_title, Toast.LENGTH_LONG).show();
        }
        else if(task.getReminder() != null && task.getReminder().before(new Date())){
            Toast.makeText(context, R.string.toast_reminder_date_in_past, Toast.LENGTH_LONG).show();
            guiBtnTime.setTextColor(getResources().getColor(R.color.colorAccent));
        }
        else{
            listener.onTaskChanged(task);
        }
    }

    /**
     * This should be called by the hosting activity if there is
     * no other fragment in the activity (smartphone layout). It displays
     * a dialog to ask of the task should be saved.
     */
    public void onBackPressed() {

        //show save dialog if a title was entered
        if(guiEditTextTitle.getText().length()==0) {
            getActivity().finish();
        }
        else{
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.dialog_save_task))
                    .setNegativeButton(getString(R.string.discard), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            listener.onTaskUnchanged();
                        }
                    })
                    .setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            saveTask();
                        }
                    })
                    .create()
                    .show();
        }
    }

    /**
     * Initializes all views
     * @param rootView the root view of the fragment
     */
    private void initGui(@NonNull final View rootView){

        guiEditTextTitle = (EditText) rootView.findViewById(R.id.edit_text_title);
        guiEditTextNotes = (EditText) rootView.findViewById(R.id.edit_text_notes);
        guiBtnDate = (Button) rootView.findViewById(R.id.btn_task_date);
        guiBtnTime = (Button) rootView.findViewById(R.id.btn_task_time);
        guiCheckboxReminder = (CheckBox) rootView.findViewById(R.id.checkbox_reminder_enabled);

        guiBtnDate.setOnClickListener(this);
        guiBtnTime.setOnClickListener(this);
        guiCheckboxReminder.setOnCheckedChangeListener(this);

        guiEditTextTitle.requestFocus();
    }

    /**
     * Reads the date of the passed {@link Date} and splits it into a string
     * with the date and the time. These are returned as a string array where the first
     * string is the date and the second one is the time.
     * @param reminderDate A {@link Date} to read the date and time from.
     * @return Two strings in an array, the first at index 0 is the date and the second at index 1 is the time.
     */
    private String[] getDateStrings(@NonNull final Date reminderDate){

        dateFormatter.toLocalizedPattern(); //date and time are separated by a space
        return dateFormatter.format(reminderDate).split(" ");
    }

    /**
     * Updates the text of the reminder date and time buttons.
     * @param reminderDate A {@link Date} to read the date and time from.
     */
    private void updateReminderDateButtons(@NonNull final Date reminderDate){
        String[] date = getDateStrings(reminderDate);
        guiBtnDate.setText(date[0]);
        guiBtnTime.setText(date[1]);
    }

    /**
     * Reads the data from the current {@link Task} and updates
     * the views accordingly
     */
    private void updateViews(){
        if(task == null) {
            Log.w(TAG, "Task is null, can not update views");
            return;
        }

        guiEditTextTitle.setText(task.getTitle());
        guiEditTextNotes.setText(task.getDetails().getNotes());

        //bypass the change listener since this is not a manual input
        guiCheckboxReminder.setOnCheckedChangeListener(null);


        if(task.getReminder() != null){
            guiCheckboxReminder.setChecked(true);
            updateReminderDateButtons(task.getReminder());
            guiBtnDate.setEnabled(true);
            guiBtnTime.setEnabled(true);
        }
        else{
            guiCheckboxReminder.setChecked(false);
            updateReminderDateButtons(Global.getDefaultReminderDate());
            guiBtnDate.setEnabled(false);
            guiBtnTime.setEnabled(false);
        }
        guiCheckboxReminder.setOnCheckedChangeListener(this);
    }

    /**
     * Sets the task that should be edited.
     * @param taskToEdit The task to edit.
     */
    public void setTask(@NonNull final Task taskToEdit){
        this.task = taskToEdit;
        if(this.task.getId() == Task.TRANSIENT) isEdit = false;

        //update the options menu if it is already visible
        if(optionsMenu!=null){
            MenuItem deleteItem = optionsMenu.findItem(R.id.action_task_delete);
            if(isEdit) deleteItem.setVisible(true);
            else deleteItem.setVisible(false);
            getActivity().invalidateOptionsMenu();
            Log.d(TAG, "Updated options menu");
        }

        updateViews();
    }

    @Override
    public void onClick(View v) {

        if(v == guiBtnDate){
            Log.d(TAG, "Date");
            new DatePickerFragment().show(getChildFragmentManager(), "datePicker");
        }
        else if(v == guiBtnTime){
            Log.d(TAG, "Time");
            TimePickerFragment fragment = new TimePickerFragment();
            fragment.setTask(task);
            fragment.show(getChildFragmentManager(), "timePicker");
        }
        else{
            if(Global.DEBUG) throw new AssertionError("Click from unknown view" + v);
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        guiBtnDate.setEnabled(isChecked);
        guiBtnTime.setEnabled(isChecked);

        if(isChecked){
            //set the default reminder date to tomorrow
            task.setReminder(Global.getDefaultReminderDate());
            updateReminderDateButtons(task.getReminder());
        }
        else {
            //remove reminder date
            Log.d(TAG, "Set reminder null");
            task.setReminder(null);
        }
    }

    @Override
    public void onTimeSet(Task task) {
        updateReminderDateButtons(task.getReminder());
        guiBtnTime.setTextColor(guiBtnDate.getCurrentTextColor()); //reset the text color
    }

    @Override
    public void onDateSet(Task task) {
        updateReminderDateButtons(task.getReminder());
    }

    @Override
    public Task getCurrentTask() {
        return task;
    }

    /**
     * Allows to interact with the {@link EditTaskFragment}.
     */
    public interface OnFragmentInteraction {

        /**
         * Is called if the task update was edited.
         * @param task The edited {@link Task}.
         */
        void onTaskChanged(Task task);


        /**
         * Is called if the user decided to delete the task.
         * @param task The {@link Task} to delete.
         */
        void onTaskDeleted(Task task);

        /**
         * Is called if no changes were performed on the task object.
         */
        void onTaskUnchanged();
    }

}
