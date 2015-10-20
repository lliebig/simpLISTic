package de.leoliebig.simpLISTic.model;

import android.util.Log;

import java.util.Date;

import de.leoliebig.simpLISTic.Global;

/**
 * Data object representing a single task.
 *
 * @author info@leoliebig.de
 */
public class Task {

    private static final String TAG = "Task";

    public static final int TRANSIENT = -1;

    private long id = TRANSIENT;
    private int listPosition = 0;
    private String title;
    private Date reminder = null;
    private boolean done = false;
    private Detail details = new Detail();

    /**
     * Creates a new instance with the passed title.
     * @param title The title of the task.
     */
    public Task(String title) {
        this.title = title;
    }

    /**
     * Creates a new instance initialized with the passed arguments.
     * @param id The id of the task as used as primary key in the database.
     * @param title The title of the task.
     * @param listPosition The position of the task in the list GUI component.
     * @param dateInMillis The due date in milliseconds.The value is the number of milliseconds since Jan. 1, 1970, midnight GMT.
     * @param doneInt 1 if the task is done, otherwise 0.
     * @param detailsJson A string with a JSON representation of the {@link Task.Detail} object for this task.
     */
    public Task(long id, String title, int listPosition, long dateInMillis, int doneInt, final String detailsJson) {
        this.listPosition = listPosition;
        this.title = title;
        this.id = id;
        this.reminder = null;
        if(dateInMillis > 0) reminder = new Date(dateInMillis);
        this.done = false;
        if(doneInt == 1) this.done = true;

        if(detailsJson != null && !detailsJson.isEmpty()){
            this.details = Global.gson.fromJson(detailsJson, Detail.class);
        }
        else{
            Log.e(TAG, "Details json string is empty or null: " + detailsJson);
        }

    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getReminder() {
        return reminder;
    }

    public void setReminder(Date reminder) {
        this.reminder = reminder;
    }

    public int getListPosition() {
        return listPosition;
    }

    public void setListPosition(int listPosition) {
        this.listPosition = listPosition;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public Detail getDetails() {
        return details;
    }

    public void setDetails(Detail details) {
        this.details = details;
    }

    /**
     * Returns the id of the task. If the id is {@link Task#TRANSIENT} the task
     * was not persisted yet.
     * @return An integer > 0 or {@link Task#TRANSIENT}.
     */
    public long getId() {
        return id;
    }

    /**
     * Data object representing the details of a task. The data of this object is store
     * as a schema-less JSON representation in the database. Therefore it should only contain
     * data that does not need to be indexed or sorted via the DBMS.
     */
    public class Detail{

        private String notes;
        //int revision = 0;
        //private String imageUri;
        //private List<String> webUris;

        public Detail() {
            this.notes = "";
        }

        public Detail(String notes) {
            this.notes = notes;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

}
