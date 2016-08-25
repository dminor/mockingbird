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

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class ChoosePlaylistActivity extends AppCompatActivity {

    int REQUEST_PERMISSION = 42;
    String externalStoragePath = Environment.getExternalStorageDirectory().getPath() + "/Mockingbird";

    private String currentWorkingDirectory;
    private ArrayList<String> paths;
    private ArrayAdapter<String> adapter;
    private String playlistPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_playlist);

        final ListView playlistView = (ListView) findViewById(com.recentbirds.mockingbird.R.id.playlistView);
        if (playlistView == null) {
            return;
        }

        paths = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, paths);
        playlistView.setAdapter(adapter);
        playlistView.setSelector(android.R.color.darker_gray);

        playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Object o = playlistView.getItemAtPosition(position);
                playlistPath = (String)o;

                //If the selected item is a directory and contains another directory, we will
                //change the current working directory.
                String newPath = currentWorkingDirectory + '/' + playlistPath;
                File dir = new File(newPath);
                if (dir.exists() && dir.isDirectory()) {
                    boolean hasSubDirectory = false;
                    for (String s : dir.list()) {
                        if (s.startsWith(".")) {
                            //Attempt to ignore hidden files and directories
                            continue;
                        }

                        File f = new File(newPath + '/' + s);
                        if (f.exists() && f.isDirectory()) {
                            hasSubDirectory = true;
                            break;
                        }
                    }

                    if (hasSubDirectory) {
                        currentWorkingDirectory = newPath;
                        playlistPath = null;
                        populateListView();
                    }
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        } else {
            populateListView();
        }

        final Intent intent = new Intent(this, PlaylistActivity.class);

        final Button backButton = (Button) findViewById(com.recentbirds.mockingbird.R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (currentWorkingDirectory != null && !currentWorkingDirectory.equals(externalStoragePath)) {
                        currentWorkingDirectory = currentWorkingDirectory.substring(0, currentWorkingDirectory.lastIndexOf('/'));
                        populateListView();
                    }
                }
            });
        }

        final Button startPlaylistButton = (Button) findViewById(com.recentbirds.mockingbird.R.id.choosePlaylistButton);
        if (startPlaylistButton != null) {
            startPlaylistButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (playlistPath != null) {
                        intent.putExtra("playlistPath", currentWorkingDirectory +  "/" + playlistPath);
                        startActivity(intent);
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateListView();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.no_permission_message)
                        .setTitle(R.string.no_permission_title)
                        .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        });
                builder.create().show();
            }
        }
    }

    public void populateListView() {

        if (currentWorkingDirectory == null) {
            File dir = new File(externalStoragePath);
            if (!dir.exists()) {
                File exampleDir = new File(externalStoragePath + "/Example Playlist");
                if (exampleDir.mkdirs()) {
                    AssetManager assetManager = this.getAssets();
                    try {
                        for (String filename : assetManager.list("example")) {
                            InputStream in = assetManager.open("example/" + filename);
                            FileOutputStream out = new FileOutputStream(exampleDir.getPath() + "/" + filename);
                            byte[] buff = new byte[1024];
                            int read = 0;

                            try {
                                while ((read = in.read(buff)) > 0) {
                                    out.write(buff, 0, read);
                                }
                            } finally {
                                in.close();
                                out.close();
                            }

                            //Work around https://code.google.com/p/android/issues/detail?id=38282
                            File f = new File(exampleDir + "/" + filename);
                            MediaScannerConnection.scanFile(this, new String[]{f.getPath()}, null, null);
                            this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
                        }
                    } catch (FileNotFoundException e) {
                    } catch (IOException e) {
                    }
                }
            }
            currentWorkingDirectory = externalStoragePath;
        }

        paths.clear();

        File dir = new File(currentWorkingDirectory);
        if (dir.exists() && dir.isDirectory()) {
            for (String path : dir.list()) {
                File f = new File(currentWorkingDirectory + '/' + path);
                if (f.exists() && f.isDirectory()) {
                    paths.add(path);
                }
            }
            Collections.sort(paths);
            adapter.notifyDataSetChanged();
        }
    }
}
