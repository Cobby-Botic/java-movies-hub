package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public class MoviesHandler extends BaseHttpHandler {

    private final Gson gson = new Gson();
    private final MoviesStore moviesStore;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String[] path = ex.getRequestURI().getPath().split("/");
        Headers headers = ex.getRequestHeaders();
        List<String> contentTypeRequest = headers.get("Content-type");

        Map<String, String> params =
                parseQuery(ex.getRequestURI().getQuery());

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
                } else if (params.containsKey("year")) {

                    try {
                        int year = Integer.parseInt(params.get("year"));
                        List<Movie> movies =
                                moviesStore.getMoviesByYear(year);

                        sendJson(ex, 200, gson.toJson(movies));
                    } catch (NumberFormatException e) {
                        sendJson(ex, 400,
                                gson.toJson(
                                        "Некорректный параметр запроса — year"
                                ));
                        return;
                    }
                } else {
                    sendJson(ex, 200, gson.toJson(moviesStore.getMovies().values()));
                }
                break;
            case "POST":
                if (contentTypeRequest == null ||
                        !contentTypeRequest.contains("application/json")) {

                    sendJson(ex, 415,
                            gson.toJson("Неверный Content-Type"));
                    return;
                }

                Movie movie = postMovie(ex);

                if (movie != null) {
                    sendJson(ex, 201, gson.toJson(movie));
                }
                break;
            case "DELETE":

                if (path.length == 2) {
                    moviesStore.deleteAll();
                    sendNoContent(ex);
                    break;
                } else {
                    int filmId;
                    try {
                        filmId = Integer.parseInt(path[2]);
                    } catch (NumberFormatException e) {
                        sendJson(ex, 400, gson.toJson("Некорректный id фильма"));
                        return;
                    }
                    boolean isDelete = moviesStore.deleteMovie(filmId);
                    if (isDelete) {
                        sendNoContent(ex);
                    } else {
                        sendJson(ex, 404, gson.toJson("Фильм не найден"));
                    }
                    break;
                }
            default:
                sendJson(ex, 405, gson.toJson("Метод не поддерживается"));
        }
    }

    private Movie postMovie(HttpExchange ex) throws IOException {

        String body = new String(
                ex.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        CreateMovieRequest request;

        try {
            request = gson.fromJson(
                    body,
                    CreateMovieRequest.class
            );
        } catch (JsonSyntaxException e) {
            sendJson(
                    ex,
                    400,
                    gson.toJson("Некорректный JSON")
            );
            return null;
        }

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
            errors.add("длина названия не может быть больше 100 символов");
        }

        if (!errors.isEmpty()) {
            return new ErrorResponse(
                    "Ошибка валидации",
                    errors
            );
        }
        return null;
    }

    private static Map<String, String> parseQuery(String query) {

        Map<String, String> result = new HashMap<>();

        if (query == null || query.isBlank()) {
            return result;
        }

        String[] pairs = query.split("&");

        for (String pair : pairs) {

            String[] keyValue = pair.split("=");

            if (keyValue.length == 2) {
                result.put(keyValue[0], keyValue[1]);
            }
        }

        return result;
    }
}
