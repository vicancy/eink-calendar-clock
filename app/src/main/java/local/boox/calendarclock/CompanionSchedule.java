package local.boox.calendarclock;

import java.util.Random;

/** Local, time-of-day scene choices; quiet holds do not require any frame updates. */
final class CompanionSchedule {
    enum Scene {
        REST(0, 1), ALONE(1, 29), BALL(30, 45), PLAY(75, 33), READ(108, 33);

        final int firstTile, frames;
        Scene(int firstTile, int frames) { this.firstTile = firstTile; this.frames = frames; }
    }

    private final Random random;
    private Scene previous = Scene.REST;

    CompanionSchedule(Random random) { this.random = random; }

    Scene next(int hour, boolean opening) {
        // Most of the day is quiet. Evenings favour reading; nights have no rough play.
        int[] weights = hour < 7 || hour >= 22 ? new int[]{80, 0, 0, 0, 20}
                : hour >= 19 ? new int[]{45, 20, 4, 1, 30} : new int[]{40, 30, 10, 5, 15};
        if (opening) weights[Scene.REST.ordinal()] = 0;
        if (previous != Scene.REST) weights[previous.ordinal()] = 0;
        int total = 0;
        for (int weight : weights) total += weight;
        if (total == 0) { previous = Scene.REST; return previous; }
        int choice = random.nextInt(total);
        for (Scene scene : Scene.values()) {
            choice -= weights[scene.ordinal()];
            if (choice < 0) { previous = scene; return scene; }
        }
        throw new AssertionError("Scene weights did not cover the selection");
    }

    long restMillis() { return 300_000L + random.nextInt(300_001); }
}
