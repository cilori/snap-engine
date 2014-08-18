/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.util;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * DatUtils Tester.
 *
 * @author lveci
 */
public class TestDatUtils {

    @Test
    public void testFindHomeFolder() {
        final File homeFolder = ResourceUtils.findHomeFolder();
        final File file = new File(homeFolder, "config" + File.separator + ResourceUtils.getContextID()+".config");

        assertTrue(file.exists());
    }
}