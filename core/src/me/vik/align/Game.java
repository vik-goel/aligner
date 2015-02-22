package me.vik.align;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;

import java.util.Random;

//TODO: Better game name, Comet Shooter?
//TODO: Better icon
//TODO: Better music or just remove the music?
//TODO: Add achievements
//TODO: Test leaderboards on release builds

public class Game extends ApplicationAdapter {

    private static final Color[] colors = {
        Util.getColor(166, 255, 122),//green
        Util.getColor(166, 217, 255),//blue
        Util.getColor(255, 255, 122),//yellow
        Util.getColor(255, 166, 217),//pink
        Util.getColor(220, 172, 255),//purple
    };

    public static final String fileOutputName = "me.vik.aligner";
    public static Preferences prefs;
    
    private String[] intStrings;
    private int score, highscore;
    private String scoreString, highscoreString;
    private String highscorePrefsString = "highScore";
    
    private static final float INNER_ROTATION = -2.05f;
    private static final float OUTER_ROTATION = 1.35f;

    private OrthographicCamera camera;
    private boolean shaking = false;
    private float shakeVelX = 0, shakeVelY = 0;
    private float shakeTime = 0, shakeTimeCounter = 0;

    private ShapeRenderer sr;
    private SpriteBatch fontBatch, texBatch;
    private BitmapFont font;
    private Random random = new Random();

    private float playerX, playerY;
    private float playerVelX = 0, playerVelY = 0;
    private Color playerColor = colors[0];
    private int playerColorIndex = 0;
    private float playerRadius = 0.025f;
    private boolean onInside, onOutside, lastLoc;

    private float innerRadius;
    private float outerRadius;
    private float centerX, centerY;
    private float progressBarPercent = 0f;
    private float innerRotation = 0f, outerRotation = 0f;

    private boolean playing = false, playedFirstGame = false, paused = false, gameStarted = false, turning = false, fadingTurningString = true, everPaused = false;
    private float buttonAlpha = 1f, topTextAlpha = 1f;
    private Button[] buttons;

    private Vector2[] spikeBuffer = new Vector2[6];

    //TODO: Might be able to combine these two arrays
    private Spike[] innerSpikes, outerSpikes;

    private double coinAngle;
    private float coinAlpha = 0f;
    private float timeSinceCoinSpawned;
    private float plus2X, plus2Y, plus2Alpha = 0, plus2VelX, plus2VelY;

    private float scaleFactor = 1f;

    private String topMessage, launchMessage, tapMessage, resumeMessage;
    private String scoreTitleString = "Score", bestString = "Best", gameOverString = "Game Over";
    private float topMessageSizeTime = 0;
    
    private float gameOverDisplayTime = 80f, gameOverDisplayCounter = 0f;

    private MyLeaderboard leaderboard;
    private boolean startedLeaderboard = false;
    
    private Comet[] trail = new Comet[100];
    private int cometIndex = 0;
    private Vector2 v2 = new Vector2();
    
    private float maskAlpha = 1;
    private float timerAlpha = 1;
    private float timeSinceLastHit = Float.MAX_VALUE;
    private int lastPlayerColorIndex;
    
    public void create() {
        Textures.loadTextures();
        prefs = Gdx.app.getPreferences(fileOutputName);
        Sounds.init();
        
        if (Util.onMobile()) {
        	topMessage = tapMessage = "Tap to Play";
        	resumeMessage = "Tap to Resume";
        	launchMessage = "Tap to Launch";
        } else {
        	topMessage = tapMessage = "Click to Play";
        	resumeMessage = "Click to Resume";
        	launchMessage = "Click to Launch";
        }
        
        intStrings = new String[1000];
        for (int i = 0; i < intStrings.length; i++)
            intStrings[i] = String.valueOf(i);
        
        for (int i = 0; i < spikeBuffer.length; i++)
        	spikeBuffer[i] = new Vector2();

        centerX = Util.getAspectRatio() / 2f;
        centerY = 0.5f;
        innerRadius = centerX * 0.3f;
        outerRadius = centerX * 0.85f;

        float buttonRadius = (outerRadius - innerRadius) / 3.5f;
        float buttonX = buttonRadius * 1.25f;
        float range = outerRadius - innerRadius - buttonRadius * 2;
        float buttonY = centerY - innerRadius - buttonRadius - range / 4f;

        if (leaderboard == null) {
            buttons = new Button[] {
                    new SoundButton(centerX - buttonX, buttonY, buttonRadius),
                    new MusicButton(centerX + buttonX, buttonY, buttonRadius),
            };
        } else {
            LeaderboardButton leaderboardButton = new LeaderboardButton(centerX - buttonX * 2f, buttonY, buttonRadius);
            leaderboardButton.setLeaderboard(leaderboard);

            buttons = new Button[]{
                    new SoundButton(centerX, buttonY, buttonRadius),
                    leaderboardButton,
                    new MusicButton(centerX + buttonX * 2f, buttonY, buttonRadius)
            };
        }

        coinAngle = getRandomRad();
        timeSinceCoinSpawned = 0;

        innerSpikes = new Spike[10];
        outerSpikes = new Spike[25];

        for (int i = 0; i < innerSpikes.length; i++) {
            innerSpikes[i] = new Spike();
        }

        for (int i = 0; i < outerSpikes.length; i++) {
            outerSpikes[i] = new Spike();
        }

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Util.getAspectRatio(), 1);

