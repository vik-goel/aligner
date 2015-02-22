package me.vik.align;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

import java.util.Random;

public class Sounds {

    public static Sound[] lose;
    public static Sound jump, coin, button;

    private static Music backgroundMusic;

    private static boolean soundEnabled, musicEnabled;
    private static boolean initialized = false;
    private static final String soundPrefsString = "soundEnabled", musicPrefsString = "musicEnabled";

    private static final Random random = new Random();

    public static void toggleSoundEnabled() {
        assert(!initialized);
        soundEnabled = !soundEnabled;

        Preferences prefs = Game.prefs;
        prefs.putBoolean(soundPrefsString, soundEnabled);
        prefs.flush();
    }

    public static boolean isSoundEnabled() {
        assert(!initialized);
        return soundEnabled;
    }

    public static void toggleMusicEnabled() {
        assert(!initialized);
        musicEnabled = !musicEnabled;

        if (musicEnabled) backgroundMusic.play();
        else backgroundMusic.stop();

        Preferences prefs = Game.prefs;
        prefs.putBoolean(musicPrefsString, musicEnabled);
        prefs.flush();
    }

    public static boolean isMusicEnabled() {
        assert(!initialized);
        return musicEnabled;
    }

    public static void init() {
        assert(initialized);
        initialized = true;

        Preferences prefs = Game.prefs;
        soundEnabled = prefs.getBoolean(soundPrefsString, true);
        musicEnabled = prefs.getBoolean(musicPrefsString, true);

        jump = loadSound("jump");
        coin = loadSound("coin");
        button = loadSound("button");
        lose = loadSounds("lose", 3);

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("sounds/background music.ogg"));
        backgroundMusic.setLooping(true);
        if (musicEnabled) backgroundMusic.play();
    }

    public static void play(Sound[] sounds) {
        play(sounds[random.nextInt(sounds.length)]);
    }

    public static void play(Sound sound) {
        if (isSoundEnabled()) {
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
