package com.example.movie.userservice.repository;

import com.example.movie.userservice.model.UserActivity;
import com.example.movie.userservice.model.UserPreference;
import com.example.movie.userservice.model.UserProfile;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Repository
public class UserRepository {
    private final Map<Integer, UserProfile> users = new ConcurrentHashMap<>();
    private final Map<Integer, List<UserActivity>> userActivities = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, UserPreference>> userPreferences = new ConcurrentHashMap<>();
    private final AtomicInteger activityIdCounter = new AtomicInteger(1);
    
    @PostConstruct
    public void init() {
        // Initialize with sample users
        addUser(UserProfile.builder()
                .id(1)
                .username("john_doe")
                .email("john@example.com")
                .favoriteGenres(List.of("Action", "Sci-Fi"))
                .accountAgeDays(365)
                .activityLevel(8)
                .build());
        
        addUser(UserProfile.builder()
                .id(2)
                .username("jane_smith")
                .email("jane@example.com")
                .favoriteGenres(List.of("Drama", "Romance"))
                .accountAgeDays(180)
                .activityLevel(5)
                .build());
        
        addUser(UserProfile.builder()
                .id(3)
                .username("bob_johnson")
                .email("bob@example.com")
                .favoriteGenres(List.of("Comedy", "Action"))
                .accountAgeDays(90)
                .activityLevel(7)
                .build());
        
        // Add some sample activities
        addActivity(UserActivity.builder()
                .userId(1)
                .movieId(1)
                .movieTitle("The Shawshank Redemption")
                .activityType(UserActivity.ActivityType.WATCH)
                .timestamp(System.currentTimeMillis() - 86400000) // 1 day ago
                .build());
        
        addActivity(UserActivity.builder()
                .userId(1)
                .movieId(3)
                .movieTitle("The Dark Knight")
                .activityType(UserActivity.ActivityType.RATE)
                .timestamp(System.currentTimeMillis() - 43200000) // 12 hours ago
                .build());
        
        addActivity(UserActivity.builder()
                .userId(2)
                .movieId(5)
                .movieTitle("Pulp Fiction")
                .activityType(UserActivity.ActivityType.BOOKMARK)
                .timestamp(System.currentTimeMillis() - 172800000) // 2 days ago
                .build());
        
        // Add some sample preferences
        addPreference(UserPreference.builder()
                .userId(1)
                .preferenceKey("genre")
                .preferenceValue("Action")
                .weight(0.8f)
                .build());
        
        addPreference(UserPreference.builder()
                .userId(1)
                .preferenceKey("director")
                .preferenceValue("Christopher Nolan")
                .weight(0.9f)
                .build());
        
        addPreference(UserPreference.builder()
                .userId(2)
                .preferenceKey("genre")
                .preferenceValue("Drama")
                .weight(0.7f)
                .build());
    }
    
    public UserProfile getUserById(int id) {
        return users.get(id);
    }
    
    public void addUser(UserProfile user) {
        users.put(user.getId(), user);
    }
    
    public List<UserActivity> getUserActivities(int userId, int limit, long sinceTimestamp) {
        List<UserActivity> activities = userActivities.getOrDefault(userId, Collections.emptyList());
        
        return activities.stream()
                .filter(activity -> activity.getTimestamp() >= sinceTimestamp)
                .sorted(Comparator.comparing(UserActivity::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public UserActivity addActivity(UserActivity activity) {
        activity.setId(activityIdCounter.getAndIncrement());
        
        userActivities.computeIfAbsent(activity.getUserId(), k -> new ArrayList<>())
                .add(activity);
        
        return activity;
    }
    
    public UserPreference addPreference(UserPreference preference) {
        userPreferences.computeIfAbsent(preference.getUserId(), k -> new HashMap<>())
                .put(preference.getPreferenceKey() + ":" + preference.getPreferenceValue(), preference);
        
        return preference;
    }
    
    public int updatePreferences(int userId, List<UserPreference> preferences) {
        int count = 0;
        for (UserPreference preference : preferences) {
            addPreference(preference);
            count++;
        }
        return count;
    }
    
    public List<UserPreference> getUserPreferences(int userId) {
        Map<String, UserPreference> userPrefs = userPreferences.getOrDefault(userId, Collections.emptyMap());
        return new ArrayList<>(userPrefs.values());
    }
}