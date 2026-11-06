package playtime.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

// Auto-start without adb. While this service is switched on (Settings → Accessibility), Android lets the
// app open activities from the background, so the kiosk can start at boot and come back on wake.
// It doesn't read the screen or listen to any events.
public class AutoStartService extends AccessibilityService {
    @Override protected void onServiceConnected() {
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}

    @Override public void onInterrupt() {}
}
