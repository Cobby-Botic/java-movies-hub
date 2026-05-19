package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


class MoviesHandler extends BaseHttpHandler {

    Gson gson = new Gson();
    private MoviesStore moviesStore;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        Gson gson = new Gson();
        String[] path = ex.getRequestURI().getPath().split("/");

        switch (method) {
            case "GET":
                if (path.length == 3) {

                    int filmId;

                    try {
                        filmId = Integer.parseInt(path[2]);
                    } catch (NumberFormatException e) {
                        sendJson(ex, 400, gson.toJson("Некорректный id фильма"));
                        return;
                    }

                    Optional<Movie> findMovie = moviesStore.getMovieById(filmId);

                    if (findMovie.isPresent()) {
                        Movie movie = findMovie.get();
                        sendJson(ex, 200, gson.toJson(movie));
                    } else {
                        sendJson(ex, 404, gson.toJson("Фильм не найден"));
                    }
                } else {
                    sendJson(ex, 200, gson.toJson(moviesStore.getMovies()));
                }
                break;
            case "POST":
                Movie movie = postMovie(ex);

                if (movie != null) {
                    sendJson(ex, 201, gson.toJson(movie));
                }
                break;
            default:
                sendNoContent(ex);
        }
    }

    public Movie postMovie(HttpExchange ex) throws IOException {

        String body = new String(
                ex.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        CreateMovieRequest request =
                gson.fromJson(body, CreateMovieRequest.class);

        ErrorResponse error = validate(request);

        if (error != null) {
            sendJson(ex, 422, gson.toJson(error));
            return null;
        }

        Movie movie = new Movie(
                request.getYear(),
                request.getTitle()
        );

        moviesStore.addMovie(movie);

        return movie;
    }

    private ErrorResponse validate(CreateMovieRequest request) {

        List<String> errors = new ArrayList<>();

        int currentYear = LocalDate.now().getYear();

        if (request.getYear() < 1888 ||
                request.getYear() > currentYear + 1) {
            errors.add(
                    "год должен быть между 1888 и " + (currentYear + 1)
            );
        }

        if (request.getTitle() == null ||
                request.getTitle().isBlank()) {
            errors.add("название не должно быть пустым");
        }

        if (request.getTitle() != null &&
                request.getTitle().length() > 100) {
            errors.add("длина названия > 100");
        }

        if (!errors.isEmpty()) {
            return new ErrorResponse(
                    "Ошибка валидации",
                    errors
            );
        }
        return null;
    }
}

public class MoviesServer {

    private MoviesStore moviesStore;
    private int port;
    private final HttpServer server;

    public MoviesServer(MoviesStore moviesStore, int port) {
        this.moviesStore = moviesStore;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/movies", new MoviesHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    public MoviesStore getMoviesStore() {
        return moviesStore;
    }

    public void setMoviesStore(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }
}


class CreateMovieRequest {
    private String title;
    private int year;

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }
}