package me.vik.align;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;

import java.util.Random;

public class Sounds {

    public static Sound[] lose;
    public static Sound jump, coin, button;

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

        jump = loadSound("jump");
        coin = loadSound("coin");
        button = loadSound("button");
        lose = loadSounds("lose", 3);
    }

    public static void play(Sound[] sounds) {
        if (isEnabled()) {
            sounds[random.nextInt(sounds.length)].play();
        }
    }

    public static void play(Sound sound) {
        if (isEnabled()) {
            sound.play();
        }
    }

    private static Sound[] loadSounds(String prefix, int numSounds) {
        Sound[] sounds = new Sound[numSounds];

        for (int i = 0; i < numSounds; i++)
            sounds[i] = loadSound(prefix + i);

        return sounds;
    }

    private static Sound loadSound(String fileName) {
        return Gdx.audio.newSound(Gdx.files.internal("sounds/" + fileName + ".wav"));
    }

}
