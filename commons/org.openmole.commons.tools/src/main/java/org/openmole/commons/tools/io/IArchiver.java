/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.commons.tools.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author reuillon
 */
public interface IArchiver {
    public void createDirArchiveWithRelativePath(final File baseDir, final OutputStream archive) throws IOException;
    public void createDirArchiveWithRelativePathNoVariableContent(final File baseDir, final OutputStream archive) throws IOException;
    public void extractDirArchiveWithRelativePath(final File baseDir, final InputStream archive) throws IOException;
}
