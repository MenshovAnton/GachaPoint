package ru.menshovanton.gachapoint.ui.fragment.home;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.ui.fragment.home.model.HomeWidgetItem;

public class HomeWidgetsReorderAdapter extends RecyclerView.Adapter<HomeWidgetsReorderAdapter.ViewHolder> {

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public interface OnWidgetChangedListener {
        void onWidgetChanged(List<HomeWidgetItem> items);
    }

    private final List<HomeWidgetItem> items = new ArrayList<>();
    private final OnStartDragListener dragListener;
    private final OnWidgetChangedListener changedListener;

    public HomeWidgetsReorderAdapter(List<HomeWidgetItem> initialItems,
                                     OnStartDragListener dragListener,
                                     OnWidgetChangedListener changedListener) {
        this.dragListener = dragListener;
        this.changedListener = changedListener;
        setItems(initialItems);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<HomeWidgetItem> newItems) {
        items.clear();
        if (newItems != null) {
            for (HomeWidgetItem item : newItems) {
                items.add(new HomeWidgetItem(item.type, item.isVisible));
            }
        }
        notifyDataSetChanged();
    }

    public List<HomeWidgetItem> getItems() {
        return new ArrayList<>(items);
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(items, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(items, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        if (changedListener != null) {
            changedListener.onWidgetChanged(getItems());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_widget_reorder, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HomeWidgetItem item = items.get(position);

        holder.tvTitle.setText(item.type.titleResId);
        holder.ivIcon.setImageResource(item.type.iconResId);

        holder.switchVisibility.setOnCheckedChangeListener(null);
        holder.switchVisibility.setChecked(item.isVisible);
        holder.switchVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.isVisible = isChecked;
            if (changedListener != null) {
                changedListener.onWidgetChanged(getItems());
            }
        });

        holder.ivDragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (dragListener != null) {
                    dragListener.onStartDrag(holder);
                }
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivDragHandle;
        public final ImageView ivIcon;
        public final TextView tvTitle;
        public final MaterialSwitch switchVisibility;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
            ivIcon = itemView.findViewById(R.id.iv_widget_icon);
            tvTitle = itemView.findViewById(R.id.tv_widget_title);
            switchVisibility = itemView.findViewById(R.id.switch_widget_visibility);
        }
    }
}
