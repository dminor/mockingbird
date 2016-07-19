package com.recentbirds.mockingbird;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    int REQUEST_PERMISSION = 42;
    String externalStoragePath = Environment.getExternalStorageDirectory().getPath();

    private ArrayList<String> paths;
    private ArrayAdapter<String> adapter;
    private String playlistPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.recentbirds.mockingbird.R.layout.activity_main);

        // First check our own directory, then generic music directory, then fallback to just the
        // sdcard path, which is not that helpful, but whatever.
        String[] potentialStoragePaths = {externalStoragePath + "/Mockingbird",
                                          externalStoragePath + "/mockingbird",
                                          externalStoragePath + "/Music"};

        for (String path : potentialStoragePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.list().length > 0) {
                externalStoragePath = path;
                break;
            }
        }

        final ListView playlistView = (ListView) findViewById(com.recentbirds.mockingbird.R.id.playlistView);

        paths = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, paths);
        playlistView.setAdapter(adapter);
        playlistView.setSelector(android.R.color.darker_gray);

        playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                Object o = playlistView.getItemAtPosition(position);
                playlistPath = (String)o;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        } else {
            populateListView();
        }

        final Intent intent = new Intent(this, PlaylistActivity.class);
        final Button startPlaylistButton = (Button) findViewById(com.recentbirds.mockingbird.R.id.startPlaylistButton);
        if (startPlaylistButton != null) {
            startPlaylistButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (playlistPath != null) {
                        intent.putExtra("playlistPath", externalStoragePath +  "/" + playlistPath);
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
                // TODO: User refused to grant permission.
            }
        }
    }

    public void populateListView() {
        File dir = new File(externalStoragePath);
        for (String path : dir.list()) {
            paths.add(path);
        }
        adapter.notifyDataSetChanged();
    }
}
