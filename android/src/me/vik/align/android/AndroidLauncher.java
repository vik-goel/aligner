package me.vik.align.android;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import me.vik.align.Game;

public class AndroidLauncher extends AndroidApplication {

    private static final String TOP_AD_UNIT_ID = "ca-app-pub-3934076033972627/3629436992";
    private static final String BOTTOM_AD_UNIT_ID = "ca-app-pub-3934076033972627/3489836197";
    protected AdView topAdView, bottomAdView;
    protected View gameView;

    private AndroidLeaderboard leaderboard;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.numSamples = 2;
        config.useAccelerometer = config.useCompass = false;
        config.hideStatusBar = false;

        // Do the stuff that initialize() would do for you
        /*requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);*/

        leaderboard = new AndroidLeaderboard(this);

        RelativeLayout layout = new RelativeLayout(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layout.setLayoutParams(params);

        topAdView = new AdView(this);
        bottomAdView = new AdView(this);

        AdView topView = createAdView(topAdView, true);
        layout.addView(topView);
        AdView bottomView = createAdView(bottomAdView, false);
        layout.addView(bottomView);
        View gameView = createGameView(config);
        layout.addView(gameView);


        setContentView(layout);
        startAdvertising(topView);
        startAdvertising(bottomView);
    }

    private AdView createAdView(AdView view, boolean top) {
        view.setAdSize(AdSize.BANNER);
        view.setAdUnitId(top ? TOP_AD_UNIT_ID : BOTTOM_AD_UNIT_ID);
        view.setId(top ? 5436 : 5437); // this is an arbitrary id, allows for relative positioning in createGameView()
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        int verticalAlignment = top ? RelativeLayout.ALIGN_PARENT_TOP : RelativeLayout.ALIGN_PARENT_BOTTOM;
        params.addRule(verticalAlignment, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);

        view.setLayoutParams(params);
        view.setBackgroundColor(Color.BLACK);
        return view;
    }

    private View createGameView(AndroidApplicationConfiguration config) {
        Game game = new Game();
        game.setLeaderboard(leaderboard);

        gameView = initializeForView(game, config);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.BELOW, topAdView.getId());
        params.addRule(RelativeLayout.ABOVE, bottomAdView.getId());
        gameView.setLayoutParams(params);
        return gameView;
    }

    private void startAdvertising(AdView adView) {
        AdRequest adRequest = new AdRequest.Builder().addTestDevice("203CB223AF17C67A86D9826D47229C56").build();
        adView.loadAd(adRequest);
    }

    public void onStart() {
        super.onStart();
        leaderboard.onStart();
    }

    public void onResume() {
        super.onResume();
        if (topAdView != null) topAdView.resume();
    }

    public void onPause() {
        if (topAdView != null) topAdView.pause();
        super.onPause();
    }

    public void onStop() {
        super.onStart();
        leaderboard.onStop();
    }

    public void onDestroy() {
        if (topAdView != null) topAdView.destroy();
        super.onDestroy();
    }

}
