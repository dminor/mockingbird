package com.thegreatpotoo.mockingbird;

/**
 * Created by dminor on 02/02/17.
 */

import android.content.Context;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class PlaylistTest {

    Context mContext;

    @Rule
    public TemporaryFolder playlistFolder = new TemporaryFolder();

    @Test
    public void choicesForSong_isCorrect() throws Exception {
        Playlist playlist = new Playlist(mContext, playlistFolder.getRoot().getPath());

        playlistFolder.newFile("bird.ogg");
        playlistFolder.newFile("bird 2.ogg");
        playlistFolder.newFile("bird 4.ogg");
        playlist.indexSongsSync();
        assertEquals(3, playlistFolder.getRoot().listFiles().length);

        ArrayList<String> choices = playlist.choicesForSong(playlist.getSong(0));
        assertEquals(1, choices.size());
        assertEquals(true, choices.contains(playlist.getSong(0).prettifiedName));

        playlistFolder.newFile("other bird.ogg");
        playlist.indexSongsSync();
        choices = playlist.choicesForSong(playlist.getSong(0));
        assertEquals(2, choices.size());

        playlistFolder.newFile("yet another bird.ogg");
        playlistFolder.newFile("super rare bird.ogg");
        playlist.indexSongsSync();
        choices = playlist.choicesForSong(playlist.getSong(0));
        assertEquals(3, choices.size());

        HashMap<String, Boolean> s = new HashMap<>();
        for (String choice: choices) {
            s.put(choice, true);
        }

        assertEquals(3, s.size());

        playlist.recordAnswer(playlist.getSong(0), "ivory-billed woodpecker", true);
        choices = playlist.choicesForSong(playlist.getSong(0));
        assertEquals(3, choices.size());
        assertEquals(false, choices.contains("ivory-billed woodpecker"));

        playlist.recordAnswer(playlist.getSong(0), "ivory-billed woodpecker", false);
        choices = playlist.choicesForSong(playlist.getSong(0));
        assertEquals(3, choices.size());
        assertEquals(true, choices.contains("ivory-billed woodpecker"));
    }

    @Test
    public void deleteSong_isCorrect() throws Exception {
        Playlist playlist = new Playlist(mContext, playlistFolder.getRoot().getPath());

        playlistFolder.newFile("test.ogg");
        playlist.indexSongsSync();
        assertTrue(playlist.hasSongs());

        playlist.deleteSong(0);
        assertEquals(false, playlist.hasSongs());
        assertEquals(0, playlistFolder.getRoot().listFiles().length);
    }

    @Test
    public void getName_isCorrect() throws Exception {
        Playlist playlist = new Playlist(mContext, playlistFolder.getRoot().getPath());
        assertEquals(playlistFolder.getRoot().getName(), playlist.getName());
    }

    @Test
    public void getPlaylistPath_isCorrect() throws Exception {
        Playlist playlist = new Playlist(mContext, playlistFolder.getRoot().getPath());
        assertEquals(playlistFolder.getRoot().getPath(), playlist.getPlaylistPath());
    }

    @Test
    public void indexSongs_isCorrect() throws Exception {
        Playlist playlist = new Playlist(mContext, playlistFolder.getRoot().getPath());
        assertEquals(false, playlist.hasSongs());

        playlist.indexSongsSync();
        assertEquals(false, playlist.hasSongs());

        playlistFolder.newFile("test.ogg");
        playlist.indexSongsSync();
        assertTrue(playlist.hasSongs());
        assertEquals("test.ogg", playlist.currentSong().fileName);
        assertEquals("test", playlist.currentSong().prettifiedName);

        //check we only index valid types
        playlistFolder.newFolder("ignore-me");
        File mp3 = playlistFolder.newFile("test.mp3");
        playlistFolder.newFile("test.wav");
        playlistFolder.newFile("test.jpg");
        playlistFolder.newFile("blah.txt");
        playlist.indexSongsSync();
        for (Playlist.PlaylistSong s: playlist.getSongs()) {
            assertTrue(s.fileName.contains("mp3") || s.fileName.contains("ogg") || s.fileName.contains("wav"));
        }
        assertEquals(3, playlist.getSongs().size());

        //indexing handles deletes ok
        mp3.delete();
        playlist.indexSongsSync();
        assertEquals(2, playlist.getSongs().size());
    }

    @Test
    public void prettifySongName_isCorrect() throws Exception {
        Playlist playlist = new Playlist(mContext, playlistFolder.getRoot().getPath());

        assertEquals("", playlist.prettifySongName(""));
        assertEquals("a really awesome bird", playlist.prettifySongName("a really awesome bird.ogg"));
        assertEquals("awesome bird", playlist.prettifySongName("17. awesome bird.ogg"));
        assertEquals("awesome bird", playlist.prettifySongName("17 - awesome bird.ogg"));
        assertEquals("awesome bird", playlist.prettifySongName("awesome bird 04.ogg"));
        assertEquals("awesome bird", playlist.prettifySongName("awesome bird - 04.ogg"));
        assertEquals("dickcissel", playlist.prettifySongName("dickcissel (Bigus dickus).mp3"));
    }

}
