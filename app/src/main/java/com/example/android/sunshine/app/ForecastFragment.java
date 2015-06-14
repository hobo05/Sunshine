package com.example.android.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

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

        List<String> weatherDataList = new ArrayList<String>();
        weatherDataList.add("Today - Sunny - 88/63");
        weatherDataList.add("Tomorrow - Foggy - 70/46");
        weatherDataList.add("Weds - Cloudy - 72/63");
        weatherDataList.add("Thurs - Rainy - 64/51");
        weatherDataList.add("Fri - Foggy - 70/46");
        weatherDataList.add("Sat - Sunny - 76/68");

        // Duplicated list to see scrolling effect
        weatherDataList.addAll(Arrays.asList(weatherDataList.toArray(new String[weatherDataList.size()])));

        // Create an ArrayAdapter that will
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                // The current context, the fragment's parent activity
                getActivity(),
                // ID of the list item layout
                R.layout.list_item_forecast,
                // ID of the TextView to populate
                R.id.list_item_forecast_textview,
                // Forecast data
                weatherDataList);

        // Inflates fragment and create root view
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Find list view by using its ID inside the rootView
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        // Set the adapter to the ListView
        listView.setAdapter(adapter);

        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Use the id of the refresh menu item to
        // find out when it is selected
        int itemId = item.getItemId();
        if (itemId == R.id.action_refresh) {
            // Execute task
            makeToast("Start downloading weather data!");
            new FetchWeatherTask("viladecans").execute();
            return true;
        }
        // Call super method
        return super.onOptionsItemSelected(item);
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
     * @param message
     */
    private void makeToast(String message) {
        Toast toast = Toast.makeText(
                getActivity().getApplicationContext(),
                message,
                Toast.LENGTH_SHORT);
        toast.show();
    }

    private class FetchWeatherTask extends AsyncTask<Void, Void, String> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        // Constants for query parameters of the weather api
        public static final String PARAM_QUERY = "q";
        public static final String PARAM_MODE = "mode";
        public static final String PARAM_UNIT = "units";
        public static final String PARAM_DAYS = "cnt";

        private String query;

        /**
         * Constructor that takes any query for the weather api
         *
         * @param query
         */
        public FetchWeatherTask(String query) {
            this.query = query;
        }

        @Override
        protected String doInBackground(Void... params) {
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
                Uri.Builder builder = new Uri.Builder();
                Uri weatherUri = Uri.parse("http://api.openweathermap.org/data/2.5")
                        .buildUpon()
                        .appendPath("forecast")
                        .appendPath("daily")
                        .appendQueryParameter(PARAM_QUERY, query)
                        .appendQueryParameter(PARAM_MODE, "json")
                        .appendQueryParameter(PARAM_UNIT, "metric")
                        .appendQueryParameter(PARAM_DAYS, Integer.toString(7))
                        .build();

                URL url = new URL(weatherUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
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
                    buffer.append(line + "\n");
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

            return forecastJsonStr;
        }

        @Override
        protected void onPostExecute(String jsonString) {
            makeToast("Finished downloading weather data");
            Log.v(LOG_TAG, "Forecast json string: " + jsonString);
//            JsonReader reader = new JsonReader(new StringReader(jsonString));
//            try {
//                List<String> weatherData = readWeatherJson(reader);
//            } catch (IOException e) {
//                Log.e(getClass().getName(), "Failed to read json from weather api: ", e);
//            }finally{
//                try {
//                    reader.close();
//                } catch (IOException e) {
//                    Log.e(getClass().getName(), "Failed to close JsonReader: ", e);
//                }
//            }
        }

        /**
         * Read the json object returned from the {@link http://openweathermap.org}
         *
         * @param reader
         * @return
         * @throws IOException
         */
//        private List<String> readWeatherJson(JsonReader reader) throws IOException {
//            List<String> weatherData = null;
//
//            // Reader json object
//            reader.beginObject();
//            while(reader.hasNext()) {
//                String fieldName = reader.nextName();
//                if (fieldName.equals("list") && reader.peek() != JsonToken.NULL) {
//
//                } else
//                    reader.skipValue();
//            }
//            reader.endObject();
//
//            return weatherData;
//        }
    }
}
