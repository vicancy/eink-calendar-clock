package local.boox.calendarclock;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class CalendarActivity extends Activity {
    private static final int CALENDAR_PERMISSION = 10;
    private static final long RECHECK_MS = 15 * 60_000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ClockFace face;
    private SharedPreferences preferences;
    private boolean resumed, loading, pending, observing, demo;
    private long lastLoad;
    private final Runnable changed = this::load;
    private final Runnable minute = this::tick;
    private final ContentObserver observer = new ContentObserver(handler) {
        @Override public void onChange(boolean selfChange) {
            if (resumed) { handler.removeCallbacks(changed); handler.postDelayed(changed, 500); }
        }
    };
    private final BroadcastReceiver timeChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (resumed) { load(); scheduleMinute(); }
        }
    };

    @Override public void onCreate(Bundle savedState) {
        // Debug-only synthetic previews never read or modify device calendars.
        demo = BuildConfig.DEBUG && getIntent().getBooleanExtra("preview", false);
        super.onCreate(savedState);
        preferences = getSharedPreferences("clock", MODE_PRIVATE);
        applyOrientation(demo ? getIntent().getStringExtra("preview_orientation")
                : preferences.getString("orientation", "landscape"));
        Context faceContext = this;
        if (demo && getIntent().hasExtra("preview_locale")) {
            Configuration override = new Configuration(getResources().getConfiguration());
            override.setLocale(Locale.forLanguageTag(getIntent().getStringExtra("preview_locale")));
            faceContext = createConfigurationContext(override);
        }
        face = new ClockFace(faceContext);
        setContentView(face);
        face.setOnClickListener(view -> menu());
        IntentFilter timeFilter = new IntentFilter();
        timeFilter.addAction(Intent.ACTION_TIME_CHANGED);
        timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(timeChanged, timeFilter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(timeChanged, timeFilter);
        if (!demo && !hasCalendarPermission() && !preferences.getBoolean("permissionAsked", false)) askPermission();
    }

    private boolean hasCalendarPermission() {
        return checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    private void askPermission() {
        preferences.edit().putBoolean("permissionAsked", true).apply();
        requestPermissions(new String[]{Manifest.permission.READ_CALENDAR}, CALENDAR_PERMISSION);
    }

    private void observeIfAllowed() {
        if (!demo && hasCalendarPermission() && !observing) {
            getContentResolver().registerContentObserver(CalendarContract.CONTENT_URI, true, observer);
            observing = true;
        }
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        immersive();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        observeIfAllowed();
        load();
        scheduleMinute();
    }

    @Override protected void onPause() {
        resumed = false;
        handler.removeCallbacks(minute);
        handler.removeCallbacks(changed);
        if (observing) { getContentResolver().unregisterContentObserver(observer); observing = false; }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        unregisterReceiver(timeChanged);
        super.onDestroy();
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) immersive();
    }

    @SuppressWarnings("deprecation")
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void scheduleMinute() {
        handler.removeCallbacks(minute);
        if (resumed) handler.postDelayed(minute, 60_000 - System.currentTimeMillis() % 60_000 + 100);
    }

    private void tick() {
        if (!resumed) return;
        face.invalidate();
        if (!demo && (!face.agenda.window.today.equals(LocalDate.now())
                || !face.agenda.window.zone.equals(ZoneId.systemDefault())
                || SystemClock.elapsedRealtime() - lastLoad >= RECHECK_MS)) load();
        scheduleMinute();
    }

    private void load() {
        if (!resumed) return;
        if (demo) { face.show(DemoAgenda.create(face.getContext()), DemoAgenda.NOW); return; }
        if (loading) { pending = true; return; }
        loading = true;
        Set<String> excluded = new HashSet<>(preferences.getStringSet("excludedCalendars", new HashSet<>()));
        CalendarRepository.load(this, excluded, agenda -> {
            loading = false;
            if (isDestroyed() || !resumed) return;
            lastLoad = SystemClock.elapsedRealtime();
            face.show(agenda, null);
            if (pending) { pending = false; load(); }
        });
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == CALENDAR_PERMISSION && resumed) { observeIfAllowed(); load(); }
    }

    private void menu() {
        String[] choices = {getString(R.string.choose_calendars), getString(R.string.sync_setup),
                getString(R.string.orientation), getString(R.string.about), getString(R.string.exit)};
        new AlertDialog.Builder(this).setTitle(R.string.app_name).setItems(choices, (dialog, which) -> {
            switch (which) {
                case 0: chooseCalendars(); break;
                case 1: syncSetup(); break;
                case 2: chooseOrientation(); break;
                case 3: new AlertDialog.Builder(this).setTitle(R.string.about).setMessage(R.string.about_body)
                        .setPositiveButton(R.string.done, null).show(); break;
                case 4: finish(); break;
                default: break;
            }
        }).setNegativeButton(R.string.keep_displaying, null).show();
    }

    private void chooseCalendars() {
        if (!hasCalendarPermission()) {
            new AlertDialog.Builder(this).setMessage(R.string.permission_needed)
                    .setPositiveButton(R.string.grant_access, (d, w) -> askPermission())
                    .setNeutralButton(R.string.app_settings, (d, w) -> startActivity(new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))))
                    .setNegativeButton(R.string.cancel, null).show();
            return;
        }
        if (face.agenda.calendars.isEmpty()) {
            new AlertDialog.Builder(this).setMessage(R.string.no_calendars)
                    .setPositiveButton(R.string.sync_setup, (d, w) -> syncSetup()).show();
            return;
        }
        // Capture the displayed list while asynchronous calendar refreshes continue.
        java.util.List<Agenda.Calendar> calendars = new java.util.ArrayList<>(face.agenda.calendars);
        Set<String> excluded = new HashSet<>(preferences.getStringSet("excludedCalendars", new HashSet<>()));
        String[] names = new String[calendars.size()];
        boolean[] checked = new boolean[calendars.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = calendars.get(i).name;
            checked[i] = !excluded.contains(calendars.get(i).id);
        }
        new AlertDialog.Builder(this).setTitle(R.string.choose_calendars)
                .setMultiChoiceItems(names, checked, (d, index, selected) -> {
                    if (selected) excluded.remove(calendars.get(index).id); else excluded.add(calendars.get(index).id);
                }).setPositiveButton(R.string.done, (d, w) -> {
                    preferences.edit().putStringSet("excludedCalendars", excluded).apply(); load();
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void syncSetup() {
        new AlertDialog.Builder(this).setTitle(R.string.sync_setup).setMessage(R.string.sync_help)
                .setPositiveButton(R.string.open_davx, (d, w) -> openSyncApp("at.bitfire.davdroid"))
                .setNeutralButton(R.string.open_outlook, (d, w) -> openSyncApp("com.microsoft.office.outlook"))
                .setNegativeButton(R.string.account_settings, (d, w) -> startActivity(new Intent(Settings.ACTION_SYNC_SETTINGS)))
                .show();
    }

    private void openSyncApp(String packageName) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch == null) Toast.makeText(this, R.string.no_sync_app, Toast.LENGTH_LONG).show();
        else startActivity(launch);
    }

    private void chooseOrientation() {
        String[] keys = {"auto", "landscape", "portrait"};
        String[] labels = {getString(R.string.system_orientation), getString(R.string.landscape), getString(R.string.portrait)};
        String current = preferences.getString("orientation", "landscape");
        int selected = current.equals("auto") ? 0 : current.equals("portrait") ? 2 : 1;
        new AlertDialog.Builder(this).setTitle(R.string.orientation).setSingleChoiceItems(labels, selected, (d, index) -> {
            preferences.edit().putString("orientation", keys[index]).apply();
            d.dismiss(); applyOrientation(keys[index]);
        }).setNegativeButton(R.string.cancel, null).show();
    }

    private void applyOrientation(String orientation) {
        setRequestedOrientation("portrait".equals(orientation) ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                : "auto".equals(orientation) ? ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }
}
