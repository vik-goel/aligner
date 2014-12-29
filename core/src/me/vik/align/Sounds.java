package me.vik.align;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;

import java.util.Random;

public class Sounds {

    public static Sound[] jump, coin, button, lose;

    private static boolean enabled;
    private static boolean initialized = false;
    private static final String prefsString = "soundEnabled";

    private static final Random random = new Random();

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

        jump = loadSounds("jump", 5);
        coin = loadSounds("coin", 3);
        button = loadSounds("button", 3);
        lose = loadSounds("lose", 3);
    }

    public static void playRandom(Sound[] sounds) {
        if (isEnabled()) {
            sounds[random.nextInt(sounds.length)].play();
        }
    }

    private static Sound[] loadSounds(String prefix, int numSounds) {
        Sound[] sounds = new Sound[numSounds];

        for (int i = 0; i < numSounds; i++)
            sounds[i] = Gdx.audio.newSound(Gdx.files.internal("sounds/" + prefix + i + ".wav"));

        return sounds;
    }

}
