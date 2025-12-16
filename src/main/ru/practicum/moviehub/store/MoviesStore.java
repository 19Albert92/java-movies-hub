package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class MoviesStore {

    private static final AtomicLong counter = new AtomicLong(0);

    private final HashMap<Long, Movie> movies = new LinkedHashMap<>();

    public List<Movie> getMovies() {
        return movies.values().stream().toList();
    }

    public Movie saveMovie(String title, int year) {

        long newId = counter.incrementAndGet();

        Movie movie = new Movie(title, year, newId);

        movies.put(newId, movie);

        return movie;
    }

    public Optional<Movie> getMovieById(Long id) {
        return Optional.ofNullable(movies.get(id));
    }

    public boolean deleteMovie(Long id) {
        boolean isExistMovie = getMovieById(id).isPresent();
        if (isExistMovie) {
            movies.remove(id);
        }

        return isExistMovie;
    }

    public List<Movie> getMoviesByYear(int year) {
        return movies.values().stream().filter(movie -> movie.getYear() == year).toList();
    }

    public void clearMovies() {
        movies.clear();
        counter.set(0L);
    }
}