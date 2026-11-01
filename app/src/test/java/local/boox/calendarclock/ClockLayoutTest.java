package local.boox.calendarclock;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClockLayoutTest {
    @Test public void tabletAndWideLandscapePanelsStayInsideScreenWithoutOverlap() {
        for (float width : new float[]{1872, 2246, 2496}) {
            ClockLayout layout = new ClockLayout(width, 1404);
            assertTrue(layout.landscape);
            assertTrue(layout.date.right < layout.today.left);
            assertTrue(layout.today.right < layout.upcoming.left);
            inside(layout.date, width, 1404);
            inside(layout.today, width, 1404);
            inside(layout.upcoming, width, 1404);
        }
    }

    @Test public void portraitTabletAndPhoneHaveAllThreeSections() {
        for (float height : new float[]{1872, 2496, 3120}) {
            ClockLayout layout = new ClockLayout(1404, height);
            assertFalse(layout.landscape);
            assertTrue(layout.date.bottom < layout.today.top);
            assertTrue(layout.today.bottom < layout.upcoming.top);
            inside(layout.date, 1404, height);
            inside(layout.today, 1404, height);
            inside(layout.upcoming, 1404, height);
        }
    }

    private static void inside(ClockLayout.Box box, float width, float height) {
        assertTrue(box.width() > 200);
        assertTrue(box.height() > 200);
        assertTrue(box.left >= 0 && box.top >= 0 && box.right <= width && box.bottom <= height);
    }
}
