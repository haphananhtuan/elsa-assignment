package com.example.elsa.service;

import com.example.elsa.model.UserScore;
import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.api.async.RedisAsyncCommands;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class RedisService {
    //This only mock data, on real implementation, requests shall be sent to Redis Cluster proxy
    private static final Map<Integer, String> REDIS_HOSTS = Map.of(
            0, "redis://localhost:6379",
            1, "redis://localhost:6380",
            2, "redis://localhost:6381");

    private static final Map<Integer, RedisClient> clientMap = new HashMap<>();

    @PostConstruct
    public void postConstruct() {
        for (var hosts : REDIS_HOSTS.entrySet()) {
            clientMap.put(hosts.getKey(), RedisClient.create(hosts.getValue()));
        }
    }

    public List<UserScore> getTopNUsersOfQuizPerNode(int quizId, int n)
            throws ExecutionException, InterruptedException {
        List<CompletableFuture<List<ScoredValue<String>>>> futures = new ArrayList<>();
        for (var client : clientMap.entrySet()) {
            futures.add(getTopNOfQuiz(quizId, n, client.getValue().connect().async()));
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join();

        List<List<ScoredValue<String>>> topUsersLists = new ArrayList<>();
        for (CompletableFuture<List<ScoredValue<String>>> future : futures) {
            topUsersLists.add(future.get());
        }

        List<UserScore> userScores = new ArrayList<>();
        for (var scoredValues : topUsersLists) {
            for (var score : scoredValues) {
                userScores.add(new UserScore(Long.parseLong(score.getValue()), score.getScore()));
            }
        }

        return userScores;
    }

    public long getUserWithScoreHigherThan(int quizId, double score) throws ExecutionException, InterruptedException {
        List<CompletableFuture<Long>> futures = new ArrayList<>();
        for (var client : clientMap.entrySet()) {
            futures.add(getUserCountAboveScore(quizId, score, client.getValue().connect().async()));
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join();

        long count = 0;
        for (var future : futures) {
            count += future.get();
        }

        return count;
    }

    public Double getUserScore(int quizId, long userId) throws ExecutionException, InterruptedException {
        //Get user node
        int nodeId = (int) (userId % clientMap.size());
        CompletableFuture<Double> score = getUserScoreFromRedis(quizId, userId, clientMap.get(nodeId).connect().async());
        return score.get();
    }

    private static CompletableFuture<Long> getUserCountAboveScore(int quizId, double score,
                                                                  RedisAsyncCommands<String, String> asyncCommands) {
        Range<Double> range = Range.from(Range.Boundary.excluding(score), Range.Boundary.unbounded());
        return asyncCommands.zcount("quiz" + quizId, range).toCompletableFuture();
    }

    private CompletableFuture<Double> getUserScoreFromRedis(int quizId, Long userId, RedisAsyncCommands<String, String> asyncCommands) {
        return asyncCommands.zscore("quiz" + quizId, String.valueOf(userId)).toCompletableFuture();
    }

    private CompletableFuture<List<ScoredValue<String>>> getTopNOfQuiz
            (int quizId, int n, RedisAsyncCommands<String, String> asyncCommands) {

        return asyncCommands.zrevrangeWithScores("quiz" + quizId, 0, n - 1).toCompletableFuture();
    }
}
