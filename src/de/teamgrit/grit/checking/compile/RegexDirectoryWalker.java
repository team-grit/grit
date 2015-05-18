package de.teamgrit.grit.checking.compile;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of the FileVisitor pattern using the Java SimpleFileVisitor.
 * For each file that is visited we check if it matches the given Regex. If so
 * we add its filepath to the matchingFiles list.
 *
 * Do not reuse this class for a second traversal, as the list is never
 * emptied.
 *
 * @author <a href=mailto:david.kolb@uni-konstanz.de>David Kolb</a>
 *
 */
public class RegexDirectoryWalker extends SimpleFileVisitor<Path> {

    private String m_submissionFileRegex = "";
    private List<Path> m_matchingFiles = new LinkedList<>();

    /**
     * Constructor.
     *
     * @param newFileRegex
     *            is the regex for file that must be contained somewhere for
     *            the submission to be plausible.
     */
    public RegexDirectoryWalker(String newFileRegex) {
        m_submissionFileRegex = newFileRegex;
    }

    /**
     * @return List containing all files matching the give regex.
     */
    public List<Path> getFoundFiles() {
        return m_matchingFiles;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (file.toString().matches(m_submissionFileRegex)) {
            m_matchingFiles.add(file);
        }
        return FileVisitResult.CONTINUE;
    }

}
