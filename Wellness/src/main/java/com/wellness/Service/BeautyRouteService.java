package com.wellness.Service;

import com.wellness.Dto.BeautyRouteRequest;
import com.wellness.Dto.BeautyRouteResponse;
import com.wellness.Dto.BeautyRouteStepResponse;
import com.wellness.Entity.User;
import com.wellness.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BeautyRouteService {

    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public BeautyRouteResponse recommend(Long userId, BeautyRouteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        List<BeautyRouteStepResponse> candidates = createCandidates(request);
        int totalMinutes = candidates.stream()
                .mapToInt(BeautyRouteStepResponse::estimatedMinutes)
                .sum();
        int totalPrice = candidates.stream()
                .mapToInt(BeautyRouteStepResponse::estimatedPrice)
                .sum();

        return new BeautyRouteResponse(
                request.preferredDate() != null
                        ? request.preferredDate()
                        : LocalDate.now(clock),
                user.getSubscriptionPlan().name(),
                totalMinutes,
                totalPrice,
                candidates,
                false,
                null,
                "COMING_SOON",
                "파트너 업체 예약과 결제 기능은 추후 제공될 예정입니다."
        );
    }

    private List<BeautyRouteStepResponse> createCandidates(BeautyRouteRequest request) {
        List<BeautyRouteStepResponse> steps = new ArrayList<>();
        int stepNumber = 1;

        if (containsAny(request.concerns(), "ACNE", "TROUBLE", "SENSITIVE", "여드름", "민감함", "트러블")) {
            steps.add(step(
                    stepNumber++, "SKIN_DIAGNOSIS", "피부 진단과 진정 관리",
                    "현재 피부 고민을 확인하고 자극이 적은 진정 관리를 진행해요.",
                    50, 70000, request.concerns()));
        } else {
            steps.add(step(
                    stepNumber++, "SKINCARE", "맞춤 보습 케어",
                    "피부 타입과 선호에 맞춰 기본 보습 루틴을 구성해요.",
                    40, 55000, request.concerns()));
        }

        if (containsAny(request.concerns(), "ELASTICITY", "탄력")) {
            steps.add(step(
                    stepNumber++, "ELASTICITY_CARE", "탄력 집중 케어",
                    "건조함을 줄이고 탄력 관리에 집중하는 코스예요.",
                    60, 90000, List.of("피부 탄력 고민 반영")));
        } else {
            steps.add(step(
                    stepNumber++, "RELAXATION", "편안한 릴랙싱 케어",
                    "부담 없이 컨디션을 회복할 수 있는 관리예요.",
                    50, 65000, request.preferences()));
        }

        steps.add(step(
                stepNumber++, "HOME_ROUTINE", "홈케어 루틴 안내",
                "관리 후 집에서 이어갈 수 있는 짧은 루틴을 정리해드려요.",
                20, 20000, request.preferences()));

        steps.add(step(
                stepNumber, "FOLLOW_UP", "피부 변화 점검",
                "관리 이후 피부 반응을 기록하고 다음 루트를 조정해요.",
                20, 15000, List.of("구독형 상세 관리")));

        return List.copyOf(steps);
    }

    private BeautyRouteStepResponse step(
            int step,
            String category,
            String title,
            String description,
            int minutes,
            int price,
            List<String> reasons
    ) {
        return new BeautyRouteStepResponse(
                step, category, title, description, minutes, price, List.copyOf(reasons));
    }

    private boolean containsAny(List<String> values, String... candidates) {
        for (String value : values) {
            for (String candidate : candidates) {
                if (candidate.equalsIgnoreCase(value)) {
                    return true;
                }
            }
        }
        return false;
    }
}
