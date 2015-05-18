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

package de.teamgrit.grit.checking.compile;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;

import de.teamgrit.grit.checking.CompilerOutput;

/**
 * Interface which a compile checker must provide. A compile checker determines
 * whether a submitted program compiles or not, by using a specified compiler
 * on a set of submitted files.
 *
 * @author <a href=mailto:marvin.guelzow@uni-konstanz.de>Marvin Guelzow</a>
 *
 */

public interface CompileChecker {
    /**
     * @param pathToProgramFile
     *            The Path to a File that is to be compiled. Dependencies on
     *            other files are resolved automatically, if a valid package
     *            structure is found.
     * @param outputFolder
     *            The file compiler output is written to.
     * @param compilerFlags
     *            Additional flags to be passed to the compiler, each as a list
     *            item.
     * @param compilerName
     *            Name of the compiler that should be called. For example
     *            "javac" or "openjavac" if both oracle javac and openjdk are
     *            installed.
     * @return A boolean value indicating whether the program did compile
     *         (true) or not (false)
     * @throws FileNotFoundException
     *             When the given sourcefile cannot be found.
     * @throws BadCompilerSpecifiedException
     *             When the given compiler can't be located.
     * @throws BadFlagException
     *             When javac doesn't recognize a flag, this exception is
     *             thrown.
     * @throws CompilerOutputFolderExistsException
     *             When the given output folder already exists. We require it to
     *             not exist, since we might stomp on important files
     *             otherwise.
     */

    CompilerOutput checkProgram(Path pathToProgramFile, Path outputFolder,
            String compilerName, List<String> compilerFlags)
            throws FileNotFoundException, BadCompilerSpecifiedException,
            BadFlagException, CompilerOutputFolderExistsException;

    /**
     * Convenience Method to call without specifying an output folder. .class
     * files are placed in sourceFolder.
     *
     * @param pathToProgramFile
     *            See other method.
     * @param compilerName
     *            See other method.
     * @param compilerFlags
     *            See other method.
     * @return See other method.
     * @throws FileNotFoundException
     *             See other method.
     * @throws BadCompilerSpecifiedException
     *             See other method.
     * @throws BadFlagException
     *             See other method.
     */
    CompilerOutput checkProgram(Path pathToProgramFile, String compilerName,
            List<String> compilerFlags) throws FileNotFoundException,
            BadCompilerSpecifiedException, BadFlagException;

}
