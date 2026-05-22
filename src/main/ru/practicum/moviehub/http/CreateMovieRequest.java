package ru.practicum.moviehub.http;

public class CreateMovieRequest {
    private final String title;
    private final int year;


    public CreateMovieRequest(String title, int year) {
        this.title = title;
        this.year = year;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }
}
