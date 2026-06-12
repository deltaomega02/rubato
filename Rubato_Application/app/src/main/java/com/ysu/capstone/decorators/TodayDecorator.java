package com.ysu.capstone.decorators;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.ysu.capstone.R;

public class TodayDecorator implements DayViewDecorator {

    private CalendarDay today;
    private Drawable backgroundDrawable;

    public TodayDecorator(Context context) {
        this.today = CalendarDay.today();
        this.backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.drawable_today);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.equals(today);
    }

    @Override
    public void decorate(DayViewFacade view) {
        // 오늘 날짜 회색 배경
        view.setBackgroundDrawable(backgroundDrawable);
    }
}
