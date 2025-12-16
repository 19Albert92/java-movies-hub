package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.exception.BodyValidateException;
import ru.practicum.moviehub.exception.ParamIdValidateException;
import ru.practicum.moviehub.model.Endpoints;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.service.MovieService;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.practicum.moviehub.model.Endpoints.*;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore moviesStore;

    private final MovieService movieService;

    public MoviesHandler(MoviesStore moviesStore) {

        this.movieService = new MovieService();

        this.moviesStore = moviesStore;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        Endpoints endpoint = getEndpoints(ex);

        switch (endpoint) {
            case GET_MOVIES: handlerGetMoviesList(ex);
                break;
            case SAVE_MOVIE: handlerSaveMovie(ex);
                break;
            case GET_MOVIE_BY_ID: handlerGetMovieById(ex);
                break;
            case DELETE_MOVIE_BY_ID: handlerDeleteMovieById(ex);
                break;
            case GET_MOVIES_BY_YEAR_PARAM: handlerGetMoviesByYear(ex);
                break;
            default: {
                sendNoContent(ex, 405);
            }
        }
    }

    private void handlerGetMoviesByYear(HttpExchange exchange) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();

            Map<String, Integer> queryParams = Arrays.stream(query.split("&"))
                    .map(q -> q.split("="))
                    .collect(Collectors.toMap(
                            part -> part[0],
                            part -> Integer.parseInt(part[1])
                    ));

            sendJson(exchange, 200, gson.toJson(moviesStore.getMoviesByYear(queryParams.get("year"))));
        } catch (NumberFormatException exception) {
            sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный параметр запроса — 'year'")));
        }
    }

    private void handlerDeleteMovieById(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        try {

            Optional<Integer> paramId = movieService.validateMovieId(path);

            if (!moviesStore.deleteMovie((long) paramId.get())) {
                sendJson(exchange, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
            }

            sendNoContent(exchange, 204);

        } catch (ParamIdValidateException e) {
            sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный ID")));
        }
    }

    private void handlerGetMovieById(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();

        try {

            Optional<Integer> paramId = movieService.validateMovieId(path);

            Optional<Movie> movie = moviesStore.getMovieById((long) paramId.get());

            if (movie.isEmpty()) {
                sendJson(exchange, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
            }

            sendJson(exchange, 200, gson.toJson(movie.get()));

        } catch (ParamIdValidateException e) {
            sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный ID")));
        }
    }

    private void handlerSaveMovie(HttpExchange exchange) throws IOException {

        Headers headers = exchange.getRequestHeaders();

        try {
            if (!headers.containsKey("Content-Type") || !headers.get("Content-Type").contains(CT_JSON)) {
                sendJson(exchange, 415, gson.toJson(new ErrorResponse("неправильное значение заголовка")));
            }

            String requestBody = new String(exchange.getRequestBody().readAllBytes());

            Movie movie = gson.fromJson(requestBody, Movie.class);

            movieService.validateBodyParams(movie);

            sendJson(exchange, 201,
                    gson.toJson(moviesStore.saveMovie(movie.getTitle(), movie.getYear()))
            );
        } catch (BodyValidateException exception) {
            sendJson(exchange, 422, gson.toJson(
                    new ErrorResponse(exception.getMessage(), exception.getDetails()))
            );
        } catch (NullPointerException e) {
            sendJson(exchange, 400, gson.toJson(new ErrorResponse("не все параметры переданы")));
        }
    }

    private Endpoints getEndpoints(HttpExchange ex) {

        String method = ex.getRequestMethod();

        String path = ex.getRequestURI().getPath();

        String[] urlParts = path.split("/");

        String searchParams = ex.getRequestURI().getQuery();

        if (method.equalsIgnoreCase("get") && urlParts.length == 2) {
            return searchParams == null ? GET_MOVIES : GET_MOVIES_BY_YEAR_PARAM;
        } else if (method.equalsIgnoreCase("post") && urlParts.length == 2) {
            return SAVE_MOVIE;
        } else if (method.equalsIgnoreCase("get") && urlParts.length == 3) {
            return GET_MOVIE_BY_ID;
        } else if (method.equalsIgnoreCase("delete") && urlParts.length == 3) {
            return DELETE_MOVIE_BY_ID;
        } else {
            return UNKNOWN;
        }
    }

    private void handlerGetMoviesList(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, gson.toJson(moviesStore.getMovies()));
    }
}
