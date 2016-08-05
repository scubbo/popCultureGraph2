package org.scubbo.popculturegraph;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.scubbo.popculturegraph.database.DatabaseConnector;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;
import org.scubbo.popculturegraph.net.JSoupWrapper;

public class DataFetcher {

    private static final String TITLES_FOR_ACTOR_PREFIX = "http://www.imdb.com/name/nm";
    private static final String ACTORS_FOR_TITLE_PREFIX = "http://www.imdb.com/title/tt";
    private static final String ACTORS_FOR_TITLE_SUFFIX = "/fullcredits";

    private final DatabaseConnector databaseConnector;
    private final JSoupWrapper jSoupWrapper;
    private final Parser parser;

    public DataFetcher(
            DatabaseConnector databaseConnector,
            JSoupWrapper jSoupWrapper,
            Parser parser) {
        this.databaseConnector = databaseConnector;
        this.jSoupWrapper = jSoupWrapper;
        this.parser = parser;
    }

    public Collection<Pair<Actor, String>> getActorsForTitle(String id) throws IOException {
        Pair<Instant, Collection<Pair<Actor, String>>> databaseActorsForTitle =
                databaseConnector.getActorsForTitle(id);

        if (databaseActorsForTitle != null) {
            return databaseActorsForTitle.getRight();
        }

        Document doc = jSoupWrapper.getDoc(ACTORS_FOR_TITLE_PREFIX + id + ACTORS_FOR_TITLE_SUFFIX);
        final Collection<Pair<Actor, String>> actorsForTitle = parser.parseDocForActors(doc);
        databaseConnector.setActorsForTitle(actorsForTitle, id);
        return actorsForTitle;

    }


    public Collection<Pair<Title, String>> getTitlesForActor(String id) throws IOException {

        Pair<Instant, Collection<Pair<Title, String>>> databaseTitlesForActor =
                databaseConnector.getTitlesForActor(id);
        if (databaseTitlesForActor != null) {
            return databaseTitlesForActor.getRight();
        }

        Document doc = jSoupWrapper.getDoc(TITLES_FOR_ACTOR_PREFIX + id);
        Collection<Pair<Title, String>> titlesForActor = parser.parseDocForTitles(doc);
        databaseConnector.setTitlesForActor(titlesForActor, id);
        return titlesForActor;
    }
}
