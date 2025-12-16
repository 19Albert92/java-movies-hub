package ru.practicum.moviehub.model;

import java.time.LocalDate;
import java.util.Objects;

public class Movie {

    public static final int MAX_LENGTH_TITLE = 100;

    private final long id;

    private final String title;

    private final Integer year;

    public Movie(String title, int year, long id) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public Movie(String title, int year) {
        this(title, year, 0);
    }


    public boolean hasErrorValidateYear() {
        int nowYear = LocalDate.now().getYear();

        return (year < 1888 || year > nowYear + 1);
    }

    public boolean hasErrorValidateTitleMaxLength() {
        return title.length() > MAX_LENGTH_TITLE;
    }

    public String getTitle() {
        return title;
    }

    public Integer getYear() {
        return year;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", year=" + year +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return Objects.equals(title, movie.title) && Objects.equals(year, movie.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, year);
    }
}