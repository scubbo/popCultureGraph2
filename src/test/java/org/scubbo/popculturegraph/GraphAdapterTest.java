package org.scubbo.popculturegraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.scubbo.popculturegraph.exception.PopulationException;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Entity;
import org.scubbo.popculturegraph.model.Title;

public class GraphAdapterTest {

    @Mock DataFetcher mockDataFetcher;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void test() throws Exception {
        when(mockDataFetcher.getTitlesForActor("a-id-1")).thenReturn(Lists.newArrayList(
                ImmutablePair.of(new Title("t-id-1", "t-name-1"), "c-name-1"),
                ImmutablePair.of(new Title("t-id-2", "t-name-2"), "c-name-2")
        ));

        GraphAdapter adapter = new GraphAdapter(mockDataFetcher);
        Graph<Entity, String> graph = adapter.getGraph();
        adapter.initializeWithActorNode("a-id-1", "a-name-1");

        assertThat(graph.vertexSet()).hasSize(3);
        List<Entity> neighbourhood = Graphs.neighborListOf(graph, new Actor("a-id-1", "a-name-1"));
        assertThat(neighbourhood).hasSize(2);
        neighbourhood.forEach((i) -> assertThat(i instanceof Title).isTrue());

        when(mockDataFetcher.getActorsForTitle("t-id-1")).thenReturn(Lists.newArrayList(
                ImmutablePair.of(new Actor("a-id-2", "a-name-2"), "c-name-3"),
                ImmutablePair.of(new Actor("a-id-3", "a-name-3"), "c-name-4")
        ));
        when(mockDataFetcher.getActorsForTitle("t-id-2")).thenReturn(Lists.newArrayList(
                ImmutablePair.of(new Actor("a-id-4", "a-name-4"), "c-name-5"),
                ImmutablePair.of(new Actor("a-id-2", "a-name-2"), "c-name-6") // I.e. this title contains an actor who was also in t-id-1
        ));

        adapter.spreadFromActorNode(new Actor("a-id-1", "a-name-1"));

        assertThat(graph.vertexSet()).hasSize(6);
    }

    @Test
    public void testFetchingTitlesOfSociableLevels() throws Exception {
        GraphAdapter adapter = new GraphAdapter(mockDataFetcher, Lists.newArrayList(3, 2), Lists.newArrayList(200, -1));

        Actor actor = new Actor("a-id-1", "a-name-1");
        when(mockDataFetcher.getTitlesForActor(actor.getId())).thenReturn(Lists.newArrayList(
                ImmutablePair.of(new Title("t-id-1", "t-name-1"), "c-name-1"),
                ImmutablePair.of(new Title("t-id-2", "t-name-2"), "c-name-2"),
                ImmutablePair.of(new Title("t-id-3", "t-name-3"), "c-name-3")
        ));
        adapter.initializeWithActorNode(actor.getId(), actor.getName());

        // along with the original a-id-1, this title has sociability 3 => should be returned on first get
        when(mockDataFetcher.getActorsForTitle("t-id-1")).thenReturn(Lists.newArrayList(
                ImmutablePair.of(new Actor("a-id-2", "a-name-2"), "c-name-4"),
                ImmutablePair.of(new Actor("a-id-3", "a-name-3"), "c-name-5")
        ));

        when(mockDataFetcher.getActorsForTitle("t-id-2")).thenReturn(Lists.newArrayList(
                ImmutablePair.of(new Actor("a-id-4", "a-name-4"), "c-name-6")
        ));

        // OK, in actuality it would return a single list containing only the originating actor,
        // but for our purposes they're equivalent because the graph is undirected and the edge
        // is already present
        when(mockDataFetcher.getActorsForTitle("t-id-3")).thenReturn(Collections.emptyList());

        adapter.spreadFromActorNode(actor);

        List<Pair<Title,String>> sociableNeighboursOfActor = adapter.getSociableNeighboursOfActor(actor, 1);
        assertThat(sociableNeighboursOfActor).hasSize(1);
        assertThat(sociableNeighboursOfActor.get(0).getLeft().getId()).isEqualTo("t-id-1");

        sociableNeighboursOfActor = adapter.getSociableNeighboursOfActor(actor, 2);
        assertThat(sociableNeighboursOfActor).hasSize(1);
        assertThat(sociableNeighboursOfActor.get(0).getLeft().getId()).isEqualTo("t-id-2");

        sociableNeighboursOfActor = adapter.getSociableNeighboursOfActor(actor, 3);
        assertThat(sociableNeighboursOfActor).hasSize(1);
        assertThat(sociableNeighboursOfActor.get(0).getLeft().getId()).isEqualTo("t-id-3");

        sociableNeighboursOfActor = adapter.getSociableNeighboursOfActor(actor, 4);
        assertThat(sociableNeighboursOfActor).hasSize(0);
    }

