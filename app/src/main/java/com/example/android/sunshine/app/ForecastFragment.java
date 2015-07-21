package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;


/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private ArrayAdapter<String> mForecastAdapter;
    private double longitude;
    private double latitude;

    public ForecastFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Indicate that this activity has a menu
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Create an ArrayAdapter that will act as the datasource of the ListView
        mForecastAdapter = new ArrayAdapter<String>(
                // The current context, the fragment's parent activity
                getActivity(),
                // ID of the list item layout
                R.layout.list_item_forecast,
                // ID of the TextView to populate
                R.id.list_item_forecast_textview,
                // Forecast data
                new ArrayList<String>());

        // Inflates fragment and create root view
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Find list view by using its ID inside the rootView
        ListView listViewForecast = (ListView) rootView.findViewById(R.id.listview_forecast);
        // Set the adapter to the dummy data
        listViewForecast.setAdapter(mForecastAdapter);

        // Set click listener for items
        listViewForecast.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // Create explicit intent to call the DetailActivity
                        Intent detailActivityIntent = new Intent(getActivity(), DetailActivity.class);
                        // Set the forecast string inside of the intent
                        String forecastString = mForecastAdapter.getItem(position);
                        detailActivityIntent.putExtra(Intent.EXTRA_TEXT, forecastString);
                        startActivity(detailActivityIntent);
                    }
                }
        );

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Update weather on start
        updateWeather();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Use the id of the refresh menu item to
        // find out when it is selected
        int itemId = item.getItemId();
        if (itemId == R.id.action_refresh) {
            updateWeather();
            return true;
        } else if (itemId == R.id.action_view_location) {
            // Build URI for map intent
            Uri coordinatesUri = Uri.parse(String.format("geo:%f,%f", latitude, longitude));
            Log.v(LOG_TAG, "Coordinate URI: " + coordinatesUri.toString());

            Intent mapIntent = new Intent();
            mapIntent.setAction(Intent.ACTION_VIEW);
            mapIntent.setData(coordinatesUri);
            if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                makeToast("Error! Could not find an appropriate map application!");
                Log.e(LOG_TAG, "Could not find activity for map intent");
            }


        }
        // Call super method
        return super.onOptionsItemSelected(item);
    }

    /**
     * Updates the current weather with the location specified in the preferences
     */
    private void updateWeather() {
        // Get location preference, if not found, load default
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = preferences.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        // Get units string, if not found, load default
        Units units = Units.valueOf(preferences.getString(
                getString(R.string.pref_temp_units_key),
                getString(R.string.pref_temp_units_default)));
        // Get weather server
        WeatherServer weatherServer = WeatherServer.valueOf(
                preferences.getString(
                        getString(R.string.pref_weather_server_key),
                        getString(R.string.pref_weather_server_default)));

        // Execute task
        makeToast(String.format("Start downloading weather data from %s!", location));
        new FetchWeatherTask(getActivity(), mForecastAdapter)
                .execute(location,
                        units.toString(),
                        weatherServer.toString());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu we created that has the "Refresh"
        // menu option for debugging purposes
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    /**
     * Show a toast for a short time
     *
     * @param message the message to display
     */
    private void makeToast(String message) {
        Toast toast = Toast.makeText(
                getActivity(),
                message,
                Toast.LENGTH_SHORT);
        toast.show();
    }
}
