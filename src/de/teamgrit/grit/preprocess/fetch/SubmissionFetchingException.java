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

/**
 * Indicating that something went wrong during the fetching.
 *
 * @author <a href="mailto:marvin.guelzow@uni-konstanz.de">Marvin Guelzow</a>
 */

public class SubmissionFetchingException extends Exception {

    /**
     * Generated ID for serialization.
     */
    private static final long serialVersionUID = 5869051766559292046L;

    /**
     * Simply pass the message to the superclass. It implements all necessary
     * methods.
     *
     * @param message
     *            String describing the error that occurred.
     */
    public SubmissionFetchingException(String message) {
        super(message);
    }

    /**
     * Simply pass the exception to the superclass. It implements all necessary
     * methods.
     *
     * @param e
     *            The Exception that will get wrapped in this one.
     */
    public SubmissionFetchingException(Exception e) {
        super(e);
    }

    /**
     * Pass exception to superclass with message.
     *
     * @param msg
     *            String describing the error that occured
     * @param e
     *            The Exception that will get wrapped in this one.
     */
    public SubmissionFetchingException(String msg, Exception e) {
        super(msg, e);
    }
}
