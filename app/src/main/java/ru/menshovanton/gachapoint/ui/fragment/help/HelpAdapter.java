package ru.menshovanton.gachapoint.ui.fragment.help;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.menshovanton.gachapoint.R;

public class HelpAdapter extends RecyclerView.Adapter<HelpAdapter.HelpViewHolder> {

    private final List<HelpSection> sections;

    public HelpAdapter(@NonNull List<HelpSection> sections) {
        this.sections = sections;
    }

    @NonNull
    @Override
    public HelpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_help_section, parent, false);
        return new HelpViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HelpViewHolder holder, int position) {
        HelpSection section = sections.get(position);
        holder.bind(section);
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    public static class HelpViewHolder extends RecyclerView.ViewHolder {

        private final View header;
        private final ImageView icon;
        private final TextView title;
        private final ImageView chevron;
        private final View expandableContent;
        private final TextView content;
        private ValueAnimator heightAnimator;

        public HelpViewHolder(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header_help_section);
            icon = itemView.findViewById(R.id.iv_section_icon);
            title = itemView.findViewById(R.id.tv_section_title);
            chevron = itemView.findViewById(R.id.iv_chevron);
            expandableContent = itemView.findViewById(R.id.ll_expandable_content);
            content = itemView.findViewById(R.id.tv_section_content);
        }

        void bind(HelpSection section) {
            if (heightAnimator != null) {
                heightAnimator.cancel();
                heightAnimator = null;
            }
            chevron.animate().cancel();

            icon.setImageResource(section.getIconResId());
            title.setText(section.getTitleResId());
            content.setText(section.getContentResId());

            boolean expanded = section.isExpanded();
            ViewGroup.LayoutParams lp = expandableContent.getLayoutParams();
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            expandableContent.setLayoutParams(lp);
            expandableContent.setAlpha(1f);
            expandableContent.setVisibility(expanded ? View.VISIBLE : View.GONE);
            chevron.setRotation(expanded ? 180f : 0f);

            header.setOnClickListener(v -> {
                boolean newExpanded = !section.isExpanded();
                section.setExpanded(newExpanded);
                animateToggle(newExpanded);
                chevron.animate().rotation(newExpanded ? 180f : 0f).setDuration(220).start();
            });
        }

        private void animateToggle(boolean expand) {
            if (heightAnimator != null) {
                heightAnimator.cancel();
            }

            if (expand) {
                int parentWidth = itemView.getWidth();
                if (parentWidth <= 0) {
                    expandableContent.setAlpha(1f);
                    expandableContent.setVisibility(View.VISIBLE);
                    return;
                }
                int widthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                expandableContent.measure(widthSpec, heightSpec);
                int targetHeight = expandableContent.getMeasuredHeight();

                ViewGroup.LayoutParams lp = expandableContent.getLayoutParams();
                lp.height = 0;
                expandableContent.setLayoutParams(lp);
                expandableContent.setAlpha(0f);
                expandableContent.setVisibility(View.VISIBLE);

                heightAnimator = ValueAnimator.ofInt(0, targetHeight);
                heightAnimator.setDuration(220);
                heightAnimator.setInterpolator(new FastOutSlowInInterpolator());
                heightAnimator.addUpdateListener(anim -> {
                    int val = (int) anim.getAnimatedValue();
                    ViewGroup.LayoutParams params = expandableContent.getLayoutParams();
                    params.height = val;
                    expandableContent.setLayoutParams(params);
                    expandableContent.setAlpha(anim.getAnimatedFraction());
                });
                heightAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ViewGroup.LayoutParams params = expandableContent.getLayoutParams();
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        expandableContent.setLayoutParams(params);
                        expandableContent.setAlpha(1f);
                        heightAnimator = null;
                    }
                });
                heightAnimator.start();
            } else {
                int startHeight = expandableContent.getHeight();
                if (startHeight <= 0) {
                    expandableContent.setVisibility(View.GONE);
                    return;
                }

                heightAnimator = ValueAnimator.ofInt(startHeight, 0);
                heightAnimator.setDuration(200);
                heightAnimator.setInterpolator(new FastOutSlowInInterpolator());
                heightAnimator.addUpdateListener(anim -> {
                    int val = (int) anim.getAnimatedValue();
                    ViewGroup.LayoutParams params = expandableContent.getLayoutParams();
                    params.height = val;
                    expandableContent.setLayoutParams(params);
                    expandableContent.setAlpha(1f - anim.getAnimatedFraction());
                });
                heightAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        expandableContent.setVisibility(View.GONE);
                        ViewGroup.LayoutParams params = expandableContent.getLayoutParams();
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        expandableContent.setLayoutParams(params);
                        expandableContent.setAlpha(1f);
                        heightAnimator = null;
                    }
                });
                heightAnimator.start();
            }
        }
    }
}
