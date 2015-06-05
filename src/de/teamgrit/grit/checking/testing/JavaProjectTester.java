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

package de.teamgrit.grit.checking.testing;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import de.teamgrit.grit.checking.TestOutput;
import de.teamgrit.grit.checking.compile.RegexDirectoryWalker;

/**
 * This class provides methods to run a JUnit test a given submission provided
 * that there are JUnit tests files.
 *
 * @author <a href=mailto:thomas.3.schmidt@uni-konstanz.de>Thomas Schmidt</a>
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 *
 */

public class JavaProjectTester implements Tester {
    Logger LOGGER = Logger.getLogger("systemlog");
    private Path testLocation;
    private List<String> tests;

    /**
     * Constructor, creates a {@link Tester} from a path to a directory where
     * the test lie.
     *
     * @param tests
     *            Directory to the tests, in the {@link Path} constructor there
     *            should be a path.separator at the end, indicating that it's a
     *            directory ({@link URLClassLoader} wants it that way)
     */
    public JavaProjectTester(Path tests) {
        testLocation = tests;
    }

    /**
     * Runs tests in testLocation on the source code given in the parameter.
     *
     * @param submissionBinariesLocation
     *            The path to the compiled binaries of the submission.
     *
     * @return the {@link TestOutput} containing the test results.
     *
     * @throws ClassNotFoundException
     *             Throws if the loaded Classes are not found
     * @throws IOException
     *             Throws if the sourceCodeLocation is malformed or the
     *             {@link URLClassLoader} can't be closed.
     */
    @Override
    public TestOutput testSubmission(Path submissionBinariesLocation)
            throws ClassNotFoundException, IOException {

        // if there are no tests create and empty TestOutput with didTest false
        if ((testLocation == null) || testLocation.toString().isEmpty()) {
            return new TestOutput(null, false);
        } else {

            List<Result> results = new LinkedList<>();

            // create the classloader
            URL submissionURL = submissionBinariesLocation.toUri().toURL();
            URL testsURL = testLocation.toUri().toURL();

            // URLClassLoader loader =
            // new URLClassLoader(new URL[] { submissionURL, testsURL });

            URLClassLoader loader =
                    new URLClassLoader(new URL[] { testsURL, submissionURL });

            // iterate submission source code files and load the .class files.
            /*
             * We need to iterate all files in a directory and thus are using
             * the apache commons io utility FileUtils.listFiles. This needs a)
             * the directory, and b) a file filter for which we also use the
             * one supplied by apache commons io.
             */

            Path submissionBin = submissionBinariesLocation.toAbsolutePath();
            List<Path> exploreDirectory =
                    exploreDirectory(submissionBin, ExplorationType.CLASSFILES);
            for (Path path : exploreDirectory) {

                String quallifiedName = getQuallifiedName(submissionBin, path);
                loader.loadClass(quallifiedName);
            }

            // iterate tests, load them and run them
            Path testLoc = testLocation.toAbsolutePath();
            List<Path> exploreDirectory2 =
                    exploreDirectory(testLoc, ExplorationType.SOURCEFILES);
            for (Path path : exploreDirectory2) {

                String unitTestName = getQuallifiedNameFromSource(path);
                try {
                    Class<?> testerClass = loader.loadClass(unitTestName);
                    Result runClasses = JUnitCore.runClasses(testerClass);
                    results.add(runClasses);
                } catch (Throwable e) {
                    LOGGER.severe("can't load class: " + unitTestName);
                    LOGGER.severe(e.getMessage());
                }
            }

            loader.close();
            // creates new TestOutput from results and returns it
            return new TestOutput(results, true);
        }
    }

    /**
     * Creates the qualified name form a source file.
     *
     * @param path
     *            the path to the sourcefile
     * @return the qualified name
     * @throws IOException
     *             in case of io errors
     */
    private String getQuallifiedNameFromSource(Path path) throws IOException {

        final String packageRegex = "package\\s[^,;]+;";
        LineIterator it;
        String result = "";
        it = FileUtils.lineIterator(path.toFile(), "UTF-8");

        while (it.hasNext()) {
            String line = it.nextLine();
            if (line.matches(packageRegex)) {
                result = line;
                // strip not needed elements
                result = result.substring(8, result.length() - 1);
                // append classname
                return result + "."
                        + FilenameUtils.getBaseName(path.toString());
            }
        }
        return FilenameUtils.getBaseName(path.toString());
    }

    /**
     * Creates the fully qualified name of a class based on its location
     * relative to a base directory.
     *
     * @param basePath
     *            the base path
     * @param subPath
     *            the path to the classfile, must be a located bellow basepath
     *            in the directory tree
     * @return the fully name of the class
     */
    private String getQuallifiedName(Path basePath, Path subPath) {
        // relative resolution
        String quallifiedName =
                StringUtils
                        .difference(basePath.toString(), subPath.toString());
        quallifiedName = quallifiedName.substring(1);
        quallifiedName = FilenameUtils.removeExtension(quallifiedName);
        quallifiedName = StringUtils.replaceChars(quallifiedName, '/', '.');

        return quallifiedName;
    }

    /**
     * Finds all .class files in the specified folder and its sub folders.
     *
     * @param pathToFolder
     *            the path to the folder
     * @return found .class files
     */
    private
            List<Path>
            exploreDirectory(Path pathToFolder, ExplorationType type) {
        String regex;
        switch (type) {
        case CLASSFILES:
            regex = ".+\\.class";
            break;
        case SOURCEFILES:
            regex = ".+\\.java";
            break;
        default:
            throw new IllegalArgumentException(
                    "Exploration typ not implemented");
        }

        RegexDirectoryWalker dirWalker = new RegexDirectoryWalker(regex);
        try {
            Files.walkFileTree(pathToFolder, dirWalker);
        } catch (IOException e) {
            LOGGER.severe("Could not walk submission "
                    + pathToFolder.toString()
                    + " while building tester invocation: " + e.getMessage());
        }
        return dirWalker.getFoundFiles();
    }

    /**
     * Indicates the filetype
     *
     */
    private enum ExplorationType {
        SOURCEFILES, CLASSFILES
    }
}
