package com.example.movie.movieservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    private int id;
    private String title;
    private String description;
    private float rating;
    private String genre;
    private int year;
    private String director;
}