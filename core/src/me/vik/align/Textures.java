package me.vik.align;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Textures {

	public static TextureRegion soundOn, soundOff, musicOn, musicOff,
			leaderboard, pause, arrow, comet, player, innerShadow, star,
			background, arcWithGlow, circle;

	private Textures() {
	}

	public static void loadTextures() {
		Texture atlas = createTexture("atlas_small.png");

		soundOff = createRegion(atlas, 0, 0, 128, 128);
		soundOn = createRegion(atlas, 128 * 2, 0, 128, 128);
		musicOff = createRegion(atlas, 128 * 5, 128 * 2, 128, 128);
		musicOn = createRegion(atlas, 128 * 7, 128 * 2, 128, 128);
		leaderboard = createRegion(atlas, 128 * 4, 0, 128, 128);
		pause = createRegion(atlas, 128 * 6, 0, 128, 128);
		arrow = createRegion(atlas, 128 * 8, 0, 128, 128);
		comet = createRegion(atlas, 0, 256, 512, 512);
        player = createRegion(atlas, 9*64, 7*64, 512, 512);
		background = createRegion(atlas, 0, 1024, 1024, 1024);
        innerShadow = createRegion(atlas, 9 * 128, 10*128, 768, 768);
		star = createRegion(atlas, 128 * 10, 0, 140, 140);
        arcWithGlow = createRegion(atlas, 2048-3*128+8, 128-34-8, 290, 290);
        circle = createRegion(atlas, 9 * 128, 3 * 128, 768, 768);
	}

	private static TextureRegion createRegion(Texture atlas, int x, int y, int width, int height) {
		return new TextureRegion(atlas, x/2, y/2, width/2, height/2);
	}

	private static Texture createTexture(String fileName) {
		Texture texture = null;

		try {
			texture = new Texture(Gdx.files.internal("textures/" + fileName));
			texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			texture.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
		} catch (Exception e) {
			System.err.println("Failed to load texture from file: " + fileName);
			e.printStackTrace();
		}

		return texture;
	}

}
