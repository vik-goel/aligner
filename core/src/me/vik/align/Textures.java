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
		Texture atlas = createTexture("atlas.png");

		soundOff = createRegion(atlas, 0, 0, 64*3, 64*3);
		soundOn = createRegion(atlas, 64*4, 0, 64*3, 64*3);
		musicOff = createRegion(atlas, 0, 64*3+10, 64*3, 64*3);
		musicOn = createRegion(atlas, 64*4, 64*3+10, 64*3, 64*3);
		leaderboard = createRegion(atlas, 64*8, 0, 64*3, 64*3);
		pause = createRegion(atlas, 64*12, 0, 64*3, 64*3);
		arrow = createRegion(atlas, 64*16, 0, 64*3, 64*3);
		comet = createRegion(atlas, 0, 64*7, 512, 512);
        player = createRegion(atlas, 9*64, 7*64, 512, 512);
		background = createRegion(atlas, 0, 1024, 1024, 1024);
        innerShadow = createRegion(atlas, 9 * 128, 10*128, 768, 768);
		star = createRegion(atlas, 64*20, 0, 280, 280);
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
