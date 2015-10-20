package de.leoliebig.simpLISTic.db;

import android.provider.BaseColumns;

/**
 * A container for constants that defines the schema for the tasks database.
 *
 * @author info@leoliebig.de
 */
public abstract class Schema {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Tasks.db";

    public static abstract class TagTable {
        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TaskEntry.TABLE_NAME +
                        " (" +
                        TaskEntry._ID + " integer primary key autoincrement," +
                        TaskEntry.COLUMN_TITLE + " text not null," +
                        TaskEntry.COLUMN_LIST_POS + " integer default 0," +
                        TaskEntry.COLUMN_DUE_DATE + " integer default 0," +
                        TaskEntry.COLUMN_DONE + " integer default 0," +
                        TaskEntry.COLUMN_JSON_DETAIL + " text" +
                        " );";
        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TaskEntry.TABLE_NAME;
    }

    public static abstract class TaskEntry implements BaseColumns {
        public static final String TABLE_NAME = "tasks";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_LIST_POS = "listpos";
        public static final String COLUMN_DUE_DATE = "duedate";
        public static final String COLUMN_DONE = "done";
        public static final String COLUMN_JSON_DETAIL = "details";
    }

}
