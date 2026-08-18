package com.wellness.Service;

import org.springframework.stereotype.Component;

@Component
public class EnergyBandMessagePolicy {

    public String homeMessage(Integer energyScore) {
        if (energyScore == null) {
            return "에너지를 판단할 답변이 아직 부족해요.";
        }
        if (energyScore <= 20) {
            return "오늘은 멈춰 쉬어가도 괜찮아요.";
        }
        if (energyScore <= 40) {
            return "오늘은 꼭 필요한 일만 가볍게 해보세요.";
        }
        if (energyScore <= 60) {
            return "오늘은 무리하지 않는 균형이 잘 맞아요.";
        }
        if (energyScore <= 80) {
            return "오늘은 계획한 일을 차분히 이어가도 좋아요.";
        }
        return "오늘은 좋은 에너지를 여유 있게 나눠 써보세요.";
    }

    public String routeMessage(Integer energyScore) {
        if (energyScore == null) {
            return "진단을 마치면 오늘의 루트를 간단히 알려드릴게요.";
        }
        if (energyScore <= 20) {
            return "오늘의 루트는 회복만 남겨두었어요.";
        }
        if (energyScore <= 40) {
            return "오늘의 루트는 꼭 필요한 관리만 남겨두었어요.";
        }
        if (energyScore <= 60) {
            return "오늘의 루트는 가벼운 관리 위주로 골랐어요.";
        }
        if (energyScore <= 80) {
            return "오늘의 루트는 계획한 관리를 균형 있게 담았어요.";
        }
        return "오늘의 루트는 좋은 에너지를 오래 쓰도록 구성했어요.";
    }
}
