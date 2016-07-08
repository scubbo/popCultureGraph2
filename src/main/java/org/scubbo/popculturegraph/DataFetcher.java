package org.scubbo.popculturegraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;

public class DataFetcher {

    private static final String TITLES_FOR_ACTOR_PREFIX = "http://www.imdb.com/name/nm";
    private static final String ACTORS_FOR_TITLE_PREFIX = "http://www.imdb.com/title/tt";
    private static final String ACTORS_FOR_TITLE_SUFFIX = "/fullcredits";

    public Collection<Pair<Actor, String>> getActorsForTitle(String id) throws IOException {
        Collection<Pair<Actor, String>> actorsWithCharNames = new ArrayList<>();

        Document doc = Jsoup.connect(ACTORS_FOR_TITLE_PREFIX + id + ACTORS_FOR_TITLE_SUFFIX).get();
        Element table = doc.getElementsByClass("cast_list").get(0);

        //child(0) to get inside past the <tb>
        table.child(0).children().stream()
            .filter(row -> row.classNames().contains("odd") || row.classNames().contains("even"))
            .forEach(row -> {
                if (row.nodeName() != null && !row.classNames().isEmpty()) {
                    Element actorTag = row.getElementsByClass("itemprop").get(0);

                    String actorId = actorTag.getElementsByTag("a").get(0).attr("href").split("/")[2].replace("nm", "");
                    String actorName = actorTag.getElementsByTag("span").get(0).text();

                    Element characterTag = row.getElementsByClass("character").get(0);
                    String characterName = characterTag.text().substring(0, characterTag.text().indexOf("(")-1).trim();

                    actorsWithCharNames.add(ImmutablePair.of(new Actor(actorId, actorName), characterName));
                }
        });

    return actorsWithCharNames;

    }


    public Collection<Pair<Title, String>> getTitlesForActor(String id) throws IOException {
        Collection<Pair<Title, String>> titlesWithCharNames = new ArrayList<>();

        Document doc = Jsoup.connect(TITLES_FOR_ACTOR_PREFIX + id).get();
        Element mainDiv = doc.getElementsByClass("filmo-category-section").get(0);

        mainDiv.children().stream().filter(title -> title.nodeName().equals("div") && title.classNames().contains("filmo-row")).forEach(title -> {
            title.getElementsByClass("year_column").forEach(Node::remove);

            Element bTag = title.getElementsByTag("b").get(0);
            Element titleTag = bTag.getElementsByTag("a").get(0);
            String titleName = titleTag.text().trim();
            String titleId = titleTag.attr("href").split("/")[2].substring(2);

            bTag.remove();

            for (Element child: title.children()) {
                if (child.hasText()) {
                    child.text("");
                }
                if (child.nodeName().equals("br")) {
                    child.remove();
                    break;
                }
            }

            title.getElementsByClass("filmo-episodes").forEach(Node::remove);

            String charname = title.text().trim().replace("\n", "").replaceAll("\\(.*?\\)", "");
            titlesWithCharNames.add(ImmutablePair.of(new Title(titleId, titleName), charname));
        });

        return titlesWithCharNames;
    }
}
