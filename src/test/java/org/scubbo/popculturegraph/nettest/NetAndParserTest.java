package org.scubbo.popculturegraph.nettest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;
import org.scubbo.popculturegraph.Parser;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.net.JSoupWrapper;

public class NetAndParserTest {

    private static final String TITLES_FOR_ACTOR_PREFIX = "http://www.imdb.com/name/nm";
    private static final String ACTORS_FOR_TITLE_PREFIX = "http://www.imdb.com/title/tt";
    private static final String ACTORS_FOR_TITLE_SUFFIX = "/fullcredits";

    private JSoupWrapper jSoupWrapper = new JSoupWrapper();
    private Parser parser = new Parser();

    //TODO rewrite this!
    @Test
    @Ignore
    public void testNetConnectionTitlesForActor() throws Exception {
//        final Collection<Pair<Title, String>> titlesForActor = parser.parseDocForTitlesOLD(jSoupWrapper.getDoc(TITLES_FOR_ACTOR_PREFIX + "0004770"));
//        assertThat(titlesForActor).isNotEmpty();
    }

    @Test
    public void testNetConnectionActorsForTitle() throws Exception {
        final Collection<Pair<Actor, String>> actorsForTitle =
                parser.parseDocForActors(jSoupWrapper.getDoc(ACTORS_FOR_TITLE_PREFIX + "0303461" + ACTORS_FOR_TITLE_SUFFIX));
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
        final Collection<Pair<Actor, String>> actorsForTitle =
                parser.parseDocForActors(jSoupWrapper.getDoc(ACTORS_FOR_TITLE_PREFIX + "1219024" + ACTORS_FOR_TITLE_SUFFIX));
        assertThat(actorsForTitle).isNotEmpty();
    }

    @Test
    public void testGetActorsForTitleWhenSomeCharactersHaveNoName() throws Exception {
        final Collection<Pair<Actor, String>> actorsForTitle =
                parser.parseDocForActors(jSoupWrapper.getDoc(ACTORS_FOR_TITLE_PREFIX + "1865718" + ACTORS_FOR_TITLE_SUFFIX));
        assertThat(actorsForTitle).isNotEmpty();
    }

    @Test
    public void testGetActorsForTitleWhenSomeActorHasNoCharacter() throws Exception {
        final Collection<Pair<Actor, String>> actorsForTitle =
                parser.parseDocForActors(jSoupWrapper.getDoc(ACTORS_FOR_TITLE_PREFIX + "0062595" + ACTORS_FOR_TITLE_SUFFIX));
        assertThat(actorsForTitle).isNotEmpty();
    }

    @Test
    public void testUnknownNullPointer() throws Exception {
        parser.parseDocForActors(jSoupWrapper.getDoc(ACTORS_FOR_TITLE_PREFIX + "0364828" + ACTORS_FOR_TITLE_SUFFIX));
    }

}