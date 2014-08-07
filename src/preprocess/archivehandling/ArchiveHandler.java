/*
 * Copyright (C) 2014 Team GRIT
 * 
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package preprocess.archivehandling;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.zip.ZipException;

import util.config.NoProperParameterException;

/**
 * Interface which specifies what a ArchiveHandler(e.g. Zip,Rar) must provide.
 * A ArchiveHandler extracts a (nested) archive, adds new files to the
 * folder(s) and can pack a specified folder to a archive.
 *
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 * @author <a href="mailto:david.kolb@uni-konstanz.de">David Kolb</a>
 *
 */

public interface ArchiveHandler {

    /**
     * Extracts a specified archive to a specified location.
     *
     * @param pathToZipfile
     *            Specifies a path to a given archive which has to be extracted
     * @param outputFolder
     *            folder where folders and files are extracted to
     * @throws FileNotFoundException
     *             thrown if pathToZipFile is empty
     * @throws ZipException
     *             thrown if file isn't the needed archive format
     * @throws NoProperParameterException
     *             thrown if extractiondepthlimit or tempdirectory aren't
     *             initialized
     */

    void extractZip(File pathToZipfile, File outputFolder)
            throws FileNotFoundException, ZipException,
            NoProperParameterException;

    /**
     * Sets the depth limit until which an nested archive will be extracted.
     *
     * @param value
     *            depth limit (must be positive or 0)
     * @throws NoProperParameterException
     *             thrown if given depth limit is smaller than 0
     */

    void setFolderExtractionDepthLimit(int value)
            throws NoProperParameterException;

    /**
     * Returns the value of the current extraction depth limit.
     *
     * @return current value
     */

    int getFolderExtractionDepthLimit();

    /**
     * Makes an archive by using the files specified and saves it at
     * outputLocation. If the inputLocation is no valid path the method returns
     * false and nothing will be done. If the outputLocation is no valid path
     * all necessary folders will be created. Empty directories contained in
     * the folder we want to make an archive of will be ignored.
     *
     * @param inputLocation
     *            files to be packed
     * @param outputLocation
     *            path to the location where the archive will be saved
     * @throws FileNotFoundException
     *             thrown if inputLocation is empty
     * @throws NoProperParameterException
     *             thrown if outputLocation is invalid
     *
     */

    void makeZip(File inputLocation, File outputLocation)
            throws FileNotFoundException, NoProperParameterException;

    /**
     * Gives the current TempDirectory which assists in the extraction process.
     *
     * @return current directory
     */
    File getTempDirectory();

    /**
     * Sets a new TempDirectory to assist in the extraction process. New temp
     * dir is not allowed to be null.
     *
     * @param newTempDir
     *            new directory
     * @throws NoProperParameterException
     *             thrown if given tempDirectory is null
     */
    void setTempDirectory(File newTempDir) throws NoProperParameterException;
}
