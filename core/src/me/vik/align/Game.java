package me.vik.align;

import com.badlogic.gdx.Gdx;
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

//TODO: Add difficulty curve (change spike amounts)

//TODO: Particle effects on death, this might be unnecessary
//TODO: Player Motion trail

//TODO: Add leaderboards
//TODO: Change ad id`s

public class Game extends com.badlogic.gdx.Game {

    private static final Color[] colors = {
        //Tried making colours brighter, darker versions are commented out
        Util.getColor(197, 255, 139),//Util.getColor(153, 255, 102), //green
        Util.getColor(197, 236, 255),//Util.getColor(153, 204, 255), //blue
        Util.getColor(255, 255, 139),//Util.getColor(255, 255, 102), //yellow
        Util.getColor(255, 197, 236),//.getColor(255, 153, 204), //pink
        Util.getColor(237, 203, 255) //Util.getColor(207, 159, 255)  //purple
    };

    public static final String fileOutputName = "me.vik.align";

    private String[] intStrings;
    private int score, highscore;
    private String scoreString, highscoreString;
    private String highscorePrefsString = "highScore";

    private OrthographicCamera camera;
    private boolean shaking = false;
    private float shakeVelX = 0, shakeVelY = 0;
    private float minShakeVel = 0.004f, initialShakeVel = 0.0125f, shakeEnergyLoss = 0.85f;
    private float shakeTime = 0, shakeTimeCounter = 0;
    private float minShakeTime = 1, maxShakeTime = 3;

    private ShapeRenderer sr;
    private SpriteBatch fontBatch, texBatch;
    private BitmapFont font;
    private Random random = new Random();

    private float progressBarPercent;
    private float innerRotation, outerRotation;

    private float playerX, playerY;
    private float playerVelX, playerVelY, playerSpeed;
    private Color playerColor = colors[0];
    private int playerColorIndex = 0;
    private float playerRadius = 0.02f;
    private boolean onInside, onOutside, lastLoc;

    private float innerRadius;
    private float outerRadius;
    private float centerX, centerY;

    private Spike[] innerSpikes, outerSpikes;
    private Vector2[] spikeBuffer = new Vector2[]{new Vector2(), new Vector2(), new Vector2()};

    private double coinAngle;
    private float plus5X, plus5Y, plus5Alpha = 0, plus5VelX, plus5VelY;

    private boolean playing = false, playedFirstGame = false, inMenu = true, paused = false, gameStarted = false;
    private float menuBackgroundAlpha = 1f;
    private float topTextAlpha = 1;
    private Button[] menuButtons;

    private float scaleFactor = 1f, scaleVelocity = 0f, scaleAcceleration =  0.0008f;

    private static final String align = "Aligner", tapMessage = "Tap to Launch", resumeMessage = "Tap to Resume", scoreTitleString = "Score", bestString = "Best";
    private String lastMessage;

    private MyLeaderboard leaderboard;

    public void create() {
        Textures.loadTextures();
        Sounds.init();

        intStrings = new String[1000];
        for (int i = 0; i < intStrings.length; i++)
            intStrings[i] = String.valueOf(i);

        centerX = Util.getAspectRatio() / 2f;
        centerY = 0.5f;
        innerRadius = centerX * 0.3f;
        outerRadius = centerX * 0.85f;

        float menuButtonRadius = (outerRadius - innerRadius) / 4f;
        float menuButtonX = menuButtonRadius * 2.5f;
        float range = outerRadius - innerRadius - menuButtonRadius * 2;
        float menuButtonY = centerY - innerRadius - menuButtonRadius - range / 3f;//(innerRadius + outerRadius) / 2f + innerRadius + buttonRadius + buttonXOffs / 4;

        LeaderboardButton leaderboardButton = new LeaderboardButton(centerX - menuButtonX, menuButtonY, menuButtonRadius);
        leaderboardButton.setLeaderboard(leaderboard);



        menuButtons = new Button[]{
                new PlayButton(centerX, menuButtonY, menuButtonRadius, this),
                new SoundButton(centerX + menuButtonX, menuButtonY, menuButtonRadius),
                leaderboardButton
        };

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

        progressBarPercent = 0;
        innerRotation = 0;
        outerRotation = 0;

        setScore(0);

        playerX = centerX;
        playerY = innerRadius * scaleFactor + playerRadius + centerY;
        playerVelX = playerVelY = 0;
        playerSpeed = 0.00625f;

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
        if (playing && gameStarted) paused = true;
    }

