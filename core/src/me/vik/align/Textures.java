package me.vik.align;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

public class Textures {

    public static Texture soundOn, soundOff, leaderboard, pause;

    private Textures() {}

    public static void loadTextures() {
        soundOff = createTexture("sound off.png");
        soundOn = createTexture("sound on.png");
        leaderboard = createTexture("leaderboard.png");
        pause = createTexture("pause.png");
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
