package org.scubbo.popculturegraph.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.StringJoiner;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.Pair;
import org.scubbo.popculturegraph.model.Actor;
import org.scubbo.popculturegraph.model.Title;

public class DatabaseConnector {

    private final String connectionString;

    public static void main(String[] args) {
        final DatabaseConnector d = new DatabaseConnector();
        d.setActorsForTitle(Lists.newArrayList(
                Pair.of(new Actor("test-id-1", "test-name-1"), "test-character-1"),
                Pair.of(new Actor("test-id-2", "test-name-2"), "test-character-2")),
                "title-id-1");
    }

    public DatabaseConnector() {
        this("jdbc:sqlite:test.db");

    }

    @VisibleForTesting
    public DatabaseConnector(String connectionString) {
        this.connectionString = connectionString;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC driver not found");
            System.exit(1);
        }

        try (Connection c = DriverManager.getConnection(this.connectionString)) {
            c.setAutoCommit(false);

            Statement getTableNamesStmt = c.createStatement();
            String getTableNamesSQL = "SELECT name FROM sqlite_master WHERE type='table'";
            ResultSet resultSet = getTableNamesStmt.executeQuery(getTableNamesSQL);

            Boolean hasActorsForTitleTable = false;
            Boolean hasTitlesForActorTable = false;

            while (resultSet.next()) {
                String name = resultSet.getString("name");
                if (name.equals("actors_for_title")) {
                    hasActorsForTitleTable = true;
                }
                if (name.equals("titles_for_actor")) {
                    hasTitlesForActorTable = true;
                }
            }
            resultSet.close();
            getTableNamesStmt.close();

            if (!hasActorsForTitleTable) {
                try (Statement stmt = c.createStatement()) {
                    stmt.executeUpdate(
                        "CREATE TABLE actors_for_title " +
                        "(titleId TEXT NOT NULL," +
                        "actorId TEXT NOT NULL," +
                        "actorName TEXT NOT NULL," +
                        "charName TEXT NOT NULL," +
                        "date INT NOT NULL)"
                    );
                    c.commit();
                }
            }

            if (!hasTitlesForActorTable) {
                try (Statement stmt = c.createStatement()) {
                    stmt.executeUpdate(
                        "CREATE TABLE titles_for_actor " +
                        "(actorId TEXT NOT NULL," +
                        "titleId TEXT NOT NULL," +
                        "titleName TEXT NOT NULL," +
                        "charName TEXT NOT NULL," +
                        "date INT NOT NULL)"
                    );
                    c.commit();
                }
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
    }

    public Pair<Instant, Collection<Pair<Actor, String>>> getActorsForTitle(String id) {
        try (Connection c = DriverManager.getConnection(connectionString)) {
            try (Statement stmt = c.createStatement()) {
                String sql = "SELECT * from actors_for_title where titleId='" + id + "'";
                final ResultSet resultSet = stmt.executeQuery(sql);

                Instant instant = null;
                Collection<Pair<Actor, String>> returnCollection = new ArrayList<>();

                if (!resultSet.isBeforeFirst()) {
                    return null;
                }


                while (resultSet.next()) {
                    instant = Instant.ofEpochMilli(resultSet.getLong("date")*1000);
                    String actorId = resultSet.getString("actorId");
                    String actorName = resultSet.getString("actorName");
                    String charName = resultSet.getString("charName");
                    returnCollection.add(Pair.of(new Actor(actorId, actorName), charName));
                }
                return Pair.of(instant, returnCollection);
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
        return null; // Keep IntelliJ happy
    }

    public void setActorsForTitle(Collection<Pair<Actor, String>> actors, String titleId) {
        try (Connection c = DriverManager.getConnection(connectionString)) {
            c.setAutoCommit(false);

            try (PreparedStatement stmt = c.prepareStatement("DELETE FROM actors_for_title WHERE titleId='" + titleId + "'")) {
                stmt.execute();
            }

            StringJoiner sj = new StringJoiner(",");
            actors.forEach((p) -> sj.add(
                "(" +
                "'" + titleId + "'," +
                "'" + p.getLeft().getId() + "'," +
                "'" + sanitize(p.getLeft().getName()) + "'," +
                "'" + sanitize(p.getRight()) + "'," +
                Calendar.getInstance().getTimeInMillis()/1000 +
                ")"
            ));
            String sql = "INSERT INTO actors_for_title (titleId, actorId, actorName, charName, date) VALUES " + sj.toString() + ";";
            try (PreparedStatement stmt = c.prepareStatement(sql)) {
                stmt.execute();
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
    }

    public Pair<Instant, Collection<Pair<Title, String>>> getTitlesForActor(String id) {
        try (Connection c = DriverManager.getConnection(connectionString)) {
            try (Statement stmt = c.createStatement()) {
                String sql = "SELECT * FROM titles_for_actor WHERE actorId='" + id + "'";
                final ResultSet resultSet = stmt.executeQuery(sql);

                if (!resultSet.isBeforeFirst()) {
                    return null;
                }

                Instant instant = null;
                Collection<Pair<Title, String>> returnCollection = new ArrayList<>();
                while (resultSet.next()) {
                    instant = Instant.ofEpochMilli(resultSet.getLong("date")*1000);
                    String titleId = resultSet.getString("titleId");
                    String titleName = resultSet.getString("titleName");
                    String charName = resultSet.getString("charName");
                    returnCollection.add(Pair.of(new Title(titleId, titleName), charName));
                }
                return Pair.of(instant, returnCollection);

            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
            return null; // Keep IntelliJ happy
        }
    }

    public void setTitlesForActor(Collection<Pair<Title, String>> titles, String actorId) {
        try (Connection c = DriverManager.getConnection(connectionString)) {
            c.setAutoCommit(false);


            try (PreparedStatement stmt = c.prepareStatement("DELETE FROM titles_for_actor WHERE actorId='" + actorId + "'")) {
                stmt.execute();
            }

            StringJoiner sj = new StringJoiner(",");
            titles.forEach((p) ->
                sj.add(
                    "(" +
                    "'" + actorId + "'," +
                    "'" + p.getLeft().getId() + "'," +
                    "'" + sanitize(p.getLeft().getName()) + "'," +
                    "'" + sanitize(p.getRight()) + "'," +
                    Calendar.getInstance().getTimeInMillis()/1000 +
                    ")"
                ));
            String sql = "INSERT INTO titles_for_actor (actorId, titleId, titleName, charName, date) VALUES " + sj.toString() + ";";
            try (PreparedStatement stmt = c.prepareStatement(sql)) {
                stmt.execute();
            }

            c.commit();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
    }

    private String sanitize(String s) {
        return s.replace("'", "''");
    }

    @VisibleForTesting
    void clearTables() {
        PreparedStatement stmt = null;
        try (Connection c = DriverManager.getConnection(connectionString)){
            c.setAutoCommit(false);

            stmt = c.prepareStatement("DELETE FROM actors_for_title;");
            stmt.execute();

            stmt = c.prepareStatement("DELETE FROM titles_for_actor;");
            stmt.execute();

            c.commit();
            stmt.close();
        } catch (Exception e){
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
