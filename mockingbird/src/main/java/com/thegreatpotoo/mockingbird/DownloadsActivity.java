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

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DownloadsActivity extends AppCompatActivity
    implements AudioManager.OnAudioFocusChangeListener {
    private MediaPlayer mediaPlayer;
    private Playlist downloads;
    private String downloadsPath;
    private Playlist playlist;
    private String playlistPath;
    private int selectedFile;
    private ArrayAdapter<String> adapter;

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
        setContentView(R.layout.activity_downloads);

        playlistPath = getIntent().getStringExtra("playlistPath");
        playlist = new Playlist(this, playlistPath);

        downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        downloads = new Playlist(this, downloadsPath);
        downloads.indexSongs(new Playlist.OnSongsIndexedListener() {
            @Override
            public void onSongsIndexed() {

            }
        });

        final ListView downloadsListView = (ListView) findViewById(R.id.downloadsListView);
        if (downloadsListView == null) {
            return;
        }

        selectedFile = -1;
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, downloads.getSongs());
        downloadsListView.setAdapter(adapter);
        downloadsListView.setSelector(android.R.color.darker_gray);

        final Button playPauseButton = (Button) findViewById(R.id.downloadsPlayButton);
        downloadsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                selectedFile = position;

                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer = null;
                    playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.play_label);
                }
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
                        playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.pause_label);
                        playSong();
                    }
                }
            });
        }

        final Button addToPlaylistButton = (Button) findViewById(R.id.downloadsAddToPlaylistButton);
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
        playlist.indexSongs(new Playlist.OnSongsIndexedListener() {
            @Override
            public void onSongsIndexed() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void addToPlaylist() {
        if (selectedFile == -1) {
            return;
        }

        String fileName = downloads.getSong(selectedFile);
        File src = new File(downloadsPath + "/" + fileName);
        File dest = new File(playlistPath + "/" + fileName);
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (IOException ex) {

        }

        playlist.indexSongs(new Playlist.OnSongsIndexedListener() {
            @Override
            public void onSongsIndexed() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void playSong() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        if (selectedFile == -1) {
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

        String fileName = downloads.getSong(selectedFile);
        Uri song = Uri.parse(downloads.getPlaylistPath() + "/" + fileName);
        ;
        try {
            mediaPlayer.setDataSource(this, song);
            mediaPlayer.prepare();
        } catch (IOException e) {
            return;
        }

        mediaPlayer.start();

        final Button playPauseButton = (Button) findViewById(R.id.downloadsAddToPlaylistButton);
        if (playPauseButton != null) {
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.play_label);
                }
            });
        }
    }
}
