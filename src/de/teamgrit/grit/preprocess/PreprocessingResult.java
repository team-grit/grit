package de.teamgrit.grit.preprocess;

import java.util.List;
import java.util.Map;

import de.teamgrit.grit.preprocess.tokenize.Submission;

/**
 * Stores the result of a preprocessing operation.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 */
public class PreprocessingResult {

    private Map<Student, Submission> m_studentSubmissions;
    private List<Student> m_studentsWithoutSubmission;

    /**
     * Constructor.
     *
     * @param studentSubmissions
     *            the map storing the students and their submissions.
     * @param studentsWithoutSubmissions
     *            the list of students that did not turn in a submission
     */
    public PreprocessingResult(Map<Student, Submission> studentSubmissions,
            List<Student> studentsWithoutSubmissions) {
        m_studentSubmissions = studentSubmissions;
        m_studentsWithoutSubmission = studentsWithoutSubmissions;
    }

    /**
     * @return the map storing the students and their submissions.
     */
    public Map<Student, Submission> getStudentSubmissions() {
        return m_studentSubmissions;
    }

    /**
     * @return the list of students that did not turn in a submission
     */
    public List<Student> getStudentsWithoutSubmission() {
        return m_studentsWithoutSubmission;
    }

}