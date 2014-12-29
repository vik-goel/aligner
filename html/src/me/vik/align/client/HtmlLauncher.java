package me.vik.align.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import me.vik.align.Game;

public class HtmlLauncher extends GwtApplication {

        @Override
        public GwtApplicationConfiguration getConfig () {
            GwtApplicationConfiguration gwtConfig = new GwtApplicationConfiguration(480, 800);
            gwtConfig.antialiasing = true;
            gwtConfig.fps = 60;
            return gwtConfig;
        }

        @Override
        public ApplicationListener getApplicationListener () {
                return new Game();
        }
}