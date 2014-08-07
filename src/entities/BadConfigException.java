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

package entities;

/**
 * Indicates that the configuration of the throwing object is not correct.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 */
public class BadConfigException extends Exception {

    /**
     * Generated Serial ID.
     */
    private static final long serialVersionUID = 1395434803908538850L;

    /**
     * Indicates that the configuration of the throwing object is not correct.
     *
     * @param message
     *            the exception message
     */
    public BadConfigException(String message) {
        super(message);
    }

}
