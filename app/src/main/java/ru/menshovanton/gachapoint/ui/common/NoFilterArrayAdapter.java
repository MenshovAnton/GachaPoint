package ru.menshovanton.gachapoint.ui.common;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.menshovanton.gachapoint.R;
public class NoFilterArrayAdapter<T> extends ArrayAdapter<T> {

    private final List<T> allItems;
    private int selectedPosition = -1;

    private final Filter noFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            results.values = allItems;
            results.count = allItems.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return resultValue == null ? "" : resultValue.toString();
        }
    };

    public NoFilterArrayAdapter(@NonNull Context context, int resource, @NonNull List<T> objects) {
        super(context, resource, new ArrayList<>(objects));
        this.allItems = new ArrayList<>(objects);
    }

    public NoFilterArrayAdapter(@NonNull Context context, int resource, @NonNull T[] objects) {
        super(context, resource, new ArrayList<>(Arrays.asList(objects)));
        this.allItems = new ArrayList<>(Arrays.asList(objects));
    }

    public void setSelectedPosition(int position) {
        if (this.selectedPosition != position) {
            this.selectedPosition = position;
            notifyDataSetChanged();
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedItem(@Nullable Object item) {
        if (item == null) {
            setSelectedPosition(-1);
        } else {
            setSelectedPosition(allItems.indexOf(item));
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        if (position == selectedPosition) {
            view.setBackgroundResource(R.drawable.bg_dropdown_item_selected);
        } else {
            view.setBackgroundResource(R.drawable.bg_dropdown_item_selector);
        }
        return view;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return noFilter;
    }
}
