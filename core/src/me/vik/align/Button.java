package me.vik.align;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public abstract class Button {

    private float x, y, radius;
    private float renderRadius, radiusVelocity = 0f, radiusAcceleration = -0.001f;
    private boolean selected = false;
    protected Texture texture;

    protected Button(float x, float y, float radius, Texture texture) {
        this.x = x;
        this.y = y;
        this.radius = this.renderRadius = radius;
        this.texture = texture;
    }

    //pre-condition: batch has begun drawing
    public void updateAndRender(SpriteBatch batch, float dt, boolean acceptInput) {
        //Converting screen coordinates into normalized coordinates
        float touchX = Gdx.input.getX() / (float)Gdx.graphics.getHeight();
        float touchY = 1f - Gdx.input.getY() / (float)Gdx.graphics.getHeight();
        boolean touchedWithinRadius = Vector2.dst(x, y, touchX, touchY) <= radius;

        if (Gdx.input.justTouched() && acceptInput) {
            if (radiusAcceleration > 0) radiusAcceleration *= -1;
            selected = touchedWithinRadius;
        }

        if (selected) {
            if (!touchedWithinRadius) deselect(dt);
            else if (!Gdx.input.isTouched()) {
                deselect(dt);
                onClick();
            }
        }

        if (!acceptInput) deselect(dt);

        if (selected || renderRadius != radius) {
            radiusVelocity += radiusAcceleration;
            renderRadius += radiusVelocity;
        }

        float minRadius = radius * 0.75f;

        if (renderRadius > radius) renderRadius = radius;
        else if (renderRadius < minRadius) renderRadius = minRadius;

        batch.draw(texture, x - renderRadius, y - renderRadius, renderRadius * 2, renderRadius * 2);
    }

    private void deselect(float dt) {
        selected = false;
        radiusVelocity = 0;
        if (radiusAcceleration < 0) radiusAcceleration *= -1;
    }


    protected abstract void onClick();
}

class PlayButton extends Button {

    private Game game;

    PlayButton(float x, float y, float radius, Game game) {
        super(x, y, radius, Textures.play);
        this.game = game;
    }

    protected void onClick() {
        game.play();
    }
}

class SoundButton extends Button {

    SoundButton(float x, float y, float radius) {
        super(x, y, radius, Sounds.isEnabled() ? Textures.soundOn : Textures.soundOff);
    }

    protected void onClick() {
        Sounds.toggleEnabled();

        if (Sounds.isEnabled())
            texture = Textures.soundOn;
        else texture = Textures.soundOff;
    }
}

class LeaderboardButton extends Button {
    LeaderboardButton(float x, float y, float radius) {
        super(x, y, radius, Textures.leaderboard);
    }

    protected void onClick() {

    }
}

