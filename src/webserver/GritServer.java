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

import java.nio.file.Paths;
import java.util.Collections;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;

import entities.Controller;

/**
 * This Class is instantiated on startup of the system. It is the Server for
 * the web back-end
 * 
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 */
public class GritServer extends Server {

    /**
     * Creates a new {@link GritServer}.
     * 
     * @param port
     *            the web UI will be available on this port
     */
    public GritServer(int port) {
        super(port);

        /*
         * Build a resource handler for all static files and point it to the
         * web directory.
         */
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase(Paths.get(
                System.getProperty("user.dir"), "res", "web").toString());

        /*
         * Set the resource handler to handle all requests that are not ment
         * for one of the other handlers.
         */
        ContextHandler overviewHandler = new ContextHandler("/");
        overviewHandler.setHandler(resourceHandler);

        /* Set the CourseHandler up to handle all requests to "/course". */
        ContextHandler courseHandler = new ContextHandler("/course");
        courseHandler.setHandler(new CourseHandler());

        /* Set the ExerciseHandler up to handle all requests to "/exercise". */
        ContextHandler exerciseHandler = new ContextHandler("/exercise");
        exerciseHandler.setHandler(new ExerciseHandler());

        /*
         * Set the ConnectionHandler up to handle all requests to
         * "/connection".
         */
        ContextHandler connectionHandler = new ContextHandler("/connection");
        connectionHandler.setHandler(new ConnectionHandler());

        /* Set the XmlHandler up to handle all requests to "/xml". */
        ContextHandler xmlHandler = new ContextHandler("/xml");
        xmlHandler.setHandler(new XmlHandler());

        /*
         * Use a context handler collection to aggregate the context handlers.
         * The handler collection will be wrapped in a security handler.
         */
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new ContextHandler[] { overviewHandler,
                courseHandler, exerciseHandler, connectionHandler, xmlHandler });

        /* Get the user name and passsword for the web UI from the config. */

        String pass = Controller.getController().getConfig().getAdminPassword();
        /* get the user name from the config */
        String user = Controller.getController().getConfig().getAdminName();

        /*
         * Create a HashMap based login service (map of users and passwords)
         * and add it to the server.
         */
        HashLoginService loginService = new HashLoginService("grit login");
        loginService.putUser(user, new Password(pass), new String[] { "user" });
        addBean(loginService);

        /*
         * Create a security handler and set it as the handler for all
         * requests.
         */
        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        setHandler(security);

        /*
         * Create a constraint. The constraint will store user roles and
         * actually tell the security handler to ask the user for
         * authentication.
         */
        Constraint constraint = new Constraint();
        constraint.setName("auth");
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[] { "user" });

        /* Map the constraint to all URIs. */
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        mapping.setConstraint(constraint);

        /*
         * Add the constraint to the security handler and use a .htaccess
         * authentication. Then give the security handler the login service.
         */
        security.setConstraintMappings(Collections.singletonList(mapping));
        security.setAuthenticator(new BasicAuthenticator());
        security.setLoginService(loginService);

        /* Wrap the context handler collection in the security handler. */
        security.setHandler(contexts);
    }
}
