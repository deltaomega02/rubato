package com.ysu.capstone.decorators;

import android.content.Context;
import androidx.core.content.ContextCompat;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.ysu.capstone.R;

public class StartDateDecorator implements DayViewDecorator {
    private CalendarDay startDate;
    private Context context;

    public StartDateDecorator(Context context, CalendarDay startDate) {
        this.context = context;
        this.startDate = startDate;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.equals(startDate);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.drawable_start_date));
    }
}
