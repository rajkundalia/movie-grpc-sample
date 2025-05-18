package com.example.movie.movieservice.repository;

import com.example.movie.movieservice.model.Movie;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class MovieRepository {
    private final Map<Integer, Movie> movies = new ConcurrentHashMap<>();
    private final Map<Integer, Map<Integer, Float>> userRatings = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Initialize with sample data
        addMovie(Movie.builder().id(1).title("The Shawshank Redemption").description("Two imprisoned men bond over a number of years")
                .rating(9.3f).genre("Drama").year(1994).director("Frank Darabont").build());
        addMovie(Movie.builder().id(2).title("The Godfather").description("The aging patriarch of an organized crime dynasty transfers control")
                .rating(9.2f).genre("Crime").year(1972).director("Francis Ford Coppola").build());
        addMovie(Movie.builder().id(3).title("The Dark Knight").description("The menace known as the Joker wreaks havoc on Gotham City")
                .rating(9.0f).genre("Action").year(2008).director("Christopher Nolan").build());
        addMovie(Movie.builder().id(4).title("Inception").description("A thief who steals corporate secrets through dream-sharing technology")
                .rating(8.8f).genre("Sci-Fi").year(2010).director("Christopher Nolan").build());
        addMovie(Movie.builder().id(5).title("Pulp Fiction").description("The lives of two mob hitmen, a boxer, a gangster and his wife")
                .rating(8.9f).genre("Crime").year(1994).director("Quentin Tarantino").build());
        addMovie(Movie.builder().id(6).title("The Matrix").description("A computer hacker learns about the true nature of reality")
                .rating(8.7f).genre("Sci-Fi").year(1999).director("Lana Wachowski").build());
        addMovie(Movie.builder().id(7).title("Goodfellas").description("The story of Henry Hill and his life in the mob")
                .rating(8.7f).genre("Crime").year(1990).director("Martin Scorsese").build());
        addMovie(Movie.builder().id(8).title("Fight Club").description("An insomniac office worker and a devil-may-care soapmaker form an underground fight club")
                .rating(8.8f).genre("Drama").year(1999).director("David Fincher").build());
        addMovie(Movie.builder().id(9).title("Forrest Gump").description("The presidencies of Kennedy and Johnson, the Vietnam War, and Watergate through the eyes of Forrest Gump")
                .rating(8.8f).genre("Drama").year(1994).director("Robert Zemeckis").build());
        addMovie(Movie.builder().id(10).title("Interstellar").description("A team of explorers travel through a wormhole in space")
                .rating(8.6f).genre("Sci-Fi").year(2014).director("Christopher Nolan").build());
    }

    public Movie getMovieById(int id) {
        return movies.get(id);
    }

    public List<Movie> getTrendingMovies(int limit, String genre) {
        Stream<Movie> movieStream = movies.values().stream()
                .sorted(Comparator.comparing(Movie::getRating).reversed());
        
        if (genre != null && !genre.isEmpty()) {
            movieStream = movieStream.filter(movie -> movie.getGenre().equalsIgnoreCase(genre));
        }
        
        return movieStream.limit(limit).collect(Collectors.toList());
    }

    public boolean updateRating(int movieId, int userId, float rating) {
        Movie movie = movies.get(movieId);
        if (movie != null) {
            // Update user-specific rating
            userRatings.computeIfAbsent(movieId, k -> new HashMap<>())
                    .put(userId, rating);
            
            // Recalculate overall rating (average of all user ratings)
            Map<Integer, Float> ratings = userRatings.get(movieId);
            if (ratings != null && !ratings.isEmpty()) {
                float averageRating = (float) ratings.values().stream()
                        .mapToDouble(Float::doubleValue)
                        .average()
                        .orElse(movie.getRating());
                movie.setRating(averageRating);
            }
            return true;
        }
        return false;
    }

    public void addMovie(Movie movie) {
        movies.put(movie.getId(), movie);
    }
    
    public List<Movie> getRecommendedMoviesForUser(int userId, String preferredGenre) {
        // Simple recommendation logic based on genre and rating
        return movies.values().stream()
                .filter(movie -> preferredGenre == null || movie.getGenre().equalsIgnoreCase(preferredGenre))
                .sorted(Comparator.comparing(Movie::getRating).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }
}