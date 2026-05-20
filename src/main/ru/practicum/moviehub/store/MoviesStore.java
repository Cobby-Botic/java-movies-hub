package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoviesStore {
    private final Map<Integer, Movie> moviesStore = new HashMap<>();

    public Map<Integer, Movie> getMovies() {
        return moviesStore;
    }

    public Optional<Movie> getMovieById(int idMovie) {
        return Optional.ofNullable(moviesStore.get(idMovie));
    }


    public int addMovie(Movie newMovie) {
        moviesStore.put(newMovie.getId(), newMovie);
        return newMovie.getId();
    }

    public boolean deleteMovie(int idMovie) {
        if (moviesStore.containsKey(idMovie)) {
            moviesStore.remove(idMovie);
            return true;
        } else {
            return false;
        }
    }

    public void deleteAll() {
        moviesStore.clear();
    }

    public Map<Integer, Movie> getMoviesStore() {
        return moviesStore;
    }

    public List<Movie> getMoviesByYear(int year) {
        List<Movie> result = moviesStore.values().stream()
                .filter(movie -> movie.getYearOfCreation() == year)
                .toList();
        return result;
    }
}