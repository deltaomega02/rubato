package com.ysu.capstone.decorators;

import android.animation.ObjectAnimator;
import android.view.View;

public class DotLoadingAnimation {

    public static void start(View dot1, View dot2, View dot3) {
        // 첫 번째 점 애니메이션 (0ms 지연 시작)
        createDotBounceAnimation(dot1, 0).start();

        // 두 번째 점 애니메이션 (250ms 지연 시작)
        createDotBounceAnimation(dot2, 150).start();

        // 세 번째 점 애니메이션 (500ms 지연 시작)
        createDotBounceAnimation(dot3, 300).start();
    }

    // 개별 점 튀는 애니메이션을 생성하는 메서드
    private static ObjectAnimator createDotBounceAnimation(View dot, int delay) {
        // Y축으로 위아래로 이동하는 애니메이션: 0에서 -30까지 이동 후 다시 0으로 복귀
        ObjectAnimator bounce = ObjectAnimator.ofFloat(dot, "translationY", -10, -15f, -10f);

        // 무한 반복 설정
        bounce.setRepeatCount(ObjectAnimator.INFINITE);
        bounce.setRepeatMode(ObjectAnimator.RESTART);

        // 시작 지연 시간 설정
        bounce.setStartDelay(delay);
        bounce.setDuration(500); // 도트가 튀는 속도 설정

        return bounce;
    }
}
