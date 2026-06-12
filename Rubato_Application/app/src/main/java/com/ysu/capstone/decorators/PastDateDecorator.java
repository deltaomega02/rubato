package com.ysu.capstone.decorators;

import android.content.Context;
import android.graphics.Color;
import android.text.style.ForegroundColorSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.Calendar;

public class PastDateDecorator implements DayViewDecorator {
    private final Calendar today;

    public PastDateDecorator(Context context) {
        today = Calendar.getInstance();
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        // 오늘 이전 날짜만 데코레이터 적용
        return day.getDate().before(today.getTime());
    }

    @Override
    public void decorate(DayViewFacade view) {
        // 오늘 이전 날짜를 흐린 글씨로 표시
        view.addSpan(new ForegroundColorSpan(Color.LTGRAY));
    }
}
