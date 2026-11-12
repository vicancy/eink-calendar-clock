package local.boox.calendarclock;

import org.junit.Test;
import java.util.EnumSet;
import java.util.Random;
import static org.junit.Assert.*;

public class CompanionScheduleTest {
    @Test public void daytimeVariesActivitiesWithoutConsecutiveRepeats() {
        CompanionSchedule schedule = new CompanionSchedule(new Random(5));
        EnumSet<CompanionSchedule.Scene> seen = EnumSet.noneOf(CompanionSchedule.Scene.class);
        CompanionSchedule.Scene previous = null;
        int rests = 0, games = 0;
        for (int i = 0; i < 2000; i++) {
            CompanionSchedule.Scene scene = schedule.next(12, false);
            if (scene != CompanionSchedule.Scene.REST) assertNotEquals(previous, scene);
            if (scene == CompanionSchedule.Scene.REST) rests++;
            if (scene == CompanionSchedule.Scene.BALL || scene == CompanionSchedule.Scene.PLAY) games++;
            previous = scene;
            seen.add(scene);
            long delay = schedule.restMillis();
            assertTrue(delay >= 300_000 && delay <= 600_000);
        }
        assertEquals(EnumSet.allOf(CompanionSchedule.Scene.class), seen);
        assertTrue(rests > games);
    }

    @Test public void eveningFavoursBooksAndNightStaysQuiet() {
        CompanionSchedule schedule = new CompanionSchedule(new Random(6));
        int books = 0, games = 0;
        for (int i = 0; i < 2000; i++) {
            CompanionSchedule.Scene scene = schedule.next(20, false);
            if (scene == CompanionSchedule.Scene.READ) books++;
            if (scene == CompanionSchedule.Scene.BALL || scene == CompanionSchedule.Scene.PLAY) games++;
        }
        assertTrue(books > games);
        int rests = 0, reads = 0;
        for (int i = 0; i < 2000; i++) {
            CompanionSchedule.Scene scene = schedule.next(i % 2 == 0 ? 23 : 6, false);
            assertTrue(scene == CompanionSchedule.Scene.REST || scene == CompanionSchedule.Scene.READ);
            if (scene == CompanionSchedule.Scene.REST) rests++; else reads++;
        }
        assertTrue(rests > reads);
        assertTrue(reads > 0);
    }

    @Test public void openingGivesAnActivityAppropriateToTheHour() {
        for (int hour = 0; hour < 24; hour++) {
            CompanionSchedule schedule = new CompanionSchedule(new Random(hour));
            CompanionSchedule.Scene opening = schedule.next(hour, true);
            assertNotEquals(CompanionSchedule.Scene.REST, opening);
            if (hour < 7 || hour >= 22) assertEquals(CompanionSchedule.Scene.READ, opening);
        }
    }
}
