package com.ysu.capstone.decorators;

import android.content.Context;
import androidx.core.content.ContextCompat;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.ysu.capstone.R;

public class EndDateDecorator implements DayViewDecorator {
    private CalendarDay endDate;
    private Context context;

    public EndDateDecorator(Context context, CalendarDay endDate) {
        this.context = context;
        this.endDate = endDate;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.equals(endDate);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.drawable_end_date));
    }
}
