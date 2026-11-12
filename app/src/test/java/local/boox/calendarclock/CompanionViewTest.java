package local.boox.calendarclock;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Looper;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.time.Duration;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CompanionViewTest {
    @Test public void animationFinishesAndPauseLeavesNoContinuingMotion() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            CompanionView pets = new CompanionView(controller.get());
            controller.get().setContentView(pets);
            pets.setArea(new Rect(16, 16, 400, 160));
            Bitmap rest = render(pets);
            assertEquals(Color.TRANSPARENT, rest.getPixel(0, 0));
            pets.start();
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6));
            Bitmap moving = render(pets);
            assertFalse(rest.sameAs(moving));
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(24));
            Bitmap ending = render(pets);
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(60));
            Bitmap held = render(pets);
            assertTrue(ending.sameAs(held));
            pets.stop();
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(30));
            Bitmap stopped = render(pets);
            assertTrue(rest.sameAs(stopped));
            rest.recycle(); moving.recycle(); ending.recycle(); held.recycle(); stopped.recycle();
        }
    }

    @Test public void atlasIsMonochromeAndContainsEveryFrame() {
        Bitmap atlas = BitmapFactory.decodeResource(RuntimeEnvironment.getApplication().getResources(), R.drawable.companion_sprites);
        assertEquals(1024, atlas.getWidth());
        for (CompanionSchedule.Scene scene : CompanionSchedule.Scene.values()) {
            int last = scene.firstTile + scene.frames - 1;
            assertTrue((last / 8 + 1) * 48 <= atlas.getHeight());
        }
        int[] pixels = new int[atlas.getWidth() * atlas.getHeight()];
        atlas.getPixels(pixels, 0, atlas.getWidth(), 0, 0, atlas.getWidth(), atlas.getHeight());
        for (int pixel : pixels) assertTrue(pixel == Color.BLACK || pixel == Color.WHITE);
        atlas.recycle();
    }

    private Bitmap render(CompanionView pets) {
        pets.measure(View.MeasureSpec.makeMeasureSpec(512, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY));
        pets.layout(0, 0, 512, 240);
        Bitmap bitmap = Bitmap.createBitmap(512, 240, Bitmap.Config.ARGB_8888);
        pets.draw(new Canvas(bitmap));
        return bitmap;
    }
}
