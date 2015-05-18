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

package de.teamgrit.grit.preprocess.fetch;

import java.util.List;

/**
 * Internal class to bundle svn output with the return value.
 *
 * @author <a href="mailto:marvin.guelzow@uni-konstanz.de">Marvin Guelzow</a>
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 *
 */

class SVNResultData {
    private int m_returnValue = 1;
    private List<String> m_svnOutputLines;

    /**
     * @param returnValue
     *            the return value of the svn command
     * @param outputLines
     *            the outputlines of the svn command
     */
    protected SVNResultData(int returnValue, List<String> outputLines) {
        m_returnValue = returnValue;
        m_svnOutputLines = outputLines;
    }

    /**
     * Gets the return value of the svn command.
     *
     * @return the returnValue
     */
    protected int getReturnValue() {
        return m_returnValue;
    }

    /**
     * @return the svnOutputLines
     */
    protected List<String> getSvnOutputLines() {
        return m_svnOutputLines;
    }
}