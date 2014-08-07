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
 * more details. You should have received a copy of the GNU General Public
 * License along with this program. If not, see <http://www.gnu.org/licenses/>.
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
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;

import checking.CompilerOutput;

/**
 * This class provides the means to check submissions in Haskell for correct
 * compilation.
 * 
 * @author <a href=mailto:david.kolb@uni-konstanz.de>David Kolb</a>
 * 
 */

public class HaskellCompileChecker implements CompileChecker {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    /**
     * The constructor for the compileChecker only checks if ghci can be found
     * in PATH on windows systems.
     * 
     */
    public HaskellCompileChecker() {
        if (!System.getenv("PATH").contains("ghci")
                && ("Windows".equals(System.getProperty("os.name")))) {
            LOGGER.severe("No ghci in path. This is required on Windows."
                    + "Please append the path to ghci"
                    + " to your system PATH variable.");
        }
    }

    /**
     * checkProgram invokes the Haskell compiler on a given file and reports
     * the output.
     * 
     * @param pathToProgramFile
     *            Specifies the file or folder containing that should be
     *            compiled. (accepts .lhs and .hs files)
     * @param compilerName
     *            The compiler to be used (usually ghc).
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
     *             When ghc doesn't recognize a flag, this exception is thrown.
     */
    @Override
    public CompilerOutput checkProgram(Path pathToProgramFile,
            String compilerName, List<String> compilerFlags)
            throws FileNotFoundException, BadCompilerSpecifiedException,
            BadFlagException {

        Process compilerProcess = null;

        try {
            // create compiler invocation.
            List<String> compilerInvocation = createCompilerInvocation(
                    pathToProgramFile, compilerName, compilerFlags);

            ProcessBuilder compilerProcessBuilder = new ProcessBuilder(
                    compilerInvocation);

            // make sure the compiler stays in its directory.
            compilerProcessBuilder.directory(pathToProgramFile.getParent()
                    .toFile());

            compilerProcess = compilerProcessBuilder.start();
            // this will never happen because createCompilerInvocation never
            // throws this Exception. Throw declaration needs to be in method
            // declaration because of the implemented Interface although we
            // never use it in the HaskellCompileChecker
        } catch (CompilerOutputFolderExistsException e) {
            LOGGER.severe("A problem while compiling, which never should happen, occured"
                    + e.getMessage());
        } catch (BadCompilerSpecifiedException e) {
            throw new BadCompilerSpecifiedException(e.getMessage());
        } catch (IOException e) {

            // If we cannot call the compiler we return a CompilerOutput
            // initialized with false, false, indicating
            // that the compiler wasn't invoked properly and that there was no
            // clean Compile.
            CompilerOutput compilerInvokeError = new CompilerOutput();
            compilerInvokeError.setClean(false);
            compilerInvokeError.setCompilerInvoked(false);
            return compilerInvokeError;
        }

        // Now we read compiler output. If everything is ok ghc reports
        // nothing in the errorStream.
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
            // Errors are separated via an empty line (""). But after the
            // the last error the OutputBuffer has nothing more to write.
            // In order to recognize the last error we insert an empty String
            // at the end of the list.
            // Only needs to be done when there are errors.
            if (compilerOutputLines.size() != 0) {
                line = "";
                compilerOutputLines.add(line);
            }

            compilerOutputStream.close();
            compilerStreamReader.close();
            compilerOutputBuffer.close();
            compilerProcess.destroy();

        } catch (IOException e) {

            // Reading might go wrong here if ghc should unexpectedly die
            LOGGER.severe("Error while reading from compiler stream.");
            compilerOutput.setClean(false);
            compilerOutput.setCompileStreamBroken(true);
            return compilerOutput;
        }

        // ghc -c generates a .o(object) and a .hi(haskell interface) file.
        // But we don't need those files so they can be deleted.
        // The generated files have the same name like our input file so we
        // can just exchange the file endings in order to get the
        // correct file paths for deletion
        if (Files.isDirectory(pathToProgramFile, LinkOption.NOFOLLOW_LINKS)) {

            // we use a file walker in order to find all files in the folder
            // and its subfolders
            RegexDirectoryWalker dirWalker = new RegexDirectoryWalker(
                    ".+\\.([Ll])?[Hh][Ss]");
            try {
                Files.walkFileTree(pathToProgramFile, dirWalker);
            } catch (IOException e) {
                LOGGER.severe("Could not walk submission "
                        + pathToProgramFile.toString()
                        + " while building copiler invocation: "
                        + e.getMessage());
            }

            for (Path candidatePath : dirWalker.getFoundFiles()) {
                File candidateFile = candidatePath.toFile();
                if (!candidateFile.isDirectory()) {
                    String extension = FilenameUtils.getExtension(candidateFile
                            .toString());
                    if (extension.matches("[Ll]?[Hh][Ss]")) {
                        File ghcGeneratedObject = new File(
                                FilenameUtils.removeExtension(candidateFile
                                        .toString()) + ".o");
                        File ghcGeneratedInterface = new File(
                                FilenameUtils.removeExtension(candidateFile
                                        .toString()) + ".hi");
                        ghcGeneratedObject.delete();
                        ghcGeneratedInterface.delete();
                    }
                }
            }
        } else {
            String extension = FilenameUtils.getExtension(pathToProgramFile
                    .toString());
            if (extension.matches("[Ll]?[Hh][Ss]")) {
                File ghcGeneratedObject = new File(
                        FilenameUtils.removeExtension(pathToProgramFile
                                .toString()) + ".o");
                File ghcGeneratedInterface = new File(
                        FilenameUtils.removeExtension(pathToProgramFile
                                .toString()) + ".hi");
                ghcGeneratedObject.delete();
                ghcGeneratedInterface.delete();
            }

        }

