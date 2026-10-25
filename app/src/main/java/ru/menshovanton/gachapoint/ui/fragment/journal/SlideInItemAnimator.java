package ru.menshovanton.gachapoint.ui.fragment.journal;

import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

public class SlideInItemAnimator extends DefaultItemAnimator {

    private final DecelerateInterpolator interpolator = new DecelerateInterpolator(1.5f);

    public SlideInItemAnimator() {
        setAddDuration(320);
        setMoveDuration(300);
        setChangeDuration(200);
        setSupportsChangeAnimations(false);
    }

    @Override
    public boolean animateAdd(@NonNull RecyclerView.ViewHolder holder) {
        endAnimation(holder);
        View view = holder.itemView;
        view.setAlpha(0f);
        view.setTranslationY(-view.getContext().getResources().getDisplayMetrics().density * 48f);
        view.animate()
                .translationY(0f)
                .setDuration(getAddDuration())
                .setInterpolator(interpolator)
                .start();
        return super.animateAdd(holder);
    }

    @Override
    public void endAnimation(@NonNull RecyclerView.ViewHolder item) {
        super.endAnimation(item);
        item.itemView.animate().cancel();
        item.itemView.setTranslationY(0f);
        item.itemView.setAlpha(1f);
    }

    @Override
    public void endAnimations() {
        super.endAnimations();
    }
}
