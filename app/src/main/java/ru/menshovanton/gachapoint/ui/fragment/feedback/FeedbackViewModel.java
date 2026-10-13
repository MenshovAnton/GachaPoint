package ru.menshovanton.gachapoint.ui.fragment.feedback;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;

public class FeedbackViewModel extends AndroidViewModel {

    public static final int TYPE_BUG = 0;
    public static final int TYPE_FEATURE = 1;
    public static final int TYPE_REVIEW = 2;

    public static class EmailData {
        public final String email;
        public final String subject;
        public final String body;

        public EmailData(String email, String subject, String body) {
            this.email = email;
            this.subject = subject;
            this.body = body;
        }
    }

    private final MutableLiveData<Integer> selectedTypeLiveData = new MutableLiveData<>(TYPE_BUG);
    private final MutableLiveData<Integer> ratingLiveData = new MutableLiveData<>(5);

    private final SingleLiveEvent<EmailData> sendEmailEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> openUrlEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> copyToClipboardEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> validationErrorEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> popBackStackEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> errorMessageEvent = new SingleLiveEvent<>();

    public FeedbackViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Integer> getSelectedType() {
        return selectedTypeLiveData;
    }

    public void setSelectedType(int type) {
        selectedTypeLiveData.setValue(type);
    }

    public LiveData<Integer> getRating() {
        return ratingLiveData;
    }

    public void setRating(int rating) {
        ratingLiveData.setValue(rating);
    }

    public LiveData<EmailData> getSendEmailEvent() {
        return sendEmailEvent;
    }

    public LiveData<String> getOpenUrlEvent() {
        return openUrlEvent;
    }

    public LiveData<String> getCopyToClipboardEvent() {
        return copyToClipboardEvent;
    }

    public LiveData<Integer> getValidationErrorEvent() {
        return validationErrorEvent;
    }

    public LiveData<Void> getPopBackStack() {
        return popBackStackEvent;
    }

    public LiveData<Void> getErrorMessage() {
        return errorMessageEvent;
    }

    public void onBackTriggered() {
        popBackStackEvent.call();
    }

    public void sendErrorMessage() {
        errorMessageEvent.call();
    }

    public void openTelegram() {
        openUrlEvent.setValue(getApplication().getString(R.string.feedback_telegram_url));
    }

    public void openGithubIssues() {
        openUrlEvent.setValue(getApplication().getString(R.string.feedback_github_issues_url));
    }

    public void openGoogleForm() {
        openUrlEvent.setValue(getApplication().getString(R.string.feedback_google_form_url));
    }

    public void onSendEmailClicked(String subjectInput, String messageInput, String stepsInput,
                                  boolean attachDiagnostics, String diagnosticInfo) {
        if (validateInput(messageInput)) return;

        int type = getSelectedTypeVal();
        int rating = getRatingVal();
        String subject = formatSubject(type, subjectInput, rating);
        String body = formatBody(type, messageInput, stepsInput, attachDiagnostics, diagnosticInfo);
        String email = getApplication().getString(R.string.feedback_contact_email);

        sendEmailEvent.setValue(new EmailData(email, subject, body));
    }

    public void onSendGithubClicked(String subjectInput, String messageInput, String stepsInput,
                                   boolean attachDiagnostics, String diagnosticInfo) {
        if (validateInput(messageInput)) return;

        int type = getSelectedTypeVal();
        int rating = getRatingVal();
        String subject = formatSubject(type, subjectInput, rating);
        String body = formatBody(type, messageInput, stepsInput, attachDiagnostics, diagnosticInfo);

        try {
            String encodedTitle = URLEncoder.encode(subject, "UTF-8");
            String encodedBody = URLEncoder.encode(body, "UTF-8");
            String url = "https://github.com/MenshovAnton/GachaPoint/issues/new?title=" + encodedTitle + "&body=" + encodedBody;
            openUrlEvent.setValue(url);
        } catch (UnsupportedEncodingException e) {
            openGithubIssues();
        }
    }

    public void onCopyTelegramClicked(String subjectInput, String messageInput, String stepsInput,
                                     boolean attachDiagnostics, String diagnosticInfo) {
        if (validateInput(messageInput)) return;

        int type = getSelectedTypeVal();
        int rating = getRatingVal();
        String subject = formatSubject(type, subjectInput, rating);
        String body = formatBody(type, messageInput, stepsInput, attachDiagnostics, diagnosticInfo);

        String fullMessage = subject + "\n\n" + body;
        copyToClipboardEvent.setValue(fullMessage);
    }

    private boolean validateInput(String message) {
        if (message == null || message.trim().isEmpty()) {
            validationErrorEvent.setValue(R.string.feedback_error_empty_message);
            return true;
        }
        return false;
    }

    public String formatSubject(int type, String subjectInput, int rating) {
        String cleanSubject = (subjectInput != null) ? subjectInput.trim() : "";
        if (!cleanSubject.isEmpty()) {
            return cleanSubject;
        }

        switch (type) {
            case TYPE_FEATURE:
                return getApplication().getString(R.string.feedback_default_subject_feature);
            case TYPE_REVIEW:
                return getApplication().getString(R.string.feedback_default_subject_review, rating);
            case TYPE_BUG:
            default:
                return getApplication().getString(R.string.feedback_default_subject_bug);
        }
    }

    public String formatBody(int type, String messageInput, String stepsInput,
                             boolean attachDiagnostics, String diagnosticInfo) {
        StringBuilder sb = new StringBuilder();

        switch (type) {
            case TYPE_BUG:
                sb.append("[Тип: Ошибка]\n\n");
                break;
            case TYPE_FEATURE:
                sb.append("[Тип: Предложение]\n\n");
                break;
            case TYPE_REVIEW:
                sb.append("[Тип: Отзыв (").append(getRatingVal()).append("/5)]\n\n");
                break;
        }

        sb.append(messageInput.trim());

        if (type == TYPE_BUG && stepsInput != null && !stepsInput.trim().isEmpty()) {
            sb.append("\n\nШаги для воспроизведения:\n");
            sb.append(stepsInput.trim());
        }

        if (attachDiagnostics && diagnosticInfo != null && !diagnosticInfo.trim().isEmpty()) {
            sb.append("\n\n---\nДиагностические данные:\n");
            sb.append(diagnosticInfo.trim());
        }

        return sb.toString();
    }

    private int getSelectedTypeVal() {
        Integer val = selectedTypeLiveData.getValue();
        return val != null ? val : TYPE_BUG;
    }

    private int getRatingVal() {
        Integer val = ratingLiveData.getValue();
        return val != null ? val : 5;
    }
}
