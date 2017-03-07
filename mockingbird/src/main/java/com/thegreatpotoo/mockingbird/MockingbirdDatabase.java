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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MockingbirdDatabase extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    public MockingbirdDatabase(Context context) {
        super(context, "Mockingbird", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table playlist(path text)");
        db.execSQL("create table song(playlist int, name text)");
        db.execSQL("create table answers(song int, choice text, correct int, time int)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void recordAnswer(Playlist.PlaylistSong song, String choice, boolean correct) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("song", song.databaseID);
        cv.put("choice", choice);
        cv.put("correct", correct);
        cv.put("time", System.currentTimeMillis());
        db.insertOrThrow("answers", null, cv);
    }

    public void renamePlaylist(Playlist playlist, String path) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("path", path);

        db.update("playlist", cv, "path=?", new String[]{playlist.getPlaylistPath()});
    }

    public long retrieveOrCreatePlaylist(Playlist playlist) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("select rowid from playlist where path=?", new String[]{playlist.getPlaylistPath()});
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            long databaseID = cursor.getInt(0);
            cursor.close();
            return databaseID;
        } else {
            ContentValues cv = new ContentValues();
            cv.put("path", playlist.getPlaylistPath());
            return db.insertOrThrow("playlist", null, cv);
        }
    }

    public void retrieveOrCreatePlaylistSong(long playlistDatabaseID, Playlist.PlaylistSong song) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("select rowid from song where playlist=? and name=?",
                                    new String[]{String.valueOf(playlistDatabaseID), song.fileName});
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            song.databaseID = cursor.getInt(0);

            //Don't retain too much data
            long thirtyDaysAgo = System.currentTimeMillis() - 1000L*60L*60L*24L*30L;
            db.delete("answers", "song=? and time<?",
                      new String[]{String.valueOf(song.databaseID), String.valueOf(thirtyDaysAgo)});

            cursor = db.rawQuery("select choice, correct from answers where song=?",
                                 new String[]{String.valueOf(song.databaseID)});

            while (cursor.moveToNext()) {
                if (cursor.getInt(1) == 0) {
                    song.mistakes.add(cursor.getString(0));
                } else {
                    song.correct += 1;
                }
                song.attempts += 1;
            }

            cursor.close();
        } else {
            ContentValues cv = new ContentValues();
            cv.put("playlist", playlistDatabaseID);
            cv.put("name", song.fileName);
            song.databaseID = db.insertOrThrow("song", null, cv);
        }
    }
}
