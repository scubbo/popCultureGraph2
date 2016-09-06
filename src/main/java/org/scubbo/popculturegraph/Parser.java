package org.scubbo.popculturegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;

public class Parser {

    public Collection<Pair<Actor, String>> parseDocForActors(final Document doc) {
        Collection<Pair<Actor, String>> actorsWithCharNames = new ArrayList<>();
        Element table = doc.getElementsByClass("cast_list").get(0);

        //child(0) to get inside past the <tb>
        table.child(0).children().stream()
                .filter(row -> row.classNames().contains("odd") || row.classNames().contains("even"))
                .forEach(row -> {
                    if (row.nodeName() != null) {
                        Elements actorTags = row.getElementsByClass("itemprop");
                        if (actorTags.isEmpty()) {
                            //TODO no idea what's going on here. But for some reason, consistently,
                            // Beth Broderick (nm0110803) has no itemprop tag in Castle (tt1219024)
                            return;
                        }
                        Element actorTag = actorTags.first();

                        final Elements actorNameLinks = actorTag.getElementsByTag("a");
                        if (actorNameLinks.isEmpty()) {
                            //TODO no idea what's going on here (that's something of a trend, as you'll see), but
                            // Darrin Bates (nm2721051) is badly behaved
                            return;
                        }
                        String actorId = actorNameLinks.first().attr("href").split("/")[2].replace("nm", "");
                        final Elements actorNameTags = actorTag.getElementsByTag("span");
                        if (actorNameTags.isEmpty()) {
                            //TODO as above and below. Paul O'Brien (nm0639770) in One Life To Live (tt0062595)
                            return;
                        }
                        String actorName = actorNameTags.first().text();

                        Elements characterTags = row.getElementsByClass("character");
                        if (characterTags.isEmpty()) {
                            //TODO again, no idea what's going on here, but Tony Rhune (nm0722605)
                            // has no character tag in One Life To Live (tt0062595)
                            return;
                        }
                        Element characterTag = characterTags.first();


                        String characterName;
                        final int indexOfBracket = characterTag.text().indexOf("(");
                        switch (indexOfBracket) {
                            case -1:
                                characterName = characterTag.text();
                                break;
                            case 0:
                                // It happens - IMDb data is not the best, some characters' "names" are listed as "(1 episode, 2012)" - e.g. http://www.imdb.com/title/tt1865718/fullcredits
                                characterName = "Uncredited";
                                break;
                            default:
                                characterName = characterTag.text().substring(0, indexOfBracket -1).trim();
                        }

                        actorsWithCharNames.add(ImmutablePair.of(new Actor(actorId, actorName), characterName));
                    }
                });

        return actorsWithCharNames;
    }

    public List<Pair<Title, Pair<String, Integer>>> parseDocForTitles(final Document tvDoc) {

        Element mainDiv = tvDoc.getElementsByClass("lister-list").get(0);

        return mainDiv.children().stream().filter(c -> c.hasClass("lister-item"))
            .map(c -> {
                final Element content = c.getElementsByClass("lister-item-content").first();
                final Element aLink = content.getElementsByClass("lister-item-header").first().getElementsByTag("a").first();
                final String titleName = aLink.text();
                final String titleId = aLink.attr("href").split("/")[2].substring(2);
                final String characterName = "blank-for-now";

                final Element firstRatings = content.getElementsByClass("ratings-imdb-rating").first();
                if (firstRatings == null) { // i.e. if not-yet-released
                    return Optional.<Pair<Title, Pair<String, Integer>>>empty();
                }
                final String dataValueAttr = firstRatings.attr("data-value");
                final Integer rating = Integer.valueOf(dataValueAttr.replace(".",""));
                return Optional.of(Pair.of(new Title(titleId, titleName), Pair.of(characterName, rating)));
            }).filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

}
