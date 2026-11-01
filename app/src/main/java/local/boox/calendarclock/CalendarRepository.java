package local.boox.calendarclock;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.util.Log;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class CalendarRepository {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    interface Callback { void ready(Agenda agenda); }

    static void load(Context context, Set<String> excluded, Callback callback) {
        Context app = context.getApplicationContext();
        WORKER.execute(() -> {
            Agenda result = read(app, excluded);
            MAIN.post(() -> callback.ready(result));
        });
    }

    private static Agenda read(Context context, Set<String> excluded) {
        Agenda result = new Agenda(LocalDate.now(), ZoneId.systemDefault());
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            result.errorMessage = R.string.permission_needed;
            return result;
        }
        try {
            try (Cursor cursor = context.getContentResolver().query(
                    CalendarContract.Calendars.CONTENT_URI,
                    new String[]{"_id", "calendar_displayName", "account_name"},
                    "visible=1", null, "calendar_displayName ASC")) {
                while (cursor != null && cursor.moveToNext()) {
                    String id = cursor.getString(0);
                    String name = cursor.getString(1);
                    if (name == null) name = context.getString(R.string.calendar);
                    String account = cursor.getString(2);
                    result.calendars.add(new Agenda.Calendar(id,
                            account == null || account.isEmpty() ? name : name + " · " + account));
                    if (!excluded.contains(id)) result.selectedCalendarCount++;
                }
            }
            StringBuilder selection = new StringBuilder("visible=1 AND (eventStatus IS NULL OR eventStatus!=2)");
            List<String> args = new ArrayList<>();
            for (String id : excluded) {
                selection.append(" AND calendar_id!=?");
                args.add(id);
            }
            CalendarWindow window = result.window;
            // Expand slightly for all-day UTC timestamps in either hemisphere.
            long from = window.today.minusDays(1).atStartOfDay(window.zone).toInstant().toEpochMilli();
            long to = window.until.plusDays(1).atStartOfDay(window.zone).toInstant().toEpochMilli();
            Uri.Builder uri = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(uri, from);
            ContentUris.appendId(uri, to);
            try (Cursor cursor = context.getContentResolver().query(uri.build(),
                    new String[]{"title", "calendar_displayName", "begin", "end", "allDay"},
                    selection.toString(), args.toArray(new String[0]), "begin ASC, end ASC, title ASC")) {
                while (cursor != null && cursor.moveToNext()) {
                    String title = cursor.getString(0);
                    Agenda.Event event = new Agenda.Event(
                            title == null || title.trim().isEmpty() ? context.getString(R.string.untitled) : title,
                            cursor.getString(1), cursor.getLong(2), cursor.getLong(3), cursor.getInt(4) != 0);
                    if (window.isToday(event)) result.today.add(event);
                    if (window.isUpcoming(event)) result.upcoming.add(event);
                }
            }
        } catch (RuntimeException error) {
            // Calendar contents and account details never go to logs.
            Log.w("CalendarClock", "Calendar provider query failed: " + error.getClass().getSimpleName());
            result.errorMessage = R.string.read_failed;
        }
        return result;
    }
}
