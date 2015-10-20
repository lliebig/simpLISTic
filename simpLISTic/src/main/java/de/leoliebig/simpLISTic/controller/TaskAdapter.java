package de.leoliebig.simpLISTic.controller;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import de.leoliebig.wundertest.R;
import de.leoliebig.simpLISTic.model.Task;

/**
 * Implements a custom RecyclerView adapter for populating list items with the
 * data of {@link Task} objects.
 *
 * @author info@leoliebig.de
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private final ItemActionListener listener;
    private List<Task> tasks;
    private boolean hideDoneTasks =false;


    /**
     * Creates a new instance.
     * @param tasks A {@link java.util.List} of {@link Task} objects.
     */
    public TaskAdapter(@NonNull final List<Task> tasks, @NonNull final ItemActionListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_task, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        final Task task = tasks.get(position);

        //remove the old listeners
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.label.setOnClickListener(null);

        //hide done tasks
        if (hideDoneTasks && task.isDone()) {
            holder.setVisibility(false);
            return;
        } else {
            holder.setVisibility(true);
        }

        //populate the list view with data from the task
        holder.label.setText(task.getTitle());
        holder.checkBox.setChecked(task.isDone());

        //set the new listeners
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (listener != null) listener.onItemChecked(task, isChecked);
            }
        });

        holder.label.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onTitleClicked(task);
            }
        });

    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }


    /**
     * Controls whether done tasks are hidden or not.
     * @param hideDoneTasks <code>true</code> if done tasks should be hidden, otherwise <code>false</code>.
     */
    public void setDoneTasksHidden(final boolean hideDoneTasks){
        this.hideDoneTasks = hideDoneTasks;
    }

    /**
     * Represents the data of a single list item
     */
    public static class ViewHolder extends RecyclerView.ViewHolder{
        public CheckBox checkBox;
        public TextView label;
        private View view;
        private int marginBottom;

        public ViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            checkBox = (CheckBox) itemView.findViewById(R.id.list_item_checkbox);
            label = (TextView) itemView.findViewById(R.id.list_item_title);

            marginBottom = ((RecyclerView.LayoutParams) view.getLayoutParams()).bottomMargin;
        }

        public void setBackgroundColor(int color){
            view.setBackgroundColor(color);
        }

        /**
         * Sets the visibility of a task list item.
         * @param visible <code>true</code> if the task list item should be visible, otherwise <code>false</code>.
         */
        public void setVisibility(boolean visible){

            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,0);

            if(visible){
                view.setVisibility(View.VISIBLE);
                params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                params.bottomMargin = marginBottom;
            }
            else{
                view.setVisibility(View.GONE);
                params.bottomMargin = 0;
            }
            view.setLayoutParams(params);
            view.invalidate();
        }
    }

    /**
     * Interface that allows the creator of the list view to handle
     * multiple touch events for a single list item.
     */
    public interface ItemActionListener{

        /**
         * Is called if the state of the list items checkbox changed.
         * @param task The {@link Task} associated with the list item.
         * @param isChecked The new checked state of the checkbox.
         */
        void onItemChecked(Task task, boolean isChecked);

        /**
         * Is called if the title of the list item is clicked.
         * @param task The {@link Task} associated with the list item.
         */
        void onTitleClicked(Task task);
    }

}
