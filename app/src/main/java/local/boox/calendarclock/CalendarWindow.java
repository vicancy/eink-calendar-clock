package local.boox.calendarclock;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Date windows use local midnights; Android all-day events use UTC calendar dates. */
final class CalendarWindow {
    final LocalDate today;
    final LocalDate tomorrow;
    final LocalDate until;
    final ZoneId zone;

    CalendarWindow(LocalDate today, ZoneId zone) {
        this.today = today;
        this.tomorrow = today.plusDays(1);
        this.until = tomorrow.plusYears(1);
        this.zone = zone;
    }

    static boolean overlaps(long begin, long end, boolean allDay,
                            LocalDate first, LocalDate until, ZoneId zone) {
        if (end < begin) return false;
        if (allDay) {
            return utcDate(begin).isBefore(until) && utcDate(end).isAfter(first);
        }
        long start = first.atStartOfDay(zone).toInstant().toEpochMilli();
        long finish = until.atStartOfDay(zone).toInstant().toEpochMilli();
        return begin == end ? begin >= start && begin < finish : begin < finish && end > start;
    }

    static LocalDate utcDate(long value) {
        return Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate();
    }

    boolean isToday(Agenda.Event event) {
        return overlaps(event.begin, event.end, event.allDay, today, tomorrow, zone);
    }

    boolean isUpcoming(Agenda.Event event) {
        return overlaps(event.begin, event.end, event.allDay, tomorrow, until, zone);
    }
}
