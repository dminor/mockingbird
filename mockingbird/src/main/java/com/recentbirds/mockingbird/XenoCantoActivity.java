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

package com.recentbirds.mockingbird;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

public class XenoCantoActivity extends AppCompatActivity
        implements AudioManager.OnAudioFocusChangeListener {

    private String QUERY_URL = "http://www.xeno-canto.org/api/2/recordings?query=";

    class SearchResult {
        String id;
        String name;
        String file;
        String loc;
        String date;

        public SearchResult(JSONObject json) {
            try {
                id = json.getString("id");
                name = json.getString("en");
                file = json.getString("file");
                loc = json.getString("loc");
                date = json.getString("date");
            } catch (JSONException e) {

            }
        }

        public String toString() {
            return name + " (" + loc + ", " + date + ")";
        }
    }

    private ArrayList<SearchResult> searchResults;
    private int currentSearchResult;
    private ArrayAdapter<SearchResult> adapter;

    private MediaPlayer mediaPlayer;
    private String playlistPath;

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer == null) {
                    playSong();
                } else if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                } else {
                    mediaPlayer.setVolume(1.0f, 1.0f);
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.setVolume(0.1f, 0.1f);
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xeno_canto);

        playlistPath = getIntent().getStringExtra("playlistPath");

        final ListView searchResultListView = (ListView) findViewById(R.id.searchResultListView);
        if (searchResultListView == null) {
            return;
        }

        searchResults = new ArrayList<SearchResult>();
        currentSearchResult = -1;
        adapter = new ArrayAdapter<SearchResult>(this, android.R.layout.simple_list_item_1, searchResults);
        searchResultListView.setAdapter(adapter);
        searchResultListView.setSelector(android.R.color.darker_gray);

        final Button playPauseButton = (Button) findViewById(R.id.xenoCantoPlayPauseButton);
        if (playPauseButton != null) {
            searchResultListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    Object o = searchResultListView.getItemAtPosition(position);
                    currentSearchResult = position;

                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer = null;
                        playPauseButton.setText(com.recentbirds.mockingbird.R.string.play_label);
                    }
                }
            });
        }

        final Context context = this;
        class SearchXenoCantoTask extends AsyncTask<String, Void, Integer> {
            protected Integer doInBackground(String... searchTerms) {

                String searchTerm;
                try {
                    searchTerm = URLEncoder.encode(searchTerms[0], "UTF-8");
                } catch(UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return -1;
                }

                String result = null;
                try {
                    URL url = new URL(QUERY_URL + searchTerm);
                    URLConnection conn = url.openConnection();
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
                    StringBuilder sb = new StringBuilder();

                    String line = null;
                    while ((line = reader.readLine()) != null)
                    {
                        sb.append(line + "\n");
                    }
                    result = sb.toString();
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                } finally {

                }

                if (result != null) {
                    try {
                        JSONObject json = new JSONObject(result);
                        JSONArray recordings = json.getJSONArray("recordings");
                        searchResults.clear();
                        currentSearchResult = -1;
                        for (int i = 0; i < recordings.length(); ++i)  {
                            JSONObject recording = recordings.getJSONObject(i);
                            searchResults.add(new SearchResult(recording));
                        }
                    } catch (JSONException e) {
                        return -1;
                    }
                }

                return 0;
            }

            protected void onPostExecute(Integer result) {
                if (result == 0) {
                    adapter.notifyDataSetChanged();
                }
            }
        }

        final Button searchButton = (Button) findViewById(R.id.searchButton);
        final EditText searchEditText = (EditText) findViewById(R.id.searchEditText);

        if (searchButton != null && searchEditText != null) {
            searchButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String searchText = searchEditText.getText().toString();
                    new SearchXenoCantoTask().execute(searchText);
                }
            });
        }

        if (playPauseButton != null) {
            playPauseButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mediaPlayer != null) {
                        if (mediaPlayer.isPlaying()) {
                            playPauseButton.setText(com.recentbirds.mockingbird.R.string.play_label);
                            mediaPlayer.pause();
                        } else {
                            playPauseButton.setText(com.recentbirds.mockingbird.R.string.pause_label);
                            mediaPlayer.start();
                        }
                    } else {
                        playSong();
                        playPauseButton.setText(com.recentbirds.mockingbird.R.string.pause_label);
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
    public void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        playSong();
    }

    @Override
    public void onStop() {
        super.onStop();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
    }

    private void addToPlaylist() {
        if (currentSearchResult == -1) {
            return;
        }

        class DownloadTask extends AsyncTask<SearchResult, Void, Integer> {
            protected Integer doInBackground(SearchResult... searchResults) {

                SearchResult sr = searchResults[0];

                try {
                    URL url = new URL(sr.file);
                    java.io.BufferedInputStream in = new java.io.BufferedInputStream(url.openStream());

                    java.io.FileOutputStream fos = new java.io.FileOutputStream(playlistPath + "/" + sr.name + "-xc" + sr.id + ".mp3");
                    java.io.BufferedOutputStream out = new BufferedOutputStream(fos, 2048);
                    byte[] data = new byte[2048];
                    int c = 0;
                    while((c = in.read(data, 0, 2048)) >= 0) {
                        out.write(data, 0, c);
                    }
                    fos.close();
                    out.close();
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                } finally {

                }

                return 0;
            }

            protected void onPostExecute(Integer result) {
                if (result == 0) {
                }
            }
        }

        new DownloadTask().execute(searchResults.get(currentSearchResult));
    }

    private void playSong() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        if (currentSearchResult == -1) {
            return;
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

        SearchResult sr = searchResults.get(currentSearchResult);
        Uri song = Uri.parse(sr.file);
        try {
            mediaPlayer.setDataSource(this, song);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            return;
        }

        final Button playPauseButton = (Button) findViewById(R.id.xenoCantoPlayPauseButton);
        if (playPauseButton != null) {
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    playPauseButton.setText(com.recentbirds.mockingbird.R.string.play_label);
                }
            });
        }
    }
}
