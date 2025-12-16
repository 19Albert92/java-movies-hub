package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String CT_JSON = "application/json; charset=UTF-8";

    private static final String BASE = "http://localhost:8080";

    private static MoviesServer server;
    private static MoviesStore store;
    private static HttpClient client;

    private static Gson gson;

    @BeforeAll
    static void beforeAll() {

        gson = new Gson();

        store = new MoviesStore();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        server = new MoviesServer(store, 8080);
        server.start();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void beforeUp() {
        store.clearMovies();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws IOException, InterruptedException {
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
    void getMovies_whenMoviesExist_returnsMoviesList() {

        int expected = 3;

        IntStream.range(0, expected)
                .forEach(val -> {
                    store.saveMovie("Test title film_" + val, 1993);
                });

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        try {
            HttpResponse<String> resp =
                    client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

            assertFalse(movies.isEmpty(), "Не должно быть пустой список");

            assertEquals(expected, movies.size(), "Должно было добавиться 3 записи");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void saveMovie_withValidData_returnsCreatedMovie() {

        Movie movie = new Movie("Test title film", 1991, 1);

        String movieToString = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieToString))
                .header("Content-Type", CT_JSON)
                .build();

        try {
            HttpResponse<String> resp =
                    client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertEquals(gson.toJson(movie), resp.body(),
                    "Должен вернуться тот же фильм что и сохранили");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void saveMovie_whenInvalidContentType_returnsError415() {
        String movieToString = gson.toJson(new Movie("Test title film", 1995));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieToString))
                .header("Content-Type", "123")
                .build();

        HttpRequest reqWithEmptyHeader = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieToString))
                .build();

        try {
            HttpResponse<String> resp =
                    client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertEquals(415, resp.statusCode(),
                    "Должен вернуться статус 415 так как не правильное значение в header Content-Type");

            HttpResponse<String> respWithEmptyHeader =
                    client.send(reqWithEmptyHeader, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertEquals(415, respWithEmptyHeader.statusCode(),
                    "Должен вернуться статус 415 так как не добавлен header Content-Type");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void saveMovie_whenBodyParamsIsNotValid_returnsError422() {
        String movieWithErrorTitle = gson.toJson(new Movie("Test title film Test title film Test title film Test title film Test title film Test title filmtitle film Test title film", 1991));

        String movieWithErrorYear = gson.toJson(new Movie("Test title", 1879));

        HttpRequest reqWithErrorTitle = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(movieWithErrorTitle))
                .build();

        HttpRequest reqWithErrorYear = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(movieWithErrorYear))
                .build();

        try {
            HttpResponse<String> respErrorTitle =
                    client.send(reqWithErrorTitle, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertEquals(422, respErrorTitle.statusCode(),
                    "Должен вернуться статус 422 так как название больше 100 символов");

            HttpResponse<String> respErrorYear =
                    client.send(reqWithErrorYear, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertEquals(422, respErrorYear.statusCode(),
                    "Должен вернуться статус 422 так как дата создание фильма меньше 1888");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getMovieById_whenIdExists_returnsMovie() {

        Movie movie = new Movie("Test title film", 1991);

        String movieToString = gson.toJson(movie);

        HttpRequest reqPost = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieToString))
                .header("Content-Type", CT_JSON)
                .build();

        HttpRequest reqGet = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        try {
            client.send(reqPost, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            HttpResponse<String> resp =
                    client.send(reqGet, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            Movie createdMovie = gson.fromJson(resp.body(), Movie.class);

            assertEquals(movie, createdMovie, "Должен вернуться фильм только что сохраненный");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getMovieById_whenIdIsNotNumeric_returnsError400() {

        int expected = 400;

        Movie movie = new Movie("Test title film", 1991);

        String movieToString = gson.toJson(movie);

        HttpRequest reqPost = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieToString))
                .header("Content-Type", CT_JSON)
                .build();

        HttpRequest reqGet = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/test"))
                .GET()
                .build();

        try {
            client.send(reqPost, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            HttpResponse<String> resp =
                    client.send(reqGet, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertEquals(expected, resp.statusCode(), "Должен вернуться статус 400 так как id не цифра");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void deleteMovie_whenIdExists_returnsNoContent204() {

        int expected = 204;

        Movie movie = new Movie("Test title film", 1993);

        String movieToString = gson.toJson(movie);

        HttpRequest reqPost = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieToString))
                .header("Content-Type", CT_JSON)
                .build();

        HttpRequest reqGet = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        try {
            client.send(reqPost, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            HttpResponse<String> resp =
                    client.send(reqGet, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertEquals(expected, resp.statusCode(), "Должен вернуться статус 204, удаление прошло успешно");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void deleteMovie_whenIdIsNotNumeric_returnsError400() {
        int expected = 400;

        Movie movie = new Movie("Test title film", 1993);

        String movieToString = gson.toJson(movie);

        HttpRequest reqPost = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieToString))
                .header("Content-Type", CT_JSON)
                .build();

        HttpRequest reqGet = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/test"))
                .DELETE()
                .build();

        try {
            client.send(reqPost, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            HttpResponse<String> resp =
                    client.send(reqGet, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertEquals(expected, resp.statusCode(), "Должен вернуться статус 204, удаление прошло успешно");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getMoviesByYear_whenNoMatches_returnsListEmptyOrFilled() {

        Movie movie = new Movie("Test title film", 1993);

        String movieToString = gson.toJson(movie);

        HttpRequest reqPost = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieToString))
                .header("Content-Type", CT_JSON)
                .build();

        HttpRequest reqFilter1994 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1994"))
                .GET()
                .build();

        HttpRequest reqFilter1993 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1993"))
                .GET()
                .build();

        try {
            client.send(reqPost, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            HttpResponse<String> resp1994 =
                    client.send(reqFilter1994, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            List<Movie> moviesListFiltered1994 = gson.fromJson(resp1994.body(), new ListOfMoviesTypeToken().getType());

            assertEquals(0, moviesListFiltered1994.size(),
                    "Должен пустой массив так как по данному году записей нет");

            HttpResponse<String> resp =
                    client.send(reqFilter1993, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            List<Movie> moviesListFiltered1993 = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

            assertEquals(1, moviesListFiltered1993.size(),
                    "Должен пустой массив так как по данному году записей нет");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getMoviesByYear_whenYearIsNotNumeric_returnsError400() {
        int expected = 400;

        HttpRequest reqGet = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=test"))
                .GET()
                .build();

        try {

            HttpResponse<String> resp =
                    client.send(reqGet, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertEquals(expected, resp.statusCode(),
                    "Должен вернуться статус 400, так как не корректный параметр");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}