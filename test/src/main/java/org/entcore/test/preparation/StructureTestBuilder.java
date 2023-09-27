package org.entcore.test.preparation;

public final class StructureTestBuilder {
    private String id;
    private String name;

    private StructureTestBuilder() {
    }

    public static StructureTestBuilder aStructureTest() {
        return new StructureTestBuilder();
    }

    public StructureTestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public StructureTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public StructureTest build() {
        return new StructureTest(id, name);
    }
}
