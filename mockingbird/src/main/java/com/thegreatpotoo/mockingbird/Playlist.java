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
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class Playlist {

    private int MAX_BINS = 4;

    public interface OnSongsIndexedListener {
        void onSongsIndexed();
    }

    //private Context context;
    private String playlistPath;

    private long databaseID;

    public class PlaylistSong {
        public PlaylistSong(String fileName) {
            this.fileName = fileName;
            fullPath = playlistPath + "/" + fileName;
            uri = Uri.parse(fullPath);
            prettifiedName = "";
            databaseID = -1;

            bin = 0;
            mistakes = new ArrayList<>();
        }

        public String toString() {
            return fileName;
        }

        public String fileName;
        public String fullPath;
        public String prettifiedName;
        public Uri uri;

        public long databaseID;

        public int bin;
        public ArrayList<String> mistakes;

    }

    private ArrayList<PlaylistSong> playlistSongs;
    private int currentSong;

    private HashMap<Integer, ArrayList<PlaylistSong>> bins;

    private Random random = new Random();

    private MockingbirdDatabase mockingbirdDatabase;

    private int currentStreak;

    public Playlist(MockingbirdDatabase db, String path) {
        playlistPath = path;
        playlistSongs = new ArrayList<>();
        currentSong = 0;
        currentStreak = 0;

        bins = new HashMap<>();
        for (int i = 0; i < MAX_BINS; ++i) {
            bins.put(i, new ArrayList<PlaylistSong>());
        }

        mockingbirdDatabase = db;
        databaseID = mockingbirdDatabase.retrieveOrCreatePlaylist(this);
    }

    public ArrayList<String> choicesForSong(PlaylistSong song) {
        ArrayList<String> result = new ArrayList<>();
        result.add(song.prettifiedName);

        if (!song.mistakes.isEmpty() && random.nextBoolean()) {
            int i = random.nextInt(song.mistakes.size());
            result.add(song.mistakes.get(i));
        }

        HashMap<Integer, Boolean> checked = new HashMap<>();
        while (checked.size() < playlistSongs.size() && result.size() < 3) {
            int i = random.nextInt(playlistSongs.size());
            if (checked.containsKey(i)) {
                continue;
            }
            checked.put(i, true);
            String choice = getSong(i).prettifiedName;
            if (result.contains(choice)) {
                continue;
            }

            result.add(choice);
        }

        Collections.sort(result);

        return result;
    }

    public PlaylistSong currentSong() {
        return getSong(currentSong);
    }

    public void deleteSong(int index) {
        File f = new File(playlistSongs.get(index).fullPath);
        if (f.delete()) {
            mockingbirdDatabase.deleteSong(playlistSongs.get(index));
            playlistSongs.remove(index);
        }
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public PlaylistSong getSong(int index) {
        PlaylistSong song = playlistSongs.get(index);
        if (song.prettifiedName.isEmpty()) {
            song.prettifiedName = prettifySongName(song.fileName);
        }

        if (song.databaseID == -1) {
            mockingbirdDatabase.retrieveOrCreatePlaylistSong(databaseID, song);

            if (song.bin >= MAX_BINS) {
                song.bin = MAX_BINS - 1;
            }

            bins.get(song.bin).add(song);
        }

        return song;
    }

    public ArrayList<PlaylistSong> getSongs() { return playlistSongs; }

    public String getName() {
        return playlistPath.substring(playlistPath.lastIndexOf('/') + 1);
    }

    public String getPlaylistPath() {
        return playlistPath;
    }

    public boolean hasSongs() {
        return !playlistSongs.isEmpty();
    }

    public void indexSongs(final OnSongsIndexedListener listener) {
        class IndexFilesTask extends AsyncTask<String, Void, Integer> {
            protected Integer doInBackground(String... paths) {
                return indexSongsSync();
            }

            protected void onPostExecute(Integer result) {
                listener.onSongsIndexed();
            }
        }

        new IndexFilesTask().execute();
    }

    public int indexSongsSync() {
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
                playlistSongs.add(new PlaylistSong(song));
            }
        }

        return 0;
    }

    public void nextSong() {

        if (playlistSongs.size() <= 1) {
            return;
        }

        int nextSong = currentSong;
        while (nextSong == currentSong) {
            int bin;

            double r = Math.random();
            if (r <= 0.4) {
                bin = 0;
            } else if (r <= 0.7) {
                bin = 1;
            } else if (r <= 0.9) {
                bin = 2;
            } else {
                bin = 3;
            }

            int binSize = bins.get(bin).size();
            while (binSize == 0) {
                bin += 1;
                if (bin == MAX_BINS) {
                    bin = 0;
                }
                binSize = bins.get(bin).size();
            }

            nextSong = playlistSongs.indexOf(bins.get(bin).get(random.nextInt(binSize)));
        }

        currentSong = nextSong;
    }

    public String prettifySongName(String fileName) {

        Uri uri = Uri.parse(playlistPath + "/" + fileName);

        // Attempt to get song name from media metadata. If it is not set or this just fails, we try
        // to do something sensible with the file name itself.
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(playlistPath + "/" + fileName);
        String songName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (songName == null || songName.length() == 0) {
            int lastdot = fileName.lastIndexOf('.');
            if (lastdot != -1) {
                songName = fileName.substring(0, lastdot);
            } else {
                songName = fileName;
            }
        }

        // Remove parentheses, no one cares about latin anyway
        songName = songName.replaceAll("[(].+[)]", "");

        // Attempt to trim leading and trailing numbers, dots, etc. that might be part of a
        // filename that don't really belong in a song name.
        songName = songName.replaceAll("^([0-9 .-])+", "");
        songName = songName.replaceAll("([0-9 .-]+)$", "");

        return songName;
    }

    public void recordAnswer(PlaylistSong song, String choice, boolean correct) {
        if (!correct) {
            currentStreak = 0;
            bins.get(song.bin).remove(song);
            song.bin = 0;
            bins.get(song.bin).add(song);
            song.mistakes.add(choice);
        } else {
            ++currentStreak;
            if (song.bin + 1 < MAX_BINS) {
                song.bin += 1;
                bins.get(song.bin).add(song);
            }
        }

        mockingbirdDatabase.recordAnswer(song, choice, correct);
    }

    public void rename(String name) {
        File dir = new File(playlistPath);
        String basePath = playlistPath.substring(0, playlistPath.lastIndexOf('/') + 1);
        File newDir = new File(basePath + name);
        if (dir.renameTo(newDir)) {
            mockingbirdDatabase.renamePlaylist(this, basePath + name);
            playlistPath = basePath + name;
        }
    }

    public void restore(Bundle savedInstanceState) {
        ArrayList<String> songs = savedInstanceState.getStringArrayList("playlistSongs");
        for (String s: songs) {
            playlistSongs.add(new PlaylistSong(s));
        }
        currentSong = savedInstanceState.getInt("currentSong");
        currentStreak = savedInstanceState.getInt("currentStreak");
    }

    public void save(Bundle savedInstanceState) {
        savedInstanceState.putString("playlistPath", playlistPath);

        ArrayList<String> fileNames = new ArrayList<>();
        for (PlaylistSong s: playlistSongs) {
            fileNames.add(s.fileName);
        }
        savedInstanceState.putStringArrayList("playlistSongs", fileNames);
        savedInstanceState.putInt("currentSong", currentSong);
        savedInstanceState.putInt("currentStreak", currentStreak);
    }

    public void shuffle() {
        shuffle(playlistSongs);
    }

    public <T> void shuffle(ArrayList<T> array) {
        int length = array.size();
        for (int i = 0; i < length - 1; ++i) {
            int j = i + random.nextInt(length - i);
            T s = array.get(i);
            T t = array.get(j);
            array.set(i, t);
            array.set(j, s);
        }
    }
}
