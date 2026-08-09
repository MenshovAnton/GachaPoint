package ru.menshovanton.gachapoint.models;

public class Wish {
    private final int id;
    private final String dateTime;
    private final String content;

    public Wish(int id, String dateTime, String content) {
        this.id = id;
        this.dateTime = dateTime;
        this.content = content;
    }

    public int getId() { return id; }
    public String getDateTime() { return dateTime; }
    public String getContent() { return content; }
}
