package me.vik.align;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class Game extends com.badlogic.gdx.Game {

    private OrthographicCamera camera;

	public void create () {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Util.getAspectRatio(), 1);
	}

	public void render () {
		Gdx.gl.glClearColor(1, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}
}
