package ru.menshovanton.gachapoint.ui.animation;

import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

public class CarouselAnimator {

    private static final long CAROUSEL_TRANSITION_DURATION = 240L;

    public static void setupItemAnimator(RecyclerView recyclerView) {
        if (recyclerView == null) return;
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(240);
        animator.setRemoveDuration(200);
        animator.setMoveDuration(260);
        animator.setChangeDuration(150);
        animator.setSupportsChangeAnimations(false);
        recyclerView.setItemAnimator(animator);
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
