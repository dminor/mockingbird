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

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

public class Playlist {

    public interface OnSongsIndexedListener {
        void onSongsIndexed();
    }

    private Context context;
    private String playlistPath;

    private ArrayList<String> playlistSongs;
    private int currentSong;

    private Random random = new Random();

    public Playlist(Context c, String path) {
        context = c;
        playlistPath = path;
        playlistSongs = new ArrayList<String>();
        currentSong = 0;
    }

    public String currentSong() {
        return playlistSongs.get(currentSong);
    }

    public void deleteSong(int index) {
        File f = new File(playlistPath + "/" + playlistSongs.get(index));
        if (f.delete()) {
            playlistSongs.remove(index);
        }
    }

    public String getSong(int index) {
        return playlistSongs.get(index);
    }

    public ArrayList<String> getSongs() {
        return playlistSongs;
    }

    public String getName() {
        return playlistPath.substring(playlistPath.lastIndexOf('/') + 1);
    }

    public String getPlaylistPath() {
        return playlistPath;
    }

    public void indexSongs(final OnSongsIndexedListener listener) {
        class IndexFilesTask extends AsyncTask<String, Void, Integer> {
            protected Integer doInBackground(String... paths) {
                File dir = new File(playlistPath);
                if (!dir.exists()) {
                    //Since we receive this value picked from a directory listing, this shouldn't normally
                    //happen.
                    return 1;
                }
                playlistSongs.clear();

                for (String song : dir.list()) {
                    String s = song.toLowerCase();
                    if (s.endsWith(".mp3") || s.endsWith(".ogg") || s.endsWith(".wav")) {
                        playlistSongs.add(song);
                    }
                }

                return 0;
            }

            protected void onPostExecute(Integer result) {
                listener.onSongsIndexed();
            }
        }

        new IndexFilesTask().execute();
    }

    public boolean hasSongs() {
        return !playlistSongs.isEmpty();
    }

    public void nextSong() {
        currentSong = currentSong + 1;
        if (currentSong == playlistSongs.size()) {
            shuffle(playlistSongs);
            currentSong = 0;
        }
    }

    public String prettifySongName(Uri uri, String fileName) {
        // Attempt to get song name from media metadata. If it is not set or this just fails, we try
        // to do something sensible with the file name itself.
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context, uri);
        String songName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
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
        }

        // Remove parentheses, no one cares about latin anyway
        songName = songName.replaceAll("[(].+[)]", "");

        return songName;
    }

    public void rename(String name) {
        File dir = new File(playlistPath);
        String basePath = playlistPath.substring(0, playlistPath.lastIndexOf('/') + 1);
        File newDir = new File(basePath + name);
        if (dir.renameTo(newDir)) {
            playlistPath = basePath + name;
        }
    }

    public void restore(Bundle savedInstanceState) {
        playlistSongs = savedInstanceState.getStringArrayList("playlistSongs");
        currentSong = savedInstanceState.getInt("currentSong");
    }

    public void save(Bundle savedInstanceState) {
        savedInstanceState.putString("playlistPath", playlistPath);
        savedInstanceState.putStringArrayList("playlistSongs", playlistSongs);
        savedInstanceState.putInt("currentSong", currentSong);
    }

    public void shuffle() {
        shuffle(playlistSongs);
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