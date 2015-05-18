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

/**
 * @author <a href=mailto:gabriel.einsdorf@uni-konstanz.de>Gabriel Einsdorf</a>
 */

public class CheckingResult {

    private final TestOutput testResults;
    private final CompilerOutput compilerOutput;

    /**
     * Creates an resultObj.
     * 
     * @param testOutput
     *            the {@link TestOutput} object for this submission.
     * @param compilerOutput
     *            the {@link CompilerOutput} object for this submission.
     */
    public CheckingResult(CompilerOutput compilerOutput, TestOutput testOutput) {
        testResults = testOutput;
        this.compilerOutput = compilerOutput;
    }

    /**
     * @return the compilerOutput
     */
    public CompilerOutput getCompilerOutput() {
        return compilerOutput;
    }

    /**
     * @return the testResults
     */
    public TestOutput getTestResults() {
        return testResults;
    }

}
