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

package de.teamgrit.grit.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.configuration.ConfigurationException;

import de.teamgrit.grit.entities.BadConfigException;
import de.teamgrit.grit.entities.Controller;
import de.teamgrit.grit.entities.State;
import de.teamgrit.grit.util.config.Configuration;
import de.teamgrit.grit.webserver.GritServer;

/**
 * 
 * The main class of grit.
 * 
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 */

public abstract class Boot {

    private static final Path CONFIG_PATH = Paths.get("config");
    private static final Path MAIN_CONF = Paths.get(CONFIG_PATH.toString(),
            "config.xml");
    private static final Path STATE = Paths.get(CONFIG_PATH.toString(),
            "state.xml");

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    private static Controller s_controller = null;

    private static GritServer server;

    /**
     * Starts grit.
     * 
     * @param args
     *            used for setting a custom port to reach the grit server at
     */
    public static void main(String[] args) {

        s_controller = Controller.getController();

        if (!BootHelpers.checkRequirements()) {
            LOGGER.severe("Requirements are not satisfied. Aborting boot.");
            /*
             * This println command is necessary, because to boot the system
             * you need to be logged in and you have to see this message to
             * find the cause of the fail.
             */
            System.err
                    .println("Could not start system due to missing programs."
                            + " Take a look at the log for further details.");
            return;
        }

        try {
            loadConfig();
        } catch (IOException | ConfigurationException | BadConfigException e) {
            LOGGER.severe("Could not load the config: " + e.getMessage());
            return;
        }
        try {
            loadState();
        } catch (IOException | ConfigurationException e) {
            LOGGER.severe("Could not restore the state of GRIT : "
                    + e.getMessage());

        }

        int port = s_controller.getConfig().getServerPort();
        if ((args != null) && (args.length > 0) && args[0].matches("\\d+")) {
            port = Integer.parseInt(args[0]);
        }

        server = new GritServer(port);

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled error: " + e.getMessage(), e);
        }

    }

    /**
     * Restores the state from a save file.
     * 
     * @throws ConfigurationException
     * @throws IOException
     */
    private static void loadState() throws ConfigurationException, IOException {
        if (!(Files.exists(STATE))) {
            BootHelpers.createNewStateFile(STATE);
        }
        State state = new State(STATE.toFile());
        s_controller.restoreState(state);
    }

    /**
     * Load a config.
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws BadConfigException
     *             if the check of the config fails
     * @throws org.apache.commons.configuration.ConfigurationException
     *             if the configuration can not be read
     */

    private static void loadConfig() throws IOException,
            ConfigurationException, BadConfigException {
        // If not configured, go into config mode
        if (!(Files.exists(MAIN_CONF))) {
            BootHelpers.createNewDefaultConfig(MAIN_CONF);
        }
        Configuration config = new Configuration(MAIN_CONF.toFile());
        if (checkConfig(config)) {
            s_controller.setConfig(config);
        } else {
            throw new BadConfigException("Config corrupted.");
        }
    }

    /**
     * Checks whether the config is valid by trying to get the parameters.
     * 
     * @param config
     *            the configuration that has to be tested
     * @return true if config is ok, false otherwise
     */
    private static boolean checkConfig(Configuration config) {
        try {
            config.getMailAdmin();
            config.getMailPassword();
            config.getAdminName();
            config.getAdminPassword();
            config.getSenderMailAdress();
            config.getServerPort();
            config.getSmtpHost();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * This method reboots grit by stopping the server and calling the main
     * function.
     * 
     * @throws Exception
     *             the grit-server can throw any type of exception
     */
    public static void reboot() throws Exception {
        try {
            server.stop();
            server.join();
        } catch (InterruptedException e) {
            /*
             * The exception is supposed to be thrown, because the server gets
             * interrupted. So just accept it and carry on
             */
        }
        main(null);

    }
}
