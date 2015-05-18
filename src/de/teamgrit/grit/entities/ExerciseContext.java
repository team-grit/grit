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

package de.teamgrit.grit.entities;

import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import com.google.gson.annotations.Expose;

import de.teamgrit.grit.checking.compile.CompileChecker;
import de.teamgrit.grit.checking.testing.Tester;

/**
 * Holds the Context of a Exercise object, created by an
 * {@link ExerciseContextFactory}.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 * @author <a href="mailto:david.kolb@uni-konstanz.de">David Kolb</a>
 */

public class ExerciseContext {

    // ------------------------------ FIELDS ------------------------------

    private static final Logger LOGGER = Logger.getLogger("systemlog");
    private static final DateFormat DATEFORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm");

    private static final DateFormat HOURFORMAT = new SimpleDateFormat("HH:mm");

    @Expose
    private String name;
    private String m_courseName;

    /* serialisation members */
    private LanguageType m_languageType;
    private int m_connectionId;

    /* scheduling members */
    private Calendar m_startTime;
    private Calendar m_deadline;
    private long m_period;
    @Expose
    private String startTimeString;
    @Expose
    private String deadlineString;
    @Expose
    private String periodString;

    /* compiling members */
    private CompileChecker m_compiler;
    private String m_compilerName;
    private List<String> m_compilerFlags;

    /* testing members */
    private Tester m_tester;
    private List<String> m_testerNames;

    /* storage location members */
    private Path m_fetchPath;
    private Path m_binPath;
    private Path m_tests;
    private Path m_tempPdfPath;
    private Path m_outputPath;

    private String m_archiveRegex;

    private String m_fileRegex;

    // --------------------------- CONSTRUCTOR ---------------------------

