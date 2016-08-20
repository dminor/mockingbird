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

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class PlaylistActivity extends AppCompatActivity {

    private int REQUEST_PERMISSION = 42;
    private Random random = new Random();

    private MediaPlayer mediaPlayer;
    private String playlistPath;
    private ArrayList<String> playlistImages;
    private int currentImage;
    private ArrayList<String> playlistSongs;
    private int currentSong;
    private boolean currentlyPlaying;
    private int currentPosition;

    private TextToSpeech textToSpeech;
    private boolean useTextToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.recentbirds.mockingbird.R.layout.activity_playlist);

        playlistPath =  getIntent().getStringExtra("playlistPath");
        playlistImages = new ArrayList<String>();
        playlistSongs = new ArrayList<String>();

        currentlyPlaying = true;
        currentPosition = 0;

        TextView playlistName = (TextView) findViewById(R.id.playlistName);
        if (playlistName != null) {
            String name = playlistPath.substring(playlistPath.lastIndexOf('/') + 1);
            playlistName.setText(name);
        }

        final Button playPauseButton = (Button) findViewById(com.recentbirds.mockingbird.R.id.playPauseButton);
        if (playPauseButton != null) {
            playPauseButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mediaPlayer != null) {
                        if (mediaPlayer.isPlaying()) {
                            playPauseButton.setText(com.recentbirds.mockingbird.R.string.play_label);
                            mediaPlayer.pause();
                            currentlyPlaying = false;
                        } else {
                            playPauseButton.setText(com.recentbirds.mockingbird.R.string.pause_label);
                            mediaPlayer.start();
                            currentlyPlaying = true;
                        }
                    }
                }
            });
        }

        final TextView songName = (TextView) this.findViewById(com.recentbirds.mockingbird.R.id.songName);
        final Button showAnswerButton = (Button) findViewById(com.recentbirds.mockingbird.R.id.showAnswerButton);
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
                    };
                }
            });
        }

        final Button nextButton = (Button) findViewById(com.recentbirds.mockingbird.R.id.nextButton);
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

                    if (useTextToSpeech && textToSpeech != null && songName.getVisibility() != View.VISIBLE ) {
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
                            currentSong = currentSong + 1;
                            if (currentSong == playlistSongs.size()) {
                                currentImage = currentImage + 1;
                                if (currentImage == playlistImages.size()) {
                                    currentImage = 0;
                                }
                                setPlaylistImage();
                                shuffle(playlistSongs);
                                currentSong = 0;
                            }
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

        if(savedInstanceState != null) {
            playlistPath = savedInstanceState.getString("playlistPath");
            playlistImages = savedInstanceState.getStringArrayList("playlistImages");
            playlistSongs = savedInstanceState.getStringArrayList("playlistSongs");
            currentImage = savedInstanceState.getInt("currentImage");
            currentSong = savedInstanceState.getInt("currentSong");

            setPlaylistImage();

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
            indexSongs();
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
            }
            mediaPlayer.release();
            mediaPlayer = null;
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
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putStringArrayList("playlistImages", playlistImages);
        savedInstanceState.putString("playlistPath", playlistPath);
        savedInstanceState.putStringArrayList("playlistSongs", playlistSongs);
        savedInstanceState.putInt("currentImage", currentImage);
        savedInstanceState.putInt("currentSong", currentSong);
        savedInstanceState.putBoolean("currentlyPlaying", currentlyPlaying);
        savedInstanceState.putInt("currentPosition", currentPosition);

        TextView textView = (TextView) this.findViewById(com.recentbirds.mockingbird.R.id.songName);
        if (textView != null) {
            savedInstanceState.putInt("songNameVisibility", textView.getVisibility());
        }
    }

    private void indexSongs() {

        final PlaylistActivity context = this;

        class IndexFilesTask extends AsyncTask<String, Void, Integer> {
            protected Integer doInBackground(String... paths) {
                Uri path = Uri.parse(playlistPath);
                File dir = new File(path.getPath());
                if (!dir.exists()) {
                    //Since we receive this value picked from a directory listing, this shouldn't normally
                    //happen.
                    return 1;
                }
                playlistSongs.clear();

                MediaPlayer mp = new MediaPlayer();
                for (String song : dir.list()) {
                    String s = song.toLowerCase();
                    if (s.endsWith(".jpeg") || s.endsWith(".jpg") || s.endsWith(".png")) {
                        playlistImages.add(song);
                        continue;
                    }

                    Uri uri = Uri.parse(playlistPath + "/" + song);
                    try {
                        mp.reset();
                        mp.setDataSource(context, uri);
                    } catch (IOException e) {
                        continue;
                    }

                    playlistSongs.add(song);
                }
                mp.release();

                return 0;
            }

            protected void onPostExecute(Integer result) {
                if (result == 0) {
                    shuffle(playlistImages);
                    setPlaylistImage();
                    shuffle(playlistSongs);
                    currentSong = 0;
                    currentPosition = 0;
                    playSong();
                }
            }
        }

        new IndexFilesTask().execute();
    }

    private String prettifySongName(Uri uri, String fileName) {
        // Attempt to get song name from media metadata. If it is not set or this just fails, we try
        // to do something sensible with the file name itself.
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(this, uri);
        String songName = null;//mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (songName == null || songName.length() == 0) {
            int lastdot = fileName.lastIndexOf('.');
            if (lastdot != -1) {
                songName = fileName.substring(0, lastdot);
            } else {
                songName = fileName;
            }

            // Attempt to trim leading and trailing numbers, dots, etc. that might be part of a
            // filename that don't really belong in a song name.
            songName = songName.replaceAll("^[0-9 -.]+", "");
            songName = songName.replaceAll("[0-9 -.]+$", "");

            // Remove parentheses, no one cares about latin anyway
            songName = songName.replaceAll("[(].+[)]", "");
        }

        return songName;
    }

    private void playSong() {
        if (playlistSongs.size() == 0) {
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

        String fileName = playlistSongs.get(currentSong);
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

        String songName = prettifySongName(song, fileName);

        final TextView textView = (TextView) this.findViewById(com.recentbirds.mockingbird.R.id.songName);
        if (textView != null) {
            textView.setText(songName);
        }

        final Button playPauseButton = (Button) findViewById(com.recentbirds.mockingbird.R.id.playPauseButton);
        if (playPauseButton != null) {
            if (currentlyPlaying) {
                playPauseButton.setText(R.string.pause_label);
            } else {
                playPauseButton.setText(R.string.play_label);
            }

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    playPauseButton.setText(com.recentbirds.mockingbird.R.string.play_label);
                    currentlyPlaying = false;
                    currentPosition = 0;
                }
            });
        }
    }

    private void setPlaylistImage() {
        final ImageView playlistImageView = (ImageView) findViewById(R.id.playlistImageView);

        if (playlistImageView == null) {
            return;
        }

        class LoadImageTask extends AsyncTask<String, Void, Integer> {
            private int imageViewWidth;
            private int imageViewHeight;
            private Bitmap bm;

            protected void onPreExecute() {
                imageViewWidth = playlistImageView.getWidth();
                imageViewHeight = playlistImageView.getHeight();
            }

            protected Integer doInBackground(String... paths) {

                String imagePath = playlistPath + "/" + playlistImages.get(currentImage);

                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                bm = BitmapFactory.decodeFile(imagePath, o);

                int imageWidth = o.outWidth;
                int imageHeight = o.outHeight;
                int sampleSize = 1;
                while (imageWidth > imageViewWidth || imageHeight > imageViewHeight) {
                    imageWidth /= 2;
                    imageHeight /= 2;
                    sampleSize *= 2;
                }
                o.inSampleSize = sampleSize;
                o.inJustDecodeBounds = false;
                bm = BitmapFactory.decodeFile(imagePath, o);

                return 0;
            }

            protected void onPostExecute(Integer result) {
                if (result == 0 && bm != null) {
                    playlistImageView.setImageBitmap(bm);
                }
            }
        }

        // We want to execute after layout has completed so that we have the ImageView height
        // and width available.
        playlistImageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                playlistImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                if (!playlistImages.isEmpty()) {
                    new LoadImageTask().execute();
                } else {
                    playlistImageView.setImageResource(android.R.color.transparent);
                }
            }
        });
    }

    private void shuffle(ArrayList<String> arrayList) {
        int length = arrayList.size();
        for (int i = 0; i < length - 1; ++i) {
            int j = i + random.nextInt(length - i);
            String s = arrayList.get(i);
            String t = arrayList.get(j);
            arrayList.set(i, t);
            arrayList.set(j, s);
        }
    }
}
