package org.scubbo.popculturegraph.nettest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.scubbo.popculturegraph.DataFetcher;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;
import org.scubbo.popculturegraph.net.JSoupWrapper;

public class DataFetcherTest {

    private DataFetcher fetcher = new DataFetcher(new JSoupWrapper());

    @Test
    public void testNetConnectionTitlesForActor() throws Exception {
        final Collection<Pair<Title, String>> titlesForActor = fetcher.getTitlesForActor("0004770");
        assertThat(titlesForActor).isNotEmpty();
    }

    @Test
    public void testNetConnectionActorsForTitle() throws Exception {
        final Collection<Pair<Actor, String>> actorsForTitle = fetcher.getActorsForTitle("0303461");
        assertThat(actorsForTitle).isNotEmpty();
        for (Pair<Actor, String> actorAndChar: actorsForTitle) {
            if (actorAndChar.getLeft().getId().equals("0277213")) {
                assertThat(actorAndChar.getLeft().getName()).contains("Nathan");
                assertThat(actorAndChar.getRight()).contains("Mal");
                return;
            }
        }
        fail();
    }

    @Test
    public void testGetActorsFromTV() throws Exception {
        final Collection<Pair<Actor, String>> actorsForTitle = fetcher.getActorsForTitle("1219024");
        assertThat(actorsForTitle).isNotEmpty();
    }

    @Test
    public void testGetActorsForTitleWhenSomeCharactersHaveNoName() throws Exception {
        final Collection<Pair<Actor, String>> actorsForTitle = fetcher.getActorsForTitle("1865718");
        assertThat(actorsForTitle).isNotEmpty();
    }

    @Test
    public void testGetActorsForTitleWhenSomeActorHasNoCharacter() throws Exception {
        final Collection<Pair<Actor, String>> actorsForTitle = fetcher.getActorsForTitle("0062595");
        assertThat(actorsForTitle).isNotEmpty();
    }

}