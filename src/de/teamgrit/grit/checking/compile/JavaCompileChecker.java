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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import de.teamgrit.grit.checking.CompilerOutput;

/**
 * This class provides the means to check submissions in java for correct
 * compilation.
 *
 * @author <a href=mailto:marvin.guelzow@uni-konstanz.de>Marvin Guelzow</a>
 * @author <a href=mailto:eike.heinz@uni-konstanz.de>Eike Heinz</a>
 *
 */

public class JavaCompileChecker implements CompileChecker {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    // folder containing the junit jar
    private final Path m_junitLocation = Paths
            .get("res", "javac", "junit.jar").toAbsolutePath();
    // folder containing additional libraries 
    private final Path m_libLocation = Paths
    		.get("res", "javalib").toAbsolutePath();
    
    private Path m_junitTestFilesLocation = null;

    /**
     * The constructor for the compileChecker only checks if javac can be found
     * in PATH on windows systems.
     *
     * @param pathToJunitTests
     *            points to the location of the provided JUnit test files. If
     *            none are given this parameter must be null.
     *
     */
    public JavaCompileChecker(Path pathToJunitTests) {
        m_junitTestFilesLocation = pathToJunitTests.toAbsolutePath();
    }

    /**
     * Invokes the compiler on a given file and reports the output.
     *
     * @param pathToSourceFolder
     *            Specifies the folder where source files are located.
     * @param outputFolder
     *            Directory where the resulting binaries are placed
     * @param compilerName
     *            The compiler to be used (usually javac).
     * @param compilerFlags
     *            Additional flags to be passed to the compiler.
     * @throws FileNotFoundException
     *             Is thrown when the file in pathToProgramFile cannot be
     *             opened
     * @throws BadCompilerSpecifiedException
     *             Is thrown when the given compiler cannot be called
     * @return A {@link CompilerOutput} that contains all compiler messages and
     *         flags on how the compile run went.
     * @throws BadFlagException
     *             When javac doesn't recognize a flag, this exception is
     *             thrown.
     * @throws CompilerOutputFolderExistsException
     *             The output folder may not exist. This is thrown when it does
     *             exist.
     */
    @Override
    public CompilerOutput checkProgram(
            Path pathToSourceFolder, Path outputFolder, String compilerName,
            List<String> compilerFlags)
            throws FileNotFoundException, BadCompilerSpecifiedException,
            BadFlagException, CompilerOutputFolderExistsException {

        // First we build the command to invoke the compiler. This consists of
        // the compiler executable, the path of the
        // file to compile and compiler flags.
    	
    	LOGGER.info("Checkprogram Name: " + compilerName);
        List<String> compilerInvocation =
                createCompilerInvocation(pathToSourceFolder, outputFolder,
                        compilerName, compilerFlags);
        // Now we build a launchable process from the given parameters and set
        // the working directory.
        CompilerOutput result =
                runJavacProcess(compilerInvocation, pathToSourceFolder, false);

        compilerInvocation.clear();

        compilerInvocation.add("javac");
        compilerInvocation.add("-cp");
        // Add testDependencies to classpath
        String cp = ".:" + m_junitLocation + ":" + outputFolder.toAbsolutePath();        
        // Add all additional .jar files contained in javalib directory to the classpath
        if (!m_libLocation.toFile().exists()){
        	m_libLocation.toFile().mkdir();
        } else {
        	for(File f : FileUtils.listFiles(m_libLocation.toFile(), new String[]{"jar"}, false)){
        		cp = cp + ":" + f.getAbsolutePath(); 		
        	}
        }
        compilerInvocation.add(cp);
        
        //make sure java uses utf8 for encoding
        compilerInvocation.add("-encoding");
        compilerInvocation.add("UTF-8");

        compilerInvocation.add("-d");
        compilerInvocation.add(m_junitTestFilesLocation.toAbsolutePath()
                .toString());
        List<Path> foundUnitTests = exploreDirectory(m_junitTestFilesLocation);
        for (Path path : foundUnitTests) {
            compilerInvocation.add(path.toAbsolutePath().toString());
        }
        runJavacProcess(compilerInvocation, m_junitTestFilesLocation, true);
        return result;

    }

