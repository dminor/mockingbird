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
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

class XenoCantoSuggestionsAdapter extends ArrayAdapter<String> implements Filterable {

    // it would be nice to not use an "internal" api for this...
    private String QUERY_URL = "http://www.xeno-canto.org/api/internal/completion/species?query=";
    private ArrayList<String> suggestions;
    private String searchString;

    public XenoCantoSuggestionsAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        suggestions = new ArrayList<String>();
    }

    @Override
    public int getCount() {
        return suggestions.size();
    }

    @Override
    public String getItem(int index) {
        return suggestions.get(index);
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    getXenoCantoSearchSuggestions();
                    filterResults.values = suggestions;
                    filterResults.count = suggestions.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
            }};

        return filter;
    }

    public void setSearchString(String s) {
        searchString = s;
        //notifyDataSetChanged();
    }

    private void getXenoCantoSearchSuggestions() {
        String result = null;
        try {
            searchString = URLEncoder.encode(searchString, "UTF-8");
            URL url = new URL(QUERY_URL + searchString);
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }

            result = sb.toString();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }

        if (result != null) {
            try {
                JSONObject json = new JSONObject(result);
                JSONArray data = json.getJSONArray("data");
                suggestions.clear();
                for (int i = 0; i < data.length(); ++i)  {
                    JSONObject datum = data.getJSONObject(i);
                    suggestions.add(datum.getString("common_name"));
                }
            } catch (JSONException e) {

            }
        }
    }
}