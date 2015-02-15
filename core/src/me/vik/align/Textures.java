package me.vik.align;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Textures {

    public static Texture soundOn, soundOff, musicOn, musicOff, leaderboard, pause, arrow, comet, player, innerShadow, star, background;
    public static TextureRegion arcWithGlow, arc;

    private Textures() {}

    public static void loadTextures() {
        soundOff = createTexture("sound off.png");
        soundOn = createTexture("sound on.png");
        musicOff = createTexture("music off.png");
        musicOn = createTexture("music on.png");
        leaderboard = createTexture("leaderboard.png");
        pause = createTexture("pause.png");
        arrow = createTexture("arrow.png");
        comet = createTexture("comet.png");
        player = createTexture("player.png");
        background = createTexture("bgv2.jpg");
        innerShadow = createTexture("inner shadow 3.png");
        star = createTexture("star.png");
        arcWithGlow = new TextureRegion(createTexture("arc4.png"));
        arc = new TextureRegion(createTexture("arc2.png"));
    }

    private static Texture createTexture(String fileName) {
        Texture texture = null;

        try {
            texture = new Texture(Gdx.files.internal("textures/" + fileName));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            System.err.println("Failed to load texture from file: " + fileName);
            e.printStackTrace();
        }

        return texture;
    }

}
