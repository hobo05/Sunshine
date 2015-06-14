package com.example.android.sunshine.app;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Tim on 6/14/2015.
 */
public class WeatherDataParser {

    private static final String LOG_TAG = WeatherDataParser.class.getSimpleName();

    /**
     * Given a string of the form returned by the api call:
     * http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7
     * retrieve the maximum temperature for the day indicated by dayIndex
     * (Note: 0-indexed, so 0 would refer to the first day).
     */
    public static double getMaxTemperatureForDay(String weatherJsonStr, int dayIndex)
            throws JSONException {

        JSONObject weatherJson = new JSONObject(weatherJsonStr);
        // Print out JSON string
        Log.v(LOG_TAG, weatherJsonStr);

        // Get the weather on that day
        JSONArray daysArray = weatherJson.getJSONArray("list");
        JSONObject dayInfo = daysArray.getJSONObject(dayIndex);

        // Get the max temperature
        JSONObject tempInfo = dayInfo.getJSONObject("temp");
        Double maxTemp = tempInfo.getDouble("max");

        return maxTemp;
    }

}
