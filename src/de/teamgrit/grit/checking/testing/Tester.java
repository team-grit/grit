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
import java.net.URLClassLoader;
import java.nio.file.Path;

import de.teamgrit.grit.checking.TestOutput;
import de.teamgrit.grit.preprocess.tokenize.Submission;

/**
 * Interface for the Testers, A tester receives a Path to a folder containing
 * compiled tests which it tests a {@link Submission}.
 *
 * @author <a href=mailto:thomas.3.schmidt@uni-konstanz.de>Thomas Schmidt</a>
 *
 */

public interface Tester {

    /**
     * returns an {@link TestOutput} after checking Submission with the results
     * of the Tests.
     *
     * @param submissionCodeLocation
     *            The {@link Submission} to be tested.
     *
     * @return the {@link TestOutput} containing the test results.
     *
     * @throws ClassNotFoundException
     *             Throws if the loaded Classes are not found
     * @throws IOException
     *             Throws if the sourceCodeLocation ist malformed or the
     *             {@link URLClassLoader} can't be closed
     */
    TestOutput testSubmission(Path submissionCodeLocation)
            throws ClassNotFoundException, IOException;

}
