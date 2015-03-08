package me.vik.align.android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RelativeLayout;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import me.vik.align.Game;

public class AndroidLauncher extends AndroidApplication {

    private AndroidLeaderboard leaderboard;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.numSamples = 2;
        config.useAccelerometer = config.useCompass = false;
        config.hideStatusBar = false;
        //config.useImmersiveMode = true;
        
        leaderboard = new AndroidLeaderboard(this);

        Game game = new Game();
        game.setLeaderboard(leaderboard);

        RelativeLayout layout = new RelativeLayout(this);
        layout.addView(initializeForView(game, config));
        setContentView(layout);
    }

    public void onStop() {
        super.onStart();
        leaderboard.onStop();
    }

    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);
        leaderboard.onActivityResult(requestCode, responseCode, intent);
    }

}
