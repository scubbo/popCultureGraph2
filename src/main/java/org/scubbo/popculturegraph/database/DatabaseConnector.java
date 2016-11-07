package org.scubbo.popculturegraph.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;

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
            Boolean hasTitleTable = false;
            Boolean hasActorTable = false;
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                if (name.equals("actors_for_title")) {
                    hasTitleTable = true;
                }
                if (name.equals("titles_for_actor")) {
                    hasActorTable = true;
                }
            }
            resultSet.close();
            getTableNamesStmt.close();

            if (!hasTitleTable) {
                try (Statement stmt = c.createStatement()) {
                    stmt.executeUpdate(
                        "CREATE TABLE actors_for_title " +
                        "(id INT PRIMARY KEY NOT NULL," +
                        "actors BLOB NOT NULL," +
                        "date INT NOT NULL)"
                    );
                    c.commit();
                }
            }

            if (!hasActorTable) {
                try (Statement stmt = c.createStatement()) {
                    stmt.executeUpdate(
                        "CREATE TABLE titles_for_actor " +
                        "(id INT PRIMARY KEY NOT NULL," +
                        "titles BLOB NOT NULL," +
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

                String sql = "SELECT actors, date FROM actors_for_title WHERE id='" + id + "'";
                final ResultSet resultSet = stmt.executeQuery(sql);
                if (resultSet.next()) {
                    Instant date = Instant.ofEpochMilli(resultSet.getLong("date"));
                    Collection<Pair<Actor, String>> actors;
                    try (InputStream b = new ByteArrayInputStream(resultSet.getBytes("actors"))) {
                        try (ObjectInputStream o = new ObjectInputStream(b)) {
                            actors = ((Collection) o.readObject());
                        }
                    }
                    if (actors == null) {
                        System.out.println("Actors was null for id " + id);
                        return null;
                    }
                    return Pair.of(date, actors);
                } else {
                    return null;
                }
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

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(actors);
                    oos.flush();
                    oos.close();

                    if (getActorsForTitle(titleId) != null) {
                        try (PreparedStatement stmt = c.prepareStatement("UPDATE actors_for_title SET actors=?, date=? WHERE id='" + titleId + "'")) {
                            stmt.setBytes(1, bos.toByteArray());
                            stmt.setDate(2, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                            stmt.execute();
                        }
                    } else {
                        try (PreparedStatement stmt = c.prepareStatement("INSERT INTO actors_for_title (id, actors, date) VALUES (?, ?, ?)")) {
                            stmt.setString(1, titleId);
                            stmt.setBytes(2, bos.toByteArray());
                            stmt.setDate(3, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                            stmt.execute();
                        }
                    }

                    c.commit();

                }
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
    }

    public Pair<Instant, Collection<Pair<Title, String>>> getTitlesForActor(String id) {
        try (Connection c = DriverManager.getConnection(connectionString)) {
            try (Statement stmt = c.createStatement()) {

                String sql = "SELECT titles, date FROM titles_for_actor WHERE id='" + id + "'";
                final ResultSet resultSet = stmt.executeQuery(sql);
                if (resultSet.next()) {
                    Instant date = Instant.ofEpochMilli(resultSet.getLong("date"));
                    Collection<Pair<Title, String>> titles;
                    try (InputStream b = new ByteArrayInputStream(resultSet.getBytes("titles"))) {
                        try (ObjectInputStream o = new ObjectInputStream(b)) {
                            titles = ((Collection) o.readObject());
                        }
                    }
                    if (titles == null) {
                        System.out.println("Titles was null for id " + id);
                        return null;
                    }
                    return Pair.of(date, titles);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }

        return null;

    }

    public void setTitlesForActor(Collection<Pair<Title, String>> titles, String actorId) {
        try (Connection c = DriverManager.getConnection(connectionString)) {
            c.setAutoCommit(false);

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(titles);
                    oos.flush();
                    oos.close();

                    if (getTitlesForActor(actorId) != null) {
                        try (PreparedStatement stmt = c.prepareStatement("UPDATE titles_for_actor SET titles=?, date=? WHERE id='" + actorId + "'")) {
                            stmt.setBytes(1, bos.toByteArray());
                            stmt.setDate(2, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                            stmt.execute();
                        }
                    } else {
                        try (PreparedStatement stmt = c.prepareStatement("INSERT INTO titles_for_actor (id, titles, date) VALUES (?, ?, ?)")) {
                            stmt.setString(1, actorId);
                            stmt.setBytes(2, bos.toByteArray());
                            stmt.setDate(3, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                            stmt.execute();
                        }
                    }

                    c.commit();

                }
            }
        } catch (Exception e){
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
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
