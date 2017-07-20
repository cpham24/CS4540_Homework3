package com.sargent.mark.todolist;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.sargent.mark.todolist.data.Contract;

/**
 * Created by mark on 7/4/17.
 */

public class ToDoListAdapter extends RecyclerView.Adapter<ToDoListAdapter.ItemHolder> {

    private Cursor cursor;
    private ItemClickListener clickListener;
    private CheckedListener checkedListener;
    private String TAG = "todolistadapter";

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.item, parent, false);
        ItemHolder holder = new ItemHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {
        holder.bind(holder, position);
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    public interface ItemClickListener {
        void onItemClick(int pos, String description, String category, String duedate, long id);
    }

    // added this interface to use as checkbox listener
    public interface CheckedListener {
        void onCheckedChange(boolean checked, long id);
    }

    public ToDoListAdapter(Cursor cursor, ItemClickListener clickListener, CheckedListener checkedListener) {
        this.cursor = cursor;
        this.clickListener = clickListener;
        this.checkedListener = checkedListener;
    }

    public void swapCursor(Cursor newCursor) {
        if (cursor != null) cursor.close();
        cursor = newCursor;
        if (newCursor != null) {
            // Force the RecyclerView to refresh
            this.notifyDataSetChanged();
        }
    }

    /*
     |  Added a CheckBox property named cb that references the checkbox in the view
     |  Added and implemented necessary interfaces and methods to capture when the state of the checkbox changes
     |
     */
    class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        TextView descr;
        TextView due;
        CheckBox cb;
        String duedate;
        String description;
        String checked;
        String category;
        long id;

        // modified the constructor to get reference to checkbox view
        ItemHolder(View view) {
            super(view);
            descr = (TextView) view.findViewById(R.id.description);
            due = (TextView) view.findViewById(R.id.dueDate);
            cb = (CheckBox) view.findViewById(R.id.checkbox);
            view.setOnClickListener(this);
            cb.setOnCheckedChangeListener(this);
        }

        // changed this function to also update the checkbox with info from database
        public void bind(ItemHolder holder, int pos) {
            cursor.moveToPosition(pos);
            id = cursor.getLong(cursor.getColumnIndex(Contract.TABLE_TODO._ID));
            Log.d(TAG, "fetching todo item id: " + id);

            duedate = cursor.getString(cursor.getColumnIndex(Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE));
            description = cursor.getString(cursor.getColumnIndex(Contract.TABLE_TODO.COLUMN_NAME_DESCRIPTION));
            checked = cursor.getString(cursor.getColumnIndex(Contract.TABLE_TODO.COLUMN_NAME_CHECKED));
            category = cursor.getString(cursor.getColumnIndex(Contract.TABLE_TODO.COLUMN_NAME_CATEGORY));
            descr.setText(description);
            due.setText(duedate);
            cb.setChecked(checked.equals("yes"));
            holder.itemView.setTag(id);
        }

        @Override
        public void onClick(View v) {
            int pos = getAdapterPosition();
            clickListener.onItemClick(pos, description, category, duedate, id);
        }

        // overridden this function to capture checkbox state change
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            checkedListener.onCheckedChange(isChecked, id);
        }
    }

}
