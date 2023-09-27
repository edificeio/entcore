package org.entcore.test.preparation;

public class ClassTest {
    private final String id;
    private final String name;

    public ClassTest(final String id, final String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
