/**
 * HomeActivity.java
 * Dec 7, 2014
 * Sarang Joshi
 */

package com.sarangjoshi.docschedulerdoc;

import com.parse.*;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeActivity extends FragmentActivity implements Schedule.SaveToParseListener, SetPlaceUpdateFragment.PlaceUpdateDialogListener {
    public static final String SCHEDULE_KEY = "schedule";
    public static ProgressDialog d;
    private static Schedule mSchedule;

    TextView userView, placesEmptyText;
    Button logoutBtn, saveUpdateBtn;
    ListView todaysPlacesList;
    TextView updateText;
    ProgressBar updateProgressBar;

    List<String> todaysPlaces;
    ArrayAdapter<String> todaysPlacesAdapter;

    String today;
    String todaysUpdate = "";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ParseUser user = ParseUser.getCurrentUser();
        mSchedule = new Schedule(this);

        if (user == null || user.getSessionToken() == null) {
            // User not logged in
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            setContentView(R.layout.activity_home);

            userView = (TextView) findViewById(R.id.userText);
            userView.setText("Logged in as: " + user.getUsername());

            logoutBtn = (Button) findViewById(R.id.logoutBtn);
            saveUpdateBtn = (Button) findViewById(R.id.saveUpdateBtn);
            saveUpdateBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SetPlaceUpdateFragment dialog = new SetPlaceUpdateFragment();
                    dialog.update = todaysUpdate;
                    dialog.show(getSupportFragmentManager(), "SetPlaceUpdateFragment");
                }
            });
            todaysPlaces = new ArrayList<String>();
            todaysPlacesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, todaysPlaces);
            todaysPlacesList = (ListView) findViewById(R.id.todayList);
            todaysPlacesList.setAdapter(todaysPlacesAdapter);
            placesEmptyText = (TextView) findViewById(R.id.todayEmptyText);
            updateText = (TextView) findViewById(R.id.updateEdit);
            updateProgressBar = (ProgressBar) findViewById(R.id.updateProgressBar);

            retrieveSchedule();
        }
    }

    // INITIALIZATION SCHEDULE METHODS

    /**
     * Retrieves the current schedule from the Parse database.
     */
    private void retrieveSchedule() {
        d = ProgressDialog.show(this, "", "Loading...");
        ParseUser user = ParseUser.getCurrentUser();
        ParseObject o = user.getParseObject("schedule");
        if (o != null)
            o.fetchInBackground(new GetCallback<ParseObject>() {
                public void done(ParseObject sched, ParseException e) {
                    if (e == null)
                        initSchedule(sched);
                }
            });
        else {
            d.dismiss();
        }
    }

    /**
     * Once the Schedule ParseObject has been retrieved, actually initialize the
     * schedule into the Schedule Java object.
     *
     * @param sched
     */
    private void initSchedule(ParseObject sched) {
        mSchedule.setInitialized(false);
        mSchedule.resetPlaces();
        // Get places
        ParseRelation<ParseObject> placeRelation = sched
                .getRelation(Schedule.PLACES_KEY);
        placeRelation.getQuery().findInBackground(
                new FindCallback<ParseObject>() {
                    public void done(List<ParseObject> places, ParseException e) {
                        if (e == null) {
                            for (ParseObject place : places) {
                                Place p = new Place();
                                p.construct(place);
                                // savePlaceId(place.getObjectId());
                                mSchedule.getPlaces().add(p);
                            }
                            mSchedule.setInitialized(true);
                            loadToday();
                        }
                    }
                });
    }

    /**
     * Once the Schedule Java object has been initialized, parses today's schedule into the view.
     */
    private void loadToday() {
        todaysPlaces.clear();

        // Goes through all the places and sees if any are today
        today = DayTime.getStringFromInt(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
        for (Place p : mSchedule.getPlaces()) {
            for (DayTime t : p.getDayTimes()) {
                if (t.getDay().equals(today)) {
                    todaysPlaces.add(p.getName() + ": " + t.getStartString() + " to " + t.getEndString());
                }
            }
        }
        if (!todaysPlaces.isEmpty()) {
            // Places not empty
            placesEmptyText.setVisibility(View.GONE);
            todaysPlacesList.setVisibility(View.VISIBLE);
            todaysPlacesAdapter.notifyDataSetChanged();
        } else {
            // Places empty
            placesEmptyText.setVisibility(View.VISIBLE);
            todaysPlacesList.setVisibility(View.GONE);
        }
        loadUpdate();
    }

    /**
     * Loads today's update.
     */
    private void loadUpdate() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("DayEdit");
        String today = getTodayString();
        query.whereEqualTo("day", today);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    if (!parseObjects.isEmpty()) {
                        // Current update exists
                        ParseObject todayUpdateObj = parseObjects.get(0);
                        todaysUpdate = (String) todayUpdateObj.get("status");
                    } else {
                        // No update
                        todaysUpdate = "";
                    }
                    updateViewsLocal();
                }
                d.dismiss();
            }
        });
    }

    // FUNCTIONS

    /**
     * Opens a new dialog to save update for the chosen place
     */
    private void saveUpdate() {
        updateProgressBar.setVisibility(View.VISIBLE);
        updateText.setVisibility(View.GONE);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("DayEdit");
        final String today = getTodayString();
        query.whereEqualTo("day", today);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    ParseObject todayUpdateObj;
                    // First, checks to see if there is an existent update
                    if (!parseObjects.isEmpty()) {
                        todayUpdateObj = parseObjects.get(0);
                    } else {
                        todayUpdateObj = new ParseObject("DayEdit");
                    }
                    todayUpdateObj.put("status", todaysUpdate);
                    todayUpdateObj.put("day", today);
                    todayUpdateObj.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Toast.makeText(HomeActivity.this, "Saved!", Toast.LENGTH_SHORT).show();
                                updateViewsLocal();
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Locally updates views. Currently, just updates today's update.
     */
    private void updateViewsLocal() {
        updateProgressBar.setVisibility(View.GONE);
        updateText.setVisibility(View.VISIBLE);
        updateText.setText(todaysUpdate);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_home, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                retrieveSchedule();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onBackPressed() {
        finish();
    }

    /**
     * Logs the user out.
     */
    public void logout(View v) {
        ParseUser.logOut();
        ParseUser currentUser = ParseUser.getCurrentUser();

        if (currentUser == null || currentUser.getSessionToken() == null) {
            // Successfully logged out
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Starts the schedule edit sequence, starting the EditScheduleActivity.
     */
    public void editSched(View v) {
        Intent intent = new Intent(this, EditScheduleActivity.class);
        startActivityForResult(intent, 0);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            d = ProgressDialog.show(this, "", "Saving...");
            mSchedule.saveToParse();
        }
    }

    // INTERFACE IMPLEMENTATION
    @Override
    public void onSaveCompleted() {
        d.dismiss();
        retrieveSchedule();
    }

    @Override
    public void onDialogPositiveClick(SetPlaceUpdateFragment dialog, String newUpdate) {
        closeKeyboard(dialog);
        todaysUpdate = newUpdate;
        saveUpdate();
    }

    @Override
    public void onDialogNegativeClick(SetPlaceUpdateFragment dialog) {
        closeKeyboard(dialog);
    }

    /**
     * Closes keyboard.
     */
    private void closeKeyboard(SetPlaceUpdateFragment d) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(d.updateEdit.getApplicationWindowToken(), 0);
        // imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    /**
     * Format: DD-MM-YYYY
     */
    private String getTodayString() {
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_MONTH), month = c.get(Calendar.MONTH) + 1,
                year = c.get(Calendar.YEAR);
        String s = ((day < 10) ? "0" + day : day) + "-";
        s += ((month < 10) ? "0" + month : month) + "-";
        s += year;

        return s;
    }

    /**
     * Gets the current Schedule Java object.
     */
    public static Schedule getSchedule() {
        return mSchedule;
    }

}
