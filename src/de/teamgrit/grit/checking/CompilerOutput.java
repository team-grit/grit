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

import java.util.ArrayList;
import java.util.List;

/**
 * Output Object that stores the compiler output.
 * 
 * @author <a href=mailto:gabriel.einsdorf@uni-konstanz.de>Gabriel Einsdorf</a>
 */

public class CompilerOutput {
    private boolean m_compilerInvoked;
    private boolean m_compilerStreamBroken;
    private boolean m_cleanCompile;
    private final List<String> m_compilerErrors;
    private final List<String> m_compilerWarnings;
    private final List<String> m_compilerInfos;

    /**
     * The constructor initializes the lists for errors, warnings and infos
     * from the compiler. Also flags are set to default values. compilerInvoked
     * is set to false, since no compiler has been called at this point from
     * this objects perspective. The compiler stream isn't broken since it does
     * not exist and there was no clean compile run yet.
     */
    public CompilerOutput() {
        m_compilerInvoked = false;
        m_compilerStreamBroken = false;
        m_cleanCompile = false;
        m_compilerErrors = new ArrayList<>();
        m_compilerWarnings = new ArrayList<>();
        m_compilerInfos = new ArrayList<>();
    }

    /**
     * Compiler errors are flaws in the code that make compilation impossible.
     * 
     * @param newError
     *            The error message for exactly one compile error. You should
     *            not put more than one error in one entry.
     */
    public void addError(String newError) {
        m_compilerErrors.add(newError);
    }

    /**
     * Warnings are flaws in the code that do not hinder compilation but are
     * problematic nontheless, like calls deprecated methods.
     * 
     * @param newWarning
     *            Exactly one warning. Do not put more that one warning into
     *            one entry.
     */
    public void addWarning(String newWarning) {
        m_compilerWarnings.add(newWarning);
    }

    /**
     * Compiler infos are messages that do not concern flaws in the code but
     * rather messages in which the compiler conveys some internal information,
     * such as which config file is used or what version of the compiler we are
     * running.
     * 
     * @param newInfo
     *            Exactly one info. Do not put more than one in one entry.
     */
    public void addInfo(String newInfo) {
        m_compilerInfos.add(newInfo);
    }

    /**
     * The clean flag indicates whether the compiler ran without errors or
     * warnings.
     * 
     * @param compileRunWasClean
     *            A boolean value to set the internal flag to.
     */
    public void setClean(boolean compileRunWasClean) {
        m_cleanCompile = compileRunWasClean;
    }

    /**
     * The compileStreamBroken flag indicates whether a successful read from
     * the compiler output stream has taken place.
     * 
     * @param streamBroken
     *            A boolean value to set the internal flag to.
     */
    public void setCompileStreamBroken(boolean streamBroken) {
        m_compilerStreamBroken = streamBroken;
    }

    /**
     * The compilerInvoked flag indicates whether the compiler could actually
     * be called of if no call occured.
     * 
     * @param compilerWasInvoked
     *            A boolean value to set the internal flag to.
     */
    public void setCompilerInvoked(boolean compilerWasInvoked) {
        m_compilerInvoked = compilerWasInvoked;
    }

    /**
     * Checks if compiler invoked.
     * 
     * @return true, if successful compiler invocation.
     */
    public boolean compilerInvoked() {
        return m_compilerInvoked;
    }

    /**
     * Checks if compiler stream broken.
     * 
     * @return true, if compiler stream broken
     */
    public boolean compilerStreamBroken() {
        return m_compilerStreamBroken;
    }

    /**
     * Checks if compile was clean.
     * 
     * @return true, if clean compile
     */
    public boolean isCleanCompile() {
        return m_cleanCompile;
    }

    /**
     * Gets the compiler errors.
     * 
     * @return the compiler errors
     */
    public List<String> getCompilerErrors() {
        return m_compilerErrors;
    }

    /**
     * Gets the compiler warnings.
     * 
     * @return the compiler warnings
     */
    public List<String> getCompilerWarnings() {
        return m_compilerWarnings;
    }

    /**
     * Gets the compiler infos.
     * 
     * @return the compiler infos
     */
    public List<String> getCompilerInfos() {
        return m_compilerInfos;
    }
}
