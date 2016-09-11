package org.scubbo.popculturegraph.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;
import org.scubbo.popculturegraph.net.JSoupWrapper;

public class RequestHandler extends AbstractHandler {

    public static final String TITLE_COLOR = "#f00";
    public static final String ACTOR_COLOR = "#00f";
    private final DataFetcher dataFetcher = new DataFetcher(new DatabaseConnector("jdbc:sqlite:prod.db"), new JSoupWrapper(), new Parser());
    private final GraphAdapter adapter = new GraphAdapter(dataFetcher);

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {

        try {
            System.out.println("Receieved request: " + target + ":" + request);
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
        } catch (Exception e) {
            System.out.println("Caught an exception!");
            System.out.println(e.getMessage());
            e.printStackTrace();
            throw e;
        }
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

        // TODO: This should be a switch
        if (splitTarget.length > 2 && splitTarget[2].equals("title")) {
            callAPIForTitle(baseRequest, request, response);
        }
        if (splitTarget.length > 2 && splitTarget[2].equals("actor")) {
            callAPIForActor(baseRequest, request, response);
        }
        if (splitTarget.length > 2 && splitTarget[2].equals("startup")) {
            callAPIForStartup(baseRequest, request, response);
        }
        if (splitTarget.length > 2 && splitTarget[2].equals("characterName")) {
            callAPIForCharacterName(baseRequest, request, response);
        }

    }

    private void callAPIForCharacterName(final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        String actorId = request.getParameter("actorId");
        String titleId = request.getParameter("titleId");

        JSONObject returnObject = new JSONObject();
        String characterName = dataFetcher.fetchAndUpdateCharacterNameForTitleAndActor(titleId, actorId);
        returnObject.put("characterName", characterName);
        returnObject.put("actorId", actorId);
        returnObject.put("titleId", titleId);
        response.getWriter().println(returnObject.toString());
    }

    private void callAPIForStartup(final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        JSONArray nodes = new JSONArray();
        JSONArray edges = new JSONArray();

        if (request.getParameter("type").equals("actor")) {

            Actor actor = dataFetcher.searchForActor(request.getParameter("val"));
            List<Pair<Title, String>> popularNeighboursOfActor = adapter.getPopularNeighboursOfActor(actor, Collections.emptyList());

            JSONObject node = new JSONObject();
            node.put("id", "actor_" + actor.getId());
            node.put("color", ACTOR_COLOR);
            node.put("name", actor.getName());
            nodes.put(node);

            popularNeighboursOfActor.forEach((p) -> {
                JSONObject titleNode = new JSONObject();
                titleNode.put("id", "title_" + p.getLeft().getId());
                titleNode.put("color", TITLE_COLOR);
                titleNode.put("name", p.getLeft().getName());
                nodes.put(titleNode);

                JSONObject edge = new JSONObject();
                JSONArray edgeNodes = new JSONArray();
                edgeNodes.put("title_" + p.getLeft().getId());
                edgeNodes.put("actor_" + actor.getId());
                edge.put("nodes", edgeNodes);
                edge.put("name", p.getRight());
                edges.put(edge);
            });

        } else {

            Title title = dataFetcher.searchForTitle(request.getParameter("val"));
            List<Pair<Actor, String>> popularNeighboursOfTitle = adapter.getPopularNeighboursOfTitle(title, Collections.emptyList());

            JSONObject node = new JSONObject();
            node.put("id", "title_" + title.getId());
            node.put("color", TITLE_COLOR);
            node.put("name", title.getName());
            nodes.put(node);

            popularNeighboursOfTitle.forEach((p) -> {
                JSONObject actorNode = new JSONObject();
                actorNode.put("id", "actor_" + p.getLeft().getId());
                actorNode.put("color", ACTOR_COLOR);
                actorNode.put("name", p.getLeft().getName());
                nodes.put(actorNode);

                JSONObject edge = new JSONObject();
                JSONArray edgeNodes = new JSONArray();
                edgeNodes.put("actor_" + p.getLeft().getId());
                edgeNodes.put("title_" + title.getId());
                edge.put("nodes", edgeNodes);
                edge.put("name", p.getRight());
                edges.put(edge);
            });

        }

        JSONObject data = new JSONObject();
        data.put("nodes", nodes);
        data.put("edges", edges);

        JSONObject json = new JSONObject();
        json.put("data", data);

        response.getWriter().println(json.toString());
    }

    private void callAPIForActor(final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        String actorId = request.getParameter("id");
        String name = request.getParameter("name");
        List<String> neighbours =
                Arrays.stream(request.getParameterValues("neighbours[]"))
                        .map(s -> s.substring(s.indexOf("_") + 1))
                        .collect(Collectors.toList());
        List<Pair<Title, String>> popularNeighboursOfActor =
                adapter.getPopularNeighboursOfActor(new Actor(actorId, name), neighbours);

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        JSONArray nodes = new JSONArray();
        JSONArray edges = new JSONArray();

        for (Pair<Title, String> titleAndCharacterName: popularNeighboursOfActor) {
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

        }

        JSONObject data = new JSONObject();
        data.put("nodes", nodes);
        data.put("edges", edges);

        JSONObject json = new JSONObject();
        json.put("data", data);

        response.getWriter().println(json.toString());
    }

    private void callAPIForTitle(final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        String titleId = request.getParameter("id");
        String name = request.getParameter("name");
        List<String> neighbours =
                Arrays.stream(request.getParameterValues("neighbours[]"))
                .map(s -> s.substring(s.indexOf("_") + 1))
                .collect(Collectors.toList());
        List<Pair<Actor, String>> popularNeighboursOfTitle =
                adapter.getPopularNeighboursOfTitle(new Title(titleId, name), neighbours);

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        JSONArray nodes = new JSONArray();
        JSONArray edges = new JSONArray();

        for (Pair<Actor, String> actorAndCharacterName: popularNeighboursOfTitle) {
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

        }

        JSONObject data = new JSONObject();
        data.put("nodes", nodes);
        data.put("edges", edges);

        JSONObject json = new JSONObject();
        json.put("data", data);

        response.getWriter().println(json.toString());
    }

}
