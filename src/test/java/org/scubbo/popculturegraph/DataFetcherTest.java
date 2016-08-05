package org.scubbo.popculturegraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.scubbo.popculturegraph.database.DatabaseConnector;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;
import org.scubbo.popculturegraph.net.JSoupWrapper;

@RunWith(MockitoJUnitRunner.class)
public class DataFetcherTest {

    private static final String TEST_TITLE_ID = "test-title-id";
    private static final String TEST_ACTOR_ID = "test-actor-id";
    private static final String TEST_ACTOR_NAME = "test-actor-name";
    private static final String TEST_TITLE_NAME = "test-title-name";
    private static final String TEST_CHARACTER_NAME = "test-character-name";

    private static final String TEST_ACTOR_URL = "http://www.imdb.com/name/nm" + TEST_ACTOR_ID;
    private static final String TEST_TITLE_URL = "http://www.imdb.com/title/tt" + TEST_TITLE_ID + "/fullcredits";

    private static final Collection<Pair<Actor, String>> TEST_ACTORS = Lists.newArrayList(
            Pair.of(new Actor(TEST_ACTOR_ID, TEST_ACTOR_NAME), TEST_CHARACTER_NAME)
    );
    private static final Collection<Pair<Title, String>> TEST_TITLES = Lists.newArrayList(
            Pair.of(new Title(TEST_TITLE_ID, TEST_TITLE_NAME), TEST_CHARACTER_NAME)
    );

    @Mock Document mockDoc;

    @Mock DatabaseConnector databaseConnector;
    @Mock JSoupWrapper jSoupWrapper;
    @Mock Parser parser;

    @InjectMocks
    private DataFetcher dataFetcher;

    @Test
    public void testDatabaseOperationActorsForTitle() throws Exception {
        when(databaseConnector.getActorsForTitle(TEST_TITLE_ID))
                .thenReturn(Pair.of(Instant.now(), Lists.newArrayList(
                        Pair.of(new Actor(TEST_ACTOR_ID, TEST_ACTOR_NAME), TEST_CHARACTER_NAME)
                )));
        Collection<Pair<Actor, String>> actorsForTitle =
                dataFetcher.getActorsForTitle(TEST_TITLE_ID);
        verify(databaseConnector).getActorsForTitle(TEST_TITLE_ID);
        verifyZeroInteractions(jSoupWrapper);
        assertThat(actorsForTitle).hasSize(1);
        Pair<Actor, String> firstPair = actorsForTitle.iterator().next();
        assertThat(firstPair.getLeft().getId()).isEqualTo(TEST_ACTOR_ID);
        assertThat(firstPair.getLeft().getName()).isEqualTo(TEST_ACTOR_NAME);
        assertThat(firstPair.getRight()).isEqualTo(TEST_CHARACTER_NAME);
    }

    @Test
    public void testDatabaseOperationTitlesForActor() throws Exception {
        when(databaseConnector.getTitlesForActor(TEST_ACTOR_ID))
                .thenReturn(Pair.of(Instant.now(), Lists.newArrayList(
                        Pair.of(new Title(TEST_TITLE_ID, TEST_TITLE_NAME), TEST_CHARACTER_NAME)
                )));
        Collection<Pair<Title, String>> titlesForActor =
                dataFetcher.getTitlesForActor(TEST_ACTOR_ID);
        verify(databaseConnector).getTitlesForActor(TEST_ACTOR_ID);
        verifyZeroInteractions(jSoupWrapper);
        assertThat(titlesForActor).hasSize(1);
        Pair<Title, String> firstPair = titlesForActor.iterator().next();
        assertThat(firstPair.getLeft().getId()).isEqualTo(TEST_TITLE_ID);
        assertThat(firstPair.getLeft().getName()).isEqualTo(TEST_TITLE_NAME);
        assertThat(firstPair.getRight()).isEqualTo(TEST_CHARACTER_NAME);
    }

    @Test
    public void testCallsOutAndSetsDatabaseWhenDatabaseIsNullTitlesForActor() throws Exception {
        when(databaseConnector.getTitlesForActor(TEST_ACTOR_ID))
                .thenReturn(null);
        when(jSoupWrapper.getDoc(TEST_ACTOR_URL))
                .thenReturn(mockDoc);
        when(parser.parseDocForTitles(mockDoc))
                .thenReturn(TEST_TITLES);

        dataFetcher.getTitlesForActor(TEST_ACTOR_ID);

        verify(databaseConnector).getTitlesForActor(TEST_ACTOR_ID);
        verify(jSoupWrapper).getDoc(TEST_ACTOR_URL);
        verify(parser).parseDocForTitles(mockDoc);
        verify(databaseConnector).setTitlesForActor(TEST_TITLES, TEST_ACTOR_ID);
    }

    @Test
    public void testCallsOutAndSetsDatabaseWhenDatabaseIsNullActorsForTitle() throws Exception {
        when(databaseConnector.getActorsForTitle(TEST_TITLE_ID))
                .thenReturn(null);
        when(jSoupWrapper.getDoc(TEST_TITLE_URL))
                .thenReturn(mockDoc);
        when(parser.parseDocForActors(mockDoc))
                .thenReturn(TEST_ACTORS);

        dataFetcher.getActorsForTitle(TEST_TITLE_ID);

        verify(databaseConnector).getActorsForTitle(TEST_TITLE_ID);
        verify(jSoupWrapper).getDoc(TEST_TITLE_URL);
        verify(parser).parseDocForActors(mockDoc);
        verify(databaseConnector).setActorsForTitle(TEST_ACTORS, TEST_TITLE_ID);
    }

}
