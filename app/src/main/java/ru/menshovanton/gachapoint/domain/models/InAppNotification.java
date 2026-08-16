package ru.menshovanton.gachapoint.domain.models;

public class InAppNotification {

    public enum Type { SUCCESS, ERROR, INFO }

    private final String message;
    private final Type type;

    public InAppNotification(String message, Type type) {
        this.message = message;
        this.type = type;
    }

    public String getMessage() { return message; }
    public Type getType() { return type; }
}