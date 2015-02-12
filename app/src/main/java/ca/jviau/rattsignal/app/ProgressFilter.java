package ca.jviau.rattsignal.app;

import android.app.Activity;
import android.widget.ProgressBar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;

/**
 * @author Jacob
 * @version 1.0
 * @since 2015-02-10
 */
public class ProgressFilter implements ServiceFilter {

    private final Activity mActivity;
    private final ProgressBar mProgressBar;

    public ProgressFilter(Activity activity, ProgressBar progressBar) {
        assert activity != null;

        this.mActivity = activity;
        this.mProgressBar = progressBar;
    }

    @Override
    public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback next) {

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressBar != null) {
                    mProgressBar.setVisibility(ProgressBar.VISIBLE);
                }
            }
        });

        SettableFuture<ServiceFilterResponse> result = SettableFuture.create();
        try {
            ServiceFilterResponse response = next.onNext(request).get();
            result.set(response);
        } catch (Exception e) {
            result.setException(e);
        }

        dismissProgressBar();
        return result;
    }

    private void dismissProgressBar() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressBar != null) {
                    mProgressBar.setVisibility(ProgressBar.GONE);
                }
            }
        });
    }
}
