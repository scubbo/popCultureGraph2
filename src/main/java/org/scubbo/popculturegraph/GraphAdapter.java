package org.scubbo.popculturegraph;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.scubbo.popculturegraph.exception.PopulationException;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Entity;
import org.scubbo.popculturegraph.model.Title;

public class GraphAdapter {

    private final DataFetcher dataFetcher;
    private final UndirectedGraph<Entity, String> graph;

    // These numbers will almost certainly need some tweaking.
    // They are based on a highly scientific and rigourous study
    // with n=3 (Nathan Fillion, Brad Pitt, and Natalie Portman),
    // attempting to determine the required levels of "sociability"
    // (here measured only by "number of actors in a film") at which
    // we would show 10% and 50% of all titles
    //
    private final List<Integer> TITLE_SOCIABILITY_LEVELS;

    // See above - these based on Fight Club, Memento, and
    // Scott Pilgrim Vs. The World
    // (Oh gosh, lawdy lawks, people might realise I'm a nerd!)
    private final List<Integer> ACTOR_SOCIABILITY_LEVELS;

    // These Collections keep track of whether a given node has been populated and/or spread,
    // in order to block double-working
    // TODO: It may end up being more efficient to store this as a variable
    // on the Node itself, which would require creating a new class to populate
    // the graph - which is probably a good idea anyway.
    //
    // Definitions:
    //  - Populated - all neighbours of the node have been added to the graph
    //  - Spread    - all neighbours of all neighbours of the node have been added to the graph
    //              - (i.e. all neighbours of the node are populated)
    private final Collection<Title> POPULATED_TITLES = Lists.newArrayList();
    private final Collection<Actor> POPULATED_ACTORS = Lists.newArrayList();
    private final Collection<Title> SPREAD_TITLES = Lists.newArrayList();
    private final Collection<Actor> SPREAD_ACTORS = Lists.newArrayList();

    public GraphAdapter(DataFetcher dataFetcher) {
        // I swear that "42" here was just a happy coincidence.
        // And, come to think of it, in the context of pop culture, "300" too.
        this(
            dataFetcher,
            Lists.newArrayList(300, 42),
            Lists.newArrayList(95, 30));
    }

    @VisibleForTesting
    GraphAdapter(DataFetcher dataFetcher, List<Integer> titleSociabilityLevels, List<Integer> actorSociabilityLevels) {
        graph = new SimpleGraph<>(String.class);
        this.dataFetcher = dataFetcher;
        TITLE_SOCIABILITY_LEVELS = titleSociabilityLevels;
        ACTOR_SOCIABILITY_LEVELS = actorSociabilityLevels;
    }


    public void initializeWithActorNode(String id, String name) throws IOException {
        //TODO - look up id based on name

        final Actor initialNode = new Actor(id, name);
        graph.addVertex(initialNode);

        final Collection<Pair<Title, String>> data = dataFetcher.getTitlesForActor(id);
        for (Pair<Title, String> character: data) {
            graph.addVertex(character.getLeft());
            graph.addEdge(initialNode, character.getLeft(), character.getRight());
        }
    }

    // This method assumes that all the node's neighbourhood exist in the graph already,
    // but that *their* neighbourhood does not
    //
    // This method fetches data for all the node's neighbours, and populates their neighbourhood
    public void spreadFromActorNode(Actor actor) throws IOException {

        if (SPREAD_ACTORS.contains(actor)) {
            return;
        }

        List<Entity> neighbours = Graphs.neighborListOf(graph, actor);

        // DEBUG
        System.out.println("Found " + String.valueOf(neighbours.size()) + " neighbours");

        for (int i = 0; i< neighbours.size(); i++) {
            Entity neighbour = neighbours.get(i);

            // Only spread if we haven't already spread from here
            if (!POPULATED_TITLES.contains(neighbour)) {
                final Collection<Pair<Actor, String>> data = dataFetcher.getActorsForTitle(neighbour.getId());
                for (Pair<Actor, String> character : data) {
                    graph.addVertex(character.getLeft());
                    graph.addEdge(neighbour, character.getLeft(), character.getRight());
                }
                POPULATED_TITLES.add((Title) neighbour);
            }
            if (i%5 == 0) {
                // DEBUG
                System.out.println("completed neighbour " + String.valueOf(i));
            }
        }
        SPREAD_ACTORS.add(actor);

    }

