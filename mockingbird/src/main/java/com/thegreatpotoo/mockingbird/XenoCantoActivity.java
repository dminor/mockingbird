/*  Mockingbird
    Copyright (C) 2016 Daniel Minor

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.thegreatpotoo.mockingbird;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

public class XenoCantoActivity extends MockingbirdAudioActivity {

    private ArrayList<XenoCanto.SearchResult> searchResults;
    private int currentSearchResult;
    private ArrayAdapter<XenoCanto.SearchResult> searchAdapter;

    private XenoCantoSuggestionsAdapter suggestionsAdapter;

    private String playlistPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xeno_canto);

        currentlyPlaying = true;

        playlistPath = getIntent().getStringExtra("playlistPath");

        final ListView searchResultListView = (ListView) findViewById(R.id.searchResultListView);
        if (searchResultListView == null) {
            return;
        }

        searchResults = new ArrayList<XenoCanto.SearchResult>();
        currentSearchResult = -1;
        searchAdapter = new ArrayAdapter<XenoCanto.SearchResult>(this, android.R.layout.simple_list_item_1, searchResults);
        searchResultListView.setAdapter(searchAdapter);
        searchResultListView.setSelector(android.R.color.darker_gray);

        final Button playPauseButton = (Button) findViewById(R.id.xenoCantoPlayPauseButton);
        if (playPauseButton != null) {
            searchResultListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    Object o = searchResultListView.getItemAtPosition(position);
                    currentSearchResult = position;

                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                        mediaPlayer = null;
                        playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.play_label);
                    }
                }
            });
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final String searchQuality = sharedPref.getString("pref_xenocanto_quality", "q:A");

        class SearchXenoCantoTask extends AsyncTask<String, Void, ArrayList<XenoCanto.SearchResult>> {
            protected ArrayList<XenoCanto.SearchResult> doInBackground(String... searchTerms) {

                String searchTerm;
                try {
                    searchTerm = URLEncoder.encode(searchTerms[0] + " " + searchQuality, "UTF-8");
                } catch(UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return new ArrayList<XenoCanto.SearchResult>();
                }

                currentSearchResult = -1;


                return XenoCanto.getInstance().search(searchTerm);
            }

            protected void onPostExecute(ArrayList<XenoCanto.SearchResult> results) {
                searchResults.clear();
                searchResults.addAll(results);
                searchAdapter.notifyDataSetChanged();
                searchResultListView.setAdapter(searchAdapter);
            }
        }

        final Button searchButton = (Button) findViewById(R.id.searchButton);
        final AutoCompleteTextView searchEditText = (AutoCompleteTextView) findViewById(R.id.searchEditText);

        if (searchButton != null && searchEditText != null) {
            searchButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                        mediaPlayer = null;
                        playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.play_label);
                    }

                    String searchText = searchEditText.getText().toString();
                    new SearchXenoCantoTask().execute(searchText);
                }
            });
        }

        suggestionsAdapter = new XenoCantoSuggestionsAdapter(this, android.R.layout.simple_dropdown_item_1line);
        searchEditText.setAdapter(suggestionsAdapter);
        searchEditText.setThreshold(4);

        searchEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                suggestionsAdapter.setSearchString(s.toString());
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        if (playPauseButton != null) {
            playPauseButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mediaPlayer != null) {
                        if (mediaPlayer.isPlaying()) {
                            playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.play_label);
                            mediaPlayer.pause();
                        } else {
                            playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.pause_label);
                            mediaPlayer.start();
                        }
                    } else {
                        playSong();
                        playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.pause_label);
                    }
                }
            });
        }

        final Button addToPlaylistButton = (Button) findViewById(R.id.addToPlaylistButton);
        if (addToPlaylistButton != null) {
            addToPlaylistButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    addToPlaylist();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        playSong();
    }

    private void addToPlaylist() {
        if (currentSearchResult == -1) {
            return;
        }

        XenoCanto.SearchResult sr = searchResults.get(currentSearchResult);
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(sr.file));
        request.setDestinationUri(Uri.parse("file://" + playlistPath + "/" + sr.name + "-xc" + sr.id + ".mp3"));
        dm.enqueue(request);
    }

    protected boolean playSong() {
        if (!super.playSong()) {
            return false;
        }

        if (currentSearchResult == -1) {
            return false;
        }

        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        } else {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
        }

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
            }
        });

        XenoCanto.SearchResult sr = searchResults.get(currentSearchResult);
        Uri song = Uri.parse(sr.file);
        try {
            mediaPlayer.setDataSource(this, song);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            return false;
        }

        final Button playPauseButton = (Button) findViewById(R.id.xenoCantoPlayPauseButton);
        if (playPauseButton != null) {
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.play_label);
                }
            });
        }

        return true;
    }
}
