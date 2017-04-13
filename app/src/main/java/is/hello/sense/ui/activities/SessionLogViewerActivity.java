package is.hello.sense.ui.activities;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.ui.activities.appcompat.SenseActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.SessionLogger;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class SessionLogViewerActivity extends SenseActivity {
    private WebView webView;
    private ProgressBar activityIndicator;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_log_viewer);

        this.webView = (WebView) findViewById(R.id.activity_session_log_viewer_web_view);
        webView.setWebViewClient(new Client());

        this.activityIndicator = (ProgressBar) findViewById(R.id.activity_session_log_viewer_activity);

        if (savedInstanceState == null) {
            reload();
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.destroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_session_log_viewer, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.action_reload) {
            reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void reload() {
        final String logFilePath = SessionLogger.getLogFilePath(this);
        final String url = "file://" + Uri.encode(logFilePath, "/.");
        webView.loadUrl(url);
    }


    private class Client extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            animatorFor(activityIndicator)
                    .fadeIn()
                    .start();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            animatorFor(activityIndicator)
                    .fadeOut(View.GONE)
                    .start();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            animatorFor(activityIndicator)
                    .fadeOut(View.GONE)
                    .start();

            ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder()
                    .withMessage(StringRef.from(description))
                    .withContextInfo(failingUrl)
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        }
    }
}