        // if there are no errors there is no Output to handle
        if (compilerOutputLines.size() != 0) {
            compilerOutput = splitCompilerOutput(compilerOutputLines,
                    compilerOutput);
        } else {
            compilerOutput.setClean(true);
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
     * @return List of string with the command for the process builder.
     * @throws BadCompilerSpecifiedException
     *             When no compiler is given.
     * @throws FileNotFoundException
     *             When the file to be compiled does not exist
     * @throws CompilerOutputFolderExistsException
     *             Due to slightly uncompatible CompileCheckerInterface this
     *             exception is in the declaration but it is never thrown.
     *             JavaCompileChecker uses this exception.
     */
    private List<String> createCompilerInvocation(Path pathToProgramFile,
            String compilerName, List<String> compilerFlags)
            throws BadCompilerSpecifiedException, FileNotFoundException,
            CompilerOutputFolderExistsException {

        List<String> compilerInvocation = new LinkedList<>();
        // We need a compiler name. Without it we cannot compile anything and
        // abort.
        if (("".equals(compilerName)) || (compilerName == null)) {
            throw new BadCompilerSpecifiedException("No compiler specified.");
        } else {
            compilerInvocation.add(compilerName);
        }

        // If compiler flags are passed, append them after the compiler name.
        // If we didn't get any we append nothing.
        if ((compilerFlags != null) && (!(compilerFlags.isEmpty()))) {
            compilerInvocation.addAll(compilerFlags);
        }

        // now we tell ghc to stop after compilation because we just want to
        // see if there are syntax errors in the code
        compilerInvocation.add("-c");

        // Check for the existence of the program file we are trying to
        // compile.
        if ((pathToProgramFile == null)
                || (pathToProgramFile.compareTo(Paths.get("")) == 0)) {
            throw new FileNotFoundException("No file to compile specified");
        } else {
            if (Files.isDirectory(pathToProgramFile, LinkOption.NOFOLLOW_LINKS)) {
                // we are supposed to compile a folder. Hence we'll scan for
                // lhs files and pass them to the compiler.
                RegexDirectoryWalker dirWalker = new RegexDirectoryWalker(
                        ".+\\.([Ll])?[Hh][Ss]");
                try {
                    Files.walkFileTree(pathToProgramFile, dirWalker);
                } catch (IOException e) {
                    LOGGER.severe("Could not walk submission "
                            + pathToProgramFile.toString()
                            + " while building compiler invocation: "
                            + e.getMessage());
                }
                for (Path matchedFile : dirWalker.getFoundFiles()) {
                    compilerInvocation.add(matchedFile.toFile()
                            .getAbsolutePath());
                }

            } else if (Files.exists(pathToProgramFile,
                    LinkOption.NOFOLLOW_LINKS)) {
                // if the file exists, just pass the file name, since the
                // compiler will
                // be confined to the directory the file is in a few lines
                // down.
                compilerInvocation.add(pathToProgramFile.toString());
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
     * splitCompilerOutput splits the compiler output into errors stores that
     * data into the {@link CompilerOutput} and sets appropriate flags (clean
     * compile etc.).
     * 
     * @param lines
     *            Is the raw compiler output, one line per string in a list
     * @param compilerOutput
     *            Is the {@link CompilerOutput} we are going to put the data
     *            into
     * @return A {@link CompilerOutput} containing the data of the compile run.
     * @throws BadFlagException
     *             When ghc has found a bad flag this can only be found after
     *             it has run, hence we throw it here.
     */
    private CompilerOutput splitCompilerOutput(List<String> lines,
            CompilerOutput compilerOutput) throws BadFlagException {

        StringBuilder currentCompilerNote = new StringBuilder();

        for (String line : lines) {
            if (line.matches("<command line>: does not exist:.*")) {
                throw new BadFlagException("Flag not supported. " + line);
            } else if (line.matches("ghc: unrecognised flag:.*")) {
                throw new BadFlagException("Flag not supported. " + line);
            } else if (line.length() == 0) {
                String compilerError = currentCompilerNote.toString();
                compilerOutput.addError(compilerError);
                compilerOutput.setClean(false);
                currentCompilerNote = new StringBuilder();
            } else {
                currentCompilerNote.append(line + "\n");
            }
        }

        return compilerOutput;
    }

    /**
     * GHC doesnt produce outputfiles in our implementation so this just
     * reroutes to
     * {@link HaskellCompileChecker#checkProgram(Path, String, List)}.
     */
    @Override
    public CompilerOutput checkProgram(Path pathToProgramFile,
            Path outputFolder, String compilerName, List<String> compilerFlags)
            throws FileNotFoundException, BadCompilerSpecifiedException,
            BadFlagException, CompilerOutputFolderExistsException {
        return checkProgram(pathToProgramFile, compilerName, compilerFlags);
    }

}
