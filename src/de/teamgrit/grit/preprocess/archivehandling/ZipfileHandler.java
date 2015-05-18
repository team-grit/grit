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

package de.teamgrit.grit.preprocess.archivehandling;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import de.teamgrit.grit.util.config.NoProperParameterException;

/**
 * Handler for zipfiles.
 *
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 * @author <a href="mailto:david.kolb@uni-konstanz.de">David Kolb</a>
 *
 */

public class ZipfileHandler implements ArchiveHandler {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    // buffer to read from files
    private static final byte[] FILEBUFFER = new byte[8192];

    // this unzip method needs an temporary directory to store nested zip files
    // in order to exatract them
    private File m_unzipTempDirectory = null;

    // until this level we will unpack an nested archive
    private int m_maxFolderLevelDepth;

    // stores all files that end with .zip but aren't zipfiles.
    private Collection<File> m_failedFiles = null;

    // stores all dirs that are empty which are excluded in the makeZip method
    private Collection<File> m_emptyDirs = null;

    /**
     * Constructor with ability to set the maximum depth to which nested zip
     * files will be extracted.
     *
     * @param maxExtractionLevelDepth
     *            level to which nested archives are extracted
     * @param tempUnzipDir
     *            Temporary Directory to assist in extraction process. content
     *            will be deleted after process is finished.
     * @throws NoProperParameterException
     *             thrown if ExtractionDepthLimit or TempDirectory aren't set
     *             properly
     */

    public ZipfileHandler(int maxExtractionLevelDepth, File tempUnzipDir)
            throws NoProperParameterException {
        // set the maxExtractionLevelDepth,tempUnzipDir and check if the values
        // passed are valid. If not throw exception.
        if ((maxExtractionLevelDepth >= 0) && (tempUnzipDir != null)) {
            m_maxFolderLevelDepth = maxExtractionLevelDepth;
            m_unzipTempDirectory = tempUnzipDir;
        } else if (maxExtractionLevelDepth < 0) {
            throw new NoProperParameterException(
                    "extraction depth limit is invalid");
        } else {
            throw new NoProperParameterException("temp directory is invalid");
        }
    }

    @Override
    public int getFolderExtractionDepthLimit() {
        return m_maxFolderLevelDepth;
    }

    @Override
    public void setFolderExtractionDepthLimit(int value)
            throws NoProperParameterException {
        if (value >= 0) {
            m_maxFolderLevelDepth = value;
        } else {
            throw new NoProperParameterException("value must be positive");
        }

    }

    @Override
    public File getTempDirectory() {
        return m_unzipTempDirectory;
    }

    @Override
    public void setTempDirectory(File newTempDir)
            throws NoProperParameterException {
        if (newTempDir != null) {
            m_unzipTempDirectory = newTempDir;
        } else {
            throw new NoProperParameterException("TempDir is null");
        }
    }

    @Override
    public void extractZip(File pathToZipfile, File outputFolder)
            throws FileNotFoundException, ZipException,
            NoProperParameterException {
        if (pathToZipfile.exists()) {

            // verify that directory does exist
            if (!outputFolder.exists()) {
                outputFolder.mkdirs();
            }
            // if the output folder is valid we call the help method
            if (outputFolder.isDirectory()) {
                m_failedFiles = new ArrayList<>();
                extractZip(pathToZipfile, outputFolder, 0);
            } else {
                throw new NoProperParameterException(
                        "OutputFolder is a file: " + outputFolder.toString());
            }
        } else {
            throw new FileNotFoundException("Can't find zipfile: "
                    + pathToZipfile.toString());
        }
    }

    /**
     * method to limit the depth to which the ZipfileHandler unpacks an
     * archive.
     *
     * @param pathToZipFile
     *            points to the zip archive that we want to extract
     * @param outputFolder
     *            points to the output location where the archive shall be
     *            extracted to
     * @param folderLevel
     *            current folder depth level in the archive
     * @throws ZipException
     *             if zipfile isn't a zipfile
     *
     */

