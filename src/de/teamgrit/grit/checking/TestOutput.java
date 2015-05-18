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

package de.teamgrit.grit.checking;

import java.util.LinkedList;
import java.util.List;

import org.junit.runner.Result;

import de.teamgrit.grit.checking.testing.Tester;

/**
 * Container class holding the output of a {@link Tester}.
 *
 * @author <a href=mailto:gabriel.einsdorf@uni-konstanz.de>Gabriel Einsdorf</a>
 * @author <a href=mailto:thomas.3.schmidt@uni-konstanz.de>Thomas Schmidt</a>
 *
 */

public class TestOutput {

    /** List of Result Objects created by a Tester. */
    private List<Result> m_results;

    private int m_testCount;
    private int m_passedTestCount;
    private int m_failedTestCount;

    /** True if there were tests, False if not. */
    private Boolean m_didTest;

    /**
     * Creates a TestOutput object from a List of Result objects.
     *
     * @param results
     *            List of JUnit outputs
     * @param tested
     *            If there were any tests
     */
    public TestOutput(List<Result> results, Boolean tested) {
        m_testCount = 0;
        m_passedTestCount = 0;
        m_failedTestCount = 0;
        if (tested) {
            this.m_results = results;
            for (Result result : results) {
                m_testCount++;
                if (result.wasSuccessful()) {
                    m_passedTestCount++;
                } else {
                    m_failedTestCount++;
                }
            }
        } else {
            this.m_results = new LinkedList<>();
        }
        m_didTest = tested;
    }

    /**
     * Setter method for didTest member variable.
     *
     * @param tested
     *            The value didTest gets addigned
     */
    public void setDidTest(Boolean tested) {
        m_didTest = tested;
    }

    /**
     * Gets the test count.
     *
     * @return the test count
     */
    public int getTestCount() {
        return m_testCount;
    }

    /**
     * Gets the passed test count.
     *
     * @return the passed test count
     */
    public int getPassedTestCount() {
        return m_passedTestCount;
    }

    /**
     * Gets the failed test count.
     *
     * @return the failed test count
     */
    public int getFailedTestCount() {
        return m_failedTestCount;
    }

    /**
     * Gets the results.
     *
     * @return the results
     */
    public List<Result> getResults() {
        return m_results;
    }

    /**
     * Gets didTest.
     *
     * @return a boolean whether there were tests or not
     */
    public Boolean getDidTest() {
        return m_didTest;
    }

}
