package org.scubbo.popculturegraph.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.scubbo.popculturegraph.model.Actor;

public class SerializationTest {
    Collection<Pair<Actor, String>> testList = Lists.newArrayList(
            Pair.of(new Actor("test-id-1", "test-name-1"), "test-character-1"),
            Pair.of(new Actor("test-id-2", "test-name-2"), "test-character-2"));

    @Test
    public void test1() {
        Actor actor = new Actor("test-id-1", "test-name-1");
        try {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(actor);
                    oos.flush();
                    oos.close();
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray())) {
                        try (ObjectInputStream ois = new ObjectInputStream(bis)) {
                            assertThat((Actor)ois.readObject()).isEqualTo(actor);
                        }
                    }
                }
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void test2() {
        try {
            try(ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(testList);
                    oos.flush();
                    oos.close();
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray())) {
                        try (ObjectInputStream ois = new ObjectInputStream(bis)) {
                            assertThat(ois).isNotNull();
                            final Object actual = ois.readObject();
                            assertThat(actual).isNotNull();
                            List list = (List) actual;
                            assertThat(list).isNotEmpty();
                            Pair firstItem = (Pair) list.get(0);
                            assertThat(firstItem).isNotNull();
                            Actor firstActor = (Actor) firstItem.getLeft();
                            assertThat(firstActor).isNotNull();

                            assertThat(firstActor.getId()).isEqualTo("test-id-1");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}