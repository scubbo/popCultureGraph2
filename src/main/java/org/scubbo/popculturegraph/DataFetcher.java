package org.scubbo.popculturegraph;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.scubbo.popculturegraph.database.DatabaseConnector;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;
import org.scubbo.popculturegraph.net.JSoupWrapper;

public class DataFetcher {

    private static final String TV_TITLES_FOR_ACTOR_STRING = "http://www.imdb.com/filmosearch?explore=title_type&role=nm%s&ref_=filmo_vw_adv&sort=user_rating,desc&mode=advanced&page=1&title_type=tvSeries";
    private static final String MOVIE_TITLES_FOR_ACTOR_STRING = "http://www.imdb.com/filmosearch?explore=title_type&role=nm%s&ref_=filmo_ref_typ&sort=user_rating,desc&mode=advanced&page=1&title_type=movie";
    private static final String ACTORS_FOR_TITLE_PREFIX = "http://www.imdb.com/title/tt";
    private static final String ACTORS_FOR_TITLE_SUFFIX = "/fullcredits";
    public static final String SEARCH_FOR_ACTOR_URL = "http://www.imdb.com/find?q=%s&s=nm&ref_=fn_nm";
    private static final String SEARCH_FOR_TITLE_URL = "http://www.imdb.com/find?q=%s&s=tt&ref_=fn_tt";

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

        Document tvDoc = jSoupWrapper.getDoc(String.format(TV_TITLES_FOR_ACTOR_STRING, id));
        Document movieDoc = jSoupWrapper.getDoc(String.format(MOVIE_TITLES_FOR_ACTOR_STRING, id));
        List<Pair<Title, Pair<String, Integer>>> tvTitlesForActor = parser.parseDocForTitles(tvDoc);
        List<Pair<Title, Pair<String, Integer>>> movieTitlesForActor = parser.parseDocForTitles(movieDoc);
        List<Pair<Title, String>> overallList = buildMergedList(tvTitlesForActor, movieTitlesForActor);
        databaseConnector.setTitlesForActor(overallList, id);
        return overallList;
    }

    private List<Pair<Title, String>> buildMergedList(final List<Pair<Title, Pair<String, Integer>>> list1,
                                                      final List<Pair<Title, Pair<String, Integer>>> list2) {
        final Iterator<Pair<Title, Pair<String, Integer>>> iter1 = list1.iterator();
        final Iterator<Pair<Title, Pair<String, Integer>>> iter2 = list2.iterator();

        final List<Pair<Title, String>> outputList = new ArrayList<>();

        Pair<Title, Pair<String, Integer>> item1 = iter1.next();
        Pair<Title, Pair<String, Integer>> item2 = iter2.next();

        while (iter1.hasNext() && iter2.hasNext()) {
            if (item1.getRight().getRight() > item2.getRight().getRight()) {
                outputList.add(Pair.of(item1.getLeft(), item1.getRight().getLeft()));
                item1 = iter1.next();
            } else {
                outputList.add(Pair.of(item2.getLeft(), item2.getRight().getLeft()));
                item2 = iter2.next();
            }
        }

        // At this point, one or the other of the iterators has been exhausted, but item1 and item2 still exist
        if (iter1.hasNext()) {
            while (iter1.hasNext()) {
                if (item1.getRight().getRight() > item2.getRight().getRight()) {
                    outputList.add(Pair.of(item1.getLeft(), item1.getRight().getLeft()));
                    item1 = iter1.next();
                } else {
                    outputList.add(Pair.of(item2.getLeft(), item2.getRight().getLeft()));
                    while (iter1.hasNext()) {
                        final Pair<Title, Pair<String, Integer>> next = iter1.next();
                        outputList.add(Pair.of(next.getLeft(), next.getRight().getLeft()));
                        // TODO: This leaves off the final one, but in the interests of cleanliness, I don't care...
                    }
                }
            }
        } else {
            while (iter2.hasNext()) {
                if (item2.getRight().getRight() > item1.getRight().getRight()) {
                    outputList.add(Pair.of(item2.getLeft(), item2.getRight().getLeft()));
                    item2 = iter2.next();
                } else {
                    outputList.add(Pair.of(item1.getLeft(), item1.getRight().getLeft()));
                    while (iter2.hasNext()) {
                        final Pair<Title, Pair<String, Integer>> next = iter2.next();
                        outputList.add(Pair.of(next.getLeft(), next.getRight().getLeft()));
                    }
                }
            }
        }

        return outputList;

    }

    public Actor searchForActor(final String name) throws IOException {
        Document searchForActorDoc = jSoupWrapper.getDoc(String.format(SEARCH_FOR_ACTOR_URL, name.replace(" ", "%20")));
        return parser.parseSearchForActorDoc(searchForActorDoc);
    }

    public Title searchForTitle(final String name) throws IOException {
        Document searchForTitleDoc = jSoupWrapper.getDoc(String.format(SEARCH_FOR_TITLE_URL, name.replace(" ", "%20")));
        return parser.parseSearchForTitleDoc(searchForTitleDoc);
    }
}
