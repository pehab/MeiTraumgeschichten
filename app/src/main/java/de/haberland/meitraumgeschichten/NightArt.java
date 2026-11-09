package de.haberland.meitraumgeschichten;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Original, resolution-independent bedtime illustrations; no remote assets. */
final class NightArt extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String character;
    NightArt(Context context, String character) {
        super(context); this.character = character;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private void color(String hex) { paint.setColor(Color.parseColor(hex)); paint.setShader(null); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scale = Math.min(getWidth() / 360f, getHeight() / 160f);
        canvas.save(); canvas.translate((getWidth() - 360 * scale) / 2, (getHeight() - 160 * scale) / 2); canvas.scale(scale, scale);
        paint.setShader(new RadialGradient(180, 85, 135, new int[]{0x334F6B96, 0x004F6B96}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(180, 85, 135, paint);
        color("#EACB88"); canvas.drawCircle(275, 38, 21, paint);
        color("#242641"); canvas.drawCircle(284, 30, 21, paint);
        color("#EACB88");
        for (int i = 0; i < 15; i++) {
            float x = 30 + (i * 61) % 310, y = 10 + (i * 37) % 110;
            if (Math.abs(x - 180) < 49 && y > 40) continue;
            canvas.drawCircle(x, y, i % 3 == 0 ? 1.8f : 1f, paint);
        }
        color("#303957"); canvas.drawOval(10, 125, 350, 205, paint);
        if ("dragon".equals(character)) {
            color("#648E82");
            Path tail = new Path(); tail.moveTo(195, 133); tail.quadTo(252, 144, 245, 103); tail.quadTo(230, 122, 202, 108); canvas.drawPath(tail, paint);
            canvas.drawOval(138, 73, 218, 147, paint); canvas.drawOval(139, 49, 209, 110, paint);
            color("#AED1A9"); canvas.drawOval(155, 101, 196, 144, paint);
            color("#E6C483"); Path horn = new Path(); horn.moveTo(151, 61); horn.lineTo(148, 40); horn.lineTo(165, 54); horn.close(); canvas.drawPath(horn, paint);
        } else if ("hedgehog".equals(character)) {
            color("#A67D70");
            Path spikes = new Path(); spikes.moveTo(125, 131);
            for (int i = 0; i <= 10; i++) { double angle = Math.PI + i * Math.PI / 10; spikes.lineTo(180 + (float)Math.cos(angle) * 59, 126 + (float)Math.sin(angle) * (i % 2 == 0 ? 65 : 52)); }
            spikes.close(); canvas.drawPath(spikes, paint);
            color("#E7C5A1"); canvas.drawOval(146, 86, 218, 143, paint);
        } else {
            color("#AA91AD"); canvas.drawOval(136, 48, 220, 145, paint);
            Path ears = new Path(); ears.moveTo(138, 73); ears.lineTo(136, 37); ears.lineTo(162, 57); ears.moveTo(195, 57); ears.lineTo(221, 37); ears.lineTo(218, 77); canvas.drawPath(ears, paint);
            color("#E7D5CC"); canvas.drawOval(144, 66, 177, 105, paint); canvas.drawOval(179, 66, 212, 105, paint);
            color("#E2B76C"); Path beak = new Path(); beak.moveTo(173, 94); beak.lineTo(183, 94); beak.lineTo(178, 104); beak.close(); canvas.drawPath(beak, paint);
        }
        color("#302B3F"); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2.5f); paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(151, 76, 170, 89, 10, 160, false, paint); canvas.drawArc(186, 76, 205, 89, 10, 160, false, paint);
        paint.setStyle(Paint.Style.FILL);
        color("#C49E8A"); canvas.drawOval(148, 135, 172, 145, paint); canvas.drawOval(187, 135, 211, 145, paint);
        canvas.restore();
    }
}
