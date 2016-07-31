package org.scubbo.popculturegraph.server;

import org.eclipse.jetty.server.Server;

public class Startup {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new RequestHandler());

        server.start();
        server.join();
    }
}
