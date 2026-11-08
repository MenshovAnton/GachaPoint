package ru.menshovanton.gachapoint.ui.fragment.help;

import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        private final View divider;
        private final TextView content;

        public HelpViewHolder(@NonNull View itemView) {
            super(itemView);
            ViewGroup container = itemView.findViewById(R.id.ll_help_section_container);
            header = itemView.findViewById(R.id.header_help_section);
            icon = itemView.findViewById(R.id.iv_section_icon);
            title = itemView.findViewById(R.id.tv_section_title);
            chevron = itemView.findViewById(R.id.iv_chevron);
            divider = itemView.findViewById(R.id.divider_section);
            content = itemView.findViewById(R.id.tv_section_content);
        }

        void bind(HelpSection section) {
            icon.setImageResource(section.getIconResId());
            title.setText(section.getTitleResId());
            content.setText(section.getContentResId());

            boolean expanded = section.isExpanded();
            content.setVisibility(expanded ? View.VISIBLE : View.GONE);
            divider.setVisibility(expanded ? View.VISIBLE : View.GONE);
            chevron.setRotation(expanded ? 180f : 0f);

            header.setOnClickListener(v -> {
                boolean newExpanded = !section.isExpanded();
                section.setExpanded(newExpanded);

                ViewGroup parent = (ViewGroup) itemView.getParent();
                if (parent != null) {
                    AutoTransition transition = new AutoTransition();
                    transition.setDuration(220);
                    TransitionManager.beginDelayedTransition(parent, transition);
                }

                content.setVisibility(newExpanded ? View.VISIBLE : View.GONE);
                divider.setVisibility(newExpanded ? View.VISIBLE : View.GONE);
                chevron.animate().rotation(newExpanded ? 180f : 0f).setDuration(220).start();
            });
        }
    }
}
