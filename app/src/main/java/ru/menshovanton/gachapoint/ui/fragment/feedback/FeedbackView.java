package ru.menshovanton.gachapoint.ui.fragment.feedback;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ru.menshovanton.gachapoint.R;

public class FeedbackView extends Fragment {

    private FeedbackViewModel viewModel;

    private ImageButton btnBack;
    private View rowTelegram;
    private View rowGithubIssues;
    private View rowGoogleForms;

    private ChipGroup cgFeedbackType;

    private View layoutRatingContainer;
    private ChipGroup cgRating;

    private TextInputLayout tilSubject;
    private TextInputEditText tietSubject;

    private TextInputLayout tilMessage;
    private TextInputEditText tietMessage;

    private TextInputLayout tilSteps;
    private TextInputEditText tietSteps;

    private MaterialSwitch switchDeviceInfo;
    private MaterialCardView mcvDeviceInfoPreview;

    private MaterialButton btnSendEmail;
    private MaterialButton btnSendGithub;
    private MaterialButton btnCopyTelegram;

    public FeedbackView() {}

    public static FeedbackView newInstance() {
        return new FeedbackView();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feedback, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(FeedbackViewModel.class);

        initViews(view);
        setupListeners();
        observeViewModel();
        setupBackPressed();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        rowTelegram = view.findViewById(R.id.row_telegram);
        rowGithubIssues = view.findViewById(R.id.row_github_issues);
        rowGoogleForms = view.findViewById(R.id.row_google_forms);

        cgFeedbackType = view.findViewById(R.id.cg_feedback_type);
        Chip chipTypeBug = view.findViewById(R.id.chip_type_bug);
        Chip chipTypeFeature = view.findViewById(R.id.chip_type_feature);
        Chip chipTypeReview = view.findViewById(R.id.chip_type_review);

        layoutRatingContainer = view.findViewById(R.id.layout_rating_container);
        cgRating = view.findViewById(R.id.cg_rating);

        tilSubject = view.findViewById(R.id.til_feedback_subject);
        tietSubject = view.findViewById(R.id.tiet_feedback_subject);

        tilMessage = view.findViewById(R.id.til_feedback_message);
        tietMessage = view.findViewById(R.id.tiet_feedback_message);

        tilSteps = view.findViewById(R.id.til_feedback_steps);
        tietSteps = view.findViewById(R.id.tiet_feedback_steps);

        switchDeviceInfo = view.findViewById(R.id.switch_device_info);
        mcvDeviceInfoPreview = view.findViewById(R.id.mcv_device_info_preview);
        TextView tvDeviceInfoPreview = view.findViewById(R.id.tv_device_info_preview);

        btnSendEmail = view.findViewById(R.id.btn_send_email);
        btnSendGithub = view.findViewById(R.id.btn_send_github);
        btnCopyTelegram = view.findViewById(R.id.btn_copy_telegram);

        tvDeviceInfoPreview.setText(getDiagnosticInfo());
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> viewModel.onBackTriggered());

        if (rowTelegram != null) {
            rowTelegram.setOnClickListener(v -> viewModel.openTelegram());
        }
        if (rowGithubIssues != null) {
            rowGithubIssues.setOnClickListener(v -> viewModel.openGithubIssues());
        }
        if (rowGoogleForms != null) {
            rowGoogleForms.setOnClickListener(v -> viewModel.openGoogleForm());
        }

