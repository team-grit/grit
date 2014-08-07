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

package checking.compile;

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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import checking.CompilerOutput;

/**
 * This Compiler verifies whether a given file written in c-code is valid or
 * not. The value of the outputPath doesn't mean anything. We don't use this
 * anyway, because we're compiling with -c flag.
 * 
 * @author <a href=mailto:eike.heinz@uni-konstanz.de>Eike Heinz</a>
 * 
 */

public class GccCompileChecker implements CompileChecker {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    /**
     * Constructor.
     */

    public GccCompileChecker() {
        /*
         * Here used to be a check whether GCC is installed, sadly we can't
         * check the system's PATH because Windows does things differently
         */
    }

    @Override
    public CompilerOutput checkProgram(Path pathToProgramFile,
            Path outputFolder, String compilerName, List<String> compilerFlags)
            throws FileNotFoundException, BadCompilerSpecifiedException,
            BadFlagException {

        // First we build the command to invoke the compiler. This consists of
        // the compiler executable, the path of the
        // file to compile and compiler flags. So for example we call:
        List<String> compilerInvocation = createCompilerInvocation(
                pathToProgramFile, compilerName, compilerFlags);
        // Now we build a launchable process from the given parameters and set
        // the working directory.
        Process compilerProcess = null;

        try {
            ProcessBuilder compilerProcessBuilder = new ProcessBuilder(
                    compilerInvocation);
            // make sure the compiler stays in its directory.
            if (Files.isDirectory(pathToProgramFile)) {
                compilerProcessBuilder.directory(pathToProgramFile.toFile());
            } else {
                compilerProcessBuilder.directory(pathToProgramFile.getParent()
                        .toFile());
            }
            compilerProcess = compilerProcessBuilder.start();
        } catch (IOException e) {
            // If we cannot call the compiler we return a CompilerOutput
            // initialized with false, false, indicating
            // that the compiler wasn't invoked properly and that there was no
            // clean Compile.
            CompilerOutput compilerInvokeError = new CompilerOutput();
            compilerInvokeError.setClean(false);
            compilerInvokeError.setCompilerInvoked(false);
            LOGGER.severe("Couldn't launch GCC. Check whether it's in the system's PATH");
            return compilerInvokeError;
        }

        // Now we read compiler output. If everything is ok gcc reports
        // nothing at all.
        InputStream compilerOutputStream = compilerProcess.getErrorStream();
        InputStreamReader compilerStreamReader = new InputStreamReader(
                compilerOutputStream);
        BufferedReader compilerOutputBuffer = new BufferedReader(
                compilerStreamReader);
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
            // Reading might go wrong here if gcc should unexpectedly terminate
            LOGGER.severe("Error while reading from compiler stream.");
            compilerOutput.setClean(false);
            compilerOutput.setCompileStreamBroken(true);
            return compilerOutput;
        }

        if (compilerOutputLines.size() == 0) {
            compilerOutput.setClean(true);
        }

        compilerOutput = splitCompilerOutput(compilerOutputLines,
                compilerOutput);

        // delete all .o and .exe files
        // these are output files generated by gcc which we won't need
        // anymore
        File[] candidateToplevelFiles = pathToProgramFile.toFile().listFiles();
        for (File candidateFile : candidateToplevelFiles) {
            if (!candidateFile.isDirectory()) {
                String extension = FilenameUtils.getExtension(candidateFile
                        .toString());
                if (extension.matches("([Oo]|([Ee][Xx][Ee]))")) {
                    // We only pass the filename, since gcc will be
                    // confined to the dir the file is located in.
                    candidateFile.delete();
                }
            }

        }

