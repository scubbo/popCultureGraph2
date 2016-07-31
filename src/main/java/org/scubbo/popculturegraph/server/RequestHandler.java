package org.scubbo.popculturegraph.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class RequestHandler extends AbstractHandler {
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {

        String[] splitTarget = target.split("/");
        if (splitTarget.length > 0 && splitTarget[1].equals("api")) {
            callApi(target, baseRequest, request, response);
            return;
        }

        response.setContentType(getResponseType(target));
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        target = target.equals("/") ? "index.html" : target.substring(1);

        StringBuilder sb = new StringBuilder();
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(target);
        BufferedReader br = new BufferedReader(new InputStreamReader(resourceAsStream));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        response.getWriter().println(sb.toString());
    }

    private String getResponseType(final String target) {
        if (target.length()>1 && target.substring(target.length()-2).equals("js")) {
            return "text/javascript";
        }

        if (target.equals("/favicon.ico")) {
            return "image/x-icon";
        }

        return "text/html";
    }

    private void callApi(final String target,
                         final Request baseRequest,
                         final HttpServletRequest request,
                         final HttpServletResponse response)
            throws IOException, ServletException {

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        response.getWriter().println("{\"responseData\":123}");

    }
}
