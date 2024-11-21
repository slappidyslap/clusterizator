package org.example;

public class MapKey {
    private final String regex;
    private final int groupId;
    private final int index;

    public MapKey(String regex, int groupId, int index) {
        this.regex = regex;
        this.groupId = groupId;
        this.index = index;
    }

    public String regex() {
        return regex;
    }

    public int groupId() {
        return groupId;
    }

    public int index() {
        return index;
    }
}
