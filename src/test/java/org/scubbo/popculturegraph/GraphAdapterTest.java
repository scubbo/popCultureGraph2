package org.scubbo.popculturegraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
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


}