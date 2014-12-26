package me.vik.align.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import me.vik.align.Game;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

        config.height = 800;
        config.width = 480;
        config.samples = 4;
        config.title = "Align";
        config.foregroundFPS = 60;
        config.backgroundFPS = -1;
        config.resizable = false;

		new LwjglApplication(new Game(), config);
	}
}
