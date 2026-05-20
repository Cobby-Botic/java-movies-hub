package ru.practicum.moviehub.http;

import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore;

    @BeforeAll
    static void beforeAll() {

        moviesStore = new MoviesStore();
        server = new MoviesServer(moviesStore, 8080);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void clearStore() {
        moviesStore.deleteAll();
        Movie.resetIds();
    }


    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_returnsMovies() throws Exception {
        moviesStore.addMovie(
                new Movie(1999, "Matrix")
        );

        moviesStore.addMovie(
                new Movie(2002, "Resident Evil")
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode());

        String body = resp.body();

        assertTrue(body.contains("Matrix"));
        assertTrue(body.contains("Resident Evil"));
    }

    @Test
    void addMovie_correctData() throws Exception {
        String json = """
                {
                  "title": "Matrix",
                  "year": 1999
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(
                        req,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(201, resp.statusCode());

        String body = resp.body();

        assertTrue(body.contains("Matrix"));
        assertTrue(body.contains("1999"));
    }

    @Test
    void addMovie_WhenTitleIsEmpty_ReturnError() throws Exception {
        String json = """
                {
                  "title": "",
                  "year": 1999
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(
                        req,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );
        assertEquals(422, resp.statusCode());

        assertTrue(resp.body().contains("название не должно быть пустым"));
    }

    @Test
    void addMovie_whenTitleTooLong_returns422() throws Exception {

        String longTitle = "a".repeat(101);

        String json = """
                {
                  "title": "%s",
                  "year": 1999
                }
                """.formatted(longTitle);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(
                        req,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(422, resp.statusCode());

        assertTrue(
                resp.body().contains("длина названия не может быть больше 100 символов")
        );
    }

    @Test
    void addMovie_WhenYearIncorrect_ReturnError() throws Exception {
        String json = """
                {
                  "title": "Unnamed film",
                  "year": 1887
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(
                        req,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(422, resp.statusCode());

        assertTrue(resp.body().contains("год должен быть между 1888 и"));
    }

    @Test
    void addMovie_whenContentType_isIncorrect_returnError() throws Exception {
        String json = """
                {
                  "title": "Matrix",
                  "year": 1999
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/jsonS")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(
                        req,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(415, resp.statusCode());

        String body = resp.body();

        assertTrue(body.contains("Неверный Content-Type"));
    }

    @Test
    void addMovie_whenJsonIsIncorrect_returnError() throws Exception {
        String invalidJson = """
                {
                  "title": "Matrix",
                  "year":
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> resp =
                client.send(
                        req,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(400, resp.statusCode());

        assertTrue(
                resp.body().contains("Некорректный JSON")
        );
    }

    @Test
    void getMovieById() throws Exception {
        moviesStore.addMovie(
                new Movie(1999, "Matrix")
        );

        moviesStore.addMovie(
                new Movie(2002, "Resident Evil")
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode());

        String body = resp.body();

        assertTrue(body.contains("Matrix"));
    }

    @Test
    void getMovieById_whenIdIsEmpty() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, resp.statusCode());

        String body = resp.body();

        assertTrue(body.contains("Фильм не найден"));
    }

    @Test
    void getMovieById_whenIdIsNotInt() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode());

        String body = resp.body();

        assertTrue(body.contains("Некорректный id"));
    }

    @Test
    void deleteMovieById() throws Exception {
        moviesStore.addMovie(
                new Movie(1999, "Matrix")
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, resp.statusCode());

        String body = resp.body();

        assertTrue(moviesStore.getMovies().isEmpty());
    }

    @Test
    void deleteMovieById_whenIdIsNull() throws Exception {
        moviesStore.addMovie(
                new Movie(1999, "Matrix")
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, resp.statusCode());

        String body = resp.body();

        assertFalse(moviesStore.getMovies().isEmpty());
    }

    @Test
    void deleteMovieById_whenIdIsNotInt() throws Exception {
        moviesStore.addMovie(
                new Movie(1999, "Matrix")
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode());

        String body = resp.body();

        assertTrue(resp.body().contains("Некорректный id"));
        assertFalse(moviesStore.getMovies().isEmpty());
    }

    @Test
    void getMoviesByYear() throws Exception {
        moviesStore.addMovie(
                new Movie(1999, "Matrix")
        );

        moviesStore.addMovie(
                new Movie(2002, "Resident Evil")
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1999"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode());

        String body = resp.body();

        assertTrue(body.contains("Matrix"));
    }

    @Test
    void getMoviesByYear_whenFilmsByYearIsNull() throws Exception {
        moviesStore.addMovie(
                new Movie(1999, "Matrix")
        );

        moviesStore.addMovie(
                new Movie(2002, "Resident Evil")
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2015"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode());

        String body = resp.body();

        assertTrue(body.contains("[]"));
    }

    @Test
    void getMoviesByYear_whenYear_notInt() throws Exception {
        moviesStore.addMovie(
                new Movie(1999, "Matrix")
        );

        moviesStore.addMovie(
                new Movie(2002, "Resident Evil")
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode());

        String body = resp.body();

        assertTrue(body.contains("Некорректный параметр запроса — year"));
    }
}