        sr = new ShapeRenderer();
        texBatch = new SpriteBatch();

        projectCamera();

        fontBatch = new SpriteBatch();
        font = new BitmapFont(Gdx.files.internal("fonts/AgencyFB.fnt"), Gdx.files.internal("fonts/AgencyFB.png"), false);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        setScore(0);

        playerX = centerX;
        playerY = innerRadius * 0.885f * scaleFactor + playerRadius + centerY;

        onInside = true;
        lastLoc =  false;
        onOutside = false;

        float playerDir = 45f;
        rotatePlayer(playerDir);

        highscore = prefs.getInteger(highscorePrefsString, 0);
        highscoreString = getIntString(highscore);

        if (leaderboard != null) {
            leaderboard.publishHighscore(highscore);
        }
        
        for (int i = 0; i < trail.length; i++)
        	trail[i] = new Comet();
    }

    public void setLeaderboard(MyLeaderboard leaderboard) {
        this.leaderboard = leaderboard;
    }

    private void initShake() {
        final float minShakeVel = 0.004f, initialShakeVel = 0.0125f, shakeEnergyLoss = 0.85f;
        final float minShakeTime = 1, maxShakeTime = 3;

        camera.position.x = centerX;
        camera.position.y = centerY;
        projectCamera();

        double currentShakeVel = shaking ? Math.sqrt(shakeVelX * shakeVelX + shakeVelY * shakeVelY) : 0;
        shaking = true;
        if (currentShakeVel == 0) currentShakeVel = initialShakeVel;

        double shakeVel = currentShakeVel * shakeEnergyLoss;
        if (shakeVel < minShakeVel) {
            shaking = false;
            return;
        }

        double dir = getRandomRad();
        shakeVelX = (float)(Math.cos(dir) * shakeVel);
        shakeVelY = (float)(Math.sin(dir) * shakeVel);

        shakeTime = random.nextFloat() * (maxShakeTime - minShakeTime) + minShakeTime;
        shakeTimeCounter = 0;

    }

    private void shake(float dt) {
        if (!shaking) return;

        shakeTimeCounter += dt;

        if (shakeTimeCounter >= shakeTime) initShake();
        if (!shaking) return;

        camera.position.x += shakeVelX;
        camera.position.y += shakeVelY;
        projectCamera();
    }

    private double getRandomRad() {
        return random.nextDouble() * Math.PI * 2.0;
    }


    private void projectCamera() {
        camera.update();
        sr.setProjectionMatrix(camera.combined);
        texBatch.setProjectionMatrix(camera.combined);
    }

    public void resume() {
        super.resume();
        Textures.loadTextures();
        prefs = Gdx.app.getPreferences(fileOutputName);
    }

    public void pause() {
        super.pause();
        pauseGame();
    }
    
    public void pauseGame() {
        if (playing && gameStarted) {
        	paused = everPaused = true;
        }
    }

    public void addComet(float x, float y) {
    	Comet comet = trail[cometIndex];
    	
    	comet.x = x;
    	comet.y = y;
    	comet.alpha = 1;
    	comet.moving = false;
    	comet.headingOutwards = lastLoc;
        cometIndex++;
        cometIndex %= trail.length;
    }
    
    public void render() {
        final float maxDt = 2f;

        float unpausedDt = Gdx.graphics.getRawDeltaTime() * 60f;
        if (unpausedDt > maxDt) unpausedDt = maxDt;
        float dt = paused ? 0 : unpausedDt;
        
        topMessageSizeTime += unpausedDt;
        timeSinceLastHit += dt;

        updateProgressBarPercent(dt);
        rotateWorld(dt);
        changeButtonAlpha(dt);
        shake(dt);
        updatePlus2(dt);

        float xCenter = Util.getAspectRatio() / 2f;
        float dst = Vector2.dst(xCenter, 0.5f, playerX, playerY);
        double dir = Math.atan2(playerY - 0.5f, playerX - xCenter);
        while (dir < 0) dir += Math.PI * 2.0;
        while (dir > Math.PI * 2.0) dir -= Math.PI * 2.0;
        
        for (int i = 0; i < trail.length; i++) {
        	Comet comet = trail[i];
        	
        	if (comet.alpha > 0 ) {
	        	if (comet.moving && turning) {
	    			v2.set(comet.x - centerX, comet.y - centerY);
	    			float dstFromCenter = v2.len();
	    			float rotation = comet.headingOutwards ? OUTER_ROTATION : INNER_ROTATION;
	    			v2.nor().rotate(-rotation * dt);
	    			comet.x = v2.x * dstFromCenter + centerX;
	    			comet.y = v2.y * dstFromCenter + centerY;
	        	} 
	        	
	    		float dAlpha = -0.05f;
	        	
	        	comet.alpha += dAlpha * dt;
	        	if (comet.alpha < 0) comet.alpha = 0;
        	}
        }
        
        boolean buttonAcceptedInput = false;

        for (int i = 0; i < buttons.length; i++) {
            buttonAcceptedInput |= buttons[i].update(dt, buttonAlpha == 1 && !gameStarted);
        }

        if (!buttonAcceptedInput && Util.justDown()) {
            if (paused || (gameStarted && Vector2.dst(centerX, centerY, Util.getTouchX(), Util.getTouchY()) < innerRadius && !Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
                paused = !paused;
                everPaused = true;
            } else if (onInside || onOutside) {
            	if (!playing) play();
            	
            	if (playing) {
            		gameStarted = true;

                    float dirMul = onInside ? 1 : -1;

                    final float playerSpeed = 0.00625f;

                    playerVelX = (float) Math.cos(dir) * playerSpeed * dirMul;
                    playerVelY = (float) Math.sin(dir) * playerSpeed * dirMul;

                    lastLoc = onInside;
                    onInside = onOutside = false;

                    Sounds.play(Sounds.jump);
            	}
            }
        }
        
        if (paused || !turning) {
        	maskAlpha += 0.05f * unpausedDt;
    		if (maskAlpha > 1) maskAlpha = 1;
        } else {
        	maskAlpha -= 0.05f * unpausedDt;
    		if (maskAlpha < 0) maskAlpha = 0;
        }
        
        if (playing)  changeTopTextAlpha(paused, unpausedDt);
        else {
            gameOverDisplayCounter += dt;
            if (gameOverDisplayCounter < gameOverDisplayTime) changeTopTextAlpha(true, dt);
            else if (topMessage == gameOverString && topTextAlpha > 0){
                changeTopTextAlpha(false, dt);
            } else {
            	boolean fadeOut = fadingTurningString && turning;
                changeTopTextAlpha(!fadeOut, dt);
                if (fadeOut && topTextAlpha <= 0) fadingTurningString = false;
                if (turning && !fadeOut) topMessage = launchMessage;
                else topMessage = tapMessage;
            }
        }
        
        if (score > 0 && playing) {
			coinAlpha += 0.1f * dt;
			if (coinAlpha > 1) coinAlpha = 1;
		}

        if (!onInside && !onOutside) {
            final float unscaledMinDst = innerRadius * 0.885f + playerRadius;
            final float unscaledMaxDst = outerRadius - playerRadius;
            final float unscaledRange = unscaledMaxDst - unscaledMinDst;

            boolean changeCol = false;
            
            timerAlpha = (dst - unscaledMinDst) / unscaledRange;

            if (dst <= unscaledMinDst && !lastLoc) {
                playerX = (float)(Math.cos(dir) * unscaledMinDst + centerX );
                playerY = (float)(Math.sin(dir) * unscaledMinDst + centerY );

                onInside = true;
                changeCol = true;
            }
            else if (dst >= unscaledMaxDst && lastLoc) {
                playerX = (float)(Math.cos(dir) * unscaledMaxDst + centerX );
                playerY = (float)(Math.sin(dir) * unscaledMaxDst + centerY );

                onOutside = true;
                changeCol = true;
            }
            
            if (lastLoc) timerAlpha = 1 - timerAlpha;
            if (timerAlpha > 1) timerAlpha = 1;
            else if (timerAlpha < 0) timerAlpha = 0;
            
            if (changeCol) {
            	timeSinceLastHit = 0;
                int quadrant = getQuadrant(dir, onOutside);

                if (quadrant != playerColorIndex) {
                    lose();
                } else {
                    progressBarPercent = 0;
                    timerAlpha = 1;
                    
                    lastPlayerColorIndex = playerColorIndex;
                    
                    if (playing) {
                        setScore(score + 1);
                        int randColIndex = random.nextInt(3);

                        for (int i = 0; i < trail.length; i++) {
                        	Comet comet = trail[i];
                        	comet.moving = comet.alpha > 0;
                        }
                        
                        if (playerColor == colors[randColIndex]) {
                            playerColor = colors[3];
                            playerColorIndex = 3;
                        } else {
                            playerColor = colors[randColIndex];
                            playerColorIndex = randColIndex;
                        }
                        spawnSpikes(dir);
                    }
                }

            } else {
            	if (!paused) {
	            	float oldX = playerX, oldY = playerY;
	            	float deltaX = playerVelX * dt, deltaY = playerVelY * dt;
	            	playerX += deltaX;
	            	playerY += deltaY;
	            	
	            	for (float i = 1; i < 3; i++) {
	            		addComet(oldX + deltaX / i, oldY + deltaY / i);
	            	}
            	}
                double playerPercentageToEdge = (double)(dst - unscaledMinDst) / (double)(unscaledMaxDst - unscaledMinDst);
                if (!lastLoc) playerPercentageToEdge = 1 - playerPercentageToEdge;
                if (playerPercentageToEdge < 0) playerPercentageToEdge = 0;
                else if (playerPercentageToEdge > 1) playerPercentageToEdge = 1;
                playerPercentageToEdge *= Math.PI;
                
                scaleFactor = (float)(1 + Math.sin(playerPercentageToEdge) * 0.08);
            }
        } else {
        	scaleFactor = 1f;
        }

        changeSpikeProtrusions(innerSpikes, dt);
        changeSpikeProtrusions(outerSpikes, dt);
        drawScreen(dt);

        if (!startedLeaderboard) {
            startedLeaderboard = true;
            if (leaderboard != null) leaderboard.start();
        }
    }

    private void setScore(int score) {
        this.score = score;
        scoreString = getIntString(score);
    }

    private String getIntString(int i) {
        if (i < intStrings.length)
            return intStrings[i];
        else return String.valueOf(i);
    }

    private void updatePlus2(float dt) {
        plus2Alpha -= 0.025 * dt;
        plus2X += plus2VelX * dt;
        plus2Y += plus2VelY * dt;
        if (plus2Alpha < 0) plus2Alpha = 0;
    }

    private void spawnSpikes(double playerDir) {
        int numSpikes = 0;

        if (onOutside) {
            int maxRandom = Math.min(8, (score - 8) / 11);
            int minSpikes = Math.min(4, 1 + score / 23);

            int rand = maxRandom > 0 ? random.nextInt(maxRandom) : 0;
            numSpikes = rand + minSpikes;
        } else {
            if (score > 60) numSpikes = random.nextInt(2) == 0 ? 2 : 1;
            else if (score > 30) numSpikes = random.nextInt(2) == 0 ? 1 : 0;
            else if (score > 15) numSpikes = random.nextInt(5) == 0 ? 1 : 0;
        }


        Spike[] spikes = onOutside ? outerSpikes : innerSpikes;

        for (int i = 0; i < spikes.length; i++) {
            if (spikes[i].active) {
                spikes[i].movingIn = true;
            }
        }

        Outer: for (int i = 0; i < numSpikes; i++) {
            for (int j = 0; j < spikes.length; j++) {
                if (!spikes[j].active) {
                    FindAngle: for(int attempt = 0; attempt < 15; attempt++){
                        double spikeAngle = getRandomRad();

                        float spikeQuad = getAdjustedAngle(spikeAngle, onOutside) / 90f;
                        float edgeDistance = spikeQuad - (int)spikeQuad;
                        final float minEdgeDistance = 0.2f;

                        if (edgeDistance < minEdgeDistance || 
                        	edgeDistance > (1f - minEdgeDistance) || 
                        	Math.abs(spikeAngle - playerDir) < 0.7)
                            continue;

                        int numSpikesInQuadrant = 1;
                        int jQuadrant = (int) spikeQuad;

                        for (int k = 0; k < spikes.length; k++) {
                            if (k == j) continue;
                            if (!spikes[k].active || spikes[k].movingIn || !spikes[k].collidable) continue;

                            int kQuadrant = getQuadrant(spikes[k].angle, onOutside);
                            if (kQuadrant == jQuadrant) numSpikesInQuadrant++;

                            if (Math.abs(spikes[k].angle - spikeAngle) < 0.225) continue FindAngle;
                        }

                        if ((onOutside && numSpikesInQuadrant > 2) ||
                            (!onOutside && numSpikesInQuadrant > 1)) continue;

                        spikes[j].angle = spikeAngle;
                        spikes[j].active = true;
                        spikes[j].movingIn = false;
                        spikes[j].collidable = true;
                    	spikes[j].protrudePercent = 0;
                    	spikes[j].movingOut = true;
                        
                        continue Outer;
                    }

                    continue Outer;
                }
            }
        }
    }

    private void updateProgressBarPercent(float dt) {
        if (onOutside || onInside) {
            float progressBarIncrease = 1.5f / 360f;
            final float maxProgressBarSpeedModifier = 1.33f;

            float progressBarSpeedModifier = 1 + (score - 15) / 110f;
            if (progressBarSpeedModifier > maxProgressBarSpeedModifier) progressBarSpeedModifier = maxProgressBarSpeedModifier;

            progressBarPercent += progressBarIncrease * progressBarSpeedModifier * dt;
        }

        if (progressBarPercent >= 1) lose();
        if (!playing)  progressBarPercent = 0;
    }

    private void rotateWorld(float dt) {
        if (turning) {
            float innerRotationAmt = INNER_ROTATION * dt;
            float outerRotationAmt = OUTER_ROTATION * dt;

            innerRotation += innerRotationAmt;
            innerRotation %= 360f;

            rotateSpikes(innerSpikes, innerRotationAmt);
            rotateSpikes(outerSpikes, outerRotationAmt);

            outerRotation += outerRotationAmt;
            outerRotation %= 360f;

            if (onInside) rotatePlayer(innerRotationAmt);
            else if (onOutside) rotatePlayer(outerRotationAmt);
        }
    }

    private void changeButtonAlpha(float dt) {
        final float buttonAlphaChange = 0.04f * dt;
        if (!turning && buttonAlpha < 1) buttonAlpha += buttonAlphaChange;
        if (turning && buttonAlpha > 0) buttonAlpha -= buttonAlphaChange;
        if (buttonAlpha < 0) buttonAlpha = 0;
        if (buttonAlpha > 1) buttonAlpha = 1;
    }

    private void changeSpikeProtrusions(Spike[] spikes, float dt) {
        final float percentChange = 0.04f * dt;

        for (int i = 0; i < spikes.length; i++) {
            if (spikes[i].active) {
                float change = 0f;

                assert(spikes[i].movingIn && spikes[i].movingOut); //A spike cannot be both moving in and moving out at the same time

                if (spikes[i].movingIn)
                    change = -percentChange;
                else if (spikes[i].movingOut)
                    change = percentChange;

                spikes[i].protrudePercent += change;

                if (spikes[i].protrudePercent > 1) {
                    spikes[i].protrudePercent = 1;
                    spikes[i].movingOut = false;
                }
                else if (spikes[i].protrudePercent < 0) {
                    spikes[i].active = false;
                }
            }
        }
    }

    private void changeTopTextAlpha(boolean increase, float dt) {
        float changeInAlpha = 0.05f * dt * (increase ? 1 : -1);
        topTextAlpha += changeInAlpha;

        if (topTextAlpha < 0) topTextAlpha = 0;
        else if (topTextAlpha > 1) topTextAlpha = 1;
    }

    private void rotateSpikes(Spike[] spikes, float rotAmt) {
        for (int i = 0; i < spikes.length; i++) {
            if (spikes[i].active) {
                spikes[i].angle -= Math.toRadians(rotAmt);
                spikes[i].angle %= Math.PI * 2.0;
                if (spikes[i].angle < 0) spikes[i].angle += Math.PI * 2.0;
            }
        }
    }

    private int getQuadrant(double rad, boolean outside) {
        return (int)(getAdjustedAngle(rad, outside) / 90f);
    }

    private float getAdjustedAngle(double rad, boolean outside) {
        float angle = (float)Math.toDegrees(rad);
        if (angle < 0) angle += 360;
        float outsideAngle = outside ? outerRotation : innerRotation;
        float adjustedAngle = (angle + outsideAngle) % 360;
        if (adjustedAngle < 0) adjustedAngle += 360;
        return adjustedAngle;
    }


    private void lose() {
        if (score > highscore) {
            highscore = score;
            highscoreString = getIntString(highscore);

            prefs.putInteger(highscorePrefsString, highscore);
            prefs.flush();

            if (leaderboard != null) leaderboard.publishHighscore(highscore);
        }

        cometIndex = 0;
        
        //TODO: Clean up duplicated code, maybe by just combining the two arrays
        for (int i = 0; i < innerSpikes.length; i++) {
            innerSpikes[i].collidable = false;
            innerSpikes[i].movingIn = true;
        }

        for (int i = 0; i < outerSpikes.length; i++) {
            outerSpikes[i].collidable = false;
            outerSpikes[i].movingIn = true;
        }

        scaleFactor = 1;
        progressBarPercent = 0;
        playing = false;
        everPaused = false;
        turning = false;
        fadingTurningString = true;
        gameStarted = false;
        coinAngle = getRandomRad();
        timeSinceCoinSpawned = 0;
        gameOverDisplayCounter = 0f;
        topMessage = gameOverString;
        maskAlpha = 0;
        coinAlpha = 0;

        initShake();
        Sounds.play(Sounds.lose);
    }

    private void rotatePlayer(float degrees) {
        float dst = Vector2.dst(centerX, centerY, playerX, playerY);
        double dir = Math.atan2(playerY - centerY, playerX - centerX) - Math.toRadians(degrees);

        playerX = (float)(Math.cos(dir) * dst) + centerX;
        playerY = (float)(Math.sin(dir) * dst) + centerY;
    }
    
    private void drawScreen(float dt) {
        float xCenter = Util.getAspectRatio() / 2f;
        Util.enableBlending();
        
        texBatch.begin();
        texBatch.setColor(Color.WHITE);
        float backgroundSizeEpsilon = 0.01f;
        texBatch.draw(Textures.background, -backgroundSizeEpsilon, -backgroundSizeEpsilon, Util.getAspectRatio() + backgroundSizeEpsilon * 2, 1f + backgroundSizeEpsilon * 2);
        
        float outerArcRadius = outerRadius * 1.22f * scaleFactor;
        float outerCircleRadius = outerRadius * scaleFactor;

        boolean outerGlow = (onInside || onOutside) ? onInside : lastLoc;
        drawCircle(outerArcRadius, xCenter, outerRotation, outerGlow);

        texBatch.setColor(Color.WHITE);
        texBatch.draw(Textures.innerShadow, xCenter - outerCircleRadius, 0.5f - outerCircleRadius, outerCircleRadius * 2, outerCircleRadius * 2);
        
        texBatch.end();
        
        Util.enableBlending();
        sr.begin(ShapeType.Filled);

        float changeProgressBarColPercent = (progressBarPercent - 0.5f) * 2;
        if (changeProgressBarColPercent < 0) changeProgressBarColPercent = 0;
        float progressBarR = 1f * changeProgressBarColPercent + (1 - changeProgressBarColPercent) * colors[4].r;
        float progressBarG = 0.2f * changeProgressBarColPercent + (1 - changeProgressBarColPercent) * colors[4].g;
        float progressBarB = 0.2f * changeProgressBarColPercent + (1 - changeProgressBarColPercent) * colors[4].b;
        float progressBarA = 0.55f * timerAlpha;
        
        sr.setColor(progressBarR, progressBarG, progressBarB, progressBarA);
        if (progressBarPercent != 0) sr.arc(xCenter, 0.5f, outerCircleRadius, 90, -progressBarPercent * 360f, 60);

        drawSpikesAndDoSpikeCollisionChecks(innerSpikes, false);
        drawSpikesAndDoSpikeCollisionChecks(outerSpikes, true);
        sr.end();
        
        Util.enableBlending();
        texBatch.begin();
        
        float innerArcRadius = innerRadius * scaleFactor;
        float innerCircleRadius = innerRadius * scaleFactor * 0.78f;
        drawCircle(innerArcRadius, xCenter, innerRotation, !outerGlow);
        
        texBatch.setColor(Color.WHITE);
        texBatch.draw(Textures.innerShadow, xCenter - innerCircleRadius, 0.5f - innerCircleRadius, innerCircleRadius * 2, innerCircleRadius * 2);
        
        texBatch.setColor(playerColor);
        texBatch.draw(Textures.player, playerX - playerRadius, playerY - playerRadius, playerRadius * 2, playerRadius * 2);
        
        for (int i = 0; i < trail.length; i++) {
        	Comet comet = trail[i];
        	
        	if (comet.alpha > 0) {
	        	float renderSize = playerRadius * 2 * comet.alpha;
	        	
	        	texBatch.setColor(playerColor.r, playerColor.g, playerColor.b, comet.alpha);
	        	texBatch.draw(Textures.comet, comet.x - renderSize / 2, comet.y - renderSize / 2, renderSize, renderSize);
        	}
        }
        
        if (playing && score > 0) {
            timeSinceCoinSpawned += dt;

            texBatch.setColor(colors[2].r, colors[2].g, colors[2].b, coinAlpha);
            float coinDst = (innerRadius + outerRadius) / 2f * scaleFactor;
            float coinRadius = 0.023f + 0.002f * (float)Math.sin(timeSinceCoinSpawned * 0.135);
            float coinX = (float) (Math.cos(coinAngle) * coinDst + centerX);
            float coinY = (float) (Math.sin(coinAngle) * coinDst + centerY);

            texBatch.draw(Textures.star, coinX - coinRadius, coinY - coinRadius, coinRadius * 2, coinRadius * 2);

            if (Vector2.dst(coinX, coinY, playerX, playerY) <= coinRadius + playerRadius) {
                setScore(score + 3);
                coinAlpha = 0;

                Sounds.play(Sounds.coin);

                plus2X = coinX;
                plus2Y = coinY;
                plus2Alpha = 1;

                double plus2Dir = Math.PI / 2.0;
                final double plus2Speed = 0.001;
                plus2VelX = (float)(Math.cos(plus2Dir) * plus2Speed);
                plus2VelY = (float)(Math.sin(plus2Dir) * plus2Speed);
                timeSinceCoinSpawned = 0;

                while (true) {
                    double newCoinAngle = getRandomRad();

                    if (Math.abs(newCoinAngle - coinAngle) > 0.55) {
                        coinAngle = newCoinAngle;
                        break;
                    }
                }
            }
            
        }

        if (turning && (onInside || onOutside)) {
            texBatch.setColor(playerColor.r, playerColor.g, playerColor.b, 0.3f);
            double playerRot = Math.atan2(playerY - centerY, playerX - centerX);
            final float arrowRenderSize = 0.04f;
            final float arrowOffsetFactor = onOutside ? outerRadius - playerRadius * 3.3f : innerRadius + playerRadius * 3.3f;
            float arrowXOffset = arrowOffsetFactor * (float) Math.cos(playerRot);
            float arrowYOffset = arrowOffsetFactor * (float) Math.sin(playerRot);
            if (onOutside) playerRot += (float)Math.PI;
           
            texBatch.draw(Textures.arrow, centerX - arrowRenderSize / 2f + arrowXOffset, centerY - arrowRenderSize / 2f + arrowYOffset,
            		 	  arrowRenderSize / 2f, arrowRenderSize / 2f, arrowRenderSize, arrowRenderSize,
            		 	  1, 1, (float) Math.toDegrees(playerRot));
        }

        if (gameStarted) {
            float sizeReduce = innerRadius / 2f;
            float size = (innerRadius * 2f - sizeReduce) * scaleFactor;

            float pauseAlpha = paused ?  0.9f * topTextAlpha: 0.4f * (1 - buttonAlpha);

            texBatch.setColor(colors[4].r, colors[4].g, colors[4].b, pauseAlpha);
            texBatch.draw(Textures.pause, centerX - size / 2f, centerY - size / 2f, size, size);
        }
        texBatch.end();
        
        if (maskAlpha > 0) {
        	Util.enableBlending();
        	sr.begin(ShapeType.Filled);
        	
        	sr.setColor(0, 0, 0, maskAlpha * 0.45f);
        	sr.rect(0, 0, Util.getAspectRatio(), 1f);
        	sr.end();
        }

        Util.enableBlending();
        fontBatch.begin();

        if (playing) {
	        font.setColor(colors[4].r, colors[4].g, colors[4].b, 1 - buttonAlpha);
	        float fontScaleFactor = 0.15f;
	        if (scoreString.length() == 3) fontScaleFactor = 0.125f;
	        if (scoreString.length() > 3) fontScaleFactor = 0.105f;
	        font.setScale(fontScaleFactor * Gdx.graphics.getHeight() / 128f);
	        BitmapFont.TextBounds bounds = font.getBounds(scoreString);
	        font.draw(fontBatch, scoreString, (Gdx.graphics.getWidth() - bounds.width) / 2, (outerRadius + centerY + 0.0625f) * Gdx.graphics.getHeight() + bounds.height);
	        font.setColor(colors[4].r, colors[4].g, colors[4].b, plus2Alpha);
	        font.draw(fontBatch, "+3", plus2X * Gdx.graphics.getWidth(), plus2Y * Gdx.graphics.getHeight());
        }

        if (topTextAlpha > 0) {
            String msg;

            if (paused || everPaused ) msg = resumeMessage;
            else if (gameStarted) msg = launchMessage;
            else msg = topMessage;
            
            if (msg == gameOverString) topMessageSizeTime = 0;
            float fontSize = (float)(0.0075 * Math.sin(topMessageSizeTime * 0.125f) + 0.08);
            font.setScale(fontSize * Gdx.graphics.getHeight() / 128f);
            BitmapFont.TextBounds bounds = font.getBounds(msg);
            
            font.setColor(colors[4].r, colors[4].g, colors[4].b, topTextAlpha);
            font.draw(fontBatch, msg, (Gdx.graphics.getWidth() - bounds.width) / 2, (outerRadius + centerY - (outerRadius - innerRadius) / 2f) * Gdx.graphics.getHeight());
        } else {
        	topMessageSizeTime = 0;
        }

        if (playedFirstGame && !gameStarted) {
            font.setScale(0.045f * Gdx.graphics.getHeight() / 128f);

            font.setColor(colors[4].r, colors[4].g, colors[4].b, buttonAlpha);
            BitmapFont.TextBounds bounds = font.getBounds(bestString);
            final float bestYSeperation = bounds.height * 0.1f;
            float yOffs = bounds.height * 1f + bestYSeperation;
            float yPos = Gdx.graphics.getHeight() / 2f + yOffs * 2;
            font.draw(fontBatch, bestString, (Gdx.graphics.getWidth() - bounds.width) / 2, yPos);

            yPos -= yOffs;
            bounds = font.getBounds(highscoreString);
            font.draw(fontBatch, highscoreString, (Gdx.graphics.getWidth() - bounds.width) / 2, yPos);
            yPos -= bestYSeperation * 2f;

            bounds = font.getBounds(scoreTitleString);
            yPos -= yOffs;
            font.draw(fontBatch, scoreTitleString, (Gdx.graphics.getWidth() - bounds.width) / 2, yPos);
            bounds = font.getBounds(scoreString);
            yPos -= yOffs;
            font.draw(fontBatch, scoreString, (Gdx.graphics.getWidth() - bounds.width) / 2, yPos);
        }

        fontBatch.end();

        texBatch.begin();

        if (buttonAlpha > 0) {
            texBatch.setColor(1, 1, 1, buttonAlpha);

            for (int i = 0; i < buttons.length; i++)
                 buttons[i].render(texBatch);
        }

        texBatch.end();
    }
    
    private void drawCircle(float radius, float xCenter, float rotation, boolean glows) {
    	final float circleRadius = radius * 0.88f;
    	texBatch.draw(Textures.circle, xCenter - circleRadius, 0.5f - circleRadius, circleRadius, circleRadius, circleRadius * 2, circleRadius * 2, 1, 1, -rotation);

        drawArcGlow(0, xCenter, radius, -rotation, glows);
        drawArcGlow(1, xCenter, radius, 90 - rotation, glows);
        drawArcGlow(2, xCenter, radius, 180 - rotation, glows);
        drawArcGlow(3, xCenter, radius, 270 - rotation, glows);
    }

    private void drawArcGlow(int colorIndex, float xCenter, float radius, float rotation, boolean glows) {
    	final float fadeTime = 12f;
    	float glowAlpha = (fadeTime - timeSinceLastHit) / fadeTime;
    	
    	boolean fadingOut = colorIndex == lastPlayerColorIndex && !glows;
    	boolean fadingIn = colorIndex == playerColorIndex && glows;
    	
    	if (fadingIn) {
    		glowAlpha = 1 - glowAlpha;
    		if (glowAlpha < 0) glowAlpha = 1;
    	}
    	
    	if (glowAlpha > 0 && (fadingOut || fadingIn)) {
    		if (glowAlpha > 1) glowAlpha = 1;
    		
    		texBatch.setColor(colors[colorIndex].r, colors[colorIndex].g, colors[colorIndex].b, glowAlpha);
    		texBatch.draw(Textures.arcWithGlow, xCenter, 0.5f, 0f, 0f, radius, radius, 1, 1, rotation);
    	}
	}

	private void drawSpikesAndDoSpikeCollisionChecks(Spike[] spikes, boolean outside) {
        for (int i = 0; i < spikes.length; i++) {
            if (!spikes[i].active) continue;

            double spikeAngle = spikes[i].angle + Math.PI / 2.0;

            final float sideLength = 0.05f * (float)spikes[i].protrudePercent;

            float spikeOffs = outside ? 0.045f : 0.02f;
            if (outside) spikeOffs *= spikes[i].protrudePercent;

            float spike0YOffs = outside ? -sideLength : 0;
            float spike2YOffs = outside ? sideLength : -sideLength;
            float radius = (outside ? outerRadius : innerRadius) * scaleFactor;

            spikeBuffer[0].set(centerX - sideLength / 2f, centerY - radius + spikeOffs + spike0YOffs);
            spikeBuffer[1].set(spikeBuffer[0].x + sideLength, spikeBuffer[0].y);
            spikeBuffer[2].set((spikeBuffer[0].x + spikeBuffer[1].x) / 2f, spikeBuffer[0].y + spike2YOffs);

            for (int j = 0; j < 3; j++)
                rotateVector2(spikeBuffer[j], spikeAngle);

            boolean shouldCollideWithSpike = spikes[i].collidable && timeSinceLastHit == 0;

            if (shouldCollideWithSpike) {
            //This interpolates between the vertices to get additional points to check collisions against
                for (int k = 0; k < 3; k++) {
                    int currentVertex = (k + 1) % 3;
                    int nextVertex = currentVertex + 3;

                    float dx = spikeBuffer[currentVertex].x - spikeBuffer[k].x;
                    float dy = spikeBuffer[currentVertex].y - spikeBuffer[k].y;

                    spikeBuffer[nextVertex].set(spikeBuffer[k].x + dx / 2f, spikeBuffer[k].y + dy / 2f);
                    rotateVector2(spikeBuffer[nextVertex], spikeAngle);
                }

            }


            if (shouldCollideWithSpike) {
                for (int j = 0; j < spikeBuffer.length; j++) {
                    if (Vector2.dst2(playerX, playerY, spikeBuffer[j].x, spikeBuffer[j].y) < playerRadius * playerRadius) {
                        lose();
                        break;
                    }
                }
            }

            int quadrant = getQuadrant(spikes[i].angle, outside);
            sr.setColor(colors[quadrant]);

            sr.triangle(spikeBuffer[0].x, spikeBuffer[0].y, spikeBuffer[1].x, spikeBuffer[1].y, spikeBuffer[2].x, spikeBuffer[2].y);
        }
    }

    private void rotateVector2(Vector2 vec, double rad) {
        vec.sub(centerX, centerY);
        vec.rotateRad((float) rad);
        vec.add(centerX, centerY);
    }

    public void play() {
        if (turning) {
        	playing = true;
        	setScore(0);
        	
        	if (!playedFirstGame) {
        		if (Util.onMobile()) {
        			tapMessage = "Tap to Restart";
        		} else {
        			tapMessage = "Click to Restart";
        		}
        		
        		playedFirstGame = true;
        	}
        } else {
        	turning = true;
        }
    }

    class Spike {
        public double angle;
        public double protrudePercent = 1;
        public boolean movingIn = false, movingOut = false;
        public boolean active = false, collidable = true;
    }
    
    class Comet {
    	float x, y;
    	boolean moving, headingOutwards;
    	float alpha;
    }

}
