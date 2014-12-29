package me.vik.align;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;

public class Util {

    private Util() {}

    public static float getAspectRatio() {
        return (float) Gdx.graphics.getWidth() / (float) Gdx.graphics.getHeight();
    }

    public static Color getColor(int r, int g, int b) {
        return new Color(r / 255f, g / 255f, b / 255f, 1f);
    }

    public static boolean onMobile() {
        return Gdx.app.getType() == ApplicationType.Android || Gdx.app.getType() == ApplicationType.iOS;
    }

    public static boolean justDown() {
        return (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE));
    }

    public static float getTouchX() {
        return Gdx.input.getX() / (float)Gdx.graphics.getHeight();
    }

    public static float getTouchY() {
        return 1f - Gdx.input.getY() / (float)Gdx.graphics.getHeight();
    }

}
