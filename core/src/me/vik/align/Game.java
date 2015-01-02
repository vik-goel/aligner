package me.vik.align;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.Random;

//TODO: Change ad id`s
//TODO: Better game name

//TODO: Particle effects on death, this might be unnecessary
//TODO: Player Motion trail, this might be unnecessary

//TODO: Tutorial
//TODO: Better music?
//TODO: Add achievements
//TODO: Test leaderboards on release builds
//TODO: Try to reduce startup time, if not possible add a splash screen

public class Game extends com.badlogic.gdx.Game {

    private static final Color[] colors = {
        Util.getColor(166, 255, 122),//green
        Util.getColor(166, 217, 255),//blue
        Util.getColor(255, 255, 122),//yellow
        Util.getColor(255, 166, 217),//pink
        Util.getColor(220, 172, 255) //purple
    };

    public static final String fileOutputName = "me.vik.align";
    private String[] intStrings;
    private int score, highscore;
    private String scoreString, highscoreString;
    private String highscorePrefsString = "highScore";

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

    private boolean playing = false, playedFirstGame = false, paused = false, gameStarted = false;
    private float buttonAlpha = 1f, topTextAlpha = 1f;
    private Button[] buttons;

    //TODO: Might be able to combine these two arrays
    private Vector2[] spikeBuffer = new Vector2[]{new Vector2(), new Vector2(), new Vector2()};
    private Spike[] innerSpikes, outerSpikes;

    private double coinAngle;
    private float plus2X, plus2Y, plus2Alpha = 0, plus2VelX, plus2VelY;

    private float scaleFactor = 1f, scaleVelocity = 0f;

    private String topMessage, tapMessage, resumeMessage, scoreTitleString = "Score", bestString = "Best", gameOverString = "Game Over";
    private float gameOverDisplayTime = 80f, gameOverDisplayCounter = 0f;

    private MyLeaderboard leaderboard;
    private boolean startedLeaderboard = false;

