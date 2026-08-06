package com.wellness.Controller;

import com.wellness.Dto.QuestionAnswerRequest;
import com.wellness.Dto.QuestionAnswerResponse;
import com.wellness.Service.WellnessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/question-answers")
@RequiredArgsConstructor
public class WellnessController {

    private final WellnessService wellnessService;

    @PostMapping
    public ResponseEntity<QuestionAnswerResponse> saveAnswer(
            @Valid @RequestBody QuestionAnswerRequest request) {
        return ResponseEntity.ok(wellnessService.saveOrUpdate(request));
    }
}
