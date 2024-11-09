package com.example.elsa.service;

import com.example.elsa.model.UserScore;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisService redisService;

    public List<UserScore> getLeaderboard(int quizId, int n) throws ExecutionException, InterruptedException {
        var userScores = redisService.getTopNUsersOfQuizPerNode(quizId, n);
        userScores.sort(Comparator.comparingDouble(UserScore::score).reversed());
        if (userScores.size() < n) {
            return userScores;
        }
        return userScores.subList(0, n);
    }

    public Long getUserRank(int quizId, long userId)
            throws ExecutionException, InterruptedException, BadRequestException {
        Double userScore = redisService.getUserScore(quizId, userId);
        if (userScore == null) {
            throw new BadRequestException("User not exist in the quiz");
        }
        long higherScoreUserCount = redisService.getUserWithScoreHigherThan(quizId, userScore);
        return higherScoreUserCount + 1;
    }
}
