package org.scubbo.popculturegraph.database;

import java.io.File;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;

public class MigrationScript {

    public static void main(String[] args) {
        new File("prod.db").renameTo(new File("prod_old.db"));

        DatabaseConnector oldConnector = new DatabaseConnector("jdbc:sqlite:prod_old.db");
        DatabaseConnector newConnector = new DatabaseConnector("jdbc:sqlite:prod.db");

        Collection<String> actorIds = oldConnector.getActorIds();
        System.out.println("Got actorIds (" + actorIds.size() + "): " + Objects.toString(actorIds));
        Collection<String> titleIds = oldConnector.getTitleIds();
        System.out.println("Got titleIds (" + titleIds.size() + "): " + Objects.toString(titleIds));

        actorIds.forEach((actorId) -> newConnector.setTitlesForActor(oldConnector.getTitlesForActorOld(actorId).getRight(), actorId));
        System.out.println("Updated titles_for_actor");
        titleIds.forEach((titleId) -> newConnector.setActorsForTitle(oldConnector.getActorsForTitleOld(titleId).getRight(), titleId));
        System.out.println("Updated actors_for_title");

        System.out.println("Verifying");
        actorIds.forEach((actorId) -> {
            AtomicInteger checkedActors = new AtomicInteger(0);
            Collection<Pair<Title, String>> oldValue = oldConnector.getTitlesForActorOld(actorId).getRight();
            Collection<Pair<Title, String>> newValue = newConnector.getTitlesForActor(actorId).getRight();
            oldValue.forEach((p) -> {
                if (!newValue.contains(p)) {
                    throw new RuntimeException("Pair " + p.toString() + " not in newvalue for actor " + actorId + ": " + Objects.toString(newValue));
                } else {
                    checkedActors.incrementAndGet();
                }
            });
            Integer totalCheckedActors = checkedActors.intValue();
            if (newValue.size() != totalCheckedActors) {
                throw new RuntimeException("Size mismatch! old had " + totalCheckedActors.toString() + " but new had " + String.valueOf(newValue.size()) + " for actor " + actorId);
            }
        });

        titleIds.forEach((titleId) -> {
            AtomicInteger checkedTitles = new AtomicInteger(0);
            Collection<Pair<Actor, String>> oldValue = oldConnector.getActorsForTitleOld(titleId).getRight();
            Collection<Pair<Actor, String>> newValue = newConnector.getActorsForTitle(titleId).getRight();
            oldValue.forEach((p) -> {
                if (!newValue.contains(p)) {
                    throw new RuntimeException("Pair " + p.toString() + " not in newvalue for title " + titleId);
                } else {
                    checkedTitles.incrementAndGet();
                }
            });
            Integer totalCheckedTitles = checkedTitles.intValue();
            if (newValue.size() != totalCheckedTitles) {
                throw new RuntimeException("Size mismatch! old had " + totalCheckedTitles.toString() + " but new had " + String.valueOf(newValue.size()) + " for title " + titleId);
            }
        });

        System.out.println("Verified - you're good to go!");
    }
}
