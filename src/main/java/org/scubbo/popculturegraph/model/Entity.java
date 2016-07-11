package org.scubbo.popculturegraph.model;

import lombok.Data;

@Data
public abstract class Entity {
    private final String id;
    private final String name;
}
