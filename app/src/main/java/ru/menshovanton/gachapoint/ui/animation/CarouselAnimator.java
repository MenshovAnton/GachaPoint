package ru.menshovanton.gachapoint.ui.animation;

import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;

public class CarouselAnimator {

    private static final long CAROUSEL_TRANSITION_DURATION = 240L;

    public static void setupItemAnimator(RecyclerView recyclerView) {
        if (recyclerView == null) return;
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(240);
        animator.setRemoveDuration(200);
        animator.setMoveDuration(260);
        animator.setChangeDuration(0);
        animator.setSupportsChangeAnimations(false);
        recyclerView.setItemAnimator(animator);
    }

    public static void alignSelectedGame(RecyclerView recyclerView, PillsAdapter adapter, GameType game, boolean smooth) {
        if (recyclerView == null || adapter == null || game == null) return;
        int pos = adapter.getPositionForGame(game);
        if (pos != -1) {
            alignPositionToStart(recyclerView, pos, smooth);
        }
    }

    public static void alignPositionToStart(RecyclerView recyclerView, int targetPosition, boolean smooth) {
        if (recyclerView == null || targetPosition < 0 || recyclerView.getContext() == null) return;
        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
        if (!(lm instanceof LinearLayoutManager)) return;
        LinearLayoutManager layoutManager = (LinearLayoutManager) lm;

        if (!recyclerView.isLaidOut() || recyclerView.getWidth() == 0) {
            recyclerView.post(() -> {
                if (recyclerView.isAttachedToWindow()) {
                    alignPositionToStart(recyclerView, targetPosition, false);
                }
            });
            return;
        }

        View targetChild = layoutManager.findViewByPosition(targetPosition);
        if (targetChild != null) {
            int dx = targetChild.getLeft() - recyclerView.getPaddingStart();
            if (Math.abs(dx) <= 2) {
                return;
            }
            if (dx < 0 && !recyclerView.canScrollHorizontally(-1)) {
                return;
            }
            if (dx > 0 && !recyclerView.canScrollHorizontally(1)) {
                return;
            }
            if (smooth) {
                recyclerView.smoothScrollBy(dx, 0);
            } else {
                recyclerView.scrollBy(dx, 0);
            }
            return;
        }

        if (smooth) {
            LinearSmoothScroller smoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
                @Override
                protected int getHorizontalSnapPreference() {
                    return SNAP_TO_START;
                }
            };
            smoothScroller.setTargetPosition(targetPosition);
            layoutManager.startSmoothScroll(smoothScroller);
        } else {
            layoutManager.scrollToPositionWithOffset(targetPosition, 0);
        }
    }

    public static void setCarouselVisibility(View carouselView, ViewGroup rootContainer, boolean visible) {
        if (carouselView == null) return;
        int targetVisibility = visible ? View.VISIBLE : View.GONE;
        if (carouselView.getVisibility() == targetVisibility) return;

        ViewGroup container = rootContainer;
        if (container == null && carouselView.getParent() instanceof ViewGroup) {
            container = (ViewGroup) carouselView.getParent();
        }

        if (container != null && carouselView.isLaidOut()) {
            TransitionSet set = new TransitionSet();
            set.setOrdering(TransitionSet.ORDERING_TOGETHER);
            set.addTransition(new Fade());
            set.addTransition(new ChangeBounds());
            set.setDuration(CAROUSEL_TRANSITION_DURATION);
            set.setInterpolator(new AccelerateDecelerateInterpolator());
            TransitionManager.beginDelayedTransition(container, set);
        }

        carouselView.setVisibility(targetVisibility);
    }
}
