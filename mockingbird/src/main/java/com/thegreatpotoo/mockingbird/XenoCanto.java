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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

public class XenoCanto {

    class SearchResult {
        String id;
        String genus;
        String species;
        String name;
        String file;
        String loc;
        String type;
        String date;
        String q;

        public SearchResult(JSONObject json) {
            try {
                id = json.getString("id");
                genus = json.getString("gen");
                species = json.getString("sp");
                name = json.getString("en");
                file = json.getString("file");
                loc = json.getString("loc");
                type = json.getString("type");
                date = json.getString("date");
                q = json.getString("q");
            } catch (JSONException e) {

            }
        }

        public String toString() {
            return name + " (" + type + ", " + loc + ", " + date + ")";
        }
    }

    public String SEARCH_URL = "http://www.xeno-canto.org/api/2/recordings?query=";

    // it would be nice to not use an "internal" api for this...
    public String SUGGESTIONS_URL = "http://www.xeno-canto.org/api/internal/completion/species?query=";

    private static final XenoCanto instance = new XenoCanto();

    private XenoCanto() {

    }

    public static XenoCanto getInstance() {
        return instance;
    }

    public ArrayList<SearchResult> search(String searchTerm) {
        String result = null;
        HttpURLConnection conn = null;
        InputStream is = null;
        ArrayList<SearchResult> searchResults = new ArrayList<>();
        try {
            //TODO: we don't handle paged results here
            URL url = new URL(SEARCH_URL + searchTerm);
            conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line = null;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line + "\n");
            }
            result = sb.toString();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        if (result != null) {
            try {
                JSONObject json = new JSONObject(result);
                JSONArray recordings = json.getJSONArray("recordings");
                for (int i = 0; i < recordings.length(); ++i)  {
                    JSONObject recording = recordings.getJSONObject(i);
                    searchResults.add(new SearchResult(recording));
                }
            } catch (JSONException e) {
            }
        }

        return searchResults;
    }

    public ArrayList<String> searchSuggestions(String searchString) {
        ArrayList<String> searchSuggestions = new ArrayList<String>();
        String jsonResult = null;
        try {
            searchString = URLEncoder.encode(searchString, "UTF-8");
            URL url = new URL(SUGGESTIONS_URL + searchString);
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }

            jsonResult = sb.toString();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }

        if (jsonResult != null) {
            try {
                JSONObject json = new JSONObject(jsonResult);
                JSONArray data = json.getJSONArray("data");
                for (int i = 0; i < data.length(); ++i)  {
                    JSONObject datum = data.getJSONObject(i);
                    searchSuggestions.add(datum.getString("common_name"));
                }
            } catch (JSONException e) {

            }
        }

        return searchSuggestions;
    }
}
