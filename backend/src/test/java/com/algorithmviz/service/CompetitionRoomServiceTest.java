package com.algorithmviz.service;

import com.algorithmviz.dto.CompetitionSubmitRequest;
import com.algorithmviz.dto.CreateCompetitionRoomRequest;
import com.algorithmviz.dto.JoinCompetitionRoomRequest;
import com.algorithmviz.model.CompetitionPlayer;
import com.algorithmviz.model.CompetitionRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CompetitionRoomServiceTest {
    private CompetitionRoomService service;

    @BeforeEach
    void setUp() {
        service = new CompetitionRoomService();
    }

    @Test
    void questionsDoNotExposeAnswersAndDuplicateSubmitDoesNotScoreTwice() {
        CompetitionRoom room = createRoom("quick-sort", 1L, "Player A");
        join(room.getRoomId(), 2L, "Player B");
        service.markReady(room.getRoomId(), 1L);
        service.markReady(room.getRoomId(), 2L);

        assertTrue(room.getQuestions().stream().allMatch(question -> question.getInput().isEmpty()));

        CompetitionSubmitRequest submit = submitRequest(1L, "quick-sort-q1", "O(n log n)");
        var first = service.submit(room.getRoomId(), submit);
        var duplicate = service.submit(room.getRoomId(), submit);

        assertTrue(first.isCorrect());
        assertEquals(120, first.getScore());
        assertTrue(duplicate.isDuplicate());
        assertEquals(0, duplicate.getAwardedPoints());
        assertEquals(120, duplicate.getScore());
    }

    @Test
    void leavingWaitingRoomReleasesRoom() {
        CompetitionRoom room = createRoom("bfs", 1L, "Player A");

        service.leaveRoom(room.getRoomId(), 1L);

        assertThrows(IllegalArgumentException.class, () -> service.getRoom(room.getRoomId()));
    }

    @Test
    void forfeitedPlayerRanksLast() {
        CompetitionRoom room = createRoom("bfs", 1L, "Winner");
        join(room.getRoomId(), 2L, "Quitter");
        service.markReady(room.getRoomId(), 1L);
        service.markReady(room.getRoomId(), 2L);

        service.leaveRoom(room.getRoomId(), 2L);
        List<CompetitionPlayer> rankings = service.getRankings(room.getRoomId());

        assertEquals("Winner", rankings.get(0).getDisplayName());
        assertEquals("Quitter", rankings.get(1).getDisplayName());
        assertTrue(rankings.get(1).isForfeited());
        assertEquals("finished", room.getStatus());
    }

    @Test
    void submissionBeforeBothPlayersAreReadyIsRejected() {
        CompetitionRoom room = createRoom("quick-sort", 1L, "Player A");
        join(room.getRoomId(), 2L, "Player B");

        assertThrows(
                IllegalStateException.class,
                () -> service.submit(
                        room.getRoomId(),
                        submitRequest(1L, "quick-sort-q1", "O(n log n)")
                )
        );
    }

    @Test
    void waitingRoomPublicViewHidesQuestions() {
        CompetitionRoom room = createRoom("quick-sort", 1L, "Player A");

        CompetitionRoom view = service.publicView(room);

        assertEquals(3, view.getQuestionCount());
        assertTrue(view.getQuestions().isEmpty());
        assertFalse(room.getQuestions().isEmpty());
    }

    @Test
    void questionsMustBeSubmittedInOrder() {
        CompetitionRoom room = createRoom("quick-sort", 1L, "Player A");
        join(room.getRoomId(), 2L, "Player B");
        service.markReady(room.getRoomId(), 1L);
        service.markReady(room.getRoomId(), 2L);

        assertThrows(
                IllegalStateException.class,
                () -> service.submit(
                        room.getRoomId(),
                        submitRequest(1L, "quick-sort-q2", "更小")
                )
        );
    }

    @Test
    void serverCalculatesSpeedBonusAndElapsedTime() {
        CompetitionRoom room = createRoom("quick-sort", 1L, "Player A");
        join(room.getRoomId(), 2L, "Player B");
        service.markReady(room.getRoomId(), 1L);
        service.markReady(room.getRoomId(), 2L);

        var result = service.submit(
                room.getRoomId(),
                submitRequest(1L, "quick-sort-q1", "O(n log n)")
        );

        assertEquals(120, result.getAwardedPoints());
        assertTrue(room.getPlayers().get(0).getTotalTimeMs() < 5_000);
    }

    @Test
    void expiredWaitingRoomsAreRemoved() {
        CompetitionRoom room = createRoom("bfs", 1L, "Player A");
        room.setLastActivityAt(Instant.now().minusSeconds(31 * 60));

        service.removeExpiredRooms();

        assertThrows(IllegalArgumentException.class, () -> service.getRoom(room.getRoomId()));
    }

    @Test
    void unsupportedCompetitionAlgorithmIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> createRoom("data-structure-3d", 1L, "Player A")
        );
    }

    private CompetitionRoom createRoom(String algorithm, Long userId, String displayName) {
        CreateCompetitionRoomRequest request = new CreateCompetitionRoomRequest();
        request.setAlgorithm(algorithm);
        request.setUserId(userId);
        request.setDisplayName(displayName);
        return service.createRoom(request);
    }

    private void join(String roomId, Long userId, String displayName) {
        JoinCompetitionRoomRequest request = new JoinCompetitionRoomRequest();
        request.setUserId(userId);
        request.setDisplayName(displayName);
        service.joinRoom(roomId, request);
    }

    private CompetitionSubmitRequest submitRequest(Long userId, String questionId, String answer) {
        CompetitionSubmitRequest request = new CompetitionSubmitRequest();
        request.setUserId(userId);
        request.setQuestionId(questionId);
        request.setAnswer(answer);
        return request;
    }
}
