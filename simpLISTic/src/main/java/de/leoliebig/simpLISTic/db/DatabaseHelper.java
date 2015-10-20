package de.leoliebig.simpLISTic.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import de.leoliebig.simpLISTic.Global;
import de.leoliebig.simpLISTic.model.Task;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Implements the data access object for CRUD operations on the database and the SQLiteOpenHelper
 * for database maintenance.
 *
 * @author info@leoliebig.de
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    private static DatabaseHelper instance = null;

    //specifies which columns will be read from the database
    private static final String[] PROJECTION = {
            Schema.TaskEntry._ID,
            Schema.TaskEntry.COLUMN_TITLE,
            Schema.TaskEntry.COLUMN_LIST_POS,
            Schema.TaskEntry.COLUMN_DUE_DATE,
            Schema.TaskEntry.COLUMN_DONE,
            Schema.TaskEntry.COLUMN_JSON_DETAIL,
    };

    /**
     * Returns the {@link DatabaseHelper} instance.
     * @param context The application context
     */
    public static synchronized DatabaseHelper getInstance(@NonNull Context context){

        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;

    }

    /**
     * Private constructor for creating a new instance.
     * @param context The application context.
     */
    private DatabaseHelper(final Context context){
        super(context, Schema.DATABASE_NAME, null, Schema.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Schema.TagTable.SQL_CREATE_ENTRIES);
        Log.d(TAG, "Database created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //not implemented because changing parts of the data should be stored into the JSON
        //data field
    }

    /**
     * Saves or updates the passed {@link Task} and returns the row id of the entry or -1 in case
     * of errors. This operation is performed in an synchronous manner.
     * @param task The {@link Task} to store.
     * @return The new row id of the entry or -1 if the insert or update failed.
     * @throws java.lang.IllegalArgumentException In case the passed argument is null.
     */
    public synchronized long save(@NonNull final Task task){
        if(task.getId() != Task.TRANSIENT) {
            return updateTask(task);
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = getContentValues(task);
        long newRowId = db.insert(Schema.TaskEntry.TABLE_NAME, null, values);
        db.close();

        return newRowId;
    }

    /**
     * Saves or/and updates all the tasks of the passed {@link Task} list. This operation is performed in an synchronous manner.
     * @param tasks The tasks to save and/or update.
     * @return <code>true</code> if all tasks were saved and <code>false</code> in case of errors.
     */
    public synchronized boolean saveAll(@NonNull final List<Task> tasks){

        if(tasks.isEmpty()) {
            Log.w(TAG, "The passed tasks collection was empty");
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values;
        int updateCount;
        long id;

        for(Task task : tasks){
            values = getContentValues(task);
            if(task.getId() == Task.TRANSIENT){
                //new task
                id = db.insert(Schema.TaskEntry.TABLE_NAME, null, values);
                if(id == -1) {
                    db.close();
                    return false;
                }
            }
            else{
                //update
                String selection = Schema.TaskEntry._ID + " LIKE ?";
                String[] selectionArgs = { String.valueOf(task.getId()) };
                updateCount = db.update(Schema.TaskEntry.TABLE_NAME, values, selection, selectionArgs);
                if(updateCount != 1) {
                    db.close();
                    return false;
                }
            }
        }

        db.close();
        return true;
    }

    /**
     * Converts the passed {@link Task} into {@link ContentValues} for writing
     * them into the database.
     * @param task The {@link Task} to convert.
     * @return The {@link ContentValues} representing the task.
     */
    private ContentValues getContentValues(@NonNull final Task task) {
        String jsonData = Global.gson.toJson(task.getDetails());

        ContentValues values = new ContentValues();
        values.put(Schema.TaskEntry.COLUMN_TITLE, task.getTitle());
        values.put(Schema.TaskEntry.COLUMN_LIST_POS, task.getListPosition());
        if(task.getReminder() != null) values.put(Schema.TaskEntry.COLUMN_DUE_DATE, task.getReminder().getTime());
        else values.put(Schema.TaskEntry.COLUMN_DUE_DATE, 0);
        values.put(Schema.TaskEntry.COLUMN_DONE, task.isDone());
        values.put(Schema.TaskEntry.COLUMN_JSON_DETAIL, jsonData);
        return values;
    }

    /**
     * Updates the passed {@link Task}.
     * @param task The {@link Task} to update.
     * @return the number of updated rows, should be 1
     */
    private long updateTask(@NonNull final Task task){

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = getContentValues(task);

        //define 'where' part of query
        String selection = Schema.TaskEntry._ID + " LIKE ?";
        //specify arguments in placeholder order
        String[] selectionArgs = { String.valueOf(task.getId()) };

        int count = db.update(Schema.TaskEntry.TABLE_NAME, values, selection, selectionArgs);
        db.close();

        if(count == 1){
            Log.d(TAG, "Updated " + count + " row(s)");
            return task.getId();
        }else{
            Log.e(TAG, "Error updating task " + task.getTitle() + " (" + task.getId()+")");
            return -1;
        }

    }

    /**
     * Returns the {@link Task} associated to the passed id. This operation is performed in an synchronous manner.
     * @param id The id of the {@link Task}, must be larger as 0.
     * @return The {@link Task} associated to the passed id or <code>null</code> if not found.
     */
    public synchronized Task getTask(final long id){
        if(id < 1) throw new IllegalArgumentException("The passed id must be larger as 0.");

        SQLiteDatabase db = getReadableDatabase();

        //define the 'where' part of query
        String selection = Schema.TaskEntry._ID + " LIKE ?";
        //specify arguments in placeholder order
        String[] selectionArgs = { String.valueOf(id) };

        Cursor cursor = db.query(
                Schema.TaskEntry.TABLE_NAME,               //the table to query
                PROJECTION,                               //the columns to return
                selection,                                //the columns for the WHERE clause
                selectionArgs,                            //the values for the WHERE clause
                null,                                     //do not group the rows
                null,                                     //do not filter by row groups
                null                                      //do not sort
        );

        if( cursor!=null && cursor.moveToFirst() ){

            String title = cursor.getString(cursor.getColumnIndexOrThrow(Schema.TaskEntry.COLUMN_TITLE));
            int listPos = cursor.getInt(cursor.getColumnIndexOrThrow(Schema.TaskEntry.COLUMN_LIST_POS));
            long dueDateMillis= cursor.getLong(cursor.getColumnIndexOrThrow(Schema.TaskEntry.COLUMN_DUE_DATE));
            int doneInt = cursor.getInt(cursor.getColumnIndexOrThrow(Schema.TaskEntry.COLUMN_DONE));
            String detailsJson = cursor.getString(cursor.getColumnIndexOrThrow(Schema.TaskEntry.COLUMN_JSON_DETAIL));

            cursor.close();
            db.close();

            return new Task(
                    id,
                    title,
                    listPos,
                    dueDateMillis,
                    doneInt,
                    detailsJson
            );
        }
        else{
            db.close();
            return null; //invalid id
        }
    }

    /**
     * Deletes the {@link Task} for the tag with the passed id. This operation is performed in an synchronous manner.
     * @param id The id of the {@link Task}, must be larger as 0.
     * @return The number of deleted rows, should be 1.
     */
    public synchronized int deleteTask(final long id){

        if(id == Task.TRANSIENT){
            Log.w(TAG, "The requested task to delete was transient, id: " + id);
            return 0;
        }

        SQLiteDatabase db = getWritableDatabase();

        //define 'where' part of query
        String selection = Schema.TaskEntry._ID + " LIKE ?";
        //specify arguments in placeholder order
        String[] selectionArgs = { String.valueOf(id) };

        int count = db.delete(Schema.TaskEntry.TABLE_NAME, selection, selectionArgs);
        db.close();
        Log.d(TAG, "Deleted " + count + " row(s)");
        return count;
    }

    /**
     * Fetches all tasks from the database in an synchronous manner.
     * @return A list of all found {@Task} objects or an empty List if the table is empty.
     */
    public synchronized List<Task> getAll(){

        List<Task> tasks = new LinkedList<>();

        SQLiteDatabase db = getReadableDatabase();

        //sort by list pos
        String orderBy =  Schema.TaskEntry.COLUMN_LIST_POS + " ASC";

        //SELECT * FROM tags
        Cursor cursor = db.query(
                Schema.TaskEntry.TABLE_NAME,
                PROJECTION,
                null,
                null,
                null,
                null,
                orderBy
        );


        if( cursor!=null && cursor.moveToFirst() ){

            long id;
            String title;
            int listPos;
            long dueDateMillis;
            int doneInt;
            String detailsJson;

            while (!cursor.isAfterLast()) {

                id = cursor.getLong(cursor.getColumnIndexOrThrow(Schema.TaskEntry._ID));
                title = cursor.getString(cursor.getColumnIndexOrThrow(Schema.TaskEntry.COLUMN_TITLE));
                listPos = cursor.getInt(cursor.getColumnIndexOrThrow(Schema.TaskEntry.COLUMN_LIST_POS));
                dueDateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(Schema.TaskEntry.COLUMN_DUE_DATE));
                doneInt = cursor.getInt(cursor.getColumnIndexOrThrow(Schema.TaskEntry.COLUMN_DONE));
                detailsJson = cursor.getString(cursor.getColumnIndexOrThrow(Schema.TaskEntry.COLUMN_JSON_DETAIL));

                tasks.add(new Task(
                        id,
                        title,
                        listPos,
                        dueDateMillis,
                        doneInt,
                        detailsJson
                ));
                cursor.moveToNext();
            }

            cursor.close();
        }
        db.close();
        return tasks;
    }

    /**
     * Deletes all tasks from the database by dropping the tags table and recreating it.
     * This operation is performed in an synchronous manner.
     */
    public synchronized void deleteAll(){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(Schema.TagTable.SQL_DELETE_ENTRIES);
        db.execSQL(Schema.TagTable.SQL_CREATE_ENTRIES);
        db.close();
    }

    /**
     * Fetches all tasks from the database in an asynchronous manner with RxAndroid and returns
     * the created {@link Subscription} object.
     * @param fetchTasksObserver An {@link Observer} object that implements the callbacks for getting
     *                           the result of the async operation.
     */
    public Subscription getAllAsync(@NonNull final Observer<List<Task>> fetchTasksObserver) {

        return getFetchTasksObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fetchTasksObserver);
    }

    /**
     * Saves or/and updates all the tasks of the passed {@link Task} list in an asynchronous manner with
     * RxAndroid and returns the created {@link Subscription} object.
     * @param updateTasksObserver An {@link Observer} object that implements the callbacks for getting
     *                           the result of the async operation.
     * @param tasksToUpdate A list of {@link Task} objects to save or/and update.
     */
    public Subscription saveAllAsync(@NonNull final Observer<Boolean> updateTasksObserver, @NonNull final List<Task> tasksToUpdate) {

        return getUpdateTasksObservable(tasksToUpdate)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(updateTasksObserver);
    }

    /**
     * Creates a new {@link Observable} that fetches all tasks from the database
     * and returns them as a list of {@link Task} objects.
     * @return The {@link Observable} to subscribe on for fetching all tasks from the database.
     */
    private Observable<List<Task>> getFetchTasksObservable(){
        return Observable.create(new Observable.OnSubscribe<List<Task>>() {
            @Override
            public void call(Subscriber<? super List<Task>> subscriber) {
                try {
                    List<Task> tasks = getAll();
                    subscriber.onNext(tasks);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    /**
     * Creates a new {@link Observable} that updates all tasks in the database
     * and returns a boolean indicating whether the update was successful.
     * @param tasksToUpdate A list of {@link Task} objects to update.
     * @return The {@link Observable} to subscribe on for updating all tasks in the database.
     */
    private Observable<Boolean> getUpdateTasksObservable(@NonNull final List<Task> tasksToUpdate){
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    Boolean result = saveAll(tasksToUpdate);
                    subscriber.onNext(result);
                    subscriber.onCompleted();
                }
                catch (Exception e){
                    subscriber.onError(e);
                }
            }
        });
    }

}
