package local.boox.calendarclock;

/** Logical coordinates, uniformly scaled by the view to avoid stretched text. */
final class ClockLayout {
    static final class Box {
        final float left, top, right, bottom;
        Box(float left, float top, float right, float bottom) {
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        }
        float width() { return right - left; }
        float height() { return bottom - top; }
    }

    final boolean landscape;
    final Box date, today, upcoming;

    ClockLayout(float width, float height) {
        landscape = width >= height;
        if (landscape) {
            date = new Box(56, 84, width * .30f - 40, height - 84);
            today = new Box(width * .34f, 84, width * .76f, height - 84);
            upcoming = new Box(width * .80f, 84, width - 56, height - 84);
        } else {
            float header = Math.min(600, height * .29f);
            float split = header + (height - header) * .60f;
            date = new Box(56, 60, width - 56, header);
            today = new Box(56, header + 40, width - 56, split);
            upcoming = new Box(56, split + 56, width - 56, height - 56);
        }
    }
}
