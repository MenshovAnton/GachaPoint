package ru.menshovanton.gachapoint.ui.fragment.tracker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.GridLayout;

import androidx.annotation.NonNull;

import ru.menshovanton.gachapoint.R;

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

    private boolean swipedDuringGesture = false;

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        if (width > 0 && getChildCount() > 0) {
            float density = getResources().getDisplayMetrics().density;
            int marginPx = getResources().getDimensionPixelSize(R.dimen.calendar_cell_margin);
            int availableWidth = width - (getPaddingLeft() + getPaddingRight());
            int cellWidth = Math.max(0, (availableWidth - (marginPx * 2 * 7)) / 7);
            int minHeightPx = getResources().getDimensionPixelSize(R.dimen.calendar_cell_min_height);
            int maxHeightPx = getResources().getDimensionPixelSize(R.dimen.calendar_cell_max_height);
            int cellHeight = Math.max(minHeightPx, Math.min((int) (cellWidth * 0.95f), maxHeightPx));

            for (int i = 0; i < getChildCount(); i++) {
                android.view.View child = getChildAt(i);
                android.view.ViewGroup.LayoutParams lp = child.getLayoutParams();
                if (lp != null && (lp.width != cellWidth || lp.height != cellHeight)) {
                    lp.width = cellWidth;
                    lp.height = cellHeight;
                }
            }
        }
        super.onMeasure(widthSpec, heightSpec);
    }

    public void setOnSwipeListener(OnSwipeListener listener) {
        this.swipeListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialX = ev.getX();
                initialY = ev.getY();
                swipedDuringGesture = false;
                gestureDetector.onTouchEvent(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                float diffX = ev.getX() - initialX;
                float diffY = ev.getY() - initialY;

                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > touchSlop) {
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                float diffX = event.getX() - initialX;
                float diffY = event.getY() - initialY;
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > touchSlop) {
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
        }

        boolean handled = gestureDetector.onTouchEvent(event);

        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (!handled && !swipedDuringGesture) {
                float diffX = event.getX() - initialX;
                float diffY = event.getY() - initialY;
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > touchSlop * 2.5f) {
                    if (swipeListener != null) {
                        if (diffX > 0) {
                            swipeListener.onSwipeRight();
                        } else {
                            swipeListener.onSwipeLeft();
                        }
                    }
                    handled = true;
                    swipedDuringGesture = true;
                }
            }

            if (!handled && !swipedDuringGesture) {
                performClick();
            }
        }
        return handled || super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_VELOCITY_THRESHOLD = 80;

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null) return false;

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY) && (Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD || Math.abs(diffX) > touchSlop * 2)) {
                if (swipeListener != null) {
                    if (diffX > 0) {
                        swipeListener.onSwipeRight();
                    } else {
                        swipeListener.onSwipeLeft();
                    }
                }
                swipedDuringGesture = true;
                return true;
            }
            return false;
        }
    }
}
