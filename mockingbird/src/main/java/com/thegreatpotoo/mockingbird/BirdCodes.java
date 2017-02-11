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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class BirdCodes {

    private HashMap<String, String> codes;

    public String getCode(String s) {
        return codes.get(s.toLowerCase());
    }

    public void read(InputStream ins) {
        codes = new HashMap<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",");
                if (row.length >= 2) {
                    codes.put(row[1].toLowerCase(), row[0]);
                }
            }
        } catch (IOException e) {

        } finally {
            try {
                ins.close();
            } catch (IOException e) {

            }
        }
    }
}
