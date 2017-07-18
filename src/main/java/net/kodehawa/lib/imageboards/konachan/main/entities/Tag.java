package net.kodehawa.lib.imageboards.konachan.main.entities;

public class Tag {

    private boolean ambiguos;
    private int count;
    private int id;
    private String name;
    private int type;

    public Tag(int id, String name, int count, int type, boolean ambiguos) {
        this.id = id;
        this.name = name;
        this.count = count;
        this.type = type;
        this.ambiguos = ambiguos;
    }

    public int getCount() {
        return count;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public boolean isAmbiguos() {
        return ambiguos;
    }
}
