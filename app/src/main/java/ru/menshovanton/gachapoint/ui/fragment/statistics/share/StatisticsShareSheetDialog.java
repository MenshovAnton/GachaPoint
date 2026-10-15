package ru.menshovanton.gachapoint.ui.fragment.statistics.share;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import ru.menshovanton.gachapoint.R;

public class StatisticsShareSheetDialog extends BottomSheetDialogFragment {

    public interface OnOptionSelectedListener {
        void onShareSend();
        void onSaveToGallery();
    }

    private OnOptionSelectedListener listener;

    public static StatisticsShareSheetDialog newInstance() {
        return new StatisticsShareSheetDialog();
    }

    public void setListener(OnOptionSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_share_sheet, container, false);

        View rowSend = view.findViewById(R.id.row_share_send);
        View rowSave = view.findViewById(R.id.row_share_save);

        if (rowSend != null) {
            rowSend.setOnClickListener(v -> {
                dismiss();
                if (listener != null) {
                    listener.onShareSend();
                }
            });
        }

        if (rowSave != null) {
            rowSave.setOnClickListener(v -> {
                dismiss();
                if (listener != null) {
                    listener.onSaveToGallery();
                }
            });
        }

        return view;
    }
}
