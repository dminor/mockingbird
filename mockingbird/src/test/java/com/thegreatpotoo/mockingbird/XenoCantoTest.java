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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class XenoCantoTest {

    @Test
    public void search_isCorrect() throws Exception {
        MockWebServer server = new MockWebServer();

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("xeno-canto-search-results.json");
        StringBuilder sb = new StringBuilder(151*1024);
        try {
            Reader r = new InputStreamReader(is, "UTF-8");
            char[] buffer = new char[1024];
            while (r.read(buffer) != -1) {
                sb.append(buffer);
            }
        } catch (IOException e) {
        }

        MockResponse response = new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(sb.toString());

        server.enqueue(response);

        server.start();
        XenoCanto.getInstance().SEARCH_URL = server.url("/").toString();
        ArrayList<XenoCanto.SearchResult> searchResults = XenoCanto.getInstance().search("Northern Cardinal");

        assertTrue(!searchResults.isEmpty());
        assertEquals(345, searchResults.size());

        XenoCanto.SearchResult searchResult = searchResults.get(200);
        assertEquals("Cardinalis", searchResult.genus);
        assertEquals("cardinalis", searchResult.species);
        assertEquals("Northern Cardinal", searchResult.name);
        assertEquals("song", searchResult.type);

        server.shutdown();
    }

    @Test
    public void search_withPagedResults_isCorrect() throws Exception {
        MockWebServer server = new MockWebServer();

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("xeno-canto-search-results-page1.json");
        StringBuilder sb = new StringBuilder(213 * 1024);
        try {
            Reader r = new InputStreamReader(is, "UTF-8");
            char[] buffer = new char[1024];
            while (r.read(buffer) != -1) {
                sb.append(buffer);
            }
        } catch (IOException e) {
        }

        MockResponse response = new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(sb.toString());

        server.enqueue(response);

        is = this.getClass().getClassLoader().getResourceAsStream("xeno-canto-search-results-page2.json");
        sb = new StringBuilder(208 * 1024);
        try {
            Reader r = new InputStreamReader(is, "UTF-8");
            char[] buffer = new char[1024];
            while (r.read(buffer) != -1) {
                sb.append(buffer);
            }
        } catch (IOException e) {
        }

        response = new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(sb.toString());

        server.enqueue(response);

        server.start();
        XenoCanto.getInstance().SEARCH_URL = server.url("/").toString();
        ArrayList<XenoCanto.SearchResult> searchResults = XenoCanto.getInstance().search("House Wren");

        assertTrue(!searchResults.isEmpty());
        assertEquals(995, searchResults.size());

        XenoCanto.SearchResult searchResult = searchResults.get(200);
        assertEquals("Troglodytes", searchResult.genus);
        assertEquals("aedon", searchResult.species);
        assertEquals("House Wren", searchResult.name);
        assertEquals("song", searchResult.type);

        server.shutdown();
    }

    @Test
    public void searchSuggestions_isCorrect() throws Exception {
        MockWebServer server = new MockWebServer();

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("xeno-canto-search-suggestions-results.json");
        StringBuilder sb = new StringBuilder(7*1024);
        try {
            Reader r = new InputStreamReader(is, "UTF-8");
            char[] buffer = new char[1024];
            while (r.read(buffer) != -1) {
                sb.append(buffer);
            }
        } catch (IOException e) {
        }

        MockResponse response = new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(sb.toString());

        server.enqueue(response);

        server.start();
        XenoCanto.getInstance().SUGGESTIONS_URL = server.url("/").toString();
        ArrayList<String> suggestions = XenoCanto.getInstance().searchSuggestions("Northern");

        assertTrue(!suggestions.isEmpty());
        assertTrue(suggestions.contains("Northern Cardinal"));

        server.shutdown();
    }
}
