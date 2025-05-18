package com.example.movie.userservice.service;

import com.example.movie.userservice.model.*;
import com.example.movie.userservice.proto.*;
import com.example.movie.userservice.proto.UserActivityEvent;
import com.example.movie.userservice.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@GrpcService
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;
    private final ConcurrentHashMap<Integer, List<StreamObserver<UserInsightResponse>>> activeUserStreams = new ConcurrentHashMap<>();

    @Autowired
    public UserGrpcService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Unary RPC: Fetches user profile details
    @Override
    public void getUserProfile(UserRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        int userId = request.getUserId();
        UserProfile userProfile = userRepository.getUserById(userId);

        if (userProfile != null) {
            UserProfileResponse response = UserProfileResponse.newBuilder()
                    .setUserId(userProfile.getId())
                    .setUsername(userProfile.getUsername())
                    .setEmail(userProfile.getEmail())
                    .addAllFavoriteGenres(userProfile.getFavoriteGenres())
                    .setAccountAgeDays(userProfile.getAccountAgeDays())
                    .setActivityLevel(userProfile.getActivityLevel())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            // Handle case when user not found
            responseObserver.onError(new RuntimeException("User with ID " + userId + " not found"));
        }
    }

    // Server Streaming RPC: Fetches user activity history
    @Override
    public void getUserActivityHistory(UserHistoryRequest request, StreamObserver<UserActivityResponse> responseObserver) {
        int userId = request.getUserId();
        int limit = request.getLimit();
        long sinceTimestamp = request.getSinceTimestamp();

        List<UserActivity> activities = userRepository.getUserActivities(userId, limit, sinceTimestamp);

        for (UserActivity activity : activities) {
            UserActivityResponse.ActivityType activityType;
            switch (activity.getActivityType()) {
                case VIEW:
                    activityType = UserActivityResponse.ActivityType.VIEW;
                    break;
                case RATE:
                    activityType = UserActivityResponse.ActivityType.RATE;
                    break;
                case BOOKMARK:
                    activityType = UserActivityResponse.ActivityType.BOOKMARK;
                    break;
                case WATCH:
                    activityType = UserActivityResponse.ActivityType.WATCH;
                    break;
                case SHARE:
                    activityType = UserActivityResponse.ActivityType.SHARE;
                    break;
                default:
                    activityType = UserActivityResponse.ActivityType.VIEW;
            }

            UserActivityResponse response = UserActivityResponse.newBuilder()
                    .setActivityId(activity.getId())
                    .setUserId(activity.getUserId())
                    .setMovieId(activity.getMovieId())
                    .setMovieTitle(activity.getMovieTitle())
                    .setActivityType(activityType)
                    .setTimestamp(activity.getTimestamp())
                    .build();

            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }

    // Client Streaming RPC: Allows updating user preferences in batches
    @Override
    public StreamObserver<UserPreferenceRequest> updateUserPreferences(StreamObserver<UpdatePreferencesResponse> responseObserver) {
        return new StreamObserver<>() {
            private final List<UserPreference> preferences = new ArrayList<>();
            private int userId = -1;
            private final List<String> updatedPreferences = new ArrayList<>();

            @Override
            public void onNext(UserPreferenceRequest request) {
                if (userId == -1) {
                    userId = request.getUserId();
                }

                UserPreference preference = UserPreference.builder()
                        .userId(request.getUserId())
                        .preferenceKey(request.getPreferenceKey())
                        .preferenceValue(request.getPreferenceValue())
                        .weight(request.getWeight())
                        .build();

                preferences.add(preference);
                updatedPreferences.add(request.getPreferenceKey() + ":" + request.getPreferenceValue());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error updating user preferences: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                int updatedCount = userRepository.updatePreferences(userId, preferences);

                UpdatePreferencesResponse response = UpdatePreferencesResponse.newBuilder()
                        .setUpdatedCount(updatedCount)
                        .setSuccess(updatedCount > 0)
                        .addAllUpdatedPreferences(updatedPreferences)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<UserActivityEvent> trackUserActivity(StreamObserver<UserInsightResponse> responseObserver) {
        return new StreamObserver<UserActivityEvent>() {
            private int userId = -1;

            @Override
            public void onNext(UserActivityEvent event) {
                // Extract user ID for tracking
                if (userId == -1) {
                    userId = event.getUserId();
                    // Register this observer to receive insights for this user
                    activeUserStreams.computeIfAbsent(userId, k -> new ArrayList<>()).add(responseObserver);
                }

                // Convert proto event to model event
                com.example.movie.userservice.model.UserActivityEvent.EventType eventType;
                switch (event.getEventType()) {
                    case PAGE_VIEW:
                        eventType = com.example.movie.userservice.model.UserActivityEvent.EventType.PAGE_VIEW;
                        break;
                    case SEARCH:
                        eventType = com.example.movie.userservice.model.UserActivityEvent.EventType.SEARCH;
                        break;
                    case CLICK:
                        eventType = com.example.movie.userservice.model.UserActivityEvent.EventType.CLICK;
                        break;
                    case PLAY:
                        eventType = com.example.movie.userservice.model.UserActivityEvent.EventType.PLAY;
                        break;
                    case PAUSE:
                        eventType = com.example.movie.userservice.model.UserActivityEvent.EventType.PAUSE;
                        break;
                    case FINISH:
                        eventType = com.example.movie.userservice.model.UserActivityEvent.EventType.FINISH;
                        break;
                    case RATE:
                        eventType = com.example.movie.userservice.model.UserActivityEvent.EventType.RATE;
                        break;
                    default:
                        eventType = com.example.movie.userservice.model.UserActivityEvent.EventType.PAGE_VIEW;
                }

                com.example.movie.userservice.model.UserActivityEvent modelEvent = com.example.movie.userservice.model.UserActivityEvent.builder()
                        .userId(event.getUserId())
                        .eventType(eventType)
                        .eventData(event.getEventData())
                        .timestamp(event.getTimestamp())
                        .build();

                // Process the event and generate insights (in a real system, this might involve ML models)
                generateInsights(modelEvent, insightResponse -> {
                    responseObserver.onNext(insightResponse);
                });
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error tracking user activity: " + t.getMessage());
                cleanup();
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
                cleanup();
            }

            private void cleanup() {
                if (userId != -1) {
                    List<StreamObserver<UserInsightResponse>> userObservers = activeUserStreams.get(userId);
                    if (userObservers != null) {
                        userObservers.remove(responseObserver);
                        if (userObservers.isEmpty()) {
                            activeUserStreams.remove(userId);
                        }
                    }
                }
            }
        };
    }

    // In a real implementation, this would use ML models or analytics to generate insights
    private void generateInsights(com.example.movie.userservice.model.UserActivityEvent event, Consumer<UserInsightResponse> insightConsumer) {
        // For demo purposes, generate a simple insight based on the event
        String insightType;
        String insightData;
        float confidenceScore;

        switch (event.getEventType()) {
            case PLAY:
                insightType = "engagement";
                insightData = "{\"message\": \"User started watching content\", \"content\": " + event.getEventData() + "}";
                confidenceScore = 0.8f;
                break;
            case PAUSE:
                insightType = "engagement";
                insightData = "{\"message\": \"User paused content\", \"content\": " + event.getEventData() + "}";
                confidenceScore = 0.7f;
                break;
            case FINISH:
                insightType = "engagement";
                insightData = "{\"message\": \"User completed content\", \"content\": " + event.getEventData() + "}";
                confidenceScore = 0.9f;
                break;
            case RATE:
                insightType = "preference_shift";
                insightData = "{\"message\": \"User rated content\", \"content\": " + event.getEventData() + "}";
                confidenceScore = 0.85f;
                break;
            case SEARCH:
                insightType = "interest";
                insightData = "{\"message\": \"User searched for content\", \"query\": " + event.getEventData() + "}";
                confidenceScore = 0.75f;
                break;
            default:
                insightType = "activity";
                insightData = "{\"message\": \"User activity detected\", \"activity\": \"" + event.getEventType() + "\"}";
                confidenceScore = 0.6f;
                break;
        }

        // Create the insight response directly using proto classes
        UserInsightResponse response = UserInsightResponse.newBuilder()
                .setUserId(event.getUserId())
                .setInsightType(insightType)
                .setInsightData(insightData)
                .setConfidenceScore(confidenceScore)
                .build();

        insightConsumer.accept(response);
    }
}