package com.emergency.controller;

import com.emergency.dto.EventResponse;
import com.emergency.repository.ApplicationEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventsController {

    private final ApplicationEventRepository eventRepository;

    public EventsController(ApplicationEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (size > 200) size = 200;
        Page<EventResponse> result = eventRepository
                .findAllByOrderByTimestampDesc(PageRequest.of(page, size))
                .map(EventResponse::from);
        return ResponseEntity.ok(result);
    }
}