    @Test
    public void testFetchingActorsOfSociableLevels() throws Exception {
        GraphAdapter adapter = new GraphAdapter(mockDataFetcher, Lists.newArrayList(200, -1), Lists.newArrayList(3, 2));

        Title title = new Title("t-id-1", "t-name-1");
        when(mockDataFetcher.getActorsForTitle(title.getId())).thenReturn(Lists.newArrayList(
                ImmutablePair.of(new Actor("a-id-1", "a-name-1"), "c-name-1"),
                ImmutablePair.of(new Actor("a-id-2", "a-name-2"), "c-name-2"),
                ImmutablePair.of(new Actor("a-id-3", "a-name-3"), "c-name-3")
        ));
        adapter.initializeWithTitleNode(title.getId(), title.getName());

        // along with the original t-id-1, this title has sociability 3 => should be returned on first get
        when(mockDataFetcher.getTitlesForActor("a-id-1")).thenReturn(Lists.newArrayList(
                ImmutablePair.of(new Title("t-id-2", "t-name-2"), "c-name-4"),
                ImmutablePair.of(new Title("t-id-3", "t-name-3"), "c-name-5")
        ));

        when(mockDataFetcher.getTitlesForActor("a-id-2")).thenReturn(Lists.newArrayList(
                ImmutablePair.of(new Title("t-id-4", "t-name-4"), "c-name-6")
        ));

        // OK, in actuality it would return a single list containing only the originating title,
        // but for our purposes they're equivalent because the graph is undirected and the edge
        // is already present
        when(mockDataFetcher.getTitlesForActor("a-id-3")).thenReturn(Collections.emptyList());

        adapter.spreadFromTitleNode(title);

        List<Actor> sociableNeighboursOfTitle = adapter.getSociableNeighboursOfTitle(title, 1);
        assertThat(sociableNeighboursOfTitle).hasSize(1);
        assertThat(sociableNeighboursOfTitle.get(0).getId()).isEqualTo("a-id-1");

        sociableNeighboursOfTitle = adapter.getSociableNeighboursOfTitle(title, 2);
        assertThat(sociableNeighboursOfTitle).hasSize(1);
        assertThat(sociableNeighboursOfTitle.get(0).getId()).isEqualTo("a-id-2");

        sociableNeighboursOfTitle = adapter.getSociableNeighboursOfTitle(title, 3);
        assertThat(sociableNeighboursOfTitle).hasSize(1);
        assertThat(sociableNeighboursOfTitle.get(0).getId()).isEqualTo("a-id-3");

        sociableNeighboursOfTitle = adapter.getSociableNeighboursOfTitle(title, 4);
        assertThat(sociableNeighboursOfTitle).hasSize(0);
    }

    @Test(expected = PopulationException.class)
    public void testThrowsPopulationExceptionIfQueriedForTitleBeforeSpread() throws Exception {
        GraphAdapter adapter = new GraphAdapter(mockDataFetcher);
        Title title = new Title("t-id-1", "t-name-1");
        when(mockDataFetcher.getActorsForTitle(title.getId())).thenReturn(Collections.emptyList());
        adapter.initializeWithTitleNode(title.getId(), title.getName());
        adapter.getSociableNeighboursOfTitle(title, 1);
    }

    @Test(expected = PopulationException.class)
    public void testThrowsPopulationExceptionIfQueriedForActorBeforeSpread() throws Exception {
        GraphAdapter adapter = new GraphAdapter(mockDataFetcher);
        Actor actor = new Actor("a-id-1", "a-name-1");
        adapter.getSociableNeighboursOfActor(actor, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotAcceptSociabilityLevelLessThan1ForActor() throws Exception {
        GraphAdapter adapter = new GraphAdapter(mockDataFetcher);
        Actor actor = new Actor("a-id-1", "a-name-1");
        when(mockDataFetcher.getTitlesForActor(actor.getId())).thenReturn(Collections.emptyList());
        adapter.initializeWithActorNode(actor.getId(), actor.getName());
        adapter.spreadFromActorNode(actor);
        adapter.getSociableNeighboursOfActor(actor, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotAcceptSociabilityLevelLessThan1ForTitle() throws Exception {
        GraphAdapter adapter = new GraphAdapter(mockDataFetcher);
        Title title = new Title("t-id-1", "t-name-1");
        when(mockDataFetcher.getActorsForTitle(title.getId())).thenReturn(Collections.emptyList());
        adapter.initializeWithTitleNode(title.getId(), title.getName());
        adapter.spreadFromTitleNode(title);
        adapter.getSociableNeighboursOfTitle(title, 0);
    }


}