        cgFeedbackType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_type_bug) {
                viewModel.setSelectedType(FeedbackViewModel.TYPE_BUG);
            } else if (checkedId == R.id.chip_type_feature) {
                viewModel.setSelectedType(FeedbackViewModel.TYPE_FEATURE);
            } else if (checkedId == R.id.chip_type_review) {
                viewModel.setSelectedType(FeedbackViewModel.TYPE_REVIEW);
            }
        });

        cgRating.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            int rating = 5;
            if (checkedId == R.id.chip_star_1) rating = 1;
            else if (checkedId == R.id.chip_star_2) rating = 2;
            else if (checkedId == R.id.chip_star_3) rating = 3;
            else if (checkedId == R.id.chip_star_4) rating = 4;
            viewModel.setRating(rating);
        });

        switchDeviceInfo.setOnCheckedChangeListener((buttonView, isChecked) ->
                mcvDeviceInfoPreview.setVisibility(isChecked ? View.VISIBLE : View.GONE)
        );

        btnSendEmail.setOnClickListener(v -> {
            clearErrors();
            viewModel.onSendEmailClicked(
                    getText(tietSubject),
                    getText(tietMessage),
                    getText(tietSteps),
                    switchDeviceInfo.isChecked(),
                    getDiagnosticInfo()
            );
        });

        btnSendGithub.setOnClickListener(v -> {
            clearErrors();
            viewModel.onSendGithubClicked(
                    getText(tietSubject),
                    getText(tietMessage),
                    getText(tietSteps),
                    switchDeviceInfo.isChecked(),
                    getDiagnosticInfo()
            );
        });

        btnCopyTelegram.setOnClickListener(v -> {
            clearErrors();
            viewModel.onCopyTelegramClicked(
                    getText(tietSubject),
                    getText(tietMessage),
                    getText(tietSteps),
                    switchDeviceInfo.isChecked(),
                    getDiagnosticInfo()
            );
        });
    }

    private void observeViewModel() {
        viewModel.getSelectedType().observe(getViewLifecycleOwner(), type -> {
            if (type == null) return;
            switch (type) {
                case FeedbackViewModel.TYPE_FEATURE:
                    tilSubject.setHint(getString(R.string.feedback_subject_hint_feature));
                    tilMessage.setHint(getString(R.string.feedback_message_hint_feature));
                    tilSteps.setVisibility(View.GONE);
                    layoutRatingContainer.setVisibility(View.GONE);
                    btnSendGithub.setVisibility(View.VISIBLE);
                    break;
                case FeedbackViewModel.TYPE_REVIEW:
                    tilSubject.setHint(getString(R.string.feedback_subject_hint_review));
                    tilMessage.setHint(getString(R.string.feedback_message_hint_review));
                    tilSteps.setVisibility(View.GONE);
                    layoutRatingContainer.setVisibility(View.VISIBLE);
                    btnSendGithub.setVisibility(View.GONE);
                    break;
                case FeedbackViewModel.TYPE_BUG:
                default:
                    tilSubject.setHint(getString(R.string.feedback_subject_hint_bug));
                    tilMessage.setHint(getString(R.string.feedback_message_hint_bug));
                    tilSteps.setVisibility(View.VISIBLE);
                    layoutRatingContainer.setVisibility(View.GONE);
                    btnSendGithub.setVisibility(View.VISIBLE);
                    break;
            }
        });

        viewModel.getSendEmailEvent().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + data.email));
            intent.putExtra(Intent.EXTRA_SUBJECT, data.subject);
            intent.putExtra(Intent.EXTRA_TEXT, data.body);
            try {
                startActivity(Intent.createChooser(intent, getString(R.string.feedback_email_chooser_title)));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireContext(), R.string.feedback_email_client_not_found, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getOpenUrlEvent().observe(getViewLifecycleOwner(), this::openUrl);

        viewModel.getCopyToClipboardEvent().observe(getViewLifecycleOwner(), text -> {
            if (text == null) return;
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("GachaPoint Feedback", text);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(requireContext(), R.string.feedback_copied_toast, Toast.LENGTH_SHORT).show();
            openUrl(getString(R.string.feedback_telegram_url));
        });

        viewModel.getValidationErrorEvent().observe(getViewLifecycleOwner(), resId -> {
            if (resId != null) {
                tilMessage.setError(getString(resId));
                tietMessage.requestFocus();
            }
        });

        viewModel.getPopBackStack().observe(getViewLifecycleOwner(), unused -> {
            if (isAdded()) {
                NavHostFragment.findNavController(this).popBackStack();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), unused -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.form_load_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBackPressed() {
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        NavHostFragment.findNavController(FeedbackView.this).popBackStack();
                    }
                }
        );
    }

    private void clearErrors() {
        tilMessage.setError(null);
    }

    private String getText(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString();
    }

    private String getDiagnosticInfo() {
        String device = Build.MANUFACTURER + " " + Build.MODEL;
        String androidVersion = Build.VERSION.RELEASE;
        int apiLevel = Build.VERSION.SDK_INT;
        String appVersion = "0.2.0";
        try {
            appVersion = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception ignored) {}

        return getString(R.string.feedback_device_info_format, device, androidVersion, apiLevel, appVersion);
    }

    private void openUrl(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.form_load_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        btnBack = null;
        rowTelegram = null;
        rowGithubIssues = null;
        rowGoogleForms = null;
        cgFeedbackType = null;
        layoutRatingContainer = null;
        cgRating = null;
        tilSubject = null;
        tietSubject = null;
        tilMessage = null;
        tietMessage = null;
        tilSteps = null;
        tietSteps = null;
        switchDeviceInfo = null;
        mcvDeviceInfoPreview = null;
        btnSendEmail = null;
        btnSendGithub = null;
        btnCopyTelegram = null;
    }
}