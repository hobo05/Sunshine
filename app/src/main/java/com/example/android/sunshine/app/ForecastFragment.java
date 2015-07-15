package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;


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
        new FetchWeatherTask(weatherServer, location, units).execute();
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

    /**
     * Enum representing the units for the weather
     */
    private enum Units {
        metric,
        imperial
    }

    private enum WeatherServer {
        OPEN_WEATHER_API,
        TEST_SERVER
    }

    private class FetchWeatherTask extends AsyncTask<Void, Void, String[]> {

        public static final int DAYS = 7;
        public static final String APPID = "8701f91a0201cb3d1fc11382f5ae32b7";

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        // Constants for query parameters of the weather api
        public static final String PARAM_QUERY = "q";
        public static final String PARAM_MODE = "mode";
        public static final String PARAM_UNIT = "units";
        public static final String PARAM_DAYS = "cnt";
        public static final String PARAM_APPID = "APPID";

        private WeatherServer weatherServer;
        private String query;
        private Units units;

        /**
         * Constructor that takes any query for the weather api
         *
         * @param weatherServer Server to get weather data from
         * @param query the query to use when fetching the weather data
         * @param units the units for the weather temperature
         */
        public FetchWeatherTask(WeatherServer weatherServer, String query, Units units) {
            this.weatherServer = weatherServer;
            this.query = query;
            this.units = units;
        }

        @Override
        protected String[] doInBackground(Void... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                // TODO remove after testing
                String baseUrl = null;
                switch (weatherServer) {
                    case OPEN_WEATHER_API:
                        baseUrl = "http://api.openweathermap.org";
                        break;
                    case TEST_SERVER:
                        baseUrl = "http://192.168.0.104";
                    default:
                }

                Uri weatherUri = Uri.parse(baseUrl)
                        .buildUpon()
                        .appendPath("data")
                        .appendPath("2.5")
                        .appendPath("forecast")
                        .appendPath("daily")
                        .appendQueryParameter(PARAM_QUERY, query)
                        .appendQueryParameter(PARAM_MODE, "json")
                        .appendQueryParameter(PARAM_UNIT, "metric")
                        .appendQueryParameter(PARAM_DAYS, Integer.toString(DAYS))
                        .appendQueryParameter(PARAM_APPID, APPID)
                        .build();

                URL url = new URL(weatherUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line).append("\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            // Parse the weather data into an array of strings
            // that describe each day's weather
            String[] weatherDataArray = null;
            try {
                weatherDataArray = getWeatherDataFromJson(forecastJsonStr, DAYS);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error while parsing weather data from json: ", e);
            }

            return weatherDataArray;
        }

        @Override
        protected void onPostExecute(String[] weatherDataArray) {
            if (weatherDataArray == null) {
                makeToast("Error parsing the weather data!");
                return;
            }

            // Clear the old weather data and set the new data
            // inside the adapter
            mForecastAdapter.clear();
            for (String curWeather : weatherDataArray) {
                mForecastAdapter.add(curWeather);
            }

            makeToast("Updated the weather data successfully!");
        }

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd", Locale.US);
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            return roundedHigh + "/" + roundedLow;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";
            final String OWM_CITY = "city";
            final String OWM_COORDINATES = "coord";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);

            // Set the coordinates so we can use it for our map intent
            JSONObject coordinateJson = forecastJson
                    .getJSONObject(OWM_CITY)
                    .getJSONObject(OWM_COORDINATES);
            latitude = coordinateJson.getDouble("lat");
            longitude = coordinateJson.getDouble("lon");

            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                // Change to imperial units if specified
                if (this.units == Units.imperial) {
                    high = convertToFahrenheit(high);
                    low = convertToFahrenheit(low);
                }

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            return resultStrs;

        }

        /**
         * Covnerts Fahrenheit to Celsius
         *
         * @param celsius the given celsius
         * @return the resulting Fahrenheit
         */
        private double convertToFahrenheit(double celsius) {
            return celsius * 1.8 + 32;
        }
    }
}
