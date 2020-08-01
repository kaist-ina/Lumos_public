package edu.cmu.hcii.sugilite;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;

//for showing learning instances
public class LearningDataForAppActivity extends AppCompatActivity {
    private SugiliteScriptDao sugiliteScriptDao;
    private AlertDialog dialog;
    private String target_instance;

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning_data_for_app);
        sugiliteScriptDao = new SugiliteScriptDao(this);
        Intent it = getIntent();
        setTitle(it.getStringExtra("app_name") + " " + "Learning Instances");

        final ListView learning_list = (ListView) findViewById(R.id.LearningInstances);

        InstanceLearningAdapter adapter = new InstanceLearningAdapter();
        learning_list.setAdapter(adapter);

        setUpScriptList(adapter, it.getStringExtra("app_name"));

        /*
        * Developer: Jeongmin
        * This is for adding pre-added learning instances
        * */
        String app_name = it.getStringExtra("app_name");
        if (app_name.equals("Netflix"))
            adapter.addItem(ContextCompat.getDrawable(this, R.drawable.netflix_request),"streaming_video_to_Chromecast");
        else if (app_name.equals("HUE"))
            adapter.addItem(ContextCompat.getDrawable(this, R.drawable.hue_turn_on),"turn_off_bulb");
        else if (app_name.equals("SmartThings"))
        {
            adapter.addItem(ContextCompat.getDrawable(this, R.drawable.st_motion_not_detected),"motion_not_detected");
            adapter.addItem(ContextCompat.getDrawable(this, R.drawable.st_door_closed),"door_closed");
            adapter.addItem(ContextCompat.getDrawable(this, R.drawable.st_hue_turn_off), "turn off a bulb");
            adapter.addItem(ContextCompat.getDrawable(this, R.drawable.st_hue_turn_off), "turn off a plug");
        }
        else if (app_name.equals("Wink"))
        {
            adapter.addItem(ContextCompat.getDrawable(this,R.drawable.wink_motion_detected), "motion_detected");
            adapter.addItem(ContextCompat.getDrawable(this,R.drawable.wink_door_opened), "door_opened");
            adapter.addItem(ContextCompat.getDrawable(this,R.drawable.wink_play_chime), "play_siren");
        }
        else if (app_name.equals("Winix"))
        {
            adapter.addItem(ContextCompat.getDrawable(this,R.drawable.winix_power_off), "aircleaner_power_on");
        }
        else if (app_name.equals("Wemo"))
        {
            adapter.addItem(ContextCompat.getDrawable(this, R.drawable.wemo_power_off), "plug_power_off");
        }
        else if (app_name.equals("August"))
        {
            adapter.addItem(ContextCompat.getDrawable(this, R.drawable.august_door_closed), "door_closed");
        }
        else if (app_name.equals("Insteon"))
        {
            adapter.addItem(ContextCompat.getDrawable(this, R.drawable.insteon_plug_off), "water_leaked");
            adapter.addItem(ContextCompat.getDrawable(this, R.drawable.insteon_water_leaked), "plug_power_off");
        }
    }

    void show()
    {
        final List<String> ListItems = new ArrayList<>();
        ListItems.add("AND");
        ListItems.add("OR");
        final CharSequence[] items =  ListItems.toArray(new String[ ListItems.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose AND OR");
//        builder.setMessage("AlertDialog Content");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {
                InteroperationDialog.click_counter ++;
                String selectedText = items[pos].toString();
                InteroperationDialog.current_conditions.add(selectedText);
                InteroperationDialog.current_conditions.add(target_instance);
                Toast.makeText(LearningDataForAppActivity.this, InteroperationDialog.current_conditions.toString(), Toast.LENGTH_LONG).show();
                onBackPressed();
            }
        });
        builder.show();
    }

    @Override
    public void onBackPressed()
    {
        // Go to MainActivity
        Intent intent = new Intent(this, InteroperationManagerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * update the script list displayed at the main activity according to the DB
     */
    private void setUpScriptList(InstanceLearningAdapter adapter, String targetapp){
        List<String> names = sugiliteScriptDao.getAllNames();
        for(String name : names){
            String extractedname = name.replace(".SugiliteScript", "").split((" "))[0];
            String semanticTag = name.replace(".SugiliteScript", "").split((" "))[1];
            if (extractedname.equals(targetapp))
                adapter.addItem(null, semanticTag);
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

    private class InstanceLearningAdapter extends BaseAdapter {
        private ArrayList<EachInstance> items = new ArrayList<>();

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final int pos = position;
            final Context context = parent.getContext();

            // "listview_item" Layout을 inflate하여 convertView 참조 획득.
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.each_learning_instance, parent, false);
            }

            // 화면에 표시될 View(Layout이 inflate된)으로부터 위젯에 대한 참조 획득
            ImageView iconImageView = (ImageView) convertView.findViewById(R.id.instance_img) ;
            final TextView titleTextView = (TextView) convertView.findViewById(R.id.textView) ;


            // For testing event handler
            final String text = items.get(position).getSemantictag();
            Button button = (Button)convertView.findViewById(R.id.set_condition);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    InteroperationDialog.click_counter++;
                    if (InteroperationDialog.current_conditions.size() > 0) {
                        target_instance = text;
                        show();
                    }
                    else {
                        InteroperationDialog.current_conditions.add(text);
                        Toast.makeText(LearningDataForAppActivity.this, InteroperationDialog.current_conditions.toString(), Toast.LENGTH_LONG).show();
                        onBackPressed();
                    }
                }
            });

            button = (Button)convertView.findViewById(R.id.set_control);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //must to add this control.
                    InteroperationDialog.click_counter++;
                    InteroperationDialog.current_controls.add(titleTextView.getText().toString());
                    Toast.makeText(LearningDataForAppActivity.this, InteroperationDialog.current_controls.toString(), Toast.LENGTH_LONG).show();
                    onBackPressed();
                }
            });

            // Data Set(listViewItemList)에서 position에 위치한 데이터 참조 획득
            EachInstance listViewItem = items.get(position);

            // showing data
            iconImageView.setImageDrawable(listViewItem.getIcon());
            titleTextView.setText(listViewItem.getSemantictag());

            return convertView;
        }

        // a method to add a new item.
        public void addItem(Drawable icon, String tag) {
            EachInstance item = new EachInstance();
            item.setIcon(icon);
            item.setSemantictag("Semantic Tag: " + tag);
            items.add(item);
        }
    }
}
