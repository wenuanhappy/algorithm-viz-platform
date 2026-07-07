package com.algorithmviz.controller;

import com.algorithmviz.dto.CompetitionSubmitRequest;
import com.algorithmviz.dto.CompetitionSubmitResponse;
import com.algorithmviz.dto.CreateCompetitionRoomRequest;
import com.algorithmviz.dto.JoinCompetitionRoomRequest;
import com.algorithmviz.model.CompetitionPlayer;
import com.algorithmviz.model.CompetitionRoom;
import com.algorithmviz.service.CompetitionRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/competition")
public class CompetitionController {
    private final CompetitionRoomService competitionRoomService;

    public CompetitionController(CompetitionRoomService competitionRoomService) {
        this.competitionRoomService = competitionRoomService;
    }

    @PostMapping("/rooms")
    public ResponseEntity<?> createRoom(@RequestBody CreateCompetitionRoomRequest request) {
        try {
            CompetitionRoom room = competitionRoomService.createRoom(request);
            return ResponseEntity.ok(competitionRoomService.publicView(room));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/rooms/{roomId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String roomId, @RequestBody JoinCompetitionRoomRequest request) {
        try {
            return ResponseEntity.ok(competitionRoomService.publicView(competitionRoomService.joinRoom(roomId, request)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/rooms/{roomId}/ready")
    public ResponseEntity<?> ready(@PathVariable String roomId, @RequestBody Map<String, Long> request) {
        try {
            return ResponseEntity.ok(competitionRoomService.publicView(
                    competitionRoomService.markReady(roomId, request.get("userId"))));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable String roomId) {
        try {
            return ResponseEntity.ok(competitionRoomService.publicView(competitionRoomService.getRoom(roomId)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String roomId, @RequestBody Map<String, Long> request) {
        try {
            return ResponseEntity.ok(competitionRoomService.publicView(
                    competitionRoomService.leaveRoom(roomId, request.get("userId"))));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/rooms/{roomId}/submit")
    public ResponseEntity<?> submit(@PathVariable String roomId, @RequestBody CompetitionSubmitRequest request) {
        try {
            CompetitionSubmitResponse result = competitionRoomService.submit(roomId, request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/rooms/{roomId}/result")
    public ResponseEntity<?> result(@PathVariable String roomId) {
        try {
            List<CompetitionPlayer> rankings = competitionRoomService.getRankings(roomId);
            return ResponseEntity.ok(Map.of("rankings", rankings));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("message", ex.getMessage()));
        }
    }
}
