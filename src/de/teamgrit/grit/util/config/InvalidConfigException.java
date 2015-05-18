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

package de.teamgrit.grit.util.config;

/**
 * Indicates the Config is invalid.
 * 
 * @author <a href=mailto:gabriel.einsdorf@uni-konstanz.de>Gabriel Einsdorf</a>
 */

public class InvalidConfigException extends Exception {

    /**
     * The serial ID.
     */
    private static final long serialVersionUID = 2116535009732954026L;

    /**
     * Creates the exception with a message.
     * 
     * @param message
     *            the message string
     */
    public InvalidConfigException(String message) {
        super(message);
    }

    /**
     * Creates the exception by encapsulating another exception.
     * 
     * @param cause
     *            another exception
     */
    public InvalidConfigException(Exception cause) {
        super(cause);
    }

    /**
     * Creating the exception by providing a message and another exception.
     * 
     * @param message
     *            the message string
     * @param cause
     *            another exception
     */
    public InvalidConfigException(String message, Exception cause) {
        super(message, cause);
    }
}
