package org.scubbo.popculturegraph.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;

public class DatabaseConnectorTest {

    private static final List<Pair<Actor, String>> TEST_ACTOR_LIST_1 = Lists.newArrayList(
            Pair.of(new Actor("test-id-1", "test-name-1"), "test-character-1"),
            Pair.of(new Actor("test-id-2", "test-name-2"), "test-character-2"));
    private static final List<Pair<Actor, String>> TEST_ACTOR_LIST_2 = Lists.newArrayList(
            Pair.of(new Actor("test-id-1", "test-name-1"), "test-character-1"),
            Pair.of(new Actor("test-id-2", "test-name-2"), "test-character-2"));
    private static final List<Pair<Title, String>> TEST_TITLE_LIST_1 = Lists.newArrayList(
            Pair.of(new Title("test-id-1", "test-name-1"), "test-character-1"),
            Pair.of(new Title("test-id-2", "test-name-2"), "test-character-2"));
    private static final List<Pair<Title, String>> TEST_TITLE_LIST_2 = Lists.newArrayList(
            Pair.of(new Title("test-id-1", "test-name-1"), "test-character-1"),
            Pair.of(new Title("test-id-2", "test-name-2"), "test-character-2"));

    // I'd rather this wasn't static, but needs to be to be used in BeforeClass
    private static final DatabaseConnector connector = new DatabaseConnector();

    @BeforeClass
    public static void setUpClass() {
        connector.clearTables();
        assertThat(connector.getActorsForTitle("test-title-id-1")).isNull();
    }

    @Test
    public void testIO() {
        connector.setActorsForTitle(TEST_ACTOR_LIST_1, "test-title-id-1");
        assertThat(connector.getActorsForTitle("test-title-id-1").getRight()).isEqualTo(TEST_ACTOR_LIST_1);
        assertThat(connector.getActorsForTitle("test-title-id-2")).isNull();

        connector.setTitlesForActor(TEST_TITLE_LIST_1, "test-actor-id-1");
        assertThat(connector.getTitlesForActor("test-actor-id-1").getRight()).isEqualTo(TEST_TITLE_LIST_1);
        assertThat(connector.getTitlesForActor("test-title-id-2")).isNull();
    }

    @Test
    public void testUpdate() throws Exception {
        connector.setActorsForTitle(TEST_ACTOR_LIST_1, "test-title-id-1");
        Pair<Instant, Collection<Pair<Actor, String>>> actorResponse1 = connector.getActorsForTitle("test-title-id-1");
        Thread.sleep(1000);
        connector.setActorsForTitle(TEST_ACTOR_LIST_2, "test-title-id-1");
        Pair<Instant, Collection<Pair<Actor, String>>> actorResponse2 = connector.getActorsForTitle("test-title-id-1");
        assertThat(actorResponse2.getLeft()).isGreaterThan(actorResponse1.getLeft());
        assertThat(actorResponse2.getRight()).isEqualTo(TEST_ACTOR_LIST_2);

        connector.setTitlesForActor(TEST_TITLE_LIST_1, "test-actor-id-1");
        Pair<Instant, Collection<Pair<Title, String>>> titleResponse1 = connector.getTitlesForActor("test-actor-id-1");
        Thread.sleep(1000);
        connector.setTitlesForActor(TEST_TITLE_LIST_2, "test-actor-id-1");
        Pair<Instant, Collection<Pair<Title, String>>> titleResponse2 = connector.getTitlesForActor("test-actor-id-1");
        assertThat(titleResponse2.getLeft()).isGreaterThan(titleResponse1.getLeft());
        assertThat(titleResponse2.getRight()).isEqualTo(TEST_ACTOR_LIST_2);
    }

}