package de.leoliebig.simpLISTic.view;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.leakcanary.RefWatcher;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.leoliebig.simpLISTic.Global;
import de.leoliebig.wundertest.R;
import de.leoliebig.simpLISTic.controller.TaskAdapter;
import de.leoliebig.simpLISTic.model.Task;

/**
 * Implements the GUI for displaying a list of tasks.
 *
 * Activities that contain this fragment must implement {@link TasksFragment.OnFragmentInteraction}
 * to handle interaction events.
 *
 * @author info@leoliebig.de
 */
public class TasksFragment extends Fragment implements TaskAdapter.ItemActionListener {

    private static final String TAG = TasksFragment.class.getSimpleName();

    private Context context;
    private OnFragmentInteraction listener;
    private List<Task> tasks = new LinkedList<>();
    private RecyclerView.Adapter adapter;

    public TasksFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tasks, container, false);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view_tasks);
        recyclerView.setHasFixedSize(true); //the content does not change the size of the recyclerView
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new TaskAdapter(tasks, this);
        recyclerView.setAdapter(adapter);

        TaskTouchCallback taskTouchCallback = new TaskTouchCallback();
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(taskTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        return rootView;
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
        this.listener = null;
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = Global.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }

    //ItemActionListener callbacks

    @Override
    public void onItemChecked(@NonNull final Task task, final boolean isChecked) {
        if(Global.DEBUG) Log.d(TAG, "Item checked: " + task.getTitle());
        task.setDone(isChecked);
        adapter.notifyDataSetChanged();
        listener.onTaskDone(task);
    }

    @Override
    public void onTitleClicked(@NonNull final Task task) {
        if(Global.DEBUG) Log.d(TAG, "Item edit: " + task.getTitle());
        listener.onTaskEdit(task);
    }

    /**
     * Updates the data and the list view of the fragment
     * @param tasks The changed list of {@link Task} objects.
     */
    public synchronized void updateContent(@NonNull final List<Task> tasks){

        //updating the profiles collection
        //note: clear() and addAll() necessary to keep the same reference as the adapter
        //a reassignment of profiles makes the call adapter.notifyDataSetChanged() useless
        this.tasks.clear();
        this.tasks.addAll(tasks);

        //notify the adapter and invalidate the list views
        adapter.notifyDataSetChanged();
    }

    /**
     * Controls whether done tasks are hidden or not.
     * @param hideDoneTasks <code>true</code> if done tasks should be hidden, otherwise <code>false</code>.
     */
    public void setDoneTasksHidden(final boolean hideDoneTasks){
        ((TaskAdapter) this.adapter).setDoneTasksHidden(hideDoneTasks);
        adapter.notifyDataSetChanged();
    }

    /**
     * Shows a {@link Snackbar} that allows to undo a delete operation. If the {@link Snackbar} is dismissed
     * the {@link Task} is deleted permanently.
     * @param position The original list position of the task to delete.
     * @param deletedTask The {@link Task} to delete.
     */
    private void showUndoSnackbar(final int position, final Task deletedTask) {

        Snackbar deleteSnackBar = Snackbar.make(getView(), context.getString(R.string.snackbar_deleted_task) + deletedTask.getTitle(), Snackbar.LENGTH_LONG);
        deleteSnackBar.setAction(R.string.action_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tasks.add(position, deletedTask);
                adapter.notifyItemInserted(position);
            }
        });
        deleteSnackBar.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);

                Log.d(TAG, "Dismissed event: " + event);

                //orientation changes dismiss the Snackbar, ignore the delete
                if(listener!= null && event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                    Log.d(TAG, "Deleting task " + deletedTask.getId());
                    listener.onTaskDeleted(deletedTask);
                    updateTaskPositions();
                }
            }
        });
        deleteSnackBar.show();
    }

    /**
     * Updates the list positions of the tasks and notifies the parent
     * activity about the change
     */
    private void updateTaskPositions() {
        for(int i=0; i < tasks.size(); i++){
            tasks.get(i).setListPosition(i);
        }
        listener.onTaskMoved(tasks);
    }

    /**
     * Implements callback methods for touch gestures performed on the list items of the RecyclerView. It keeps
     * the data model in sync if the list is reordered or if items are deleted.
     *
     * <p>
     * The implementation is based on <a href="https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf">
     *     this source</a>.</p>
     */
    class TaskTouchCallback extends ItemTouchHelper.Callback {

        private TaskAdapter.ViewHolder selectedItem;
        private final int colorSelected = getResources().getColor(R.color.colorAccentLight);
        private final int colorDefault = getResources().getColor(R.color.white);

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            Collections.swap(tasks, fromPosition, toPosition);
            adapter.notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            //backup the deleted tasks and its position
            final int position = viewHolder.getAdapterPosition();
            final Task deletedTask = tasks.get(position);

            tasks.remove(position);
            adapter.notifyItemRemoved(position);
            //show snackbar with undo action, if the user let's it disappear: delete task
            showUndoSnackbar(position, deletedTask);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        @Override
        public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
            updateTaskPositions();
            super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {

            //change the background of the list item to show the user, that it is selected
            if(viewHolder != null) {
                selectedItem = (TaskAdapter.ViewHolder) viewHolder;
                selectedItem.setBackgroundColor(colorSelected);
            }
            else{
                //undo
                if(selectedItem!=null){
                    selectedItem.setBackgroundColor(getResources().getColor(R.color.white));
                    selectedItem = null;
                }
            }

            super.onSelectedChanged(viewHolder, actionState);
        }
    }

    /**
     * Allows to interact with the {@link TasksFragment}.
     */
    public interface OnFragmentInteraction {

        /**
         * Is called by the fragment if the user wants to edit a task.
         * @param task The {@link Task} that was done.
         */
        void onTaskEdit(Task task);

        /**
         * Is called by the fragment if the user set a task to done
         * @param task The {@link Task} that was done.
         */
        void onTaskDone(Task task);

        /**
         * Is called if the user deletes a task by swiping it from the list
         */
        void onTaskDeleted(Task deletedTask);

        /**
         * Is called if the user moves tasks and changes the order of the list.
         * @param tasksWithNewPositions A list of {@link Task} with the updated positions.
         */
        void onTaskMoved(List<Task> tasksWithNewPositions);

    }
}
