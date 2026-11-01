package local.boox.calendarclock;

import android.content.Context;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Synthetic debug previews only: no account access and no device calendar writes. */
final class DemoAgenda {
    static final ZonedDateTime NOW = ZonedDateTime.of(2026, 5, 12, 10, 24, 0, 0, ZoneId.of("UTC"));

    static Agenda create(Context context) {
        Agenda result = new Agenda(NOW.toLocalDate(), NOW.getZone());
        result.selectedCalendarCount = 1;
        String source = context.getString(R.string.demo_source);
        result.today.add(event(context.getString(R.string.demo_reading), source, 0, 9));
        result.today.add(event(context.getString(R.string.demo_walk), source, 0, 18));
        int[] titles = {R.string.demo_workshop, R.string.demo_lunch, R.string.demo_library};
        for (int i = 0; i < 18; i++) result.upcoming.add(event(context.getString(titles[i % titles.length]), source, i + 1, 11 + i % 5));
        return result;
    }

    private static Agenda.Event event(String title, String source, int days, int hour) {
        ZonedDateTime begin = NOW.plusDays(days).withHour(hour).withMinute(0);
        return new Agenda.Event(title, source, begin.toInstant().toEpochMilli(), begin.plusHours(1).toInstant().toEpochMilli(), false);
    }
}
