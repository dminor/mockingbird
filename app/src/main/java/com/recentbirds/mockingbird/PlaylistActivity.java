package com.recentbirds.mockingbird;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

public class PlaylistActivity extends AppCompatActivity {

    private int REQUEST_PERMISSION = 42;
    private Random random = new Random();

    private MediaPlayer mediaPlayer;
    private String playlistPath;
    private ArrayList<String> playlistSongs;
    private int currentSong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.recentbirds.mockingbird.R.layout.activity_playlist);

        playlistPath =  getIntent().getStringExtra("playlistPath");
        playlistSongs = new ArrayList<String>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        } else {
            indexSongs();
        }

        final Button playPauseButton = (Button) findViewById(com.recentbirds.mockingbird.R.id.playPauseButton);
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
                    }
                }
            });
        }

        final TextView textView = (TextView) this.findViewById(com.recentbirds.mockingbird.R.id.songName);
        final Button showAnswerButton = (Button) findViewById(com.recentbirds.mockingbird.R.id.showAnswerButton);
        if (textView != null && showAnswerButton != null) {
            showAnswerButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (textView.getVisibility() == View.VISIBLE) {
                        textView.setVisibility(View.INVISIBLE);
                    } else {
                        textView.setVisibility(View.VISIBLE);
                    };
                }
            });
        }

        final Button nextButton = (Button) findViewById(com.recentbirds.mockingbird.R.id.nextButton);
        if (nextButton != null) {
            nextButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    textView.setVisibility(View.VISIBLE);
                    final Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                    fadeOut.setDuration(1000);
                    textView.startAnimation(fadeOut);
                    fadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            currentSong = currentSong + 1;
                            if (currentSong == playlistSongs.size()) {
                                shuffleSongs();
                                currentSong = 0;
                            }
                            playSong();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Button playPauseButton = (Button) findViewById(com.recentbirds.mockingbird.R.id.playPauseButton);
        if (mediaPlayer != null && playPauseButton != null) {
            if (mediaPlayer.isPlaying()) {
                playPauseButton.setText(com.recentbirds.mockingbird.R.string.play_label);
                mediaPlayer.pause();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                indexSongs();
            } else {
                // TODO: User refused to grant permission.
            }
        }
    }

    public void indexSongs() {

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
                for (String song : dir.list()) {
                    Uri uri = Uri.parse(playlistPath + "/" + song);
                    MediaPlayer mp = MediaPlayer.create(context, uri);
                    if (mp != null) {
                        playlistSongs.add(song);
                        mp.release();
                    }
                }

                return 0;
            }

            protected void onPostExecute(Integer result) {
                if (result == 0) {
                    shuffleSongs();
                    currentSong = 0;
                    playSong();
                }
            }
        }

        new IndexFilesTask().execute();
    }

    public void shuffleSongs() {
        // Shuffle songs. At some point we might want to adapt this based on error rates in
        // identifying the birds, e.g. use a "three deck" system.
        int length = playlistSongs.size();
        for (int i = 0; i < length - 1; ++i) {
            int j = i + random.nextInt(length - i);
            String s = playlistSongs.get(i);
            String t = playlistSongs.get(j);
            playlistSongs.set(i, t);
            playlistSongs.set(j, s);
        }
    }

    public void playSong() {
        if (playlistSongs.size() == 0) {
            return;
        }

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        String fileName = playlistSongs.get(currentSong);
        Uri song = Uri.parse(playlistPath + "/" + fileName);
        mediaPlayer = MediaPlayer.create(this, song);
        if (mediaPlayer == null) {
            //TODO: this should not normally happen since we check each song while indexing
            return;
        }
        mediaPlayer.start();

        // Attempt to get song name from media metadata. If it is not set or this just fails, we try
        // to do something sensible with the file name itself.
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(this, song);
        String songName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (songName == null || songName.length() == 0) {
            int lastdot = fileName.lastIndexOf('.');
            if (lastdot != -1) {
                songName = fileName.substring(0, lastdot);
            } else {
                songName = fileName;
            }
        }
        final TextView textView = (TextView) this.findViewById(com.recentbirds.mockingbird.R.id.songName);
        if (textView != null) {
            textView.setText(songName);
            textView.setVisibility(View.INVISIBLE);
        }

        final Button playPauseButton = (Button) findViewById(com.recentbirds.mockingbird.R.id.playPauseButton);
        if (playPauseButton != null) {
            playPauseButton.setText(com.recentbirds.mockingbird.R.string.pause_label);

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    playPauseButton.setText(com.recentbirds.mockingbird.R.string.play_label);
                }
            });
        }
    }
}
