package me.vik.align;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public abstract class Button {

    private float x, y, radius;
    private float renderRadius, radiusVelocity = 0f, radiusAcceleration = -0.001f;
    private boolean selected = false;
    protected TextureRegion texture;
    protected boolean acceptsSpace = false;
    private boolean spaceWasDown = false;

    protected Button(float x, float y, float radius, TextureRegion texture) {
        this.x = x;
        this.y = y;
        this.radius = this.renderRadius = radius;
        this.texture = texture;
    }

    public boolean update(float dt, boolean acceptsInput) {
        float touchX = Util.getTouchX();
        float touchY = Util.getTouchY();
        boolean touchedWithinRadius = Vector2.dst(x, y, touchX, touchY) <= radius;
        boolean justHitSpace = acceptsSpace && Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        boolean spaceDown = acceptsSpace && Gdx.input.isKeyPressed(Input.Keys.SPACE);

        if ((Gdx.input.justTouched() || justHitSpace) && acceptsInput) {
            if (radiusAcceleration > 0) radiusAcceleration *= -1;
            selected = touchedWithinRadius || justHitSpace;
        }

        if (selected && !spaceDown) {
            if (!touchedWithinRadius && !spaceWasDown) deselect();
            else if (!Gdx.input.isTouched()) {
                deselect();
                onClick();
                Sounds.play(Sounds.button);
            }
        }

        spaceWasDown = spaceDown;

        if (!acceptsInput) deselect();

        if (selected || renderRadius != radius) {
            radiusVelocity += radiusAcceleration;
            renderRadius += radiusVelocity * dt;
        }

        float minRadius = radius * 0.75f;

        if (renderRadius > radius) renderRadius = radius;
        else if (renderRadius < minRadius) renderRadius = minRadius;

        return selected;
    }

    //pre-condition: batch has begun drawing
    public void render(SpriteBatch batch) {
        batch.draw(texture, x - renderRadius, y - renderRadius, renderRadius * 2, renderRadius * 2);
    }

    private void deselect() {
        selected = false;
        radiusVelocity = 0;
        if (radiusAcceleration < 0) radiusAcceleration *= -1;
    }

    protected abstract void onClick();
}

class SoundButton extends Button {

    SoundButton(float x, float y, float radius) {
        super(x, y, radius, Sounds.isSoundEnabled() ? Textures.soundOn : Textures.soundOff);
    }

    protected void onClick() {
        Sounds.toggleSoundEnabled();

        if (Sounds.isSoundEnabled())
            texture = Textures.soundOn;
        else texture = Textures.soundOff;
    }
}

class MusicButton extends Button {

    MusicButton(float x, float y, float radius) {
        super(x, y, radius, Sounds.isMusicEnabled() ? Textures.musicOn : Textures.musicOff);
    }

    protected void onClick() {
        Sounds.toggleMusicEnabled();

        if (Sounds.isMusicEnabled())
            texture = Textures.musicOn;
        else texture = Textures.musicOff;
    }
}

class LeaderboardButton extends Button {
    private MyLeaderboard leaderboard;

    LeaderboardButton(float x, float y, float radius) {
        super(x, y, radius, Textures.leaderboard);
    }

    public void setLeaderboard(MyLeaderboard leaderboard) {
        this.leaderboard = leaderboard;
    }

    protected void onClick() {
        if (leaderboard != null) leaderboard.show();
    }
}

