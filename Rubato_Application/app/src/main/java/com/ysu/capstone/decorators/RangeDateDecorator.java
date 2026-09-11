package com.ysu.capstone.decorators;

import android.content.Context;
import androidx.core.content.ContextCompat;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.ysu.capstone.R;

public class RangeDateDecorator implements DayViewDecorator {
    private CalendarDay startDate;
    private CalendarDay endDate;
    private Context context;

    public RangeDateDecorator(Context context, CalendarDay startDate, CalendarDay endDate) {
        this.context = context;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.isAfter(startDate) && day.isBefore(endDate);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.drawable_connected_range));
    }
}
