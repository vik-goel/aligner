package me.vik.align;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;

public class Sounds {

    public static Sound[] hit;

    private static boolean enabled;
    private static boolean initialized = false;

    private static final String prefsString = "soundEnabled";

    public static void toggleEnabled() {
        assert(!initialized);
        enabled = !enabled;

        Preferences prefs = Gdx.app.getPreferences(Game.fileOutputName);
        prefs.putBoolean(prefsString, enabled);
        prefs.flush();
    }

    public static boolean isEnabled() {
        assert(!initialized);
        return enabled;
    }

    public static void init() {
        assert(initialized);
        initialized = true;

        Preferences prefs = Gdx.app.getPreferences(Game.fileOutputName);
        enabled = prefs.getBoolean(prefsString, true);

        hit = new Sound[] {
            Gdx.audio.newSound(Gdx.files.internal("sounds/hit1.wav"))
        };
    }

}
