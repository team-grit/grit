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

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Implementation of the FileVisitor pattern using the Java SimpleFileVisitor.
 * For each file that is visited we check if it matches the given Regex. If so
 * we set the flag foundMatchingFiles (retrievable via matchesFound()) to true.
 * A submission is plausible, if the flag is true after the traversal.
 *
 * Do not reuse this class for a second traversal, as the flag is never reset.
 *
 * @author <a href=mailto:marvin.guelzow@uni-konstanz.de>Marvin Guelzow</a>
 *
 */

public class SubmissionDirectoryWalker extends SimpleFileVisitor<Path> {

    private String m_submissionFileRegex = "";
    private boolean m_foundMatchingFiles = false;

    /**
     * Constructor.
     *
     * @param newFileRegex
     *            is the regex for file that must be contained somewhere for
     *            the submission to be plausible.
     */
    public SubmissionDirectoryWalker(String newFileRegex) {
        m_submissionFileRegex = newFileRegex;
    }

    /**
     * After traversal this returns true, if files required for plausibility
     * have been found.
     *
     * @return True, if matching files were found during traversal. False
     *         otherwise, or when nothing has ben traversed yet.
     */
    public boolean matchesFound() {
        return m_foundMatchingFiles;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (file.toString().matches(m_submissionFileRegex)) {
            m_foundMatchingFiles = true;
            return FileVisitResult.TERMINATE;
        } else {
            return FileVisitResult.CONTINUE;
        }
    }
}
