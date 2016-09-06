package org.scubbo.popculturegraph;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.scubbo.popculturegraph.exception.PopulationException;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;

public class GraphAdapter {

    private final DataFetcher dataFetcher;

    public GraphAdapter(DataFetcher dataFetcher) {
            this.dataFetcher = dataFetcher;
    }

    public List<Pair<Title, String>> getPopularNeighboursOfActor(Actor actor, Integer popularityLevel) throws PopulationException, IOException {
        if (popularityLevel < 1) {
            throw new IllegalArgumentException();
        }

        final Collection<Pair<Title, String>> data = dataFetcher.getTitlesForActor(actor.getId());

        return data.stream()
                //.filter(based on ids of current neighbours)
                // also pass back Character Name
                .limit(3 * popularityLevel)
                .map((t) -> Pair.of(t.getLeft(), t.getRight()))
                .collect(Collectors.toList());
    }

    public List<Pair<Actor, String>> getPopularNeighboursOfTitle(Title title, Integer popularityLevel) throws PopulationException, IOException {
        if (popularityLevel < 1) {
            throw new IllegalArgumentException();
        }

        final Collection<Pair<Actor, String>> data = dataFetcher.getActorsForTitle(title.getId());

        return data.stream()
                //.filter(based on ids of current neighbours)
                // also pass back Character Name
                .limit(3 * popularityLevel)
                .map((a) -> Pair.of(a.getLeft(), a.getRight()))
                .collect(Collectors.toList());
    }

}