    public void render() {
        float unpausedDt = Gdx.graphics.getDeltaTime() * 60f;
        float dt = paused ? 0 : unpausedDt;

        updateProgressBarPercent(dt);
        rotateWorld(dt);
        changeMenuBackgroundAlpha(dt);
        shake(dt);
        updatePlus5(dt);

        float xCenter = Util.getAspectRatio() / 2f;
        float dst = Vector2.dst(xCenter, 0.5f, playerX, playerY);
        double dir = Math.atan2(playerY - 0.5f, playerX - xCenter);
        if (dir < 0) dir += Math.PI * 2.0;

        if (!inMenu) {
            if (Util.justDown()) {
                if (paused || (gameStarted && Vector2.dst(centerX, centerY, Util.getTouchX(), Util.getTouchY()) < innerRadius)) {
                    paused = !paused;
                } else if (onInside || onOutside) {
                    if (!playing) play();
                    gameStarted = true;

                    float dirMul = onInside ? 1 : -1;

                    playerVelX = (float) Math.cos(dir) * playerSpeed * dirMul;
                    playerVelY = (float) Math.sin(dir) * playerSpeed * dirMul;

                    lastLoc = onInside;
                    onInside = onOutside = false;

                    Sounds.playRandom(Sounds.jump);
                }
            }
        }
        if (playing)  changeButtonAlpha(paused, unpausedDt);
        else changeButtonAlpha(true, dt);

        if (!onInside && !onOutside) {
            final float unscaledMinDst = innerRadius + playerRadius;
            final float unscaledMaxDst = outerRadius - playerRadius;
            final float minDst = innerRadius * scaleFactor + playerRadius;
            final float maxDst = outerRadius * scaleFactor - playerRadius;

            boolean changeCol = false;

            if (dst <= minDst && !lastLoc) {
                playerX = (float)(Math.cos(dir) * minDst + centerX );
                playerY = (float)(Math.sin(dir) * minDst + centerY );

                onInside = true;
                changeCol = true;
            }
            if (dst >= maxDst && lastLoc) {
                playerX = (float)(Math.cos(dir) * maxDst + centerX );
                playerY = (float)(Math.sin(dir) * maxDst + centerY );

                onOutside = true;
                changeCol = true;
            }

            if (changeCol) {
                int quadrant = getQuadrant(dir, onOutside);

                if (quadrant != playerColorIndex) {
                    lose();
                } else {
                    progressBarPercent = 0;
                    if (playing) setScore(score + 1);
                }

                int randColIndex = random.nextInt(3);

                if (playerColor == colors[randColIndex]) {
                    playerColor = colors[3];
                    playerColorIndex = 3;
                }
                else {
                    playerColor = colors[randColIndex];
                    playerColorIndex = randColIndex;
                }

                spawnSpikes(dir);
            } else {
                playerX += playerVelX * dt;
                playerY += playerVelY * dt;

                float range = unscaledMaxDst - unscaledMinDst;

                boolean shouldIncreaseScale = lastLoc ? dst - unscaledMinDst < range / 2.5f :
                                                        dst - unscaledMinDst >= range - range / 2.5f ;

                if (shouldIncreaseScale) {
                    if (scaleVelocity < 0) scaleVelocity = 0;
                    scaleVelocity += scaleAcceleration;
                }
                else {
                    if (scaleVelocity > 0) scaleVelocity = 0;
                    scaleVelocity -= scaleAcceleration;
                }

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
        drawScreen(dt);
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

    private void updatePlus5(float dt) {
        plus5Alpha -= 0.025 * dt;
        plus5X += plus5VelX * dt;
        plus5Y += plus5VelY * dt;
        if (plus5Alpha < 0) plus5Alpha = 0;
    }

    private void spawnSpikes(double playerDir) {
        int numSpikes = onOutside ? random.nextInt(7) + 2 : random.nextInt(4) + 1;

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
                        final float minEdgeDistance = 0.125f;

                        if (edgeDistance < minEdgeDistance || edgeDistance > (1f - minEdgeDistance))
                            continue;

                        if (Math.abs(spikeAngle - playerDir) < 0.5) continue;

                        int numSpikesInQuadrant = 1;
                        int jQuadrant = (int) spikeQuad;

                        for (int k = 0; k < spikes.length; k++) {
                            if (k == j) continue;
                            if (!spikes[k].active) continue;

                            int kQuadrant = getQuadrant(spikes[k].angle, onOutside);
                            if (kQuadrant == jQuadrant) numSpikesInQuadrant++;

                            if (Math.abs(spikes[k].angle - spikeAngle) < 0.225) continue FindAngle;
                        }

                        if ((onOutside && numSpikesInQuadrant > 4) ||
                            ((!onOutside) && numSpikesInQuadrant > 2)) continue;

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
            progressBarPercent += 2.05 / 360f * dt;

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

    private void changeMenuBackgroundAlpha(float dt) {
        final float menuBackgroundMovement = 0.04f * dt;
        if (inMenu && menuBackgroundAlpha < 1) menuBackgroundAlpha += menuBackgroundMovement;
        if (!inMenu && menuBackgroundAlpha > 0) menuBackgroundAlpha -= menuBackgroundMovement;
        if (menuBackgroundAlpha < 0) menuBackgroundAlpha = 0;
        if (menuBackgroundAlpha > 1) menuBackgroundAlpha = 1;
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

    private void changeButtonAlpha(boolean increase, float dt) {
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

        scaleFactor = 1;
        progressBarPercent = 0;
        playing = false;
        inMenu = true;
        gameStarted = false;
        coinAngle = getRandomRad();

        initShake();
        Sounds.playRandom(Sounds.lose);
    }

    private void rotatePlayer(float degrees) {
        float dst = Vector2.dst(centerX, centerY, playerX, playerY);
        double dir = Math.atan2(playerY - centerY, playerX - centerX) - Math.toRadians(degrees);

        playerX = (float)(Math.cos(dir) * dst) + centerX;
        playerY = (float)(Math.sin(dir) * dst) + centerY;
    }

    private void drawScreen(float dt) {
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

        sr.setColor(bgColor);
        sr.arc(xCenter, 0.5f, 1.1f, 90, 360f * (progressBarPercent), numSegments);

        sr.setColor(colors[0]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.035f * scaleFactor, -outerRotation, 90f, numSegments);

        sr.setColor(colors[1]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.035f * scaleFactor, 90 - outerRotation, 90f, numSegments);

        sr.setColor(colors[2]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.035f * scaleFactor, 180 - outerRotation, 90f, numSegments);

        sr.setColor(colors[3]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.035f * scaleFactor, 270 - outerRotation, 90f, numSegments);

        sr.setColor(bgColor);
        sr.circle(xCenter, 0.5f, outerRadius * scaleFactor, numSegments);

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

        drawSpikes(innerSpikes, false);
        drawSpikes(outerSpikes, true);

        if (playing && score > 0) {
            Gdx.gl20.glEnable(GL20.GL_BLEND);
            Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            sr.setColor(colors[4].r, colors[4].g, colors[4].b, 1f - topTextAlpha);
            float coinDst = (innerRadius + outerRadius) / 2f * scaleFactor;
            float coinRadius = 0.0125f;
            float coinX = (float) (Math.cos(coinAngle) * coinDst + centerX);
            float coinY = (float) (Math.sin(coinAngle) * coinDst + centerY);

            sr.circle(coinX, coinY, coinRadius, numSegments);

            if (Vector2.dst(coinX, coinY, playerX, playerY) <= coinRadius + playerRadius) {
                setScore(score + 5);

                Sounds.playRandom(Sounds.coin);

                plus5X = coinX;
                plus5Y = coinY;
                plus5Alpha = 1;

                double plus5Dir = Math.PI / 2.0;
                final double plus5Speed = 0.001;
                plus5VelX = (float)(Math.cos(plus5Dir) * plus5Speed);
                plus5VelY = (float)(Math.sin(plus5Dir) * plus5Speed);

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

        if (!inMenu && gameStarted) {
            float sizeReduce = innerRadius / 2f;
            float size = innerRadius * 2f - sizeReduce;

            float pauseAlpha = paused ?  0.9f * topTextAlpha: 0.1f * (1 - menuBackgroundAlpha);

            texBatch.begin();
            texBatch.setColor(colors[4].r, colors[4].g, colors[4].b, pauseAlpha);
            texBatch.draw(Textures.pause, centerX - innerRadius + sizeReduce / 2f, centerY - innerRadius + sizeReduce / 2f, size, size);
            texBatch.end();
        }

        fontBatch.begin();
        font.setColor(colors[4].r, colors[4].g, colors[4].b, 1 - menuBackgroundAlpha);
        float fontScaleFactor = 0.15f;
        if (scoreString.length() == 3) fontScaleFactor = 0.125f;
        if (scoreString.length() > 3) fontScaleFactor = 0.105f;
        font.setScale(fontScaleFactor * Gdx.graphics.getHeight() / 128f);
        BitmapFont.TextBounds bounds = font.getBounds(scoreString);
        if (!paused) font.draw(fontBatch, scoreString, (Gdx.graphics.getWidth() - bounds.width) / 2, (Gdx.graphics.getHeight() + bounds.height) / 2f );
        font.setColor(colors[4].r, colors[4].g, colors[4].b, plus5Alpha);
        font.draw(fontBatch, "+5", plus5X * Gdx.graphics.getWidth(), plus5Y * Gdx.graphics.getHeight());
        fontBatch.end();

        Gdx.gl20.glEnable(GL20.GL_BLEND);
        Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        final float menuCol = 0f;
        sr.setColor(menuCol, menuCol, menuCol, menuBackgroundAlpha * 0.25f);
        sr.rect(0f, 0f, Util.getAspectRatio(), 1f);
        sr.end();

        fontBatch.begin();

        if (topTextAlpha > 0) {
            String msg;

            if (inMenu) msg = align;
            else if (paused) msg = resumeMessage;
            else msg = lastMessage;

            lastMessage = msg;

            fontBatch.setColor(1, 1, 1, topTextAlpha);
            font.setScale(0.08f * Gdx.graphics.getHeight() / 128f);
            font.setColor(colors[4].r, colors[4].g, colors[4].b, topTextAlpha);
            bounds = font.getBounds(msg);
            font.draw(fontBatch, msg, (Gdx.graphics.getWidth() - bounds.width) / 2, (outerRadius + centerY - (outerRadius - innerRadius) / 2f) * Gdx.graphics.getHeight());

            if (playedFirstGame) {
                font.setScale(0.045f * Gdx.graphics.getHeight() / 128f);

                font.setColor(colors[4].r, colors[4].g, colors[4].b, menuBackgroundAlpha);
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
        }

        fontBatch.end();

        texBatch.begin();

        if (menuBackgroundAlpha > 0) {
            texBatch.setColor(1, 1, 1, menuBackgroundAlpha);

            for (int i = 0; i < menuButtons.length; i++)
                 menuButtons[i].updateAndRender(texBatch, dt, menuBackgroundAlpha == 1 && inMenu);
        }

        texBatch.end();
    }

    private void drawSpikes(Spike[] spikes, boolean outside) {
        for (int i = 0; i < spikes.length; i++) {
            double spikeAngle = spikes[i].angle + Math.PI / 2.0;

            if (!spikes[i].active)
                continue;

            final float sideLength = 0.0275f * (float)spikes[i].protrudePercent;

            float spikeOffs = outside ? 0.022f : 0.005f;
            spikeOffs *= spikes[i].protrudePercent;

            float spike0YOffs = outside ? -sideLength : 0;
            float spike2YOffs = outside ? sideLength : -sideLength;
            float radius = (outside ? outerRadius : innerRadius) * scaleFactor;

            spikeBuffer[0].set(centerX - sideLength / 2f, centerY - radius + spikeOffs + spike0YOffs);
            spikeBuffer[1].set(spikeBuffer[0].x + sideLength, spikeBuffer[0].y);
            spikeBuffer[2].set((spikeBuffer[0].x + spikeBuffer[1].x) / 2f, spikeBuffer[0].y + spike2YOffs);

            for (int j = 0; j < 3; j++)
                rotateVector2(spikeBuffer[j], spikeAngle);

            if (spikes[i].collidable) {
                for (int j = 0; j < 3; j++) {
                    if (Vector2.dst(playerX, playerY, spikeBuffer[j].x, spikeBuffer[j].y) < playerRadius) {
                        for (int k = 0; k < spikes.length; k++) {
                            spikes[k].collidable = false;
                            spikes[k].movingIn = true;
                        }

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
        if (inMenu) {
            inMenu = false;
            lastMessage = tapMessage;
            return;
        }

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
