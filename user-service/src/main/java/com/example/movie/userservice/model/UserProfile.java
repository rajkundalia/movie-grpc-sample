package com.example.movie.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private int id;
    private String username;
    private String email;
    
    @Builder.Default
    private List<String> favoriteGenres = new ArrayList<>();
    
    private int accountAgeDays;
    private int activityLevel; // 1-10 scale
}