    /**
     * Runs a command specified by a compiler invocation.
     *
     * @param compilerInvocation
     *            specifies what program with which flags is being executed
     * @param pathToSourceFolder
     *            the path to the source folder of the submission
     * @return CompilerOutput with fields initialized according to the outcome
     *         of the process
     * @throws BadFlagException
     *             if a flag is not known to javac
     */
    private CompilerOutput runJavacProcess(
            List<String> compilerInvocation, Path pathToSourceFolder,
            boolean junit) throws BadFlagException {
        Process compilerProcess = null;
        try {
            ProcessBuilder compilerProcessBuilder =
                    new ProcessBuilder(compilerInvocation);
            // make sure the compiler stays in its directory.
            if (Files.isDirectory(pathToSourceFolder,
                    LinkOption.NOFOLLOW_LINKS)) {	
                compilerProcessBuilder.directory(pathToSourceFolder.toFile());
            } else {
                compilerProcessBuilder.directory(pathToSourceFolder
                        .getParent().toFile());
            }
            compilerProcess = compilerProcessBuilder.start();
        } catch (IOException e) {
            // If we cannot call the compiler we return a CompilerOutput
            // which is
            // initialized with false, false, indicating
            // that the compiler wasn't invoked properly and that there was no
            // clean Compile.
            CompilerOutput compilerInvokeError = new CompilerOutput();
            compilerInvokeError.setClean(false);
            compilerInvokeError.setCompilerInvoked(false);
            return compilerInvokeError;
        }

        // Now we read compiler output. If everything is ok javac reports
        // nothing at all.
        InputStream compilerOutputStream = compilerProcess.getErrorStream();
        InputStreamReader compilerStreamReader =
                new InputStreamReader(compilerOutputStream);
        BufferedReader compilerOutputBuffer =
                new BufferedReader(compilerStreamReader);
        String line;

        CompilerOutput compilerOutput = new CompilerOutput();
        compilerOutput.setCompilerInvoked(true);

        List<String> compilerOutputLines = new LinkedList<>();

        try {
            while ((line = compilerOutputBuffer.readLine()) != null) {
                compilerOutputLines.add(line);
            }

            compilerOutputStream.close();
            compilerStreamReader.close();
            compilerOutputBuffer.close();
            compilerProcess.destroy();
        } catch (IOException e) {
            // Reading might go wrong here if javac should unexpectedly
            // terminate
            LOGGER.severe("Could not read compiler ourput from its output stream."
                    + " Aborting compile of: "
                    + pathToSourceFolder.toString()
                    + " Got message: " + e.getMessage());
            compilerOutput.setClean(false);
            compilerOutput.setCompileStreamBroken(true);
            return compilerOutput;
        }

        splitCompilerOutput(compilerOutputLines, compilerOutput);

        if (compilerOutputLines.size() == 0) {
            compilerOutput.setClean(true);
        }

        return compilerOutput;
    }

    /**
     * This Method generates the command required to start the compiler. It
     * generates a list of strings that can be passed to a process builder.
     *
     * @param pathToSourceFolder
     *            Where to look for the source files that will be compiled.
     * @param outputFolder
     *            Where .class files will be placed
     * @param compilerName
     *            Which compiler to call
     * @param compilerFlags
     *            User supplied flags to be passed
     * @throws BadCompilerSpecifiedException
     *             When no compiler is given.
     * @throws FileNotFoundException
     *             When the file to be compiled does not exist
     * @throws CompilerOutputFolderExistsException
     *             The output folder may not exist. This is thrown when it does
     *             exist.
     * @return List of string with the command for the process builder.
     */
    private List<String> createCompilerInvocation(
            Path pathToSourceFolder, Path outputFolder, String compilerName,
            List<String> compilerFlags)
            throws BadCompilerSpecifiedException, FileNotFoundException,
            CompilerOutputFolderExistsException {

        List<String> compilerInvocation = new LinkedList<>();
        // Check if a compiler has been supplied. Without one we abort
        // compiling. Else we start building the compiler command.
        if (("".equals(compilerName)) || (compilerName == null)) {
            throw new BadCompilerSpecifiedException("No compiler specified.");
        } else {
            compilerInvocation.add(compilerName);
        }

        // If compiler flags are passed, append them after the compiler name.
        // If we didn't get any we append nothing. Appending empty strings or
        // nulls to the compiler invocation must be avoided, since it can cause
        // javac to crash. Hence, we ignore such entries.
        if ((compilerFlags != null) && (!(compilerFlags.isEmpty()))) {
            for (String flag : compilerFlags) {
                // If javac gets passed a "" it dies due to a bug, so avoid
                // this
                if ((flag != null) && !"".equals(flag)) {
                    compilerInvocation.add(flag);
                }
            }
        }

        // Add JUnit and the current directory to the classpath.s
        compilerInvocation.add("-cp");   
        String cp = ".:" + m_junitLocation.toAbsolutePath().toString();
        // Add all additional .jar files contained in javalib directory to the classpath
        if (!m_libLocation.toFile().exists()){
        	m_libLocation.toFile().mkdir();
        } else {
        	for(File f : FileUtils.listFiles(m_libLocation.toFile(), new String[]{"jar"}, false)){
        		cp = cp + ":" + f.getAbsolutePath(); 		
        	}
        }
        compilerInvocation.add(cp);
        
        //make sure java uses utf8 for encoding
        compilerInvocation.add("-encoding");
        compilerInvocation.add("UTF-8");

        // Check for the existence of the program file we are trying to
        // compile.
        if ((pathToSourceFolder == null)
                || !(pathToSourceFolder.toFile().exists())) {
            throw new FileNotFoundException("No file to compile specified");
        } else {
            if (Files.isDirectory(pathToSourceFolder,
                    LinkOption.NOFOLLOW_LINKS)) {
                // we are supposed to compile a folder. Hence we'll scan for
                // java files and pass them to the compiler.
                List<Path> foundFiles = exploreDirectory(pathToSourceFolder);
                for (Path matchedFile : foundFiles) {
                    compilerInvocation.add(matchedFile.toFile()
                            .getAbsolutePath());
                }

                compilerInvocation.add("-d");
                compilerInvocation.add(outputFolder.toAbsolutePath()
                        .toString());
            } else {
                throw new FileNotFoundException(
                        "Program file that should be compiled does not exist."
                                + "Filename : \""
                                + pathToSourceFolder.toString() + "\"");
            }
        }

        return compilerInvocation;
    }