    private void extractZip(File pathToZipFile, File outputFolder,
            int folderLevel) throws ZipException {
        // offset for creating substring from filenames (is needed to test if
        // file is ".zip" archive)
        int offsetFileEnding = 4;
        int level = folderLevel;

        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(pathToZipFile);

            // take each ZipEntry and extract it
            for (ZipEntry entry : Collections.list(zipfile.entries())) {
                if (!m_unzipTempDirectory.exists()) {
                    m_unzipTempDirectory.mkdirs();
                }

                // nested zip and limit of depth isn't reached
                if ((entry.getName().matches(".*\\.zip"))
                        && (folderLevel < m_maxFolderLevelDepth)) {

                    // extract the current entry to the temp directory
                    // otherwise we can't extract the entries in this
                    // entry
                    extractEntry(zipfile, entry, m_unzipTempDirectory);

                    // construct the new subdirectory in which the new archive
                    // will be extracted
                    File temp =
                            new File(m_unzipTempDirectory.toString(),
                                    entry.getName());
                    File outputFolderSubfolder =
                            new File(outputFolder.toString(), entry.getName()
                                    .substring(
                                            0,
                                            entry.getName().length()
                                                    - offsetFileEnding));

                    // increase level for recursive call because we're going
                    // one level deeper into the archive
                    level++;
                    extractZip(temp, outputFolderSubfolder, level);

                    // decrease level because we are back in our normal level
                    level--;

                    deleteFiles(m_unzipTempDirectory);

                } else {

                    extractEntry(zipfile, entry, outputFolder);
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.severe("Error: outputStream couldn't be initialized: "
                    + e.getMessage());
            e.printStackTrace();
        } catch (ZipException e) {
            // add current zipFile to the list of failed ones
            // iff it's not the root zip we can add it, otherwise the whole zip
            // is invalid so we throw an exception
            if (level > 0) {
                m_failedFiles.add(pathToZipFile);
            } else {
                throw new ZipException(
                        "Specified Root zipfile isn't a zipfile: "
                                + pathToZipFile.toString());
            }
        } catch (IOException e) {
            LOGGER.severe("Error: I/O-Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (zipfile != null) {
                try {
                    zipfile.close();
                } catch (IOException e) {
                    LOGGER.severe("Error: Couldn't close ZipFile.");
                }
            }
        }
    }

    /**
     * returns a list of files that end with .zip but aren't zipfiles.
     * Attention: Only contains the CURRENT location of the file. That means if
     * it's nested it will be in temp/nameOfTheFile.zip
     *
     * No information about the former location is stored anywhere!
     *
     * @return ArrayList with Files we tried to extract but are no zipfiles.
     */

    public Collection<File> getFailedFiles() {
        return m_failedFiles;
    }

    /**
     * deletes all files in specified directory.
     *
     * @param pathName
     *            directory in which files shall be deleted.
     * @throws IOException
     *             if files couldn't be deleted
     */

    public void deleteFiles(File pathName) throws IOException {
        for (File file : pathName.listFiles()) {
            if (file.isDirectory()) {
                deleteFiles(new File(pathName.toString(), file.getName()));
                file.delete();
            } else if (file.isFile()) {
                file.delete();
            } else {
                throw new IOException("couldn't delete file " + file.getName());
            }
        }
    }

    /**
     * private method which extracts a given ZipEntry form a ZipFile to a
     * specified location.
     *
     * @param zipfile
     *            ZipFile which contains the ZipEntry
     * @param entry
     *            ZipEntry which is going to be extracted
     * @param outputFolder
     *            Folder to which the ZipEntry will be extracted
     * @throws FileNotFoundException
     *             thrown if outputstream couldn't be initialized
     * @throws IOException
     *             thrown if Inputstream couldn't be initialized
     */

    private void extractEntry(ZipFile zipfile, ZipEntry entry,
            File outputFolder) throws FileNotFoundException, IOException {

        // create the outputFolder-Hierarchy
        File outputFile = new File(outputFolder, entry.getName());

        // create the directory, if entry is one
        if (entry.isDirectory()) {
            outputFile.mkdirs();
        } else {

            // creating a file, if the entry is a file
            new File(outputFile.getParent()).mkdirs();

            // Streams which will read from the source and write to the target
            // location
            InputStream readStream = null;
            OutputStream writeStream = null;

            try {

                // set up filereader and filewriter
                readStream = zipfile.getInputStream(entry);
                writeStream = new FileOutputStream(outputFile);

                int numberOfBytesToWrite;

                // read #fileBuffer bytes from zipfile and write them in output
                // until all bytes are written
                while ((numberOfBytesToWrite = readStream.read(FILEBUFFER)) > 0) {
                    writeStream.write(FILEBUFFER, 0, numberOfBytesToWrite);
                }
            } finally {
                // if an exception is thrown (by Input or outputStreams we
                // still have to close the filestreams
                if (readStream != null) {
                    readStream.close();
                }
                if (writeStream != null) {
                    writeStream.close();
                }
            }
        }

    }

    /**
     * returns a list of files that are empty dirs which occurred in the
     * makeZip method. Empty Folders will be ignored in this method so we need
     * to document which folders will be ignored.
     *
     * @return ArrayList with Files.
     */

    public Collection<File> getEmptyDirs() {
        return m_emptyDirs;
    }

    /**
     * method which takes a filepath to a folder and searches for all files
     * contained in this folder and its subfolders. All found files will be
     * stored in a list. If a subfolder contains no files the path to this
     * folder also will be added to the list. If the given filepath is a file
     * the list will only contain this file.
     *
     * @param location
     *            Folder we want to get the paths of
     * @param list
     *            LinkedList where we want to save the paths
     * @return returns LinkedList which contains all paths
     */
    private Collection<File> getAllPaths(File location, Collection<File> list) {

        // for each entry of the given folder there are three cases
        for (File file : location.listFiles()) {
            // entry is empty directory so we need to add the path to the list
            if (file.isDirectory() && (file.list().length == 0)) {
                list.add(file);
                // entry is file so we also need to add the path to the list
            } else if (file.isFile()) {
                list.add(file);
                // entry is non empty directory so we need to go deeper
            } else if (file.isDirectory()) {
                getAllPaths(file, list);
                // error message if other case
            } else {
                LOGGER.info("not all cases of getAllPaths are dealt with");
                throw new IllegalStateException(
                        "not all cases of getAllPaths are dealt with");
            }
        }

        return list;
    }

    // maybe to-do: reduce cyclomatic complexity

    /*
     * (non-Javadoc)
     * 
     * @see util.archivehandling.ArchiveHandler#makeZip(java.io.File,
     * java.io.File)
     */
    @Override
    public void makeZip(File inputLocation, File outputLocation)
            throws FileNotFoundException, NoProperParameterException {

        // create InputStream to read files and OutputStream to write zip files
        ZipOutputStream zOutStream = null;
        FileInputStream fInStream = null;

        // ZipEntry which is written to the zip file
        ZipEntry entry = null;

        // self-explaining
        int numberOfBytesToWrite;

        // if the inputlocation is no valid path to a file or a folder an
        // exception is thrown
        if (!inputLocation.isDirectory() && !inputLocation.isFile()) {
            throw new FileNotFoundException("input location was not found");
        }

        try {

            // outputlocation must not be a file
            if (outputLocation.isFile()) {
                throw new NoProperParameterException("output location is file");
            }
            // is the given output directory does not exist we need to create
            // it
            if (!outputLocation.isDirectory()) {
                outputLocation.mkdirs();
            }

            // create correct path of output file
            File outputZip =
                    new File(outputLocation.getPath(), inputLocation.getName()
                            + ".zip");

            // print correct output path to console
            LOGGER.info("ZipFile Output Path: " + outputZip.getPath());

            // create empty zip file at correct output location
            zOutStream = new ZipOutputStream(new FileOutputStream(outputZip));

            // list which contains all paths of files and paths of empty dirs
            // from the folder which we want to zip
            Collection<File> fileList = new ArrayList<>();

            // create new list to store paths of empty dirs
            m_emptyDirs = new ArrayList<>();

            // create all paths we have to write to
            fileList = getAllPaths(inputLocation, fileList);

            // writes every file contained in List to zip file
            for (File path : fileList) {

                // we can only write files to the zipFile
                if (path.isFile()) {
                    fInStream = new FileInputStream(path);

                    // we need to reduce the paths we created above because we
                    // only need the relative path beginning
                    // from the directory where we want to write to
                    String reducedPath =
                            path.getPath().substring(
                                    inputLocation.getPath().length() + 1);
                    entry = new ZipEntry(reducedPath);
                    zOutStream.putNextEntry(entry);

                    // read from fileInputStream and write all bytes to zipFile
                    while ((numberOfBytesToWrite = fInStream.read(FILEBUFFER)) > 0) {
                        zOutStream.write(FILEBUFFER, 0, numberOfBytesToWrite);
                    }

                    zOutStream.closeEntry();
                    fInStream.close();
                    // empty directories can't be written and will be irgnored
                } else if (path.isDirectory()) {
                    // add the path of the empty dir to the list of empty dirs
                    m_emptyDirs.add(path);
                    LOGGER.info("Following folder we want to zip is empty."
                            + " It will be ignored: " + path.toString());
                    // if something unexpected happens
                } else {
                    LOGGER.info("not all cases of makeZip are dealt with");
                    throw new IllegalStateException(
                            "not all cases of makeZip are dealt with");
                }
            }

            // look at log messages
        } catch (FileNotFoundException e) {
            LOGGER.severe("Input or Output Location was not Found: "
                    + inputLocation.toString() + " AND "
                    + outputLocation.toString());
        } catch (IOException e) {
            LOGGER.severe("Cannot begin writing zipFile "
                    + outputLocation.toString());
        } finally {
            if (fInStream != null) {
                try {
                    fInStream.close();
                } catch (IOException e) {
                    LOGGER.severe("Cannot close InputStream");
                }
            }
            if (zOutStream != null) {
                try {
                    zOutStream.close();
                } catch (IOException e) {
                    LOGGER.severe("Cannot close OutputStream");
                }
            }
        }
    }

}
