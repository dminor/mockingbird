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

import com.google.common.io.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class BirdCodesTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void getCode_isCorrect() throws Exception {

        BirdCodes bc = new BirdCodes();

        File codes = temporaryFolder.newFile("codes");

        FileWriter fw = new FileWriter(codes.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("BCCH,Black-capped Chickadee,Poecile atricapillus,POEATR\n");
        bw.write("IBWO,Ivory-billed Woodpecker,Campephilus principalis,CAMPRI\n");
        bw.write("PFGO,Pink-footed Goose,Anser brachyrhynchus,ANSBRA\n");
        bw.close();

        InputStream ins = new FileInputStream(codes);
        bc.read(ins);
        assertEquals("BCCH", bc.getCode("black-capped chickadee"));
        assertEquals("IBWO", bc.getCode("ivory-billed woodpecker"));
        assertEquals("PFGO", bc.getCode("pink-footed goose"));
        assertEquals("not actually a bird", bc.getCode("not actually a bird"));
    }

}