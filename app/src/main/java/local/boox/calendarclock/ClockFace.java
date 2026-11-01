package local.boox.calendarclock;

import android.content.Context;
import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowInsets;
import android.view.DisplayCutout;
import android.os.Build;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.TimeZone;

final class ClockFace extends View {
    private static final int INK = Color.BLACK;
    private static final int MUTED = Color.rgb(100, 100, 100);
    private static final int RULE = Color.rgb(204, 204, 204);
    private final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);
    private ZonedDateTime previewTime;
    Agenda agenda = new Agenda(LocalDate.now(), ZoneId.systemDefault());

    ClockFace(Context context) {
        super(context);
        setBackgroundColor(Color.WHITE);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    void show(Agenda value, ZonedDateTime fixedTime) {
        agenda = value;
        previewTime = fixedTime;
        invalidate();
    }

    private Locale locale() { return getResources().getConfiguration().getLocales().get(0); }
    private String s(int resource, Object... args) { return getResources().getString(resource, args); }
    private String q(int resource, int count) { return getResources().getQuantityString(resource, count, count); }
    private String time(long value, ZoneId zone) {
        return Instant.ofEpochMilli(value).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm", locale()));
    }

    private void font(float size, boolean heavy, int color) {
        paint.setTextSize(size); paint.setTypeface(heavy ? bold : regular);
        paint.setColor(color); paint.setStyle(Paint.Style.FILL);
    }

    private void text(Canvas canvas, String value, float size, float x, float baseline, boolean heavy, int color) {
        font(size, heavy, color);
        canvas.drawText(value, x, baseline, paint);
    }

    private void rule(Canvas canvas, float left, float right, float y, int color) {
        paint.setColor(color); paint.setStrokeWidth(2);
        canvas.drawLine(left, y, right, y, paint);
    }

    private StaticLayout paragraph(String value, float size, float width, int maxLines, boolean heavy, int color) {
        font(size, heavy, color);
        return StaticLayout.Builder.obtain(value, 0, value.length(), paint, Math.max(1, (int) width))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false)
                .setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).build();
    }

    private void drawParagraph(Canvas canvas, StaticLayout layout, float x, float top) {
        canvas.save(); canvas.translate(x, top); layout.draw(canvas); canvas.restore();
    }

    private void paragraph(Canvas canvas, String value, float size, float x, float top,
                           float width, int maxLines, boolean heavy, int color) {
        drawParagraph(canvas, paragraph(value, size, width, maxLines, heavy, color), x, top);
    }

    @Override public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= 28) {
            DisplayCutout cutout = insets.getDisplayCutout();
            setPadding(cutout == null ? 0 : cutout.getSafeInsetLeft(), cutout == null ? 0 : cutout.getSafeInsetTop(),
                    cutout == null ? 0 : cutout.getSafeInsetRight(), cutout == null ? 0 : cutout.getSafeInsetBottom());
        }
        return super.onApplyWindowInsets(insets);
    }

    // This static display redraws once per minute or on a data change, not every animation frame.
    @SuppressLint("DrawAllocation")
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) return;
        ZonedDateTime now = previewTime == null ? ZonedDateTime.now() : previewTime;
        setContentDescription(s(R.string.summary, now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                agenda.selectedCalendarCount, agenda.today.size(), agenda.upcoming.size()));
        // The shorter edge is 1404 logical units. Both axes use the same scale.
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        if (width <= 0 || height <= 0) return;
        float scale = Math.min(width, height) / 1404f;
        ClockLayout layout = new ClockLayout(width / scale, height / scale);
        canvas.save(); canvas.translate(getPaddingLeft(), getPaddingTop()); canvas.scale(scale, scale);
        drawDate(canvas, layout.date, layout.landscape, now);
        drawToday(canvas, layout.today, now);
        drawUpcoming(canvas, layout.upcoming, layout.landscape, now);
        if (layout.landscape) {
            paint.setStrokeWidth(2); paint.setColor(INK);
            float divider = (layout.date.right + layout.today.left) / 2;
            canvas.drawLine(divider, 84, divider, layout.today.bottom, paint);
            paint.setColor(RULE);
            divider = (layout.today.right + layout.upcoming.left) / 2;
            canvas.drawLine(divider, 84, divider, layout.today.bottom, paint);
        }
        canvas.restore();
    }

    private void drawDate(Canvas canvas, ClockLayout.Box box, boolean landscape, ZonedDateTime now) {
        String month = now.format(DateTimeFormatter.ofPattern(s(R.string.month_pattern), locale()));
        String weekday = now.getDayOfWeek().getDisplayName(TextStyle.FULL, locale());
        String clock = now.format(DateTimeFormatter.ofPattern("HH:mm", locale()));
        boolean daylight = now.getZone().getRules().isDaylightSavings(now.toInstant());
        String zone = TimeZone.getTimeZone(now.getZone()).getDisplayName(daylight, TimeZone.SHORT, locale());
        text(canvas, month, 38, box.left, box.top + 66, false, INK);
        if (landscape) {
            float daySize = Math.min(430, box.width() * .84f);
            text(canvas, now.format(DateTimeFormatter.ofPattern("dd")), daySize, box.left - 14, box.top + 490, true, INK);
            paragraph(canvas, weekday, 68, box.left, box.top + 585, box.width(), 2, true, INK);
            rule(canvas, box.left, box.right, box.top + 740, INK);
            text(canvas, clock, Math.min(126, box.width() / 2.9f), box.left, box.top + 930, false, INK);
            paragraph(canvas, zone, 30, box.left, box.top + 970, box.width(), 2, false, MUTED);
        } else {
            float middle = box.left + box.width() * .55f;
            text(canvas, now.format(DateTimeFormatter.ofPattern("dd")), 300, box.left - 12, box.top + 370, true, INK);
            paragraph(canvas, weekday, 64, middle, box.top + 125, box.right - middle, 2, true, INK);
            text(canvas, clock, 108, middle, box.top + 350, false, INK);
            paragraph(canvas, zone, 28, middle, box.top + 390, box.right - middle, 1, false, MUTED);
        }
    }

    private void drawToday(Canvas canvas, ClockLayout.Box box, ZonedDateTime now) {
        text(canvas, s(R.string.today), 54, box.left, box.top + 66, true, INK);
        String count = q(R.plurals.item_count, agenda.today.size());
        font(28, false, INK);
        text(canvas, count, 28, box.right - paint.measureText(count), box.top + 66, false, INK);
        rule(canvas, box.left, box.right, box.top + 120, INK);
        float rowTop = box.top + 164;
        float footer = box.bottom - 140;
        float available = Math.max(120, footer - rowTop - 30);
        int show = Math.min(agenda.today.size(), Math.max(1, Math.min(5, (int) (available / 145))));
        float row = show == 0 ? available : Math.min(400, available / show);
        if (show == 0) {
            int message = agenda.errorMessage != 0 ? agenda.errorMessage
                    : agenda.selectedCalendarCount == 0 ? R.string.connect_calendars : R.string.empty_today;
            paragraph(canvas, s(message), 44, box.left, rowTop + 40, box.width(), 3, false, MUTED);
        }
        for (int i = 0; i < show; i++) {
            Agenda.Event event = agenda.today.get(i);
            float top = rowTop + row * i;
            boolean ended = event.hasEnded(now);
            int color = ended ? MUTED : INK;
            boolean roomy = row >= 260;
            if (!ended) { paint.setColor(INK); canvas.drawRect(box.left, top, box.left + 8, top + row - 30, paint); }
            String range = event.allDay ? s(R.string.all_day) : time(event.begin, now.getZone()) + " — " + time(event.end, now.getZone());
            paragraph(canvas, range, roomy ? 52 : 34, box.left + 26, top, box.width() - 26, 1, true, color);
            paragraph(canvas, event.title, roomy ? 46 : 36, box.left + 26, top + (roomy ? 91 : 48),
                    box.width() - 26, roomy ? 3 : 2, true, color);
            if (roomy) {
                String source = event.source == null ? s(R.string.calendar) : event.source;
                paragraph(canvas, source, 26, box.left + 26, top + row - 62, box.width() - 26, 1, false, color);
            }
            if (i < show - 1) rule(canvas, box.left + 26, box.right, top + row - 12, RULE);
        }
        rule(canvas, box.left, box.right, footer, INK);
        Agenda.Event next = null;
        boolean allDay = false;
        for (Agenda.Event event : agenda.today) {
            if (event.allDay) allDay = true;
            else if (next == null && !event.hasEnded(now)) next = event;
        }
        int label = next == null ? R.string.overview
                : next.begin <= now.toInstant().toEpochMilli() ? R.string.ongoing : R.string.next;
        text(canvas, s(label), 28, box.left, footer + 52, false, MUTED);
        String summary = next == null ? s(agenda.today.isEmpty() ? R.string.free_day
                : allDay ? R.string.all_day_remaining : R.string.day_done)
                : time(next.begin, now.getZone()) + "  " + next.title;
        paragraph(canvas, summary, 36, box.left, footer + 78, box.width(), 1, true, INK);
        if (agenda.today.size() > show) text(canvas, q(R.plurals.more_items, agenda.today.size() - show),
                24, box.left, box.bottom + 30, false, MUTED);
    }

    private void drawUpcoming(Canvas canvas, ClockLayout.Box box, boolean landscape, ZonedDateTime now) {
        int columns = landscape ? 1 : 2;
        float gap = 44;
        float width = (box.width() - gap * (columns - 1)) / columns;
        float top = box.top + 10;
        if (!landscape) {
            text(canvas, s(R.string.upcoming), 36, box.left, top + 26, true, INK);
            top += 70;
        }
        int shown = 0;
        LocalDate tomorrow = now.toLocalDate().plusDays(1);
        for (int column = 0; column < columns; column++) {
            float x = box.left + column * (width + gap);
            float y = top;
            while (shown < agenda.upcoming.size()) {
                Agenda.Event event = agenda.upcoming.get(shown);
                StaticLayout title = paragraph(event.title, 28, width, 2, true, INK);
                float row = 34 + title.getHeight() + 18;
                if (y + row > box.bottom - 20) break;
                LocalDate beginDate = event.startDate(now.getZone());
                LocalDate date = beginDate.isBefore(tomorrow) ? tomorrow : beginDate;
                String pattern = date.getYear() == now.getYear() ? "M/d" : "yyyy/M/d";
                String day = date.equals(tomorrow) ? s(R.string.tomorrow)
                        : date.format(DateTimeFormatter.ofPattern(pattern, locale())) + " "
                        + date.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale());
                String label = day + " · " + (event.allDay ? s(R.string.all_day)
                        : beginDate.isBefore(tomorrow) ? s(R.string.ongoing) : time(event.begin, now.getZone()));
                // Each StaticLayout shares this view's paint; restore its font before drawing it.
                paragraph(canvas, label, 24, x, y, width, 1, false, MUTED);
                font(28, true, INK);
                drawParagraph(canvas, title, x, y + 34);
                shown++;
                if (shown < agenda.upcoming.size()) rule(canvas, x, x + width, y + row - 7, RULE);
                y += row;
            }
        }
        if (agenda.upcoming.isEmpty()) paragraph(canvas,
                s(agenda.errorMessage == 0 ? R.string.empty_upcoming : agenda.errorMessage),
                28, box.left, top, box.width(), 2, false, MUTED);
        if (agenda.upcoming.size() > shown) text(canvas, q(R.plurals.more_items, agenda.upcoming.size() - shown),
                24, box.left, box.bottom + 30, false, MUTED);
    }
}
