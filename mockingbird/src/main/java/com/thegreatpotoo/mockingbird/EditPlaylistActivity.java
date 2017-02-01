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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.io.IOException;

public class EditPlaylistActivity extends AppCompatActivity
        implements AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mediaPlayer;
    private Playlist playlist;
    private int selectedSong;
    private ArrayAdapter<Playlist.PlaylistSong> adapter;

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
        setContentView(R.layout.activity_edit_playlist);

        String playlistPath = getIntent().getStringExtra("playlistPath");
        playlist = new Playlist(this, playlistPath);
        playlist.indexSongs(new Playlist.OnSongsIndexedListener() {
            @Override
            public void onSongsIndexed() {

            }
        });

        final ListView songsListView = (ListView) findViewById(R.id.songsListView);
        if (songsListView == null) {
            return;
        }

        selectedSong = -1;
        adapter = new ArrayAdapter<Playlist.PlaylistSong>(this, android.R.layout.simple_list_item_1, playlist.getSongs());
        songsListView.setAdapter(adapter);
        songsListView.setSelector(android.R.color.darker_gray);

        final Button playPauseButton = (Button) findViewById(R.id.editPlaylistPlayPauseButton);
        songsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                selectedSong = position;

                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer = null;
                    playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.play_label);
                }
            }
        });

        final Button renamePlaylistButton = (Button) findViewById(R.id.renamePlaylistButton);
        final EditText playlistNameEditText = (EditText) findViewById(R.id.playlistNameEditText);

        if (renamePlaylistButton != null && playlistNameEditText != null) {
            playlistNameEditText.setText(playlist.getName());

            renamePlaylistButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String searchText = playlistNameEditText.getText().toString();
                    playlist.rename(searchText);
                    playlistNameEditText.setText(playlist.getName());
                }
            });
        }

        final Intent downloadsIntent = new Intent(this, DownloadsActivity.class);
        final Button addFromDownloadsButton = (Button) findViewById(R.id.addFromDownloadsButton);
        if (addFromDownloadsButton != null) {
            addFromDownloadsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    downloadsIntent.putExtra("playlistPath", playlist.getPlaylistPath());
                    startActivity(downloadsIntent);
                }
            });
        }

        final Intent xenoCantoIntent = new Intent(this, XenoCantoActivity.class);
        final Button addFromXenoCantoButton = (Button) findViewById(R.id.addFromXenoCantoButton);
        if (addFromXenoCantoButton != null) {
            addFromXenoCantoButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    xenoCantoIntent.putExtra("playlistPath", playlist.getPlaylistPath());
                    startActivity(xenoCantoIntent);
                }
            });
        }

        final Context context = this;
        final Button deleteSongButton = (Button) findViewById(R.id.editPlaylistDeleteButton);
        if (deleteSongButton != null) {
            deleteSongButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    new AlertDialog.Builder(context)
                            .setMessage(R.string.delete_song_message)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    playlist.deleteSong(selectedSong);
                                    adapter.notifyDataSetChanged();
                                    selectedSong = -1;

                                    if (mediaPlayer != null) {
                                        mediaPlayer.stop();
                                        mediaPlayer = null;
                                        playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.play_label);
                                    }
                                }})
                            .setNegativeButton(android.R.string.no, null).show();
                }
            });
        }

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
                    } else if (selectedSong != -1) {
                        playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.pause_label);
                        playSong();
                    }
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

                Button playPauseButton = (Button) findViewById(R.id.editPlaylistPlayPauseButton);
                if (playPauseButton != null) {
                    playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.play_label);
                }
            }

            mediaPlayer.release();
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
        }
    }

    private void playSong() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        if (selectedSong == -1) {
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

        Playlist.PlaylistSong song = playlist.getSong(selectedSong);
        try {
            mediaPlayer.setDataSource(this, song.uri);
            mediaPlayer.prepare();
        } catch (IOException e) {
            return;
        }

        mediaPlayer.start();

        final Button playPauseButton = (Button) findViewById(R.id.editPlaylistPlayPauseButton);
        if (playPauseButton != null) {
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.play_label);
                }
            });
        }
    }
}
