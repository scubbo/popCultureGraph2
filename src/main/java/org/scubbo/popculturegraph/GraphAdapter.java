package org.scubbo.popculturegraph;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;

public class GraphAdapter {

    private final DataFetcher dataFetcher;

    public GraphAdapter(DataFetcher dataFetcher) {
            this.dataFetcher = dataFetcher;
    }

    public List<Pair<Title, String>> getPopularNeighboursOfActor(
                Actor actor,
                List<String> neighbours)
            throws IOException {

        final Collection<Pair<Title, String>> data = dataFetcher.getTitlesForActor(actor.getId());

        return data.stream()
                .filter((t) -> !neighbours.contains(t.getLeft().getId()))
                .limit(3)
                .map((t) ->
                    StringUtils.isBlank(t.getRight()) ? // If character name is blank...
                        Pair.of(                        // ...build a new pair from...
                            t.getLeft(),                // ...the existing title, and
                            dataFetcher.fetchAndUpdateCharacterNameForTitleAndActor(
                                t.getLeft().getId(),    // ...a re-fetched character name:
                                actor.getId())) :
                        t)                              // otherwise, just pass back
                .collect(Collectors.toList());
    }

    public List<Pair<Actor, String>> getPopularNeighboursOfTitle(
                Title title,
                List<String> neighbours)
            throws IOException {

        final Collection<Pair<Actor, String>> data = dataFetcher.getActorsForTitle(title.getId());


        return data.stream()
                .filter((a) -> !neighbours.contains(a.getLeft().getId()))
                .limit(3)
                .collect(Collectors.toList());
    }

}
