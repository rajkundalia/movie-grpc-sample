package com.example.movie.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {
    private int userId;
    private String preferenceKey;  // e.g., "genre", "actor", "director"
    private String preferenceValue;
    private float weight;  // Preference weight (0.0-1.0)
}