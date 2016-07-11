package org.scubbo.popculturegraph;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.SimpleGraph;
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
    // I swear that "42" here was just a happy coincidence
    private static final List<Integer> TITLE_SOCIABILITY_LEVELS = Lists.newArrayList(300, 42);

    // See above - these based on Fight Club, Memento, and
    // Scott Pilgrim Vs. The World
    // (Oh gosh, lawdy lawks, people might realise I'm a nerd!)
    private static final List<Integer> ACTOR_SOCIABILITY_LEVELS = Lists.newArrayList(95, 30);

    // These Collections keep track of whether a given node has been populated,
    // in order to block double-working
    // TODO: It may end up being more efficient to store this as a variable
    // on the Node itself, which would require creating a new class to populate
    // the graph - which is probably a good idea anyway.
    private static final Collection<Title> POPULATED_TITLES = Lists.newArrayList();
    private static final Collection<Actor> POPULATED_ACTORS = Lists.newArrayList();

    public GraphAdapter(DataFetcher dataFetcher) {
        graph = new SimpleGraph<>(String.class);
        this.dataFetcher = dataFetcher;
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

        List<Entity> neighbours = Graphs.neighborListOf(graph, actor);

        for (Entity neighbour: neighbours) {

            // Only spread if we haven't already spread from here
            if (!POPULATED_TITLES.contains(neighbour)) {
                final Collection<Pair<Actor, String>> data = dataFetcher.getActorsForTitle(neighbour.getId());
                for (Pair<Actor, String> character : data) {
                    graph.addVertex(character.getLeft());
                    graph.addEdge(neighbour, character.getLeft(), character.getRight());
                }
                POPULATED_TITLES.add((Title) neighbour);
            }

        }

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
    }

    public void spreadFromTitleNode(Title title) throws IOException {

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

    }

    @VisibleForTesting
    Graph getGraph() {
        return graph;
    }

}
