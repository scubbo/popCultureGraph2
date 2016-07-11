package org.scubbo.popculturegraph.net;


import java.io.IOException;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class JSoupWrapper {

    private static final int MAX_RETRIES = 3;

    public Document getDoc(String uri) throws IOException {
        Connection c = Jsoup.connect(uri).timeout(9000);

        int attempt = 0;

        while (true) {
            try {
                return c.get();
            } catch (IOException e) {
                attempt++;
                if (attempt == MAX_RETRIES) {
                    throw e;
                }
            }
        }
    }
}
