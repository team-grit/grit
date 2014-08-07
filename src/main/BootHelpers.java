package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;

/**
 * Various helper methods for the boot process.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 */
class BootHelpers {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    /**
     * Creates an empty state file.
     *
     * @param state
     *            the path to the state file
     * @throws IOException
     *             if the file can't be created
     */
    static void createNewStateFile(Path state) throws IOException {
        File file = state.toFile();
        file.createNewFile();

        if (Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            Files.delete(file.toPath());
        }

        FileWriterWithEncoding writer =
                new FileWriterWithEncoding(file, "UTF-8", true);

        writer.append("<?xml version=\"1.0\" "
                + "encoding=\"UTF-8\" standalone=\"no\"?>");

        writer.append("<state>");
        writer.append("<courses>");
        writer.append("</courses>");
        writer.append("<connections>");
        writer.append("</connections>");
        writer.append("</state>");

        writer.close();
    }

    /**
     * Creates an default config file.
     *
     * @param config
     *            the path to the state file
     * @throws IOException
     *             if the file can't be created
     */
    static void createNewDefaultConfig(Path config) throws IOException {

        String failsafeConfig =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
                        + "\n"
                        + "<config>"
                        + "\n"
                        + "<server>"
                        + "\n"
                        + "<port value=\"8080\"/>"
                        + "\n"
                        + "</server>"
                        + "\n"
                        + "<email>"
                        + "\n"
                        + "<auth password=\"\" adress=\"\" host=\"\"/>"
                        + "\n"
                        + "</email>"
                        + "\n"
                        + "<admin>"
                        + "\n"
                        + "<user name=\"username\" email=\"\" password=\"password\"/>"
                        + "\n" + "</admin>" + "\n" + "</config>";

        if (config.toFile().exists()) {
            Files.delete(config);
        }
        FileUtils.writeStringToFile(config.toFile(), failsafeConfig);
    }

    /**
     * Checks whether all the required programs are defined in the system path
     * variable.
     *
     * @return true if all are defined, false if one is not defined
     */

    static boolean checkRequirements() {
        List<String> programArguments = new LinkedList<>();
        ProcessBuilder buildExecutor = null;

        // check for svn
        programArguments.add("svn");
        programArguments.add("--version");
        buildExecutor = new ProcessBuilder(programArguments);
        boolean isSvnAvailable = runProcess(buildExecutor, "svn");

        programArguments.clear();
        buildExecutor = null;

        // check for ssh
        programArguments.add("ssh");
        programArguments.add("-V");
        buildExecutor = new ProcessBuilder(programArguments);
        boolean isSshAvailable = runProcess(buildExecutor, "ssh");

        programArguments.clear();
        buildExecutor = null;

        // check for scp
        programArguments.add("scp");
        buildExecutor = new ProcessBuilder(programArguments);
        boolean isScpAvailable = runProcess(buildExecutor, "scp");

        programArguments.clear();
        buildExecutor = null;

        // check for javac
        programArguments.add("javac");
        programArguments.add("-version");
        buildExecutor = new ProcessBuilder(programArguments);
        boolean isJavacAvailable = runProcess(buildExecutor, "javac");

        programArguments.clear();
        buildExecutor = null;

        // check for gcc
        programArguments.add("gcc");
        programArguments.add("--version");
        buildExecutor = new ProcessBuilder(programArguments);
        boolean isGccAvailable = runProcess(buildExecutor, "gcc");

        programArguments.clear();
        buildExecutor = null;

        // check for g++
        programArguments.add("g++");
        programArguments.add("--version");
        buildExecutor = new ProcessBuilder(programArguments);
        boolean isGppAvailable = runProcess(buildExecutor, "g++");

        programArguments.clear();
        buildExecutor = null;

        // check for ghc
        programArguments.add("ghc");
        programArguments.add("--version");
        buildExecutor = new ProcessBuilder(programArguments);
        boolean isGhcAvailable = runProcess(buildExecutor, "ghc");

        programArguments.clear();
        buildExecutor = null;

        // check for pdf
        programArguments.add("pdflatex");
        programArguments.add("--version");
        buildExecutor = new ProcessBuilder(programArguments);
        boolean isPdfAvailable = runProcess(buildExecutor, "pdflatex");

        return (isSvnAvailable && isSshAvailable && isScpAvailable
                && isJavacAvailable && isGccAvailable && isGhcAvailable
                && isGppAvailable && isPdfAvailable);
    }

    /**
     * Runs a process defined by the given {@link ProcessBuilder} and returns
     * whether the program returns a valid output or not.
     *
     * @param programToExecute
     *            defines the program which will be executed incl. flags
     * @param program
     *            the name of the program for an easier log message
     * @return true if the program returns a version number, false otherwise
     */
    private static boolean runProcess(ProcessBuilder programToExecute,
            String program) {
        try {
            Process executeProgram = programToExecute.start();
            InputStream programOutputStream = null;

            // some programms print their version info to the errorStream so we
            // need to handle them differently
            if ("javac".equals(program) || "ssh".equals(program)
                    || "scp".equals(program)) {
                programOutputStream = executeProgram.getErrorStream();
            } else {
                programOutputStream = executeProgram.getInputStream();
            }
            InputStreamReader programStreamReader =
                    new InputStreamReader(programOutputStream);
            BufferedReader compilerOutputBuffer =
                    new BufferedReader(programStreamReader);

            List<String> programOutputLines = new LinkedList<>();
            String line = null;

            while ((line = compilerOutputBuffer.readLine()) != null) {
                programOutputLines.add(line);
            }

            compilerOutputBuffer.close();
            programStreamReader.close();
            programOutputStream.close();
            executeProgram.destroy();

            for (String outputLine : programOutputLines) {

                if ((!"scp".equals(program))
                        && outputLine.matches(".*\\d+\\.\\d+.*")) {
                    return true;
                } else if ("scp".equals(program)
                        && outputLine.matches("usage.{1}\\sscp.*")) {

                    return true;
                }
            }
            LOGGER.warning(program + " not available or not defined in path");
            return false;

        } catch (IOException e) {
            LOGGER.warning(program + " not available or not defined in path"
                    + e.getMessage());
            return false;

        }
    }
}
