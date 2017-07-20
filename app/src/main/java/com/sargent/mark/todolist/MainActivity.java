package com.sargent.mark.todolist;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.sargent.mark.todolist.data.Contract;
import com.sargent.mark.todolist.data.DBHelper;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener,AddToDoFragment.OnDialogCloseListener, UpdateToDoFragment.OnUpdateDialogCloseListener{
    private Spinner sp;
    private RecyclerView rv;
    private FloatingActionButton button;
    private DBHelper helper;
    private Cursor cursor;
    private SQLiteDatabase db;
    ToDoListAdapter adapter;
    private final String TAG = "mainactivity";
    private String currentCategory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "oncreate called in main activity");
        button = (FloatingActionButton) findViewById(R.id.addToDo);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getSupportFragmentManager();
                AddToDoFragment frag = new AddToDoFragment();
                frag.show(fm, "addtodofragment");
            }
        });
        rv = (RecyclerView) findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        sp = (Spinner) findViewById(R.id.categories_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.categories_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        sp.setAdapter(adapter);
        sp.setOnItemSelectedListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (db != null) db.close();
        if (cursor != null) cursor.close();
    }

    @Override
    protected void onStart() {
        super.onStart();
        helper = new DBHelper(this);
        db = helper.getWritableDatabase();
        currentCategory = "All";
        cursor = getAllItems(db);

        // overridden the constructor to provide a db update for when the checkbox state changes
        adapter = new ToDoListAdapter(cursor, new ToDoListAdapter.ItemClickListener() {
            @Override
            public void onItemClick(int pos, String description, String category, String duedate, long id) {
                Log.d(TAG, "item click id: " + id);
                String[] dateInfo = duedate.split("-");
                int year = Integer.parseInt(dateInfo[0].replaceAll("\\s", ""));
                int month = Integer.parseInt(dateInfo[1].replaceAll("\\s", ""));
                int day = Integer.parseInt(dateInfo[2].replaceAll("\\s", ""));

                FragmentManager fm = getSupportFragmentManager();

                UpdateToDoFragment frag = UpdateToDoFragment.newInstance(year, month, day, description, category, id);
                frag.show(fm, "updatetodofragment");
            }
        }, new ToDoListAdapter.CheckedListener() {
            @Override
            public void onCheckedChange(boolean checked, long id) {
                Log.d(TAG, "item checked id: " + id);
                updateToDoState(db, (checked ? "yes" : "no"), id);
            }
        });

        rv.setAdapter(adapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                long id = (long) viewHolder.itemView.getTag();
                Log.d(TAG, "passing id: " + id);
                removeToDo(db, id);
                adapter.swapCursor(getAllItems(db));
            }
        }).attachToRecyclerView(rv);
    }

    @Override
    public void closeDialog(int year, int month, int day, String description, String category) {
        addToDo(db, description, category, formatDate(year, month, day));
        cursor = getItemsIn(db, currentCategory);
        adapter.swapCursor(cursor);
    }

    public String formatDate(int year, int month, int day) {
        return String.format("%d-%d-%d", year, month, day);
    }

    private Cursor getAllItems(SQLiteDatabase db) {
        return db.query(
                Contract.TABLE_TODO.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE
        );
    }

    private Cursor getItemsIn(SQLiteDatabase db, String category) {
        Log.d(TAG, "User selected category: " + category);
        currentCategory = category;

        if(category.equals("All")) {
            return getAllItems(db);
        }
        else {
            String where = Contract.TABLE_TODO.COLUMN_NAME_CATEGORY + "='" + category + "'";
            return db.query(Contract.TABLE_TODO.TABLE_NAME, null, where, null, null, null, Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE);
        }
    }

    private long addToDo(SQLiteDatabase db, String description, String category, String duedate) {
        ContentValues cv = new ContentValues();
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DESCRIPTION, description);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE, duedate);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_CHECKED, "no");
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_CATEGORY, category);
        return db.insert(Contract.TABLE_TODO.TABLE_NAME, null, cv);
    }

    private boolean removeToDo(SQLiteDatabase db, long id) {
        Log.d(TAG, "deleting id: " + id);
        return db.delete(Contract.TABLE_TODO.TABLE_NAME, Contract.TABLE_TODO._ID + "=" + id, null) > 0;
    }


    private int updateToDo(SQLiteDatabase db, int year, int month, int day, String description, String category, long id){
        String duedate = formatDate(year, month, day);

        ContentValues cv = new ContentValues();
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DESCRIPTION, description);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE, duedate);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_CATEGORY, category);

        return db.update(Contract.TABLE_TODO.TABLE_NAME, cv, Contract.TABLE_TODO._ID + "=" + id, null);
    }

    // separate query to update only the state of the clicked checkbox
    private int updateToDoState(SQLiteDatabase db, String checked, long id) {
        ContentValues cv = new ContentValues();
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_CHECKED, checked);

        Log.d(TAG, "changing the state of id: " + id);

        return db.update(Contract.TABLE_TODO.TABLE_NAME, cv, Contract.TABLE_TODO._ID + "=" + id, null);
    }

    @Override
    public void closeUpdateDialog(int year, int month, int day, String description, String category, long id) {
        updateToDo(db, year, month, day, description, category, id);
        adapter.swapCursor(getItemsIn(db, currentCategory));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String category = sp.getItemAtPosition(position).toString();
        adapter.swapCursor(getItemsIn(db, category));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // don't do anything
        Log.d(TAG, "Nothing selected");
    }
}
