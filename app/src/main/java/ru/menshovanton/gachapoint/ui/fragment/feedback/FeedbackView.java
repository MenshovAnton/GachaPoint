package ru.menshovanton.gachapoint.ui.fragment.feedback;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import ru.menshovanton.gachapoint.R;

public class FeedbackView extends Fragment {

    private static final String FORM_URL =
            "https://docs.google.com/forms/d/e/1FAIpQLSdURkqnmX6zi5o26U5C4AUQ-MV-3MWY4UQRAsxuS1Y0hve1xg/viewform?embedded=true";

    private WebView webView;
    private ProgressBar progressBar;

    private FeedbackViewModel viewModel;

    public FeedbackView() {}

    public static FeedbackView newInstance() {
        return new FeedbackView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feedback, container, false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(FeedbackViewModel.class);

        webView = view.findViewById(R.id.webViewFeedback);
        progressBar = view.findViewById(R.id.progressBarFeedback);

        observeViewModel();
        initWebView();
        setupCallbacks();
    }

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroyView();
    }

    private void observeViewModel() {
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), unused -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.form_load_error), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getPopBackStack().observe(getViewLifecycleOwner(), unused -> {
            if (isAdded()) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void setupCallbacks() {
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (webView != null && webView.canGoBack()) {
                            webView.goBack();
                        } else {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                }
        );
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(false);

        int nightMode = requireContext().getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNight = (nightMode == Configuration.UI_MODE_NIGHT_YES);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webSettings, isNight);
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            int forceDarkStrategy = isNight ? WebSettingsCompat.FORCE_DARK_ON : WebSettingsCompat.FORCE_DARK_OFF;
            WebSettingsCompat.setForceDark(webSettings, forceDarkStrategy);
        }

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar == null) return;

                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (view != null) {
                    view.stopLoading();
                    view.destroy();
                }


                if (isAdded()) {
                    viewModel.sendErrorMessage();
                    viewModel.onBackTriggered();
                }

                return true;
            }
        });

        webView.loadUrl(FORM_URL);
    }
}