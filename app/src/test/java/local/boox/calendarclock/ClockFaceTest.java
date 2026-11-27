package local.boox.calendarclock;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.*;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ClockFaceTest {
    @Test public void longPortraitTitlesLeaveSpaceForCalendarSources() throws Exception {
        Agenda agenda = new Agenda(DemoAgenda.NOW.toLocalDate(), DemoAgenda.NOW.getZone());
        agenda.selectedCalendarCount = 1;
        long begin = DemoAgenda.NOW.withHour(11).toInstant().toEpochMilli();
        for (int i = 0; i < 3; i++) agenda.today.add(new Agenda.Event(
                "Community workshop with friends at the neighbourhood library and community learning centre, "
                        + "including a guided tour, practical activities, group discussion and a question and answer session",
                "SAMPLE CALENDAR SOURCE", begin + i * 3_600_000L, begin + (i + 1) * 3_600_000L, false));
        ClockFace face = new ClockFace(RuntimeEnvironment.getApplication());
        face.show(agenda, DemoAgenda.NOW);
        face.measure(View.MeasureSpec.makeMeasureSpec(1404, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2600, View.MeasureSpec.EXACTLY));
        face.layout(0, 0, 1404, 2600);
        Bitmap image = Bitmap.createBitmap(1404, 2600, Bitmap.Config.ARGB_8888);
        try {
            face.draw(new Canvas(image));
            File directory = new File(System.getProperty("previewDir"));
            assertTrue(directory.isDirectory() || directory.mkdirs());
            try (FileOutputStream stream = new FileOutputStream(new File(directory, "long-titles-portrait.png"))) {
                assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream));
            }
            // Each source label must remain visible with a clear band above it.
            for (int sourceTop : new int[]{1017, 1292, 1568}) {
                assertFalse("Title overlaps source at y=" + sourceTop,
                        hasInk(image, 82, 1348, sourceTop - 10, sourceTop));
                assertTrue("Missing source at y=" + sourceTop,
                        hasInk(image, 82, 1348, sourceTop, sourceTop + 32));
            }
        } finally { image.recycle(); }
    }

    private static boolean hasInk(Bitmap image, int left, int right, int top, int bottom) {
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                if (image.getPixel(x, y) != Color.WHITE) return true;
            }
        }
        return false;
    }
}
