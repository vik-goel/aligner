package me.vik.align;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

public class Sounds {

    public static Sound[] hit;

    private static boolean enabled;
    private static boolean initialized = false;

    public static void toggleEnabled() {
        if (!initialized) throw new IllegalStateException("Sounds not yet initialized!");
        enabled = !enabled; //TODO: update value in file
    }

    public static boolean isEnabled() {
        if (!initialized) throw new IllegalStateException("Sounds not yet initialized!");

        return enabled;
    }

    public static void init() {
        if (initialized) throw new IllegalStateException("Sounds already initialized!");
        initialized = true;

        //TODO: Read this from a file
        enabled = true;

        hit = new Sound[] {
            Gdx.audio.newSound(Gdx.files.internal("sounds/hit1.wav"))
        };
    }

}
