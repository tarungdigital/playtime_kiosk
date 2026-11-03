package playtime.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    static final String BASE_URL = "https://playtime.tarung.space/d/";
    static final String DEFAULT_STATION = "lounge";
    static final String PREF_STATION = "station";
    static final long RETRY_MS = 10_000;
    static final String BG = "#0b0d12";
    // Pressing OK (or tapping) this many times within the window opens settings.
    static final int SETTINGS_TAPS = 5;
    static final long SETTINGS_WINDOW_MS = 3_000;

    private WebView web;
    private SharedPreferences prefs;
    private int taps;
    private long firstTapAt;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable reload = new Runnable() {
        @Override public void run() { web.loadUrl(url()); }
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
        prefs = getSharedPreferences("kiosk", MODE_PRIVATE);

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
        web.loadUrl(url());
        registerReceiver(screenOn, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    private String station() {
        return prefs.getString(PREF_STATION, DEFAULT_STATION);
    }

    private String url() {
        return BASE_URL + Uri.encode(station());
    }

    // Network is often not up yet right after boot: show a blank screen and keep retrying.
    private void showWaitingAndRetry() {
        web.loadDataWithBaseURL(null, "<body style='background:" + BG + "'></body>", "text/html", "utf-8", null);
        handler.removeCallbacks(reload);
        handler.postDelayed(reload, RETRY_MS);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_UP && isSelectKey(e.getKeyCode())) countTap();
        return super.dispatchKeyEvent(e);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP) countTap();
        return super.dispatchTouchEvent(e);
    }

    private static boolean isSelectKey(int code) {
        return code == KeyEvent.KEYCODE_DPAD_CENTER
                || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    private void countTap() {
        long now = SystemClock.uptimeMillis();
        if (now - firstTapAt > SETTINGS_WINDOW_MS) {
            taps = 0;
            firstTapAt = now;
        }
        if (++taps == SETTINGS_TAPS) {
            taps = 0;
            showSettings();
        }
    }

    private void showSettings() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setHint(DEFAULT_STATION);
        input.setText(station());
        input.setSelectAllOnFocus(true);

        FrameLayout box = new FrameLayout(this);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);

        final AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Station")
                .setMessage(BASE_URL + "{station}")
                .setView(box)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        String station = parseStation(input.getText().toString());
                        if (station.isEmpty()) return;
                        prefs.edit().putString(PREF_STATION, station).apply();
                        handler.removeCallbacks(reload);
                        web.loadUrl(url());
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent e) {
                if (actionId != EditorInfo.IME_ACTION_DONE) return false;
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
        });
        dialog.show();
    }

    // Accepts "lobby", "/d/lobby/" or a full link, keeping only the last path segment.
    static String parseStation(String text) {
        String s = text.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s.substring(s.lastIndexOf('/') + 1);
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