    public void initializeWithTitleNode(String id, String name) throws IOException {
        //TODO - look up id based on name

        final Title initialNode = new Title(id, name);
        graph.addVertex(initialNode);

        final Collection<Pair<Actor, String>> data = dataFetcher.getActorsForTitle(id);
        for (Pair<Actor, String> character: data) {
            graph.addVertex(character.getLeft());
            graph.addEdge(initialNode, character.getLeft(), character.getRight());
        }
        POPULATED_TITLES.add(initialNode);
    }

    public void spreadFromTitleNode(Title title) throws IOException {

        if (SPREAD_TITLES.contains(title)) {
            return;
        }

        List<Entity> neighbours = Graphs.neighborListOf(graph, title);

        for (Entity neighbour: neighbours) {

            if (!POPULATED_ACTORS.contains(neighbour)) {
                final Collection<Pair<Title, String>> data = dataFetcher.getTitlesForActor(neighbour.getId());
                for (Pair<Title, String> character : data) {
                    graph.addVertex(character.getLeft());
                    graph.addEdge(neighbour, character.getLeft(), character.getRight());
                }
                POPULATED_ACTORS.add((Actor) neighbour);
            }

        }

        SPREAD_TITLES.add(title);

    }

    public List<Pair<Title, String>> getSociableNeighboursOfActor(Actor actor, Integer sociabilityLevel) throws PopulationException {
        if (!SPREAD_ACTORS.contains(actor)) {
            // Something's gone wrong - we have been asked to get sociable neighbours
            // of a node that has not yet had its neighbours populated.
            throw new PopulationException("Asked to get sociable neighbours for actor " + actor + " which has not yet been populated");
        }
        if (sociabilityLevel < 1) {
            throw new IllegalArgumentException();
        }

        List<Entity> neighbours = Graphs.neighborListOf(graph, actor);
        Integer requiredSociabilityLevel = getTitleSociabilityLevel(sociabilityLevel);
        Integer previousSociabilityLevel = getTitleSociabilityLevel(sociabilityLevel - 1);

        return neighbours.stream()
                .filter((n) ->
                        graph.degreeOf(n) >= requiredSociabilityLevel &&
                        graph.degreeOf(n) < previousSociabilityLevel)
                .map((n) -> Pair.of((Title) n, graph.getEdge(n, actor)))
                .collect(Collectors.toList());
    }

    private Integer getTitleSociabilityLevel(Integer level) {
        if (level == 0) {
            return Integer.MAX_VALUE;
        }
        if (TITLE_SOCIABILITY_LEVELS.size() < level) {
            return 0;
        }
        return TITLE_SOCIABILITY_LEVELS.get(level - 1);
    }

    public List<Pair<Actor, String>> getSociableNeighboursOfTitle(Title title, Integer sociabilityLevel) throws PopulationException {
        if (!SPREAD_TITLES.contains(title)) {
            // Something's gone wrong - we have been asked to get sociable neighbours
            // of a node that has not yet had its neighbours populated.
            throw new PopulationException("Asked to get sociable neighbours for title " + title + " which has not yet been populated");
        }
        if (sociabilityLevel < 1) {
            throw new IllegalArgumentException();
        }

        List<Entity> neighbours = Graphs.neighborListOf(graph, title);
        Integer requiredSociabilityLevel = getActorSociabilityLevel(sociabilityLevel);
        Integer previousSociabilityLevel = getActorSociabilityLevel(sociabilityLevel - 1);

        return neighbours.stream()
                .filter((n) ->
                        graph.degreeOf(n) >= requiredSociabilityLevel &&
                        graph.degreeOf(n) < previousSociabilityLevel)
                .map((n) -> Pair.of((Actor) n, graph.getEdge(n, title)))
                .collect(Collectors.toList());
    }

    private Integer getActorSociabilityLevel(Integer level) {
        if (level == 0) {
            return Integer.MAX_VALUE;
        }
        if (ACTOR_SOCIABILITY_LEVELS.size() < level) {
            return 0;
        }
        return ACTOR_SOCIABILITY_LEVELS.get(level - 1);
    }

    @VisibleForTesting
    Graph getGraph() {
        return graph;
    }

}
