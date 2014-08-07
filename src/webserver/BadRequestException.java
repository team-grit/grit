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

package webserver;

/**
 * Indicates that there was an error due to a bad request. This can happen if
 * someone enters incorrect requests manually or the web-UI has a bug (which
 * should not be the case).
 *
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 */
public class BadRequestException extends Exception {

    /**
     * Generated Serial ID.
     */
    private static final long serialVersionUID = -2698125772120940122L;

    /**
     * Construct the Exception with a message.
     *
     * @param message
     *            the exception message in most cases will be passed as HTTP
     *            response
     */
    public BadRequestException(String message) {
        super(message);
    }
}