    public void create() {
        Textures.loadTextures();
        Sounds.init();

        topMessage = tapMessage = Util.onMobile() ? "Tap to Launch" : "Click to Launch";
        resumeMessage  = Util.onMobile() ? "Tap to Resume" : "Click to Resume";

        intStrings = new String[1000];
        for (int i = 0; i < intStrings.length; i++)
            intStrings[i] = String.valueOf(i);

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

        innerSpikes = new Spike[100];
        outerSpikes = new Spike[100];

        //assumes innerspikes and outerspikes arrays are the same length
        for (int i = 0; i < innerSpikes.length; i++) {
            innerSpikes[i] = new Spike();
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
        playerY = innerRadius * scaleFactor + playerRadius + centerY;

        onInside = lastLoc =  true;

        float playerDir = 45f;
        double playerDirRad = Math.toRadians(playerDir);

        rotatePlayer(playerDir);

        onOutside = true;
        spawnSpikes(playerDirRad);
        onOutside = false;
        spawnSpikes(playerDirRad);

        for (int i = 0; i < innerSpikes.length; i++) {
            innerSpikes[i].protrudePercent = 1;
        }

        for (int i = 0; i < outerSpikes.length; i++) {
            outerSpikes[i].protrudePercent = 1;
        }

        Preferences prefs = Gdx.app.getPreferences(Game.fileOutputName);
        highscore = prefs.getInteger(highscorePrefsString, 0);
        highscoreString = getIntString(highscore);

        if (leaderboard != null) {
            leaderboard.publishHighscore(highscore);
        }
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
    }

    public void pause() {
        super.pause();
        pauseGame();
    }

    public void pauseGame() {
        if (playing && gameStarted) paused = true;
    }

    public void render() {
        final float maxDt = 2f;

        float unpausedDt = Gdx.graphics.getDeltaTime() * 60f;
        if (unpausedDt > maxDt) unpausedDt = maxDt;
        float dt = paused ? 0 : unpausedDt;

        updateProgressBarPercent(dt);
        rotateWorld(dt);
        changeButtonAlpha(dt);
        shake(dt);
        updatePlus2(dt);

        float xCenter = Util.getAspectRatio() / 2f;
        float dst = Vector2.dst(xCenter, 0.5f, playerX, playerY);
        double dir = Math.atan2(playerY - 0.5f, playerX - xCenter);
        if (dir < 0) dir += Math.PI * 2.0;

        boolean buttonAcceptedInput = false;

        for (int i = 0; i < buttons.length; i++) {
            buttonAcceptedInput |= buttons[i].update(dt, buttonAlpha == 1 && !gameStarted);
        }

        if (!buttonAcceptedInput && Util.justDown()) {
            if (paused || (gameStarted && Vector2.dst(centerX, centerY, Util.getTouchX(), Util.getTouchY()) < innerRadius && !Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
                paused = !paused;
            } else if (onInside || onOutside) {
                if (!playing) play();
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
        if (playing)  changeTopTextAlpha(paused, unpausedDt);
        else {
            gameOverDisplayCounter += dt;
            if (gameOverDisplayCounter < gameOverDisplayTime) changeTopTextAlpha(true, dt);
            else if (topMessage == gameOverString && topTextAlpha > 0){
                changeTopTextAlpha(false, dt);
            } else {
                changeTopTextAlpha(true, dt);
                topMessage = tapMessage;
            }
        }

        if (!onInside && !onOutside) {
            final float unscaledMinDst = innerRadius + playerRadius;
            final float unscaledMaxDst = outerRadius - playerRadius;
            final float minDst = innerRadius * scaleFactor + playerRadius;
            final float maxDst = outerRadius * scaleFactor - playerRadius;

            boolean changeCol = false;

            if (dst <= minDst && !lastLoc) {
                playerX = (float)(Math.cos(dir) * unscaledMinDst + centerX );
                playerY = (float)(Math.sin(dir) * unscaledMinDst + centerY );

                onInside = true;
                changeCol = true;
            }
            if (dst >= maxDst && lastLoc) {
                playerX = (float)(Math.cos(dir) * unscaledMaxDst + centerX );
                playerY = (float)(Math.sin(dir) * unscaledMaxDst + centerY );

                onOutside = true;
                changeCol = true;
            }

            if (changeCol) {
                int quadrant = getQuadrant(dir, onOutside);

                if (quadrant != playerColorIndex) {
                    lose();
                } else {
                    progressBarPercent = 0;
                    if (playing) {
                        setScore(score + 1);
                        int randColIndex = random.nextInt(3);

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
                playerX += playerVelX * dt;
                playerY += playerVelY * dt;

                float range = unscaledMaxDst - unscaledMinDst;

                boolean shouldIncreaseScale = lastLoc ? dst - unscaledMinDst < range / 2.5f :
                                                        dst - unscaledMinDst >= range - range / 2.5f ;

                final float scaleAcceleration =  0.0008f;

                if (shouldIncreaseScale) {
                    if (scaleVelocity < 0) scaleVelocity = 0;
                    scaleVelocity += scaleAcceleration;
                }
                else {
                    if (scaleVelocity > 0) scaleVelocity = 0;
                    scaleVelocity -= scaleAcceleration;
                }

                final float maxScaleVelocity = 0.01f;

                if (scaleVelocity > maxScaleVelocity) scaleVelocity = maxScaleVelocity;
                else if (scaleVelocity < -maxScaleVelocity) scaleVelocity = -maxScaleVelocity;

                scaleFactor += scaleVelocity * dt;

                if (scaleFactor < 1) {
                    scaleFactor = 1;
                    scaleVelocity = 0;
                }
            }
        } else {
            scaleVelocity = 0;
            scaleFactor = 1;
        }

        changeSpikeProtrusions(innerSpikes, dt);
        changeSpikeProtrusions(outerSpikes, dt);
        drawScreen();

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

    //TODO: Change the limits where spikes can spawn to stop overlaps with boundaries and other spikes
    private void spawnSpikes(double playerDir) {
        int numSpikes = 0;

        if (onOutside) {
            int maxRandom = Math.min(8, score / 8);
            int minSpikes = Math.min(3, 1 + score / 20);

            int rand = maxRandom > 0 ? random.nextInt(maxRandom) : 0;
            numSpikes = rand + minSpikes;
        } else {
            if (score > 70) numSpikes = random.nextInt(2) == 0 ? 2 : 1;
            else if (score > 40) numSpikes = random.nextInt(2) == 0 ? 1 : 0;
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
                    int attempt = 0;

                    FindAngle: while (true) {
                        if (attempt++ > 25) continue Outer;

                        double spikeAngle = getRandomRad();

                        float spikeQuad = getAdjustedAngle(spikeAngle, onOutside) / 90f;
                        float edgeDistance = spikeQuad - (int)spikeQuad;
                        final float minEdgeDistance = 0.14f;

                        if (edgeDistance < minEdgeDistance || edgeDistance > (1f - minEdgeDistance))
                            continue;

                        if (Math.abs(spikeAngle - playerDir) < 0.5) continue;

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
                            ((!onOutside) && numSpikesInQuadrant > 1)) continue;

                        spikes[j].angle = spikeAngle;
                        spikes[j].active = spikes[j].movingOut = true;
                        spikes[j].movingIn = false;
                        spikes[j].protrudePercent = 0;
                        spikes[j].collidable = true;
                        continue Outer;
                    }
                }
            }
        }
    }

    private void updateProgressBarPercent(float dt) {
        if (onOutside || onInside)
            progressBarPercent += 1.35f / 360f * dt;

        if (progressBarPercent >= 1) {
            lose();
        }

        if (!playing)
            progressBarPercent = 0;
    }

    private void rotateWorld(float dt) {
        float innerRotationAmt = -2.05f * dt;
        float outerRotationAmt = 1.35f * dt;

        innerRotation += innerRotationAmt;
        innerRotation %= 360f;

        rotateSpikes(innerSpikes, innerRotationAmt);
        rotateSpikes(outerSpikes, outerRotationAmt);

        outerRotation += outerRotationAmt;
        outerRotation %= 360f;

        if (onInside) rotatePlayer(innerRotationAmt);
        if (onOutside)  rotatePlayer(outerRotationAmt);
    }

    private void changeButtonAlpha(float dt) {
        final float buttonAlphaChange = 0.04f * dt;
        if (!gameStarted && buttonAlpha < 1) buttonAlpha += buttonAlphaChange;
        if (gameStarted && buttonAlpha > 0) buttonAlpha -= buttonAlphaChange;
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

            Preferences prefs = Gdx.app.getPreferences(Game.fileOutputName);
            prefs.putInteger(highscorePrefsString, highscore);
            prefs.flush();

            if (leaderboard != null) leaderboard.publishHighscore(highscore);
        }

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
        gameStarted = false;
        coinAngle = getRandomRad();
        gameOverDisplayCounter = 0f;
        topMessage = gameOverString;

        initShake();
        Sounds.play(Sounds.lose);
    }

    private void rotatePlayer(float degrees) {
        float dst = Vector2.dst(centerX, centerY, playerX, playerY);
        double dir = Math.atan2(playerY - centerY, playerX - centerX) - Math.toRadians(degrees);

        playerX = (float)(Math.cos(dir) * dst) + centerX;
        playerY = (float)(Math.sin(dir) * dst) + centerY;
    }

    private void drawScreen() {
        Color bgColor = Color.BLACK;

        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float xCenter = Util.getAspectRatio() / 2f;
        int numSegments = 60;

        sr.begin(ShapeRenderer.ShapeType.Filled);

        sr.setColor(colors[0]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.08f * scaleFactor, -outerRotation, 90f, numSegments);

        sr.setColor(colors[1]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.08f * scaleFactor, 90 - outerRotation, 90f, numSegments);

        sr.setColor(colors[2]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.08f * scaleFactor, 180 - outerRotation, 90f, numSegments);

        sr.setColor(colors[3]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.08f * scaleFactor, 270 - outerRotation, 90f, numSegments);

        /*sr.setColor(bgColor);
        sr.arc(xCenter, 0.5f, 1.1f, 90, -360f * (progressBarPercent), numSegments);

        sr.setColor(colors[0]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.035f * scaleFactor, -outerRotation, 90f, numSegments);

        sr.setColor(colors[1]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.035f * scaleFactor, 90 - outerRotation, 90f, numSegments);

        sr.setColor(colors[2]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.035f * scaleFactor, 180 - outerRotation, 90f, numSegments);

        sr.setColor(colors[3]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.035f * scaleFactor, 270 - outerRotation, 90f, numSegments);*/

        sr.setColor(bgColor);
        sr.circle(xCenter, 0.5f, outerRadius * scaleFactor, numSegments);

        sr.setColor(playerColor.r, playerColor.g, playerColor.b, 0.3f);
        sr.arc(xCenter, 0.5f,outerRadius * scaleFactor, 90, -360f * (progressBarPercent), numSegments);

        sr.setColor(colors[0]);
        sr.arc(xCenter, 0.5f, innerRadius * scaleFactor, -innerRotation, 90f, numSegments);

        sr.setColor(colors[1]);
        sr.arc(xCenter, 0.5f, innerRadius * scaleFactor, 90 - innerRotation, 90f, numSegments);

        sr.setColor(colors[2]);
        sr.arc(xCenter, 0.5f, innerRadius * scaleFactor, 180 - innerRotation, 90f, numSegments);

        sr.setColor(colors[3]);
        sr.arc(xCenter, 0.5f, innerRadius * scaleFactor, 270 - innerRotation, 90f, numSegments);

        sr.setColor(bgColor);
        sr.circle(xCenter, 0.5f, innerRadius * scaleFactor * 0.9f, numSegments);

        sr.setColor(playerColor);
        sr.circle(playerX, playerY, playerRadius, numSegments);

        drawSpikesAndDoSpikeCollisionChecks(innerSpikes, false);
        drawSpikesAndDoSpikeCollisionChecks(outerSpikes, true);

        if (playing && score > 0) {
            Gdx.gl20.glEnable(GL20.GL_BLEND);
            Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            sr.setColor(colors[4].r, colors[4].g, colors[4].b, 1f - topTextAlpha);
            float coinDst = (innerRadius + outerRadius) / 2f * scaleFactor;
            float coinRadius = 0.0175f;
            float coinX = (float) (Math.cos(coinAngle) * coinDst + centerX);
            float coinY = (float) (Math.sin(coinAngle) * coinDst + centerY);

            sr.circle(coinX, coinY, coinRadius, numSegments);

            if (Vector2.dst(coinX, coinY, playerX, playerY) <= coinRadius + playerRadius) {
                setScore(score + 2);

                Sounds.play(Sounds.coin);

                plus2X = coinX;
                plus2Y = coinY;
                plus2Alpha = 1;

                double plus2Dir = Math.PI / 2.0;
                final double plus2Speed = 0.001;
                plus2VelX = (float)(Math.cos(plus2Dir) * plus2Speed);
                plus2VelY = (float)(Math.sin(plus2Dir) * plus2Speed);

                while (true) {
                    double newCoinAngle = getRandomRad();

                    if (Math.abs(newCoinAngle - coinAngle) > 0.4) {
                        coinAngle = newCoinAngle;
                        break;
                    }
                }
            }

        }
        sr.end();

        texBatch.begin();

        if (onInside || onOutside) {
            texBatch.setColor(playerColor.r, playerColor.g, playerColor.b, 0.3f);
            double playerRot = Math.atan2(playerY - centerY, playerX - centerX);
            int arrowWidth = Textures.arrow.getWidth();
            int arrowHeight = Textures.arrow.getHeight();
            final float arrowRenderSize = 0.04f;
            final float arrowOffsetFactor = onOutside ? outerRadius - playerRadius * 3.3f : innerRadius + playerRadius * 3.3f;
            float arrowXOffset = arrowOffsetFactor * (float) Math.cos(playerRot);
            float arrowYOffset = arrowOffsetFactor * (float) Math.sin(playerRot);
            texBatch.draw(Textures.arrow, centerX - arrowRenderSize / 2f + arrowXOffset, centerY - arrowRenderSize / 2f + arrowYOffset,
                    arrowRenderSize / 2f, arrowRenderSize / 2f, arrowRenderSize, arrowRenderSize,
                    1f, 1f, (float) Math.toDegrees(playerRot), 0, 0, arrowWidth, arrowHeight, onOutside, onOutside);
        }

        if (gameStarted) {
            float sizeReduce = innerRadius / 2f;
            float size = innerRadius * 2f - sizeReduce;

            float pauseAlpha = paused ?  0.9f * topTextAlpha: 0.4f * (1 - buttonAlpha);

            texBatch.setColor(colors[4].r, colors[4].g, colors[4].b, pauseAlpha);
            texBatch.draw(Textures.pause, centerX - innerRadius + sizeReduce / 2f, centerY - innerRadius + sizeReduce / 2f, size, size);
        }
        texBatch.end();

        fontBatch.begin();
        font.setColor(colors[4].r, colors[4].g, colors[4].b, 1 - buttonAlpha);
        float fontScaleFactor = 0.15f;
        if (scoreString.length() == 3) fontScaleFactor = 0.125f;
        if (scoreString.length() > 3) fontScaleFactor = 0.105f;
        font.setScale(fontScaleFactor * Gdx.graphics.getHeight() / 128f);
        BitmapFont.TextBounds bounds = font.getBounds(scoreString);
        font.draw(fontBatch, scoreString, (Gdx.graphics.getWidth() - bounds.width) / 2, (outerRadius + centerY + 0.0625f) * Gdx.graphics.getHeight() + bounds.height);
        font.setColor(colors[4].r, colors[4].g, colors[4].b, plus2Alpha);
        font.draw(fontBatch, "+2", plus2X * Gdx.graphics.getWidth(), plus2Y * Gdx.graphics.getHeight());
        fontBatch.end();

        Gdx.gl20.glEnable(GL20.GL_BLEND);
        Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);


        fontBatch.begin();

        if (topTextAlpha > 0) {
            String msg;

            if (paused) msg = resumeMessage;
            else msg = topMessage;

            font.setScale(0.08f * Gdx.graphics.getHeight() / 128f);
            font.setColor(colors[4].r, colors[4].g, colors[4].b, topTextAlpha);
            bounds = font.getBounds(msg);
            font.draw(fontBatch, msg, (Gdx.graphics.getWidth() - bounds.width) / 2, (outerRadius + centerY - (outerRadius - innerRadius) / 2f) * Gdx.graphics.getHeight());
        }

        if (playedFirstGame && !gameStarted) {
            font.setScale(0.045f * Gdx.graphics.getHeight() / 128f);

            font.setColor(colors[4].r, colors[4].g, colors[4].b, buttonAlpha);
            bounds = font.getBounds(bestString);
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

    private void drawSpikesAndDoSpikeCollisionChecks(Spike[] spikes, boolean outside) {
        for (int i = 0; i < spikes.length; i++) {
            double spikeAngle = spikes[i].angle + Math.PI / 2.0;

            if (!spikes[i].active)
                continue;

            final float sideLength = 0.05f * (float)spikes[i].protrudePercent;

            float spikeOffs = outside ? 0.045f : 0.008f;
            spikeOffs *= spikes[i].protrudePercent;

            float spike0YOffs = outside ? -sideLength : 0;
            float spike2YOffs = outside ? sideLength : -sideLength;
            float radius = (outside ? outerRadius : innerRadius) * scaleFactor;

            spikeBuffer[0].set(centerX - sideLength / 2f, centerY - radius + spikeOffs + spike0YOffs);
            spikeBuffer[1].set(spikeBuffer[0].x + sideLength, spikeBuffer[0].y);
            spikeBuffer[2].set((spikeBuffer[0].x + spikeBuffer[1].x) / 2f, spikeBuffer[0].y + spike2YOffs);

            for (int j = 0; j < 3; j++)
                rotateVector2(spikeBuffer[j], spikeAngle);

            if (spikes[i].collidable && (onInside || onOutside)) {
                for (int j = 0; j < 3; j++) {
                    if (Vector2.dst(playerX, playerY, spikeBuffer[j].x, spikeBuffer[j].y) < playerRadius) {
                        lose();
                        break;
                    }
                }
            }

            int quadrant = getQuadrant(spikeAngle, outside) - 1;
            if (quadrant < 0) quadrant = 3;
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
        setScore(0);
        assert(playing);
        playing = true;
        playedFirstGame = true;
    }

    class Spike {
        public double angle;
        public boolean active = false;
        public double protrudePercent = 0;
        public boolean movingIn = false, movingOut = false;
        public boolean collidable = true;
    }

}
