package com.example.elsa.controller;

import com.example.elsa.model.UserScore;
import com.example.elsa.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/quizs")
@RequiredArgsConstructor
public class QuizController {

    private final RankingService rankingService;

    @GetMapping("/{quizId}")
    public ResponseEntity<List<UserScore>> getLeaderBoard(@PathVariable int quizId,
                                                          @RequestParam(defaultValue = "10") int size)
            throws ExecutionException, InterruptedException {
        return ResponseEntity.ok(rankingService.getLeaderboard(quizId, size));
    }

    @GetMapping("/{quizId}/users/{userId}")
    public ResponseEntity<Long> getQuizUserRanking(@PathVariable int quizId,
                                                   @PathVariable long userId)
            throws ExecutionException, InterruptedException, BadRequestException {
        return ResponseEntity.ok(rankingService.getUserRank(quizId, userId));
    }
}
