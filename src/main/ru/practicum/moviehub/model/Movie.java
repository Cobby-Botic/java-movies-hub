package ru.practicum.moviehub.model;

public class Movie {
    private final int id;
    private String title;
    private int yearOfCreation;
    private static int currentId = 1;

    public Movie(int yearOfCreation, String title) {
        this.yearOfCreation = yearOfCreation;
        this.title = title;
        this.id = currentId;
        currentId += 1;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "title='" + title + '\'' +
                ", year=" + yearOfCreation +
                '}';
    }

    public int getId() {
        return id;
    }
}