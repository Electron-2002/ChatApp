package com.example.chatapp.models;

public class User {
    private String name;
    private String thumbnail;
    private String status;

    public User() {

    }

    public User(String name, String thumbnail, String status) {
        this.name = name;
        this.thumbnail = thumbnail;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
