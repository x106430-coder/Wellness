package com.wellness.Service;

import com.wellness.Dto.QuestionDefinitionResponse;
import com.wellness.Dto.QuestionOptionResponse;
import com.wellness.Entity.QuestionCode;
import com.wellness.Entity.QuestionFrequency;
import com.wellness.Entity.QuestionInputType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class QuestionCatalogService {

    private final Map<QuestionCode, QuestionDefinition> catalog = new EnumMap<>(QuestionCode.class);

    public QuestionCatalogService() {
        register(
                QuestionCode.TODAY_ENERGY,
                "오늘의 에너지",
                QuestionFrequency.DAILY,
                QuestionInputType.SINGLE_CHOICE,
                false,
                options("VERY_LOW", "매우 낮음", "LOW", "낮음", "NORMAL", "보통", "HIGH", "좋음", "VERY_HIGH", "매우 좋음")
        );
        register(
                QuestionCode.LAST_NIGHT_SLEEP,
                "어젯밤 수면",
                QuestionFrequency.DAILY,
                QuestionInputType.SLIDER,
                false,
                List.of()
        );
        register(
                QuestionCode.SKIN_STATUS,
                "피부 상태",
                QuestionFrequency.DAILY,
                QuestionInputType.SINGLE_CHOICE,
                false,
                options("GOOD", "좋음", "DRY", "건조함", "SENSITIVE", "민감함", "ACNE", "여드름", "OILY", "피지 많음")
        );
        register(
                QuestionCode.CARE_AVAILABLE_TIME,
                "관리 사용 가능 시간",
                QuestionFrequency.DAILY,
                QuestionInputType.SINGLE_CHOICE,
                false,
                options("TEN_MIN", "10분", "TWENTY_MIN", "20분", "THIRTY_MIN", "30분", "ONE_HOUR_PLUS", "1시간 이상")
        );
        register(
                QuestionCode.TODAY_PLANNED_CARE,
                "오늘 하려고 했던 관리",
                QuestionFrequency.DAILY,
                QuestionInputType.MULTI_CHOICE,
                true,
                options(
                        "WORKOUT", "운동",
                        "SKINCARE", "스킨케어",
                        "DIET", "식단 관리",
                        "SUPPLEMENT", "영양제",
                        "MEDITATION", "명상 / 마음 관리",
                        "STRETCHING", "스트레칭",
                        "READING", "독서",
                        "SLEEP_EARLY", "일찍 자기",
                        "OTHER", "기타"
                )
        );
        register(
                QuestionCode.STRENGTH_TRAINING_FREQUENCY,
                "근력운동 - 주 몇회",
                QuestionFrequency.WEEKLY,
                QuestionInputType.SINGLE_CHOICE,
                false,
                frequencyOptions()
        );
        register(
                QuestionCode.CARDIO_FREQUENCY,
                "유산소 - 주 몇회",
                QuestionFrequency.WEEKLY,
                QuestionInputType.SINGLE_CHOICE,
                false,
                frequencyOptions()
        );
        register(
                QuestionCode.PILATES_FREQUENCY,
                "필라테스 - 주 몇회",
                QuestionFrequency.WEEKLY,
                QuestionInputType.SINGLE_CHOICE,
                false,
                frequencyOptions()
        );
        register(
                QuestionCode.SUPPLEMENTS,
                "영양제",
                QuestionFrequency.WEEKLY,
                QuestionInputType.MULTI_CHOICE,
                true,
                options("VITAMIN_D", "비타민 D", "VITAMIN_C", "비타민 C", "OMEGA_3", "오메가 3")
        );
        register(
                QuestionCode.WATER_INTAKE,
                "수분 섭취",
                QuestionFrequency.WEEKLY,
                QuestionInputType.SINGLE_CHOICE,
                false,
                options("500ML", "500mL", "1L", "1L", "1_5L", "1.5L")
        );
        register(
                QuestionCode.SKIN_TYPE,
                "스킨케어 타입",
                QuestionFrequency.WEEKLY,
                QuestionInputType.SINGLE_CHOICE,
                false,
                options("DRY", "건성", "OILY", "지성", "COMBINATION", "복합성", "UNKNOWN", "잘 모르겠어요")
        );
        register(
                QuestionCode.SKIN_CONCERN,
                "피부 고민",
                QuestionFrequency.WEEKLY,
                QuestionInputType.MULTI_CHOICE,
                true,
                options("DRYNESS", "건조함", "ACNE", "여드름", "SENSITIVE", "민감", "SEBUM", "피지", "ELASTICITY", "탄력")
        );
        register(
                QuestionCode.SKINCARE_VISIT_FREQUENCY,
                "피부관리 - 한달에 몇번",
                QuestionFrequency.WEEKLY,
                QuestionInputType.SINGLE_CHOICE,
                false,
                options("ZERO", "0회", "ONE", "1회", "TWO", "2회", "THREE_PLUS", "3회 이상")
        );
    }

    public List<QuestionDefinitionResponse> getOnboardingQuestions() {
        return catalog.values().stream()
                .map(QuestionDefinition::toResponse)
                .toList();
    }

    public List<QuestionDefinitionResponse> getQuestionsByFrequency(QuestionFrequency frequency) {
        return catalog.values().stream()
                .filter(question -> question.frequency == frequency)
                .map(QuestionDefinition::toResponse)
                .toList();
    }

    public QuestionFrequency getFrequency(QuestionCode questionCode) {
        QuestionDefinition definition = catalog.get(questionCode);
        if (definition == null) {
            throw new IllegalArgumentException("지원하지 않는 questionCode 입니다.");
        }
        return definition.frequency;
    }

    public int countByFrequency(QuestionFrequency frequency) {
        return (int) catalog.values().stream()
                .filter(question -> question.frequency == frequency)
                .count();
    }

    public List<QuestionCode> getCodesByFrequency(QuestionFrequency frequency) {
        return catalog.entrySet().stream()
                .filter(entry -> entry.getValue().frequency == frequency)
                .map(Map.Entry::getKey)
                .toList();
    }

    private void register(
            QuestionCode code,
            String title,
            QuestionFrequency frequency,
            QuestionInputType inputType,
            boolean multiSelect,
            List<QuestionOptionResponse> options
    ) {
        catalog.put(code, new QuestionDefinition(code, title, frequency, inputType, multiSelect, options));
    }

    private List<QuestionOptionResponse> frequencyOptions() {
        return options("ZERO", "0회", "ONE", "1회", "TWO", "2회", "THREE", "3회", "FOUR_PLUS", "4회 이상");
    }

    private static List<QuestionOptionResponse> options(String... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("Option values must be provided as code/label pairs.");
        }

        java.util.ArrayList<QuestionOptionResponse> options = new java.util.ArrayList<>();
        for (int index = 0; index < values.length; index += 2) {
            options.add(new QuestionOptionResponse(values[index], values[index + 1]));
        }
        return List.copyOf(options);
    }

    private record QuestionDefinition(
            QuestionCode code,
            String title,
            QuestionFrequency frequency,
            QuestionInputType inputType,
            boolean multiSelect,
            List<QuestionOptionResponse> options
    ) {
        private QuestionDefinitionResponse toResponse() {
            return new QuestionDefinitionResponse(code, title, frequency, inputType, multiSelect, options);
        }
    }
}