    /**
     * Creates a {@link ExerciseContext}, this is only called by
     * {@link ExerciseContext}.
     **
     * @param exerciseName
     *            the name of the exercise.
     */
    protected ExerciseContext(String exerciseName) {
        name = exerciseName;
        HOURFORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // -------------------------- LOGGING METHODS --------------------------

    /**
     * Logs normal infos.
     *
     * @param message
     *            the message that is logged.
     */
    public void logInfo(String message) {
        LOGGER.info("Processing: " + getExcerciseName() + "INFO:" + message);
    }

    /**
     * Logs critical failures.
     *
     * @param message
     *            the message that is logged.
     */
    public void logError(String message) {
        LOGGER.severe("An Error occurred while processing: "
                + getExcerciseName() + " " + message);
    }

    // --------------------- GETTER / SETTER METHODS ---------------------

    /**
     * Gets the path to the Binary directory.
     *
     * @return the path to the binary main directory.
     */
    public Path getBinPath() {
        return m_binPath;
    }

    /**
     * Gets the compile checker.
     *
     * @return the compile checker
     */
    public CompileChecker getCompileChecker() {
        return m_compiler;
    }

    /**
     * Gets the compiler.
     *
     * @return the compiler
     */
    public CompileChecker getCompiler() {
        return m_compiler;
    }

    /**
     * Gets the compiler flags.
     *
     * @return the compiler flags
     */
    public List<String> getCompilerFlags() {
        return m_compilerFlags;
    }

    /**
     * Gets the compiler name.
     *
     * @return the compiler name
     */
    public String getCompilerName() {
        return m_compilerName;
    }

    /**
     * Gets the deadline.
     *
     * @return the deadline
     */
    public Calendar getDeadline() {
        return m_deadline;
    }

    /**
     * Gets the directory the submissions should be fetched to.
     *
     * @return the path where the submission are fetched to.
     */
    public Path getFetchPath() {
        return m_fetchPath;
    }

    /**
     * Gets the name of the {@link Exercise}.
     *
     * @return the name of the exercise
     */
    public String getExerciseName() {
        return name;
    }

    /**
     * Gets the output path for the Reports.
     *
     * @return the output path
     */
    public Path getOutputPath() {
        return m_outputPath;
    }

    /**
     * Gets the start time.
     *
     * @return the start time
     */
    public Calendar getStartTime() {
        return m_startTime;
    }

    /**
     * Gets the temp pdf path.
     *
     * @return the temporary pdf path
     */
    public Path getTempPdfPath() {
        return m_tempPdfPath;
    }

    /**
     * Gets the tester.
     *
     * @return the tester
     */
    public Tester getTester() {
        return m_tester;
    }

    /**
     * Gets the tester names.
     *
     * @return the tester names
     */
    public List<String> getTesterNames() {
        return m_testerNames;
    }

    /**
     * Gets the folder that stores the Tests.
     *
     * @return the {@link java.nio.file.Path} to the Test(s).
     */
    public Path getTests() {
        return m_tests;
    }

    /**
     * @return the Name of the Exercise.
     */
    public String getExcerciseName() {
        return name;
    }

    /**
     * Sets the path for the binaries.
     *
     * @param binPath
     *            the new binaries path
     */
    protected void setBinPath(Path binPath) {
        m_binPath = binPath;
    }

    /**
     * set the {@link CompileChecker}.
     *
     * @param compiler
     *            the {@link CompileChecker}
     */
    protected void setCompiler(CompileChecker compiler) {
        m_compiler = compiler;
    }

    /**
     * Sets the compiler flags.
     *
     * @param compilerFlags
     *            the new compiler flags
     */
    protected void setCompilerFlags(List<String> compilerFlags) {
        m_compilerFlags = compilerFlags;
    }

    /**
     * Sets the compiler name.
     *
     * @param name
     *            the new compiler name
     */
    protected void setCompilerName(String name) {
        m_compilerName = name;
    }

    /**
     * Sets the deadline for this submission.
     *
     * @param deadline
     *            the new deadline.
     * @throws WrongDateException
     *             when trying to set an date in the past.
     */
    public void setDeadline(Date deadline) throws WrongDateException {
        if (deadline.compareTo(m_startTime.getTime()) > 0) {
            m_deadline = new GregorianCalendar();
            m_deadline.setTime(deadline);
            deadlineString = DATEFORMAT.format(deadline);
        } else {
            throw new WrongDateException(
                    "Can't set deadline before the startTime! "
                            + deadlineString);
        }
    }

    /**
     * @return the deadline as String
     */
    public String getDeadlineString() {
        return deadlineString;
    }

    /**
     * Sets the fetch path where the submissions are downloaded to.
     *
     * @param fetchpath
     *            the new fetch path
     */
    protected void setFetchPath(Path fetchpath) {
        m_fetchPath = fetchpath;
    }

    /**
     * Sets the name of the {@link Exercise}.
     *
     * @param exerciseName
     *            the name for the exercise\
     */
    protected void setName(String exerciseName) {
        name = exerciseName;
    }

    /**
     * Sets the output path.
     *
     * @param outputPath
     *            the new output path
     */
    protected void setOutputPath(Path outputPath) {
        m_outputPath = outputPath;
    }

    /**
     * Sets the start time.
     *
     * @param startTime
     *            the new start time
     */
    public void setStartTime(Date startTime) {
        m_startTime = new GregorianCalendar();
        m_startTime.setTime(startTime);
        startTimeString = DATEFORMAT.format(startTime);
    }

    /**
     * Sets the temp pdf path.
     *
     * @param tempPdfPath
     *            the new temp pdf path
     */
    protected void setTempPdfPath(Path tempPdfPath) {
        m_tempPdfPath = tempPdfPath;
    }

    /**
     * Sets the tester.
     *
     * @param testProjectObj
     *            the new tester
     */
    protected void setTester(Tester testProjectObj) {
        m_tester = testProjectObj;
    }

    /**
     * Sets the testerNames.
     *
     * @param names
     *            the new tester names
     */
    protected void setTesterNames(List<String> names) {
        m_testerNames = names;
    }

    /**
     * Sets the tests.
     *
     * @param tests
     *            the new tests
     */
    protected void setTests(Path tests) {
        m_tests = tests;
    }

    /**
     * Sets the language type.
     *
     * @param languageType
     *            the new language type
     */
    protected void setLanguageType(LanguageType languageType) {
        m_languageType = languageType;
    }

    /**
     * Gets the language type.
     *
     * @return the language type
     */
    protected LanguageType getLanguageType() {
        return m_languageType;
    }

    /**
     * Sets the id of the used connection.
     *
     * @param id
     *            the id of used connection.
     */
    protected void setConnectionId(int id) {
        m_connectionId = id;
    }

    /**
     * Gets the connection id.
     *
     * @return the id of the used connection.
     */
    protected int getConnectionId() {
        return m_connectionId;
    }

    /**
     * Gets the time between running exercise checks.
     *
     * @return the period
     */
    public long getPeriod() {
        return m_period;
    }

    /**
     * Sets the name of the course.
     *
     * @param courseName
     *            the name of the course
     */
    protected void setCourseName(String courseName) {
        m_courseName = courseName;
    }

    /**
     * Gets the name of the course this exercise belongs to.
     *
     * @return the name of the course this exercise belongs to
     */
    protected String getCourseName() {
        return m_courseName;
    }

    /**
     * Sets the archive regex.
     *
     * @param archiveRegex
     *            the archive regex
     */
    protected void setArchiveRegex(String archiveRegex) {
        m_archiveRegex = archiveRegex;
    }

    /**
     * Gets the archive Regex, matching acceptable archive files.
     *
     * @return the archive regex
     */
    public String getArchiveRegex() {
        return m_archiveRegex;
    }

    /**
     * Gets the File Regex, matching acceptable files.
     *
     * @return the archive regex
     */
    public String getFileRegex() {
        return m_fileRegex;
    }

    /**
     * Sets the file regex.
     *
     * @param fileRegex
     *            the file regex
     */
    protected void setFileRegex(String fileRegex) {
        m_fileRegex = fileRegex;
    }

    /**
     * Sets the time period between fetches.
     *
     * @param period
     *            the period of the fetching
     */
    public void setPeriod(long period) {
        m_period = period;
        periodString = HOURFORMAT.format(period);
    }

}
