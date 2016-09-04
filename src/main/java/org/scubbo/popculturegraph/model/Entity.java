package org.scubbo.popculturegraph.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Entity implements Serializable {
    private String id;
    private String name;

    public String toString() {
        return id + " (" + name + ")";
    }
}
