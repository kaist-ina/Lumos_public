package edu.cmu.hcii.sugilite;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class InteroperationManagerActivity extends AppCompatActivity {

    @Override
    public void onBackPressed() {
        // Go to MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interoperation_manager);
        setTitle("Lumos Interoperation Manager");
        initAppList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        show_complete();
    }

    void show_complete()
    {
        if (InteroperationDialog.current_conditions.size() > 0 && InteroperationDialog.current_controls.size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Are you done?");

            builder.setMessage("Current interoperation rule \nConditions: " + InteroperationDialog.current_conditions.toString().replaceAll("Semantic Tag: ", "")
                    .replaceAll(",", "")
                    + "\nControls: " + InteroperationDialog.current_controls.toString().replaceAll("Semantic Tag: ", ""));
            builder.setPositiveButton("Done",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(), "Finish configuration ->" + InteroperationDialog.click_counter, Toast.LENGTH_LONG).show();
                            InteroperationDialog.current_controls.clear();
                            InteroperationDialog.current_conditions.clear();
                            InteroperationDialog.click_counter = 0;
                        }
                    });
            builder.setNegativeButton("Not yet",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(), "Continue configuration", Toast.LENGTH_LONG).show();
                        }
                    });
            builder.show();
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initAppList() {
        final ArrayList<String> items = new ArrayList<String>();
        items.add("August");
        items.add("Netflix");
        items.add("HUE");
        items.add("Insteon");
        items.add("Nest");
        items.add("SmartThings");
        items.add("Wemo");
        items.add("Wink");
        items.add("Winix");

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, items) ;
        final ListView applist_view = (ListView) findViewById(R.id.Applist);
        applist_view.setAdapter(adapter);

        applist_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(view != null)
                {
                    InteroperationDialog.click_counter++;
                    Intent intent = new Intent(InteroperationManagerActivity.this, LearningDataForAppActivity.class);
                    // need to app_name to get scripts that are related to "app_name"
                    intent.putExtra("app_name",((TextView) view).getText().toString());
                    startActivity(intent);
                }
            }
        });
    }
}