    /**
     * Provides a DirectoryWalker to find submissions matching the '.java' file
     * extension.
     *
     * @param pathToSourceFolder
     *            the folder in which the DirectoryWalker will search for files
     * @return a list of found files
     */
    private List<Path> exploreDirectory(Path pathToSourceFolder) {
        RegexDirectoryWalker dirWalker =
                new RegexDirectoryWalker(".+\\.[Jj][Aa][Vv][Aa]");
        try {
            Files.walkFileTree(pathToSourceFolder, dirWalker);
        } catch (IOException e) {
            LOGGER.severe("Could not walk submission "
                    + pathToSourceFolder.toString()
                    + " while building compiler invocation: " + e.getMessage());
        }
        return dirWalker.getFoundFiles();
    }

    /**
     * Populates the compiler output with the compiler output split into
     * errors, warnings and infos, stores that data into the
     * {@link CompilerOutput} and sets appropriate flags (clean compile etc.).
     *
     * @param lines
     *            Is the raw compiler output, one line per string in a list
     * @param compilerOutput
     *            Is the {@link CompilerOutput} we are going to put the data
     *            into
     * @return A {@link CompilerOutput} containing the data of the compile run.
     * @throws BadFlagException
     *             When javac has found a bad flag this can only be found after
     *             it has run, hence we throw it here.
     */
    private CompilerOutput splitCompilerOutput(
            List<String> lines, CompilerOutput compilerOutput)
            throws BadFlagException {

        // We aggregate lines into the string builder until we can recognize
        // them as a warning or as an error. Then we write them into the output
        // object and clear this builder.
        StringBuilder currentCompilerNote = new StringBuilder();

        // Go through every compiler line, checking for errors and warnings
        for (String line : lines) {
            if (line.matches("javac: invalid flag:.*")) {
                throw new BadFlagException("Flag not supported. " + line);
            } else if (line.matches("\\s*\\^\\s*")) {
                // Matches whitespaces and a ^ pointing to the error location.
                // This is the final line indicating an error.
                String compilerError = currentCompilerNote.toString();
                compilerOutput.addError(compilerError);
                compilerOutput.setClean(false);
                currentCompilerNote = new StringBuilder();
            } else if (line.matches("^Note: .*.$")
                    || line.matches("^javac: .*.$")) {
                // Notes are actually warnings from javac, like deprecation
                // warnings.
                compilerOutput.setClean(false);
                compilerOutput.addWarning(line);
            } else {
                // We might be within a multiline error message, so keep
                // collecting.
                currentCompilerNote.append(line + "\n");
            }
        }
        return compilerOutput;
    }

    @Override
    public CompilerOutput checkProgram(
            Path pathToProgramFile, String compilerName,
            List<String> compilerFlags)
            throws FileNotFoundException, BadCompilerSpecifiedException,
            BadFlagException {
        try {
            return checkProgram(pathToProgramFile, pathToProgramFile,
                    compilerName, compilerFlags);
        } catch (CompilerOutputFolderExistsException e) {
            // If this exception is thrown we have found a bug in this module,
            // since the compiler should be able to write output files
            // alongside the source files.
            throw new RuntimeException(
                    "Bug in compiler: Source folder as output "
                            + "folder not recognized properly!");
        }
    }
}
