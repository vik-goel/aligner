package me.vik.align.android;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.games.Games;

import me.vik.align.MyLeaderboard;
import me.vik.align.android.GameHelper.GameHelperListener;

//import com.google.example.games.basegameutils.GameHelper;
//import com.google.example.games.basegameutils.GameHelper.GameHelperListener;

public class AndroidLeaderboard implements MyLeaderboard {

	private static final String LEADERBOARD_ID = "CgkI4o2hh5AQEAIQAA";
	private GameHelper gameHelper;

	private Activity activity;
    private int highscore = -1;
    private boolean initialSignin;

	public AndroidLeaderboard(Activity activity) {
		this.activity = activity;
		gameHelper = new GameHelper(activity, GameHelper.CLIENT_GAMES);

		gameHelper.setup(new GameHelperListener() {
			public void onSignInFailed() {
				Log.d("Ad", "sign in failed");
                initialSignin = false;
			}

			public void onSignInSucceeded() {
				Log.d("Ad", "sign in succeeded");
				if (!initialSignin) show();
                else initialSignin = false;
			}
		});
	}

	public void show() {
		gameHelper.onStart(activity);
		
		if (gameHelper.isSignedIn()) {
			if (this.highscore >= 0)
				publishHighscore(highscore);

			activity.startActivityForResult(Games.Leaderboards.getLeaderboardIntent(gameHelper.getApiClient(), LEADERBOARD_ID), 9002);
		} else if (!gameHelper.isConnecting()) {
			loginGPGS();
		}
	}

	public void publishHighscore(int highscore) {
        if (highscore > this.highscore) this.highscore = highscore;

		if (gameHelper.isSignedIn())
			Games.Leaderboards.submitScore(gameHelper.getApiClient(), LEADERBOARD_ID, highscore);
	}

	public void onStart() {
        initialSignin = true;
		loginGPGS();
	}
    public void onStop() {
        gameHelper.onStop();
    }

    public void loginGPGS() {
        try {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    gameHelper.beginUserInitiatedSignIn();
                }
            });
        } catch (final Exception ex) {
        }
    }

}
