package org.scubbo.popculturegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;

public class Parser {

    // Some titles are just boring - e.g. chat shows, etc.
    // This removes them
    private static final Set<String> SUPPRESSED_TITLE_IDS = Sets.newHashSet(
            "1524593", // Kevin Pollak's Chat Show
            "0421460", // The Soup
            "4280606", // The Late Late Show with James Corden
            "0437729", // The Late Late Show with Craig Ferguson
            "3109382", // Nerd HQ
            "2773976", // Speakeasy: With Paul F. Tompkins
            "4335742", // Lip Sync Battle
            "3037520", // Hollywood Game Night
            "0379623", // Ellen: The Ellen DeGeneres Show
            "0115147", // The Daily Show with Trevor Noah
            "0169455", // Inside the Actors Studio
            "4877562", // The Chris Gethard Show
            "2691394", // Parks and Recreation: Dammit Jerry!
            "4311010", // Parks and Recreation in Europe
            "2138881", // Parks and Recreation: Road Trip
            "3530232", // Last Week Tonight with John Oliver
            "1245769", // The Jace Hall Show
            "3327536", // UCB Comedy Originals
            "0106052", // Late Night with Conan O'Brien
            "1231460", // Late Night with Jimmy Fallon
            "3513388", // Late Night with Seth Meyers
            "0083441", // Late Night with David Letterman
            "0072562", // SNL
            "0376434", // Tinseltown TV
            "1392211", // A Powerful Noise Live
            "1637574", // Conan
            "2326995", // The Peter Austin Noto Show
            "1535002", // Between Two Ferns with Zach Galifianakis
            "3025364", // Carson on TCM
            "0458254" // The Colbert Report
    );

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
                if (SUPPRESSED_TITLE_IDS.contains(titleId)) {
                    return Optional.<Pair<Title, Pair<String, Integer>>>empty();
                }
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

    // TODO this logic is exactly the same as the Title search, only different because of
    // type signature (and because Entity is abstract).
    public Actor parseSearchForActorDoc(final Document doc) {
        final Element link = doc.getElementsByClass("findList").first()
                                .getElementsByTag("tbody").first()
                                .getElementsByTag("tr").first()
                                .getElementsByClass("result_text").first()
                                .getElementsByTag("a").first();
        final String name = link.text();
        final String id = link.attr("href").split("/")[2].substring(2);
        return new Actor(id, name);
    }

    public Title parseSearchForTitleDoc(final Document doc) {
        final Element link = doc.getElementsByClass("findList").first()
                .getElementsByTag("tbody").first()
                .getElementsByTag("tr").first()
                .getElementsByClass("result_text").first()
                .getElementsByTag("a").first();
        final String name = link.text();
        final String id = link.attr("href").split("/")[2].substring(2);
        return new Title(id, name);
    }

}
