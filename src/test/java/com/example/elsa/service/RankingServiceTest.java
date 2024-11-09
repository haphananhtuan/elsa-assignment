package com.example.elsa.service;

import com.example.elsa.model.UserScore;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private RedisService redisService;

    @InjectMocks
    private RankingService rankingService;

    @Test
    void getLeaderboard() throws ExecutionException, InterruptedException {
        int quizId = 1;
        int n = 3;

        // Mocking RedisService to return a list of user scores
        List<UserScore> mockUserScores = Arrays.asList(
                new UserScore(1, 85.0),
                new UserScore(2, 90.0),
                new UserScore(3, 75.0),
                new UserScore(4, 95.0)
        );

        when(redisService.getTopNUsersOfQuizPerNode(quizId, n)).thenReturn(mockUserScores);

        // Call the method to test
        List<UserScore> result = rankingService.getLeaderboard(quizId, n);

        // Verify that only top N users are returned and are sorted in descending order
        assertEquals(n, result.size());
        assertEquals(95.0, result.get(0).score());
        assertEquals(90.0, result.get(1).score());
        assertEquals(85.0, result.get(2).score());
    }

    @Test
    void getLeaderboard_ReturnsAllUsers_WhenLessThanNUsersExist() throws ExecutionException, InterruptedException {
        int quizId = 1;
        int n = 5;

        // Mocking RedisService to return fewer users than requested
        List<UserScore> mockUserScores = Arrays.asList(
                new UserScore(1, 80.0),
                new UserScore(2, 85.0)
        );

        when(redisService.getTopNUsersOfQuizPerNode(quizId, n)).thenReturn(mockUserScores);

        // Call the method to test
        List<UserScore> result = rankingService.getLeaderboard(quizId, n);

        // Verify that all users are returned when there are fewer than N users
        assertEquals(2, result.size());
        assertEquals(85.0, result.get(0).score());
        assertEquals(80.0, result.get(1).score());
    }

    @Test
    void getUserRank_ReturnsCorrectRank_WhenUserExists() throws ExecutionException, InterruptedException, BadRequestException {
        int quizId = 1;
        long userId = 1;
        double userScore = 85.0;

        // Mocking RedisService to return user score and count of users with a higher score
        when(redisService.getUserScore(quizId, userId)).thenReturn(userScore);
        when(redisService.getUserWithScoreHigherThan(quizId, userScore)).thenReturn(2L);

        // Call the method to test
        Long rank = rankingService.getUserRank(quizId, userId);

        // Verify that the rank is calculated correctly
        assertEquals(3L, rank);  // 2 users have a higher score, so rank should be 3
    }

    @Test
    void getUserRank_ThrowsBadRequestException_WhenUserDoesNotExist() throws ExecutionException, InterruptedException {
        int quizId = 1;
        long userId = 1;

        // Mocking RedisService to return null for the user score, indicating the user doesn't exist
        when(redisService.getUserScore(quizId, userId)).thenReturn(null);

        // Verify that a BadRequestException is thrown
        Exception exception = assertThrows(BadRequestException.class, () -> {
            rankingService.getUserRank(quizId, userId);
        });

        assertEquals("User not exist in the quiz", exception.getMessage());
    }
}