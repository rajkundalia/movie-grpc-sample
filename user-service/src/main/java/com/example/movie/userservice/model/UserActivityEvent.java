package com.example.movie.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityEvent {
    private int userId;
    private EventType eventType;
    private String eventData;
    private long timestamp;

    public enum EventType {
        PAGE_VIEW,
        SEARCH,
        CLICK,
        PLAY,
        PAUSE,
        FINISH,
        RATE
    }
}