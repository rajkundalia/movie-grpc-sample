package com.example.movie.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivity {
    private int id;
    private int userId;
    private int movieId;
    private String movieTitle;
    private ActivityType activityType;
    private long timestamp;
    
    public enum ActivityType {
        VIEW,
        RATE,
        BOOKMARK,
        WATCH,
        SHARE
    }
}