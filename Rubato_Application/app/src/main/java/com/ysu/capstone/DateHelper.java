package com.ysu.capstone;

public class DateHelper {

    // 숫자에 따른 한국어
    public static String getKoreanDayString(int day) {
        switch (day) {
            case 1:
                return "첫날";
            case 2:
                return "둘째날";
            case 3:
                return "셋째날";
            case 4:
                return "넷째날";
            case 5:
                return "다섯번째날";
            case 6:
                return "여섯번째날";
            case 7:
                return "일곱번째날";
            case 8:
                return "여덟번째날";
            case 9:
                return "아홉번째날";
            case 10:
                return "열번째날";
            case 11:
                return "열한번째날";
            case 12:
                return "열두번째날";
            case 13:
                return "열세번째날";
            case 14:
                return "열네번째날";
            case 15:
                return "열다섯번째날";
            case 16:
                return "열여섯번째날";
            case 17:
                return "열일곱번째날";
            case 18:
                return "열여덟번째";
            case 19:
                return "열아홉번째날";
            case 20:
                return "스무번째날";
            case 21:
                return "스물한번째";
            default:
                return day + "째날"; // 그 외 경우
        }
    }
}
