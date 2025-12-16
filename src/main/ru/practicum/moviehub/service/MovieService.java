package ru.practicum.moviehub.service;

import ru.practicum.moviehub.exception.BodyValidateException;
import ru.practicum.moviehub.exception.ParamIdValidateException;
import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovieService {

    public void validateBodyParams(Movie movie) throws BodyValidateException {

        boolean hasError = false;

        List<String> errorDetails = new ArrayList<>();

        if (movie.getTitle() == null) {
            errorDetails.add("не передано значение title");
            hasError = true;
        }

        if (movie.getYear() == null) {
            errorDetails.add("не передано значение year");
            hasError = true;
        }

        if (movie.hasErrorValidateYear()) {
            errorDetails.add("год должен быть между 1888 и 2026");
            hasError = true;
        }

        if (movie.hasErrorValidateTitleMaxLength()) {
            errorDetails.add("название не должно содержать более " + Movie.MAX_LENGTH_TITLE + " символов");
            hasError = true;
        }

        if (hasError) {
            throw new BodyValidateException("ошибка валидации", errorDetails);
        }
    }

    public Optional<Integer> validateMovieId(String path) throws ParamIdValidateException {

        String[] popParam = path.split("/");

        try {
            Integer id = Integer.parseInt(popParam[popParam.length - 1]);
            return Optional.of(id);
        } catch (NumberFormatException e) {
            throw new ParamIdValidateException();
        }
    }
}
