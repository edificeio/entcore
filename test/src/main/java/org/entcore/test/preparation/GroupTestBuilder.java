package org.entcore.test.preparation;

public final class GroupTestBuilder {
    private String id;
    private String name;

    private GroupTestBuilder() {
    }

    public static GroupTestBuilder aGroupTest() {
        return new GroupTestBuilder();
    }

    public GroupTestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public GroupTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public GroupTest build() {
        return new GroupTest(id, name);
    }
}
