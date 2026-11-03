package space.tarung.lounge;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    static final String URL = "https://playtime.tarung.space/d/lounge";
    static final long RETRY_MS = 10_000;
    static final String BG = "#0b0d12";

    private WebView web;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable reload = new Runnable() {
        @Override public void run() { web.loadUrl(URL); }
    };

    // Bring the kiosk back to the front when the TV wakes from standby.
    private final BroadcastReceiver screenOn = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            startActivity(new Intent(c, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor(BG));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) showWaitingAndRetry();
            }

            @Override public void onReceivedHttpError(WebView v, WebResourceRequest r, WebResourceResponse res) {
                if (r.isForMainFrame() && res.getStatusCode() >= 500) showWaitingAndRetry();
            }

            @Override public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) {
                recreate();
                return true;
            }
        });

        setContentView(web);
        web.loadUrl(URL);
        registerReceiver(screenOn, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    // Network is often not up yet right after boot: show a blank screen and keep retrying.
    private void showWaitingAndRetry() {
        web.loadDataWithBaseURL(null, "<body style='background:" + BG + "'></body>", "text/html", "utf-8", null);
        handler.removeCallbacks(reload);
        handler.postDelayed(reload, RETRY_MS);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    // Kiosk: Back never leaves the page. Use the remote's Home button to exit.
    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
    }

    @Override protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        unregisterReceiver(screenOn);
        handler.removeCallbacks(reload);
        web.destroy();
        super.onDestroy();
    }
}
