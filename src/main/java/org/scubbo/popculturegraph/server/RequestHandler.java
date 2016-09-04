package org.scubbo.popculturegraph.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.scubbo.popculturegraph.DataFetcher;
import org.scubbo.popculturegraph.GraphAdapter;
import org.scubbo.popculturegraph.Parser;
import org.scubbo.popculturegraph.database.DatabaseConnector;
import org.scubbo.popculturegraph.exception.PopulationException;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;
import org.scubbo.popculturegraph.net.JSoupWrapper;

public class RequestHandler extends AbstractHandler {

    public static final String TITLE_COLOR = "#f00";
    public static final String ACTOR_COLOR = "#00f";
    private final GraphAdapter adapter = new GraphAdapter(new DataFetcher(new DatabaseConnector("jdbc:sqlite:prod.db"), new JSoupWrapper(), new Parser()));
    //TODO: Hardcoded, replace
    private static final Actor actor = new Actor("0277213", "Nathan Fillion");
    private static final Title title = new Title("0379786", "Serenity");

    public RequestHandler() {

        try {
            System.out.println("Building request handler");

            System.out.println("Initializing from Actor Node");
            adapter.initializeWithActorNode(actor.getId(), actor.getName());
            System.out.println("Spreading from actor node");
            adapter.spreadFromActorNode(actor);

            System.out.println("Initializing from Title Node");
            adapter.initializeWithTitleNode(title.getId(), title.getName());
            System.out.println("Spreading from title node");
            adapter.spreadFromTitleNode(title);

            System.out.println("Initial spread complete");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

        String[] splitTarget = target.split("/");
        if (splitTarget.length > 2 && splitTarget[2].equals("hardcoded")) {
            writeHardcodedNodesAndEdges(baseRequest, response);
            return;
        }

        if (splitTarget.length > 2 && splitTarget[2].equals("title")) {
            String titleId = request.getParameter("id");
            String name = request.getParameter("name");
            // DEBUG
            System.out.println("Received a request for sociable neighbours of title " + name);
            List<Pair<Actor, String>> sociableNeighboursOfTitle;
            try {
                sociableNeighboursOfTitle = adapter.getSociableNeighboursOfTitle(new Title(titleId, name), 1);

                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);

                JSONArray nodes = new JSONArray();
                JSONArray edges = new JSONArray();

                for (Pair<Actor, String> actorAndCharacterName: sociableNeighboursOfTitle) {
                    Actor actor = actorAndCharacterName.getLeft();

                    JSONObject node = new JSONObject();
                    node.put("id", "actor_" + actor.getId());
                    node.put("color", ACTOR_COLOR);
                    node.put("name", actor.getName());
                    nodes.put(node);

                    JSONObject edge = new JSONObject();
                    JSONArray edgeNodes = new JSONArray();
                    edgeNodes.put("actor_" + actor.getId());
                    edgeNodes.put("title_" + titleId);
                    edge.put("nodes", edgeNodes);
                    edge.put("name", actorAndCharacterName.getRight());
                    edges.put(edge);

                    //TODO: background-thread this!
                    adapter.spreadFromActorNode(actor);
                }

                JSONObject data = new JSONObject();
                data.put("nodes", nodes);
                data.put("edges", edges);

                JSONObject json = new JSONObject();
                json.put("data", data);

                response.getWriter().println(json.toString());
            } catch (PopulationException e) {
                e.printStackTrace();
            }
        }
        if (splitTarget.length > 2 && splitTarget[2].equals("actor")) {
            String actorId = request.getParameter("id");
            String name = request.getParameter("name");
            List<Pair<Title, String>> sociableNeighboursOfActor;
            try {
                sociableNeighboursOfActor = adapter.getSociableNeighboursOfActor(new Actor(actorId, name), 1);

                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);

                JSONArray nodes = new JSONArray();
                JSONArray edges = new JSONArray();

                for (Pair<Title, String> titleAndCharacterName: sociableNeighboursOfActor) {
                    Title title = titleAndCharacterName.getLeft();

                    JSONObject node = new JSONObject();
                    node.put("id", "title_" + title.getId());
                    node.put("color", TITLE_COLOR);
                    node.put("name", title.getName());
                    nodes.put(node);

                    JSONObject edge = new JSONObject();
                    JSONArray edgeNodes = new JSONArray();
                    edgeNodes.put("title_" + title.getId());
                    edgeNodes.put("actor_" + actorId);
                    edge.put("nodes", edgeNodes);
                    edge.put("name", titleAndCharacterName.getRight());
                    edges.put(edge);

                    //TODO - background-thread this!
                    adapter.spreadFromTitleNode(title);
                }

                JSONObject data = new JSONObject();
                data.put("nodes", nodes);
                data.put("edges", edges);

                JSONObject json = new JSONObject();
                json.put("data", data);

                response.getWriter().println(json.toString());
            } catch (PopulationException e) {
                e.printStackTrace();
            }
        }


    }

    private void writeHardcodedNodesAndEdges(final Request baseRequest, final HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        JSONArray nodes = new JSONArray();

        JSONObject node1 = new JSONObject();
        node1.put("id", "actor_0277213");
        node1.put("color", ACTOR_COLOR);
        node1.put("name", "Nathan Fillion");
        nodes.put(node1);

        JSONObject node3 = new JSONObject();
        node3.put("id", "title_0379786");
        node3.put("color", TITLE_COLOR);
        node3.put("name", "Serenity");
        nodes.put(node3);

        JSONObject edge1 = new JSONObject();
        JSONArray edgeNodes2 = new JSONArray();
        edgeNodes2.put("actor_0277213");
        edgeNodes2.put("title_0379786");
        edge1.put("nodes", edgeNodes2);
        edge1.put("name", "Mal");

        JSONArray edges = new JSONArray();
        edges.put(edge1);

        JSONObject data = new JSONObject();
        data.put("nodes", nodes);
        data.put("edges", edges);

        JSONObject json = new JSONObject();
        json.put("data", data);
        response.getWriter().println(json.toString());
    }
}
