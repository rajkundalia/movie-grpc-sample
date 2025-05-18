package com.example.movie.movieservice.service;

import com.example.movie.movieservice.proto.*;
import com.example.movie.movieservice.model.Movie;
import com.example.movie.movieservice.repository.MovieRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
@GrpcService
public class MovieServiceImpl extends MovieServiceGrpc.MovieServiceImplBase {
    
    private final MovieRepository movieRepository;
    
    // Store user preferences for recommendation
    private final Map<Integer, String> userPreferredGenres = new ConcurrentHashMap<>();

    // Unary RPC: Fetches movie details by ID
    @Override
    public void getMovie(MovieRequest request, StreamObserver<MovieResponse> responseObserver) {
        int movieId = request.getMovieId();
        log.info("Received request for movie with ID: {}", movieId);
        
        Movie movie = movieRepository.getMovieById(movieId);
        if (movie != null) {
            MovieResponse response = buildMovieResponse(movie);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            log.warn("Movie with ID {} not found", movieId);
            responseObserver.onError(new RuntimeException("Movie not found"));
        }
    }

    // Server Streaming RPC: Streams a list of trending movies
    @Override
    public void getTrendingMovies(TrendingMoviesRequest request, StreamObserver<MovieResponse> responseObserver) {
        int limit = request.getLimit() > 0 ? request.getLimit() : 10; // Default to 10
        String genre = request.getGenre().isEmpty() ? null : request.getGenre();
        
        log.info("Streaming trending movies. Limit: {}, Genre: {}", limit, genre);
        
        List<Movie> trendingMovies = movieRepository.getTrendingMovies(limit, genre);
        
        for (Movie movie : trendingMovies) {
            responseObserver.onNext(buildMovieResponse(movie));
            
            // Simulate some delay for streaming
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        responseObserver.onCompleted();
    }

    // Client Streaming RPC: Updates multiple movie ratings in batch
    @Override
    public StreamObserver<UpdateRatingRequest> updateMovieRatings(StreamObserver<UpdateRatingBatchResponse> responseObserver) {
        final AtomicInteger updatedCount = new AtomicInteger(0);
        
        return new StreamObserver<>() {
            @Override
            public void onNext(UpdateRatingRequest request) {
                log.info("Updating rating for movie ID: {} by user ID: {} with rating: {}", 
                        request.getMovieId(), request.getUserId(), request.getRating());
                
                boolean updated = movieRepository.updateRating(
                        request.getMovieId(),
                        request.getUserId(),
                        request.getRating()
                );
                
                if (updated) {
                    updatedCount.incrementAndGet();
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error while updating movie ratings", t);
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                log.info("Completed batch update of ratings. Updated {} movies", updatedCount.get());
                
                UpdateRatingBatchResponse response = UpdateRatingBatchResponse.newBuilder()
                        .setUpdatedCount(updatedCount.get())
                        .setSuccess(updatedCount.get() > 0)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }

    // Bidirectional Streaming RPC: Provides personalized movie recommendations based on user interactions
    @Override
    public StreamObserver<UserEventRequest> getPersonalizedRecommendations(StreamObserver<MovieRecommendation> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(UserEventRequest request) {
                int userId = request.getUserId();
                int movieId = request.getMovieId();
                UserEventRequest.EventType eventType = request.getEventType();
                
                log.info("Received event from user ID: {}, movie ID: {}, event type: {}", 
                        userId, movieId, eventType);
                
                // Update user preferences based on interaction
                Movie movie = movieRepository.getMovieById(movieId);
                if (movie != null) {
                    // For simplicity, just use the genre of the movie the user interacted with
                    userPreferredGenres.put(userId, movie.getGenre());
                    
                    // Get recommendations based on updated preferences
                    List<Movie> recommendations = movieRepository.getRecommendedMoviesForUser(
                            userId, userPreferredGenres.get(userId));
                    
                    for (Movie recommendedMovie : recommendations) {
                        float confidenceScore = calculateConfidenceScore(recommendedMovie, userId, eventType);
                        String reason = generateRecommendationReason(recommendedMovie, userId, eventType);
                        
                        MovieRecommendation recommendation = MovieRecommendation.newBuilder()
                                .setMovieId(recommendedMovie.getId())
                                .setTitle(recommendedMovie.getTitle())
                                .setConfidenceScore(confidenceScore)
                                .setRecommendationReason(reason)
                                .build();
                        
                        responseObserver.onNext(recommendation);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in personalized recommendations stream", t);
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                log.info("Completed personalized recommendations stream");
                responseObserver.onCompleted();
            }
        };
    }
    
    private MovieResponse buildMovieResponse(Movie movie) {
        return MovieResponse.newBuilder()
                .setMovieId(movie.getId())
                .setTitle(movie.getTitle())
                .setDescription(movie.getDescription())
                .setRating(movie.getRating())
                .setGenre(movie.getGenre())
                .setYear(movie.getYear())
                .setDirector(movie.getDirector())
                .build();
    }
    
    private float calculateConfidenceScore(Movie movie, int userId, UserEventRequest.EventType eventType) {
        // A simple algorithm to calculate confidence score
        float baseScore = movie.getRating() / 10.0f; // 0.0 - 1.0 based on rating
        
        // Adjust based on event type
        float eventTypeMultiplier = switch (eventType) {
            case RATE -> 1.2f;    // Rating is a strong signal
            case WATCH -> 1.1f;   // Watching is a moderate signal
            case BOOKMARK -> 1.0f; // Bookmarking is a neutral signal
            default -> 0.8f;      // Viewing is a weak signal
        };
        
        return Math.min(baseScore * eventTypeMultiplier, 1.0f);
    }
    
    private String generateRecommendationReason(Movie movie, int userId, UserEventRequest.EventType eventType) {
        String genre = movie.getGenre();
        String director = movie.getDirector();
        
        return switch (eventType) {
            case RATE -> String.format("Recommended because you rated movies in the %s genre", genre);
            case WATCH -> String.format("Recommended because you watched movies directed by %s", director);
            case BOOKMARK -> String.format("Recommended because you bookmarked similar %s movies", genre);
            default -> String.format("Recommended based on your interest in %s movies", genre);
        };
    }
}