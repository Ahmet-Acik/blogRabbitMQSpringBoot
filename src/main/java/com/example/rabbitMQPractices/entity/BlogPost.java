package com.example.rabbitMQPractices.entity;

import lombok.AllArgsConstructor;

import java.io.Serializable;

public class BlogPost implements Serializable {
    private String id;
    private String title;
    private String content;

    public BlogPost() {
    }

    public BlogPost(String id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "BlogPost{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}