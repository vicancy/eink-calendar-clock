package local.boox.calendarclock;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

final class Agenda {
    final List<Event> today = new ArrayList<>();
    final List<Event> upcoming = new ArrayList<>();
    final List<Calendar> calendars = new ArrayList<>();
    final CalendarWindow window;
    int selectedCalendarCount;
    int errorMessage;

    Agenda(LocalDate day, ZoneId zone) {
        window = new CalendarWindow(day, zone);
    }

    static final class Calendar {
        final String id;
        final String name;

        Calendar(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static final class Event {
        final String title;
        final String source;
        final long begin;
        final long end;
        final boolean allDay;

        Event(String title, String source, long begin, long end, boolean allDay) {
            this.title = title;
            this.source = source;
            this.begin = begin;
            this.end = end;
            this.allDay = allDay;
        }

        boolean hasEnded(ZonedDateTime now) {
            return allDay ? !CalendarWindow.utcDate(end).isAfter(now.toLocalDate())
                    : end <= now.toInstant().toEpochMilli();
        }

        LocalDate startDate(ZoneId zone) {
            return allDay ? CalendarWindow.utcDate(begin)
                    : Instant.ofEpochMilli(begin).atZone(zone).toLocalDate();
        }
    }
}
