package de.leoliebig.simpLISTic.view;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import com.squareup.leakcanary.RefWatcher;

import java.util.Calendar;
import java.util.Date;

import de.leoliebig.simpLISTic.Global;
import de.leoliebig.simpLISTic.model.Task;

/**
 * Show a dialog with a date picker.
 *
 * @author info@leoliebig.de
 */
public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private OnFragmentInteraction listener;
    private Task task;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        try {
            this.listener = (OnFragmentInteraction) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException("Hosting activity must implement a the OnFragmentInteraction interface");
        }

        final Calendar c = Calendar.getInstance();

        getTargetFragment();

        //init with the current values of the reminder date
        if(task == null) task = listener.getCurrentTask();
        if(task.getReminder() == null) c.setTime(Global.getDefaultReminderDate());
        else c.setTime(task.getReminder());

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, year, month, day);
        dialog.getDatePicker().setMinDate(new Date().getTime());

        return dialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = Global.getRefWatcher(getParentFragment().getActivity());
        refWatcher.watch(this);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {

        Date reminder = task.getReminder();
        Calendar c = Calendar.getInstance();

        if(reminder != null){
            c.setTime(reminder);
        }
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        task.setReminder(c.getTime());
        listener.onDateSet(task);
    }

    /**
     * Sets the task that should be edited.
     * @param taskToEdit The task to edit the time for.
     */
    public void setTask(@NonNull final Task taskToEdit){
        this.task = taskToEdit;
    }

    /**
     * Allows to interact with the {@link DatePickerFragment}.
     */
    public interface OnFragmentInteraction {

        /**
         * Is called if the user has picked a date.
         * @param task The {@link Task} with the updated date.
         */
        void onDateSet(Task task);

        /**
         * Is called after an orientation change in order to get the current {@link Task}.
         * @return The current {@link Task} to edit.
         */
        Task getCurrentTask();

    }
}