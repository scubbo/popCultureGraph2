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
import java.sql.SQLException;
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
        Connection c = null;
        Statement stmt = null;
        String sql = null;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(connectionString);
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            sql = "SELECT name FROM sqlite_master WHERE type='table'";
            ResultSet resultSet = stmt.executeQuery(sql);
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
            stmt.close();

            if (!hasTitleTable) {
                stmt = c.createStatement();
                sql = "CREATE TABLE actors_for_title " +
                        "(id INT PRIMARY KEY NOT NULL," +
                        "actors BLOB NOT NULL," +
                        "date INT NOT NULL)";
                stmt.executeUpdate(sql);
                c.commit();
                stmt.close();
            }

            if (!hasActorTable) {
                stmt = c.createStatement();
                sql = "CREATE TABLE titles_for_actor " +
                        "(id INT PRIMARY KEY NOT NULL," +
                        "titles BLOB NOT NULL," +
                        "date INT NOT NULL)";
                stmt.executeUpdate(sql);
                c.commit();
                stmt.close();
            }

            c.close();

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    public Pair<Instant, Collection<Pair<Actor, String>>> getActorsForTitle(String id) {
        Connection c = null;
        Statement stmt = null;
        try {
            try {
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:test.db");
                System.out.println("Opened database successfully");

                stmt = c.createStatement();
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
                    System.out.println("Database had no contents for title id " + id + ", returning null");
                    return null;
                }

            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(0);
            }

            return null;

        } finally {

            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void setActorsForTitle(Collection<Pair<Actor, String>> actors, String titleId) {
        Connection c = null;
        PreparedStatement stmt = null;
        try {
            try {
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:test.db");
                c.setAutoCommit(false);
                System.out.println("Opened database successfully");

                try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        oos.writeObject(actors);
                        oos.flush();
                        oos.close();

                        if (getActorsForTitle(titleId) != null) {
                            stmt = c.prepareStatement("UPDATE actors_for_title SET actors=?, date=? WHERE id='" + titleId + "'");
                            stmt.setBytes(1, bos.toByteArray());
                            stmt.setDate(2, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                        } else {
                            stmt = c.prepareStatement("INSERT INTO actors_for_title (id, actors, date) VALUES (?, ?, ?)");
                            stmt.setString(1, titleId);
                            stmt.setBytes(2, bos.toByteArray());
                            stmt.setDate(3, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                        }

                        stmt.execute();
                        c.commit();

                    }
                }

            } catch (Exception e){
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(0);
            }
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Pair<Instant, Collection<Pair<Title, String>>> getTitlesForActor(String id) {
        Connection c = null;
        Statement stmt = null;
        try {
            try {
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:test.db");
                System.out.println("Opened database successfully");

                stmt = c.createStatement();
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
                    System.out.println("Database had no contents for actors id " + id + ", returning null");
                    return null;
                }

            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(0);
            }

            return null;

        } finally {

            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void setTitlesForActor(Collection<Pair<Title, String>> titles, String actorId) {
        Connection c = null;
        PreparedStatement stmt = null;
        try {
            try {
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:test.db");
                c.setAutoCommit(false);
                System.out.println("Opened database successfully");

                try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        oos.writeObject(titles);
                        oos.flush();
                        oos.close();

                        if (getTitlesForActor(actorId) != null) {
                            stmt = c.prepareStatement("UPDATE titles_for_actor SET titles=?, date=? WHERE id='" + actorId + "'");
                            stmt.setBytes(1, bos.toByteArray());
                            stmt.setDate(2, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                        } else {
                            stmt = c.prepareStatement("INSERT INTO titles_for_actor (id, titles, date) VALUES (?, ?, ?)");
                            stmt.setString(1, actorId);
                            stmt.setBytes(2, bos.toByteArray());
                            stmt.setDate(3, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                        }

                        stmt.execute();
                        c.commit();

                    }
                }

            } catch (Exception e){
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(0);
            }
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @VisibleForTesting
    void clearTables() {
        Connection c = null;
        PreparedStatement stmt = null;
        try {
            try {
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:test.db");
                c.setAutoCommit(false);
                System.out.println("Opened database successfully");

                stmt = c.prepareStatement("DELETE FROM actors_for_title;");
                stmt.execute();

                stmt = c.prepareStatement("DELETE FROM titles_for_actor;");
                stmt.execute();

                c.commit();

            } catch (Exception e){
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(0);
            }
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
