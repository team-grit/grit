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

package de.teamgrit.grit.preprocess.tokenize;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Holds the structure of a submission, i.e. how directories are organized etc.
 * SubmissionStructure ensures that the structure remains valid.
 * 
 * @author <a href="mailto:marvin.guelzow@uni-konstanz.de">Marvin Guelzow</a>
 */

public class SubmissionStructure {

    private List<String> m_submissionStructure;

    /**
     * Initializes the structure with a structure form the config. Syntax of
     * the list elements is:
     * 
     * TOPLEVEL -&gt; [Directories] -&gt; SUBMISSIONS (-&gt; indicating a new
     * list element.)
     * 
     * Where [Directories] is a sequence of regular expressions that will be
     * matched against directrories.For example:
     * 
     * TOPLEVEL -&gt; Group[A-H] -&gt; [a-z]+\.[a-z]+\.?[0-9]* -&gt;
     * ex[0-2][0-9] -&gt; SUBMISSION
     * 
     * Would match
     * 
     * GroupA/fred.brooks.2/ex01/ GroupH/jane.brooks/ex02/
     * 
     * and so on.
     * 
     * @param structure
     *            List of regular expressions as described above.
     * @throws InvalidStructureException
     *             When a invalid structure is found (e.g. a part of the
     *             structure isn't specified)
     */
    public SubmissionStructure(List<String> structure)
            throws InvalidStructureException {
        if ((structure == null) || structure.isEmpty()) {
            throw new InvalidStructureException("Empty structure");
        } else if (!"TOPLEVEL".equals(structure.get(0))) {
            throw new InvalidStructureException("No TOPLEVEL tag found");
        } else if (!"SUBMISSION".equals(structure.get(structure.size() - 1))) {
            throw new InvalidStructureException("No SUBMISSION tag found");
        }

        for (String regex : structure) {
            try {
                Pattern.compile(regex);
            } catch (PatternSyntaxException regexException) {
                throw new InvalidStructureException(
                        regexException.getMessage());
            }
        }

        m_submissionStructure = structure;
    }

    /**
     * Getter for structure.
     * 
     * @return Validated submission structure.
     */
    public List<String> getStructure() {
        return m_submissionStructure;
    }
}
