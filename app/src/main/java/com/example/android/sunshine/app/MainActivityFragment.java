package com.example.android.sunshine.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public MainActivityFragment() {
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
}
