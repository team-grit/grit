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

package de.teamgrit.grit.checking.compile;

/**
 * Indicates that a {@link Compiler} received flags it didn't understood.
 *
 * @author <a href=mailto:gabriel.einsdorf@uni-konstanz.de>Gabriel Einsdorf</a>
 */

public class BadFlagException extends Exception {

    /**
     * Generated ID for serialization.
     */
    private static final long serialVersionUID = -175228020252777851L;

    /**
     * Simply pass the message to the superclass. It implements all necessary
     * methods.
     *
     * @param message
     *            String describing the error that occurred.
     */
    public BadFlagException(String message) {
        super(message);
    }

}