        return compilerOutput;
    }

    /**
     * This Method generates the command required to start the compiler. It
     * generates a list of strings that can be passed to a process builder.
     * 
     * @param pathToProgramFile
     *            Where to look for the main file that will be compiled.
     * @param compilerName
     *            Which compiler to call
     * @param compilerFlags
     *            User supplied flags to be passed
     * @throws BadCompilerSpecifiedException
     *             When no compiler is given.
     * @throws FileNotFoundException
     *             When the file to be compiled does not exist
     * @return List of string with the command for the process builder.
     */
    private List<String> createCompilerInvocation(Path pathToProgramFile,
            String compilerName, List<String> compilerFlags)
            throws BadCompilerSpecifiedException, FileNotFoundException {

        List<String> compilerInvocation = new LinkedList<>();
        // We need a compiler name. Without it we cannot compile anything and
        // abort.
        if (("".equals(compilerName)) || (compilerName == null)) {
            throw new BadCompilerSpecifiedException("No compiler specified.");
        } else {
            // search for a makefile
            Path programDirectory = null;
            if (!Files.isDirectory(pathToProgramFile)) {
                programDirectory = pathToProgramFile.getParent();
            } else {
                programDirectory = pathToProgramFile;
            }
            Collection<File> fileList = FileUtils.listFiles(
                    programDirectory.toFile(),
                    FileFilterUtils.fileFileFilter(), null);

            boolean hasMakefile = false;
            for (File f : fileList) {
                if (f.getName().matches("[Mm][Aa][Kk][Ee][Ff][Ii][Ll][Ee]")) {
                    hasMakefile = true;
                }
            }

            if (hasMakefile) {
                // add the necessary flags for make and return the invocation -
                // we don't need more flags or parameters
                LOGGER.info("Found make-file. Compiling c-code with make.");
                compilerInvocation.add("make");
                compilerInvocation.add("-k");
                compilerInvocation.add("-s");
                return compilerInvocation;
            } else {
                compilerInvocation.add(compilerName);
                LOGGER.info("Compiling c-code with " + compilerName);
            }
        }

        // If compiler flags are passed, append them after the compiler name.
        // If we didn't get any we append nothing.
        if ((compilerFlags != null) && !(compilerFlags.isEmpty())) {
            if (!compilerFlags.contains("-c")) {
                compilerFlags.add("-c");
            }
            compilerInvocation.addAll(compilerFlags);
        }

        // Check for the existance of the program file we are trying to
        // compile.
        if ((pathToProgramFile == null)
                || (pathToProgramFile.compareTo(Paths.get("")) == 0)) {
            throw new FileNotFoundException("No file to compile specified");
        } else {

            if (Files.isDirectory(pathToProgramFile, LinkOption.NOFOLLOW_LINKS)) {
                // we have to be able to compile several .c files at the same
                // time so we need to find them
                RegexDirectoryWalker dirWalker = new RegexDirectoryWalker(
                        ".+\\.[Cc][Pp]?[Pp]?");
                try {
                    Files.walkFileTree(pathToProgramFile, dirWalker);
                } catch (IOException e) {
                    LOGGER.severe("Could not walk submission "
                            + pathToProgramFile.toString()
                            + " while building compiler invocation: "
                            + e.getMessage());
                }
                for (Path matchedFile : dirWalker.getFoundFiles()) {
                    compilerInvocation.add(matchedFile.toString().substring(
                            pathToProgramFile.toString().length() + 1));
                }
            } else {
                throw new FileNotFoundException(
                        "Program file that should be compiled does not exist."
                                + "Filename : \""
                                + pathToProgramFile.toString() + "\"");
            }
        }
        return compilerInvocation;
    }

    /**
     * splitCompilerOutput splits the compiler output into errors, warnings and
     * infos, stores that data into the {@link CompilerOutput} and sets
     * appropriate flags (clean compile etc.).
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
    private CompilerOutput splitCompilerOutput(List<String> lines,
            CompilerOutput compilerOutput) throws BadFlagException {
        // We aggregate lines into the string builder until we can recognize
        // them as a warning or as an
        // error. Then we write them into the output object and clear this
        // builder.
        StringBuilder currentCompilerNote = new StringBuilder();
        boolean isError = false;
        boolean isWarning = false;
        boolean isInfo = false;

        // Go through every compiler line, checking for errors and warnings
        for (String line : lines) {
            if (line.matches("gcc: error: unrecognized command line option.*")) {
                throw new BadFlagException("Flag not supported. " + line);
            } else if (line.matches(".*error.*") || isError) {
                // begin of an error message
                currentCompilerNote.append(line + "\n");
                isError = true;
                // the second regex matches for
                // <filename>.<ending>:column:line: <error
                // message>
                if (line.matches("\\s*\\^\\s*")
                        || line.matches(".*\\..{1}\\d+.{1}\\d+.{1}.*\\.")) {
                    // end of an error message
                    compilerOutput.addError(currentCompilerNote.toString());
                    isError = false;
                    currentCompilerNote = new StringBuilder();
                }

            } else if (line.matches(".*warning.*") || isWarning) {
                // begin of a warning message
                currentCompilerNote.append(line + "\n");
                isWarning = true;
                // second regex matches the same as the one for errors
                if (line.matches("\\s*\\^\\s*")
                        || line.matches(".*\\..{1}\\d+.{1}\\d+.{1}.*\\.")) {
                    // end of a warning message
                    compilerOutput.addWarning(currentCompilerNote.toString());
                    isWarning = false;
                    currentCompilerNote = new StringBuilder();
                }
            } else if (line.matches(".*note.*") || isInfo) {
                // hopefully notes are only one line long
                compilerOutput.addInfo(line + "\n");
                isInfo = true;
                if (line.matches(".*\\.")) {
                    isInfo = false;
                    compilerOutput.addInfo(currentCompilerNote.toString());
                    currentCompilerNote = new StringBuilder();
                }
            }
        }
        if (isError) {
            compilerOutput.addError(currentCompilerNote.toString());
        } else if (isWarning) {
            compilerOutput.addWarning(currentCompilerNote.toString());
        } else {
            compilerOutput.addInfo(currentCompilerNote.toString());
        }
        return compilerOutput;
    }

    @Override
    public CompilerOutput checkProgram(Path pathToProgramFile,
            String compilerName, List<String> compilerFlags)
            throws FileNotFoundException, BadCompilerSpecifiedException,
            BadFlagException {
        return checkProgram(pathToProgramFile, null, compilerName,
                compilerFlags);

    }

}
