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
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;

public class PlaylistActivity extends AppCompatActivity
        implements AudioManager.OnAudioFocusChangeListener {

    private Playlist playlist;

    private MediaPlayer mediaPlayer;
    private String playlistPath;
    private boolean currentlyPlaying;
    private int currentPosition;

    private TextToSpeech textToSpeech;
    private boolean useTextToSpeech;

    private HashMap<String, String> birdcodes;
    private boolean useBirdCodes;

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (currentlyPlaying) {
                    if (mediaPlayer == null) {
                        playSong();
                    } else if (!mediaPlayer.isPlaying()) {
                        mediaPlayer.start();
                    } else {
                        mediaPlayer.setVolume(1.0f, 1.0f);
                    }
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
        setContentView(com.thegreatpotoo.mockingbird.R.layout.activity_playlist);

        playlistPath = getIntent().getStringExtra("playlistPath");
        playlist = new Playlist(this, playlistPath);

        currentlyPlaying = true;
        currentPosition = 0;

        TextView playlistName = (TextView) findViewById(R.id.playlistName);
        if (playlistName != null) {
            playlistName.setText(playlist.getName());
        }

        final Button playPauseButton = (Button) findViewById(com.thegreatpotoo.mockingbird.R.id.playPauseButton);
        if (playPauseButton != null) {
            playPauseButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mediaPlayer != null) {
                        if (mediaPlayer.isPlaying()) {
                            playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.play_label);
                            mediaPlayer.pause();
                            currentlyPlaying = false;
                        } else {
                            playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.pause_label);
                            mediaPlayer.start();
                            currentlyPlaying = true;
                        }
                    }
                }
            });
        }

        final TextView songName = (TextView) this.findViewById(com.thegreatpotoo.mockingbird.R.id.songName);
        final Button showAnswerButton = (Button) findViewById(com.thegreatpotoo.mockingbird.R.id.showAnswerButton);
        if (songName != null && showAnswerButton != null) {
            showAnswerButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (songName.getVisibility() == View.VISIBLE) {
                        songName.setVisibility(View.INVISIBLE);
                    } else {
                        songName.setVisibility(View.VISIBLE);
                        if (textToSpeech != null && useTextToSpeech) {
                            textToSpeech.speak(songName.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
                        }
                    }
                }
            });
        }

        final Button nextButton = (Button) findViewById(com.thegreatpotoo.mockingbird.R.id.nextButton);
        if (songName != null && nextButton != null) {
            nextButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (songName.getAnimation() != null) {
                        // Prevent multiple presses while animation is running
                        return;
                    }

                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }

                    if (useTextToSpeech && textToSpeech != null && songName.getVisibility() != View.VISIBLE) {
                        textToSpeech.speak(songName.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
                    }

                    songName.setVisibility(View.VISIBLE);
                    final Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                    fadeOut.setDuration(1000);
                    songName.startAnimation(fadeOut);
                    fadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            songName.setVisibility(View.INVISIBLE);
                            playlist.nextSong();
                            currentlyPlaying = true;
                            currentPosition = 0;
                            playSong();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                }
            });
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        useTextToSpeech = sharedPref.getBoolean("pref_say_answers", false);
        if (useTextToSpeech) {
            textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        int result = textToSpeech.setLanguage(Locale.US);
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            useTextToSpeech = false;
                        }
                    }
                }
            });
        }

        useBirdCodes = sharedPref.getBoolean("pref_use_birdcodes", false);
        if (useBirdCodes) {
            readBirdCodes();
        }

        if (savedInstanceState != null) {
            playlistPath = savedInstanceState.getString("playlistPath");
            playlist.restore(savedInstanceState);

            if (songName != null) {
                songName.setVisibility(savedInstanceState.getInt("songNameVisibility"));
            }

            currentlyPlaying = savedInstanceState.getBoolean("currentlyPlaying");
            if (playPauseButton != null) {
                if (currentlyPlaying) {
                    playPauseButton.setText(R.string.pause_label);
                } else {
                    playPauseButton.setText(R.string.play_label);
                }
            }
            currentPosition = savedInstanceState.getInt("currentPosition");
            playSong();
        } else {
            playlist.indexSongs(new Playlist.OnSongsIndexedListener() {
                @Override
                public void onSongsIndexed() {
                    playlist.shuffle();
                    currentlyPlaying = true;
                    playSong();
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                currentPosition = mediaPlayer.getCurrentPosition();
                mediaPlayer.stop();
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                audioManager.abandonAudioFocus(this);
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
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        super.onStop();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        playlist.save(savedInstanceState);
        savedInstanceState.putBoolean("currentlyPlaying", currentlyPlaying);
        savedInstanceState.putInt("currentPosition", currentPosition);

        TextView textView = (TextView) this.findViewById(com.thegreatpotoo.mockingbird.R.id.songName);
        if (textView != null) {
            savedInstanceState.putInt("songNameVisibility", textView.getVisibility());
        }
    }

    private void playSong() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        if (!playlist.hasSongs()) {
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

        String fileName = playlist.currentSong();
        Uri song = Uri.parse(playlistPath + "/" + fileName);
        try {
            mediaPlayer.setDataSource(this, song);
            mediaPlayer.prepare();
        } catch (IOException e) {
            return;
        }

        mediaPlayer.seekTo(currentPosition);

        if (currentlyPlaying) {
            mediaPlayer.start();
        }

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(this, song);
        String songName = playlist.prettifySongName(song, fileName);
        if (useBirdCodes) {
            String codedName = birdcodes.get(songName.toLowerCase());
            if (codedName != null) {
                songName = codedName;
            }
        }

        final TextView textView = (TextView) this.findViewById(com.thegreatpotoo.mockingbird.R.id.songName);
        if (textView != null) {
            textView.setText(songName);
        }

        final Button playPauseButton = (Button) findViewById(com.thegreatpotoo.mockingbird.R.id.playPauseButton);
        if (playPauseButton != null) {
            if (currentlyPlaying) {
                playPauseButton.setText(R.string.pause_label);
            } else {
                playPauseButton.setText(R.string.play_label);
            }

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    playPauseButton.setText(com.thegreatpotoo.mockingbird.R.string.play_label);
                    currentlyPlaying = false;
                    currentPosition = 0;
                }
            });
        }
    }

    private void readBirdCodes() {
        birdcodes = new HashMap<String, String>();
        InputStream ins = getResources().openRawResource(
                getResources().getIdentifier("birdcodes",
                        "raw", getPackageName()));
        BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",");
                if (row.length >= 2) {
                    birdcodes.put(row[1].toLowerCase(), row[0]);
                }
            }
        } catch (IOException e) {

        } finally {
            try {
                ins.close();
            } catch (IOException e) {

            }
        }
    }
}
