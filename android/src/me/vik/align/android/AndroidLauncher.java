package me.vik.align.android;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import me.vik.align.Game;

public class AndroidLauncher extends AndroidApplication {

    private static final String TOP_AD_UNIT_ID = "ca-app-pub-3934076033972627/3629436992";
    private static final String BOTTOM_AD_UNIT_ID = "ca-app-pub-3934076033972627/3489836197";
    protected AdView  bottomAdView;
    protected View gameView;
    private Game game;

    private AndroidLeaderboard leaderboard;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.numSamples = 2;
        config.useAccelerometer = config.useCompass = false;
        config.hideStatusBar = false;

        leaderboard = new AndroidLeaderboard(this);

        RelativeLayout layout = new RelativeLayout(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layout.setLayoutParams(params);

        bottomAdView = new AdView(this);

        AdView bottomView = createAdView(bottomAdView, false);
        layout.addView(bottomView);
        View gameView = createGameView(config);
        layout.addView(gameView);

        bottomAdView.setAdListener(new AdListener() {
            public void onAdOpened() {
                super.onAdOpened();
                game.pause();
            }
        });

        setContentView(layout);
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
        game = new Game();
        game.setLeaderboard(leaderboard);

        gameView = initializeForView(game, config);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ABOVE, bottomAdView.getId());
        gameView.setLayoutParams(params);

        return gameView;
    }

    private void startAdvertising(AdView adView) {
        AdRequest adRequest = new AdRequest.Builder().addTestDevice("203CB223AF17C67A86D9826D47229C56").build();
        adView.loadAd(adRequest);
    }

    public void onResume() {
        super.onResume();
        if (bottomAdView != null) bottomAdView.resume();
    }

    public void onPause() {
        if (bottomAdView != null) bottomAdView.pause();
        super.onPause();
    }

    public void onStop() {
        super.onStart();
        leaderboard.onStop();
    }

    public void onDestroy() {
        if (bottomAdView != null) bottomAdView.destroy();
        super.onDestroy();
    }

    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);
        leaderboard.onActivityResult(requestCode, responseCode, intent);
    }

}
