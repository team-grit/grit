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

package de.teamgrit.grit.checking.plausibility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import de.teamgrit.grit.entities.LanguageType;

/**
 * Provides the static function.
 * 
 * @author <a href=mailto:marvin.guelzow@uni-konstanz.de>Marvin Guelzow</a>
 * @author <a href="mailto:david.kolb@uni-konstanz.de">David Kolb</a>
 * 
 */

public class SubmissionPlausibilityChecker {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    /**
     * The source code location directory is traversed and it is checked
     * whether requried files are present. Then the corresponding flag in the
     * submission is set.
     * 
     * @param sourceLocation
     *            The path of the sourceCodeLocation that should be checked.
     * @param suffixRegex
     *            How a file must look for it to be accepted.
     * @return true if files where found, false otherwise
     */
    private static boolean checkLocation(Path sourceLocation, String suffixRegex) {
        SubmissionDirectoryWalker subWalker = new SubmissionDirectoryWalker(
                suffixRegex);
        try {
            Files.walkFileTree(sourceLocation, subWalker);
        } catch (IOException e) {
            LOGGER.severe("Could not walk submission "
                    + sourceLocation.toString()
                    + " while checking for plausibility: " + e.getMessage());
        }
        return subWalker.matchesFound();
    }

    /**
     * Checks in a specified directory whether files of the specified language
     * type are occuring.
     * 
     * @param sourceLocation
     *            the directory which will be searched in
     * @param languageType
     *            the language type that is being looked for
     * @return true, if there is a file, false if there is none
     */
    public static boolean checkLocation(Path sourceLocation,
            LanguageType languageType) {
        String regex;
        switch (languageType) {
        case JAVA:
            regex = ".+\\.[Jj][Aa][Vv][Aa]";
            break;
        case C:
            regex = ".+\\.[Cc]";
            break;
        case CPP:
            regex = ".+\\.[Cc][Pp][Pp]";
            break;
        case HASKELL:
            regex = ".+\\.([Ll])?[Hh][Ss]";
            break;
        default:
            throw new IllegalArgumentException(
                    "Language Type is not supported!");

        }
        return checkLocation(sourceLocation, regex);
    }
}
