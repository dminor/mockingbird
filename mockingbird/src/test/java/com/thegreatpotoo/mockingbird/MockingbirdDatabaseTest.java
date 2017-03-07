/*  Mockingbird
    Copyright (C) 2017 Daniel Minor

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

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class MockingbirdDatabaseTest {

    @Rule
    public TemporaryFolder playlistFolder = new TemporaryFolder();

    @Test
    public void retrieveOrCreatePlaylistSong_isCorrect() throws Exception {
        MockingbirdDatabase mockingbirdDatabase = new MockingbirdDatabase(RuntimeEnvironment.application);
        SQLiteDatabase db = mockingbirdDatabase.getWritableDatabase();

        Playlist p = new Playlist(mockingbirdDatabase, playlistFolder.getRoot().getPath());

        playlistFolder.newFile("bird.ogg");
        assertEquals(1, playlistFolder.getRoot().listFiles().length);
        p.indexSongsSync();

        Playlist.PlaylistSong song = p.getSong(0);
        Cursor cursor = db.rawQuery("select rowid, name from song", null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals(1, cursor.getInt(0));
        assertEquals(song.databaseID, 1);
        assertEquals("bird.ogg", cursor.getString(1));

        // Test reading existing answers into song
        Playlist p2 = new Playlist(mockingbirdDatabase, playlistFolder.getRoot().getPath());
        p2.indexSongsSync();

        long thirtyDaysAgo = System.currentTimeMillis() - 1000L*60L*60L*24L*30L;

        // We expect the old value to be deleted
        ContentValues cv = new ContentValues();
        cv.put("song", song.databaseID);
        cv.put("choice", "bird");
        cv.put("correct", true);
        cv.put("time", thirtyDaysAgo - 1);
        db.insertOrThrow("answers", null, cv);

        cv = new ContentValues();
        cv.put("song", song.databaseID);
        cv.put("choice", "a different bird");
        cv.put("correct", false);
        cv.put("time", System.currentTimeMillis());
        db.insertOrThrow("answers", null, cv);

        cv = new ContentValues();
        cv.put("song", song.databaseID);
        cv.put("choice", "bird");
        cv.put("correct", true);
        cv.put("time", System.currentTimeMillis());
        db.insertOrThrow("answers", null, cv);

        cursor = db.rawQuery("select rowid from answers", null);
        assertEquals(3, cursor.getCount());

        song = p2.getSong(0);
        cursor = db.rawQuery("select rowid from answers", null);
        assertEquals(2, cursor.getCount());
        assertEquals(1, song.mistakes.size());
        assertEquals("a different bird", song.mistakes.get(0));
        assertEquals(1, song.correct);
        assertEquals(2, song.attempts);

        cursor.close();
        db.close();
    }

    @Test
    public void recordAnswer_isCorrect() throws Exception {
        MockingbirdDatabase mockingbirdDatabase = new MockingbirdDatabase(RuntimeEnvironment.application);
        SQLiteDatabase db = mockingbirdDatabase.getWritableDatabase();

        Playlist p = new Playlist(mockingbirdDatabase, playlistFolder.getRoot().getPath());

        playlistFolder.newFile("bird.ogg");
        p.indexSongsSync();
        assertEquals(1, playlistFolder.getRoot().listFiles().length);

        p.recordAnswer(p.getSong(0), "bird", true);
        Cursor cursor = db.rawQuery("select choice, correct from answers where song=?",
                                    new String[]{String.valueOf(p.getSong(0).databaseID)});
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("bird", cursor.getString(0));
        assertEquals(1, cursor.getInt(1));

        p.recordAnswer(p.getSong(0), "another bird", false);
        cursor = db.rawQuery("select choice, correct from answers where song=?",
                             new String[]{String.valueOf(p.getSong(0).databaseID)});
        assertEquals(2, cursor.getCount());
        cursor.moveToLast();
        assertEquals("another bird", cursor.getString(0));
        assertEquals(0, cursor.getInt(1));

        cursor.close();
        db.close();
    }

    @Test
    public void renamePlaylist_isCorrect() throws Exception {
        MockingbirdDatabase mockingbirdDatabase = new MockingbirdDatabase(RuntimeEnvironment.application);
        SQLiteDatabase db = mockingbirdDatabase.getWritableDatabase();

        Playlist p = new Playlist(mockingbirdDatabase, "/a/playlist");

        Cursor cursor = db.rawQuery("select rowid, path from playlist", null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals(1, cursor.getInt(0));
        assertEquals("/a/playlist", cursor.getString(1));

        mockingbirdDatabase.renamePlaylist(p, "/another/playlist");

        cursor = db.rawQuery("select rowid, path from playlist", null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals(1, cursor.getInt(0));
        assertEquals("/another/playlist", cursor.getString(1));

        cursor.close();
        db.close();
    }

    @Test
    public void retrieveOrCreatePlaylist_isCorrect() throws Exception {
        MockingbirdDatabase mockingbirdDatabase = new MockingbirdDatabase(RuntimeEnvironment.application);
        SQLiteDatabase db = mockingbirdDatabase.getWritableDatabase();

        // Database should start empty
        Cursor cursor = db.rawQuery("select * from playlist", null);
        assertEquals(0, cursor.getCount());

        // Create a playlist, we should see a row in the database for it
        Playlist p = new Playlist(mockingbirdDatabase, "/a/playlist");
        cursor = db.rawQuery("select rowid, path from playlist", null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals(1, cursor.getInt(0));
        assertEquals("/a/playlist", cursor.getString(1));

        // Same path should not result in a new row in the database
        Playlist p2 = new Playlist(mockingbirdDatabase, "/a/playlist");
        cursor = db.rawQuery("select * from playlist", null);
        assertEquals(1, cursor.getCount());
        cursor.close();
        db.close();
    }
}