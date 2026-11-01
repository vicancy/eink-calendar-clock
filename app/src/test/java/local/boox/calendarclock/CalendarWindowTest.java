package local.boox.calendarclock;

import org.junit.Test;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import static org.junit.Assert.*;

public class CalendarWindowTest {
    private static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");
    private static long ms(String value) { return OffsetDateTime.parse(value).toInstant().toEpochMilli(); }
    private static Agenda.Event event(String begin, String end, boolean allDay) {
        return new Agenda.Event("Example", "Calendar", ms(begin), ms(end), allDay);
    }

    @Test public void todayAndUpcomingDoNotOverlapAtMidnight() {
        CalendarWindow window = new CalendarWindow(LocalDate.of(2026, 11, 1), SYDNEY);
        Agenda.Event tomorrow = event("2026-11-02T00:00+11:00", "2026-11-02T01:00+11:00", false);
        assertFalse(window.isToday(tomorrow));
        assertTrue(window.isUpcoming(tomorrow));
        assertFalse(window.isToday(event("2026-10-31T23:00+11:00", "2026-11-01T00:00+11:00", false)));
    }

    @Test public void overnightEventAppearsInBothIntersectingWindows() {
        CalendarWindow window = new CalendarWindow(LocalDate.of(2026, 11, 1), SYDNEY);
        Agenda.Event overnight = event("2026-11-01T23:00+11:00", "2026-11-02T01:00+11:00", false);
        assertTrue(window.isToday(overnight));
        assertTrue(window.isUpcoming(overnight));
    }

    @Test public void allDayDatesStayInUtcEvenWestOfGreenwich() {
        CalendarWindow window = new CalendarWindow(LocalDate.of(2026, 11, 1), ZoneId.of("America/Los_Angeles"));
        Agenda.Event allDay = event("2026-11-02T00:00Z", "2026-11-03T00:00Z", true);
        assertFalse(window.isToday(allDay));
        assertTrue(window.isUpcoming(allDay));
    }

    @Test public void daylightSavingDayUsesLocalMidnightsRatherThan24Hours() {
        CalendarWindow window = new CalendarWindow(LocalDate.of(2026, 10, 4), SYDNEY);
        assertTrue(window.isToday(event("2026-10-04T23:30+11:00", "2026-10-04T23:55+11:00", false)));
        assertFalse(window.isToday(event("2026-10-05T00:00+11:00", "2026-10-05T01:00+11:00", false)));
    }

    @Test public void futureRangeIncludesMoreThanOneWeekButExcludesItsEnd() {
        CalendarWindow window = new CalendarWindow(LocalDate.of(2026, 11, 1), SYDNEY);
        assertTrue(window.isUpcoming(event("2027-05-04T12:00+10:00", "2027-05-04T13:00+10:00", false)));
        assertFalse(window.isUpcoming(event("2027-11-02T00:00+11:00", "2027-11-02T01:00+11:00", false)));
    }

    @Test public void pointEventsAppearOnTheirStartDayAndInvalidRangesDoNot() {
        CalendarWindow window = new CalendarWindow(LocalDate.of(2026, 11, 1), SYDNEY);
        assertTrue(window.isToday(event("2026-11-01T12:00+11:00", "2026-11-01T12:00+11:00", false)));
        assertFalse(window.isToday(event("2026-11-01T12:00+11:00", "2026-11-01T11:00+11:00", false)));
    }

    @Test public void allDayEventDoesNotFadeBeforeLocalDayEnds() {
        Agenda.Event allDay = event("2026-11-02T00:00Z", "2026-11-03T00:00Z", true);
        assertFalse(allDay.hasEnded(ZonedDateTime.parse("2026-11-02T23:00-08:00[America/Los_Angeles]")));
        assertTrue(allDay.hasEnded(ZonedDateTime.parse("2026-11-03T00:00-08:00[America/Los_Angeles]")));
    }
}
