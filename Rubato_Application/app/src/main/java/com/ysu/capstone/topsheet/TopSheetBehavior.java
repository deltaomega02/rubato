package com.ysu.capstone.topsheet;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;

import com.ysu.capstone.R;

import java.lang.ref.WeakReference;

public class TopSheetBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {

    public static final int STATE_COLLAPSED = 1;
    public static final int STATE_EXPANDED = 2;

    private int mMinOffset;
    private int mMaxOffset;
    private int mState = STATE_COLLAPSED;

    private ViewDragHelper mViewDragHelper;
    private WeakReference<V> mViewRef;
    private OnStateChangedListener mOnStateChangedListener;

    public TopSheetBehavior() {}

    public TopSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }



    public static <V extends View> TopSheetBehavior<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior();
        if (!(behavior instanceof TopSheetBehavior)) {
            throw new IllegalArgumentException("The view is not associated with TopSheetBehavior");
        }
        return (TopSheetBehavior<V>) behavior;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
        // Null 체크 추가
        if (child == null) {
            Log.e("TopSheetBehavior", "Child view is null during layout.");
            return false; // 레이아웃 처리 중단
        }

        // 기본 레이아웃 설정
        parent.onLayoutChild(child, layoutDirection);

        // 검색 영역의 하단에 탑시트가 위치하도록 검색 영역의 높이를 가져온다
        View searchArea = parent.findViewById(R.id.top_search);
        if (searchArea != null) {
            mMaxOffset = searchArea.getBottom(); // 검색 영역 하단 바로 밑에 탑시트가 붙도록 설정
        }

        // 최소 상태에서 하늘색 바와 "선호사항 선택" 텍스트만 보이도록 설정
        View topSheetBar = child.findViewById(R.id.top_sheet_bar); // 하늘색 바



        // 확장 상태일 때, 전체 내용을 보여주도록
        if (mState == STATE_EXPANDED) {
            ViewCompat.offsetTopAndBottom(child, mMaxOffset - child.getTop());
        } else {
            // 최소 확장 상태에서 하늘색 바와 텍스트만 보이도록 설정
            ViewCompat.offsetTopAndBottom(child, mMinOffset - child.getTop());
        }

        // ViewDragHelper 초기화
        if (mViewDragHelper == null) {
            mViewDragHelper = ViewDragHelper.create(parent, mDragCallback);
        }

        mViewRef = new WeakReference<>(child);
        return true;
    }




    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        return mViewDragHelper != null && mViewDragHelper.shouldInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (mViewDragHelper != null) {
            mViewDragHelper.processTouchEvent(event);
            return true;
        }
        return false;
    }

    private final ViewDragHelper.Callback mDragCallback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return mViewRef != null && mViewRef.get() == child;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int targetTop;
            if (yvel > 0) {
                targetTop = mMaxOffset;
                setStateInternal(STATE_EXPANDED);
            } else {
                targetTop = mMinOffset;
                setStateInternal(STATE_COLLAPSED);
            }

            if (mViewDragHelper.settleCapturedViewAt(releasedChild.getLeft(), targetTop)) {
                ViewCompat.postOnAnimation(releasedChild, new SettleRunnable(releasedChild, mState));
            }

            if (mOnStateChangedListener != null) {
                mOnStateChangedListener.onStateChanged(releasedChild, mState);
            }
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return Math.max(mMinOffset, Math.min(top, mMaxOffset));
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mMaxOffset - mMinOffset;
        }
    };

    private class SettleRunnable implements Runnable {
        private final View mView;
        private final int mTargetState;

        SettleRunnable(View view, int targetState) {
            mView = view;
            mTargetState = targetState;
        }

        @Override
        public void run() {
            if (mViewDragHelper != null && mViewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this);
            }
        }
    }

    public void setState(int state) {
        if (state != STATE_EXPANDED && state != STATE_COLLAPSED) {
            throw new IllegalArgumentException("Invalid state");
        }
        mState = state;
        V view = mViewRef != null ? mViewRef.get() : null;
        if (view != null) {
            if (mState == STATE_EXPANDED) {
                ViewCompat.offsetTopAndBottom(view, mMaxOffset - view.getTop());
            } else {
                ViewCompat.offsetTopAndBottom(view, mMinOffset - view.getTop());
            }
        }
    }

    public int getState() {
        return mState;
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mOnStateChangedListener = listener;
    }

    public interface OnStateChangedListener {
        void onStateChanged(@NonNull View topSheet, int newState);
    }

    private void setStateInternal(int state) {
        mState = state;
        V view = mViewRef != null ? mViewRef.get() : null;
        if (view != null && mOnStateChangedListener != null) {
            mOnStateChangedListener.onStateChanged(view, state);
        }
    }
}
