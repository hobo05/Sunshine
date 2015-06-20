/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DetailActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Start intent for settings
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private ShareActionProvider mShareActionProvider;

        private String forecastString = null;

        public PlaceholderFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Indicate that this activity has a menu
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.detailfragment, menu);

            // Locate MenuItem with ShareActionProvider
            MenuItem item = menu.findItem(R.id.action_share);

            // Get the provider and hold onto it to set/change the share intent.
            // We can't directly cast it because we're using the support library and
            // we have to use the MenuItemCompat class
            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

            if (mShareActionProvider != null) {
                Intent shareIntent = createShareIntent();

                // Set the share intent into the ShareActionProvider
                setShareIntent(shareIntent);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

            // Retrieve the EXTRA_TEXT from the intent and set it
            // to the text view
            Intent intent = getActivity().getIntent();
            TextView weatherDetailView = (TextView) rootView.findViewById(R.id.fragment_detail_textview);
            if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
                // Retrieve the weather string and set it to the detail view
                forecastString = intent.getStringExtra(Intent.EXTRA_TEXT);
                weatherDetailView.setText(forecastString);
            }

            return rootView;
        }

        /**
         * Create a share {@link Intent} with the forecast string set inside
         * @return
         */
        private Intent createShareIntent() {
            // Also create a share intent
            Intent shareIntent = new Intent();
            // When context switching, makes sure that you will
            // always switch back to your own app, instead of the one that is called by the intent
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    String.format("%s #SunshineApp", forecastString));
            return shareIntent;
        }

        // Call to update the share intent
        private void setShareIntent(Intent shareIntent) {
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(shareIntent);
            }
        }
    }
}