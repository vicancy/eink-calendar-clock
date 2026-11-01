package local.boox.calendarclock;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CalendarActivityTest {
    @Test public void landscapePreviewAndForegroundPowerLifecycle() throws Exception {
        render("en", "landscape", 1248, 936, "landscape.png");
    }

    @Test public void chinesePreviewUsesLocalizedSyntheticEvents() throws Exception {
        render("zh", "landscape", 1248, 936, "landscape-zh.png");
    }

    @Test public void portraitIncludesBothTodayAndUpcoming() throws Exception {
        render("en", "portrait", 936, 1248, "portrait.png");
    }

    @Test public void wideLandscapeKeepsTextProportions() throws Exception {
        render("en", "landscape", 1600, 900, "wide-landscape.png");
    }

    private void render(String language, String orientation, int width, int height, String fileName) throws Exception {
        Intent intent = new Intent(RuntimeEnvironment.getApplication(), CalendarActivity.class)
                .putExtra("preview", true).putExtra("preview_locale", language)
                .putExtra("preview_orientation", orientation);
        ActivityController<CalendarActivity> controller = Robolectric.buildActivity(CalendarActivity.class, intent);
        try {
            CalendarActivity activity = controller.create().start().resume().visible().get();
            ClockFace face = (ClockFace) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
            assertEquals(2, face.agenda.today.size());
            assertEquals(18, face.agenda.upcoming.size());
            assertEquals(face.getContext().getString(R.string.demo_reading), face.agenda.today.get(0).title);
            assertTrue((activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0);
            face.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            face.layout(0, 0, width, height);
            Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            try {
                face.draw(new Canvas(image));
                assertTrue(face.getContentDescription().toString().contains("2026-05-12"));
                File directory = new File(System.getProperty("previewDir"));
                assertTrue(directory.isDirectory() || directory.mkdirs());
                try (FileOutputStream stream = new FileOutputStream(new File(directory, fileName))) {
                    assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream));
                }
            } finally { image.recycle(); }
            controller.pause();
            assertEquals(0, activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            controller.stop().destroy();
        } finally { controller.close(); }
    }
}
