package ru.menshovanton.gachapoint.ui.fragment.tracker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.GridLayout;

import androidx.annotation.NonNull;

public class CalendarGrid extends GridLayout {

    public interface OnSwipeListener {
        void onSwipeLeft();
        void onSwipeRight();
    }

    private OnSwipeListener swipeListener;
    private final GestureDetector gestureDetector;
    private final int touchSlop;
    private float initialX;
    private float initialY;

    public CalendarGrid(Context context) {
        this(context, null);
    }

    public CalendarGrid(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalendarGrid(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        gestureDetector = new GestureDetector(context, new SwipeGestureListener());
    }

    public void setOnSwipeListener(OnSwipeListener listener) {
        this.swipeListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = ev.getX();
                initialY = ev.getY();
                gestureDetector.onTouchEvent(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                float diffX = ev.getX() - initialX;
                float diffY = ev.getY() - initialY;

                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > touchSlop) {
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = gestureDetector.onTouchEvent(event);
        if (!handled && event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
        }
        return handled || super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null) return false;

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (swipeListener != null) {
                    if (diffX > 0) {
                        swipeListener.onSwipeRight();
                    } else {
                        swipeListener.onSwipeLeft();
                    }
                }
                return true;
            }
            return false;
        }
    }
}
