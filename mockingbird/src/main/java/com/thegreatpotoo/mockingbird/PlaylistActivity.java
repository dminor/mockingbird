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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

    private BirdCodes birdCodes;
    private boolean useBirdCodes;

    private MockingbirdDatabase mockingbirdDatabase;

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

        mockingbirdDatabase = new MockingbirdDatabase(this);

        playlistPath = getIntent().getStringExtra("playlistPath");
        playlist = new Playlist(mockingbirdDatabase, playlistPath);

        currentlyPlaying = true;
        currentPosition = 0;
        updatePlayPauseState();

        ImageView playlistImageView = (ImageView) findViewById(R.id.playlistImageView);

        playlistImageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        currentlyPlaying = false;
                        updatePlayPauseState();
                    } else {
                        mediaPlayer.start();
                        currentlyPlaying = true;
                        updatePlayPauseState();
                    }
                }
            }
        });

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
            birdCodes = new BirdCodes();
            InputStream ins = getResources().openRawResource(
                    getResources().getIdentifier("birdcodes",
                            "raw", getPackageName()));
            birdCodes.read(ins);
        }

        if (savedInstanceState != null) {
            playlistPath = savedInstanceState.getString("playlistPath");
            playlist.restore(savedInstanceState);

            currentlyPlaying = savedInstanceState.getBoolean("currentlyPlaying");
            currentPosition = savedInstanceState.getInt("currentPosition");
            updatePlayPauseState();
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

        final Playlist.PlaylistSong song = playlist.currentSong();
        try {
            mediaPlayer.setDataSource(this, song.uri);
            mediaPlayer.prepare();
        } catch (IOException e) {
            return;
        }

        mediaPlayer.seekTo(currentPosition);

        if (currentlyPlaying) {
            mediaPlayer.start();
        }

        final ArrayList<String> choices = playlist.choicesForSong(song);
        final ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < choices.size(); ++i) {
            labels.add(useBirdCodes ? birdCodes.getCode(choices.get(i)) : choices.get(i));
        }

        final String songName = useBirdCodes ? birdCodes.getCode(song.prettifiedName) : song.prettifiedName;

        final TextView songNameTextView = (TextView) this.findViewById(com.thegreatpotoo.mockingbird.R.id.songName);
        if (songNameTextView != null) {
            songNameTextView.setText(songName);
            songNameTextView.setVisibility(View.INVISIBLE);
        }

        View.OnClickListener clickListener = new View.OnClickListener() {
            public void onClick(View v) {

                Button button = (Button) v;

                ImageView playlistImageView = (ImageView) findViewById(R.id.playlistImageView);
                final LayerDrawable ld = (LayerDrawable) playlistImageView.getDrawable();

                boolean correct = button.getText().equals(songName);

                int index = labels.indexOf(button.getText());
                String choice = choices.get(index);
                playlist.recordAnswer(song, choice, correct);

                if (correct) {
                    ld.getDrawable(1).setAlpha(0);
                    ld.getDrawable(2).setAlpha(0);
                    ld.getDrawable(3).setAlpha(255);
                    ld.getDrawable(4).setAlpha(0);

                    songNameTextView.setVisibility(View.INVISIBLE);
                } else {
                    ld.getDrawable(1).setAlpha(0);
                    ld.getDrawable(2).setAlpha(0);
                    ld.getDrawable(3).setAlpha(0);
                    ld.getDrawable(4).setAlpha(255);

                    songNameTextView.setVisibility(View.VISIBLE);
                }

                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }

                if (useTextToSpeech && textToSpeech != null) {
                    textToSpeech.speak(songName, TextToSpeech.QUEUE_FLUSH, null);
                }

                final Drawable drawableForAnimation = correct ? ld.getDrawable(3) : ld.getDrawable(4);

                ObjectAnimator fadeOut = ObjectAnimator.ofPropertyValuesHolder(drawableForAnimation, PropertyValuesHolder.ofInt("alpha", 0));
                fadeOut.setTarget(drawableForAnimation);
                fadeOut.setDuration(1500);
                fadeOut.start();

                if (!correct) {
                    final Animation songNameFadeOut = new AlphaAnimation(1.0f, 0.0f);
                    songNameFadeOut.setDuration(1400);
                    songNameTextView.startAnimation(songNameFadeOut);
                    songNameFadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            songNameTextView.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                }

                fadeOut.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        playlist.nextSong();
                        currentlyPlaying = true;
                        currentPosition = 0;
                        playSong();
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });

            }
        };

        final Button firstButton = (Button) this.findViewById(R.id.firstButton);
        firstButton.setText(labels.get(0));
        firstButton.setOnClickListener(clickListener);

        final Button secondButton = (Button) this.findViewById(R.id.secondButton);
        secondButton.setOnClickListener(clickListener);
        if (choices.size() >= 2) {
            secondButton.setVisibility(View.VISIBLE);
            secondButton.setText(labels.get(1));
        } else {
            secondButton.setVisibility(View.INVISIBLE);
        }

        final Button thirdButton = (Button) this.findViewById(R.id.thirdButton);
        thirdButton.setOnClickListener(clickListener);
        if (choices.size() == 3) {
            thirdButton.setVisibility(View.VISIBLE);
            thirdButton.setText(labels.get(2));
        } else {
            thirdButton.setVisibility(View.INVISIBLE);
        }

        final TextView currentStreakTextView = (TextView) this.findViewById(R.id.currentStreakTextView);
        if (playlist.getCurrentStreak() > 0) {
            currentStreakTextView.setVisibility(View.VISIBLE);
            currentStreakTextView.setText(String.format(getResources().getString(R.string.in_a_row), playlist.getCurrentStreak()));
        } else {
            currentStreakTextView.setVisibility(View.INVISIBLE);
        }

        updatePlayPauseState();

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                currentlyPlaying = false;
                currentPosition = 0;
                updatePlayPauseState();
            }
        });
    }

    private void updatePlayPauseState() {
        ImageView playlistImageView = (ImageView) findViewById(R.id.playlistImageView);
        final LayerDrawable ld = (LayerDrawable) playlistImageView.getDrawable();

        if (currentlyPlaying) {
            ld.getDrawable(1).setAlpha(255);
            ld.getDrawable(2).setAlpha(0);
            playlistImageView.setContentDescription(getString(R.string.pause_label));
        } else {
            ld.getDrawable(1).setAlpha(0);
            ld.getDrawable(2).setAlpha(255);
            playlistImageView.setContentDescription(getString(R.string.play_label));
        }

        ld.getDrawable(3).setAlpha(0);
        ld.getDrawable(4).setAlpha(0);
    }
}
