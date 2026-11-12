package local.boox.calendarclock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import java.time.LocalTime;
import java.util.Random;

/** Decorative overlay: only the pet rectangle is invalidated during a short scene. */
final class CompanionView extends View {
    private static final int TILE_WIDTH = 128, TILE_HEIGHT = 48, COLUMNS = 8;
    private static final long FRAME_MS = 500;
    private final Bitmap sprites;
    private final Paint paint = new Paint();
    private final Rect source = new Rect(), destination = new Rect();
    private final CompanionSchedule schedule = new CompanionSchedule(new Random());
    private final Runnable advance = this::advance;
    private CompanionSchedule.Scene scene = CompanionSchedule.Scene.REST;
    private int frame;
    private boolean active, animating, opening = true;

    CompanionView(Context context) {
        super(context);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        sprites = BitmapFactory.decodeResource(getResources(), R.drawable.companion_sprites, options);
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
        setFocusable(false);
    }

    void setArea(Rect area) {
        Rect previous = new Rect(destination);
        int scale = Math.min(area.width() / TILE_WIDTH, area.height() / TILE_HEIGHT);
        if (scale < 1) destination.setEmpty();
        else {
            int width = TILE_WIDTH * scale, height = TILE_HEIGHT * scale;
            int left = area.left + (area.width() - width) / 2;
            int top = area.top + (area.height() - height) / 2;
            destination.set(left, top, left + width, top + height);
        }
        if (!previous.equals(destination)) {
            invalidate(previous);
            invalidate(destination);
            removeCallbacks(advance);
            if (active && !destination.isEmpty()) postDelayed(advance, animating ? FRAME_MS : opening ? 5_000 : schedule.restMillis());
        }
    }

    void start() {
        if (active) return;
        active = true;
        if (!destination.isEmpty()) postDelayed(advance, opening ? 5_000 : schedule.restMillis());
    }

    void stop() {
        active = false;
        removeCallbacks(advance);
        animating = false;
        scene = CompanionSchedule.Scene.REST;
        frame = 0;
        if (!destination.isEmpty()) invalidate(destination);
    }

    private void advance() {
        if (!active || destination.isEmpty()) return;
        if (animating) {
            frame++;
            invalidate(destination);
            if (frame < scene.frames - 1) postDelayed(advance, FRAME_MS);
            else { animating = false; postDelayed(advance, schedule.restMillis()); }
        } else {
            CompanionSchedule.Scene chosen = schedule.next(LocalTime.now().getHour(), opening);
            opening = false;
            boolean changed = chosen != scene || frame != 0;
            scene = chosen;
            frame = 0;
            if (changed) invalidate(destination);
            animating = scene.frames > 1;
            postDelayed(advance, animating ? FRAME_MS : schedule.restMillis());
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (destination.isEmpty()) return;
        int tile = scene.firstTile + frame;
        int x = tile % COLUMNS * TILE_WIDTH, y = tile / COLUMNS * TILE_HEIGHT;
        source.set(x, y, x + TILE_WIDTH, y + TILE_HEIGHT);
        canvas.drawBitmap(sprites, source, destination, paint);
    }

    @Override protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }
}
