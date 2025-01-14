package com.sarangjoshi.docschedulerdoc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.*;


public class PatientHomeActivity extends Activity {
    public static final String DOC_ID = "doc-id";
    public static final String DOC_NAME = "doc-name";

    private ListView doctorsList;
    private ArrayAdapter<String> adapter;
    private List<String> doctors;
    private List<String> docObjectIds;

    Data mData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_home);

        mData = new Data(this);

        doctorsList = (ListView) findViewById(R.id.doctorsList);
        doctors = new ArrayList<String>();
        docObjectIds = new ArrayList<String>();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, doctors);
        doctorsList.setAdapter(adapter);
        doctorsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(PatientHomeActivity.this, PatientViewScheduleActivity.class);
                intent.putExtra(DOC_ID, docObjectIds.get(position));
                startActivity(intent);
            }
        });

        loadDoctors();
    }

    ProgressDialog d;

    private void loadDoctors() {
        d = ProgressDialog.show(this, "", "Loading...");

        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                for(ParseUser u : parseUsers) {
                    doctors.add(u.getString("name"));
                    docObjectIds.add(u.getObjectId());
                }
                adapter.notifyDataSetChanged();
                d.dismiss();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_patient_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_switch_to_doctor) {
            mData.saveIsPatientMode(false);
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
