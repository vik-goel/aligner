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

//TODO: Change texture for leaderboard button, possibly the play button too
//TODO: The bottoms of the buttons look a little bit cut off, fix this

//TODO: Particle effects on death
//TODO: Particle effects on pick up coin
//TODO: Player Motion trail

//TODO: Add pause button which is available while playing game
//TODO: Add way to toggle music while playing game
//TODO: Think of a better title than 'Align'

//TODO: Add leaderboards
//TODO: Add banner ads to the top and bottom of the screen

public class Game extends com.badlogic.gdx.Game {

    private static final Color[] colors = {
        Util.getColor(153, 255, 102), //green
        Util.getColor(153, 204, 255), //blue
        Util.getColor(255, 255, 102), //yellow
        Util.getColor(255, 153, 204), //pink
        Util.getColor(207, 159, 255)  //purple
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

    private boolean playing = false, playedFirstGame = false, inMenu = true;
    private float menuBackgroundAlpha = 1f;
    private float topTextAlpha = 1;
    private Button[] buttons;

    private float scaleFactor = 1f, scaleVelocity = 0f, scaleAcceleration =  0.0008f;

    private static final String align = "Align", tapMessage = "Tap to Launch", scoreTitleString = "Score", bestString = "Best";

    public void create() {
        Textures.loadTextures();
        Sounds.init();

        intStrings = new String[1000];
        for (int i = 0; i < intStrings.length; i++)
            intStrings[i] = String.valueOf(i);

        centerX = Util.getAspectRatio() / 2f;
        centerY = 0.5f;
        innerRadius = centerX * 0.3f;
        outerRadius = centerX * 0.9f;

        float buttonRadius = (outerRadius - innerRadius) / 4f;
        float buttonXOffs = buttonRadius * 2.5f;
        float buttonY = (innerRadius + outerRadius) / 2f + innerRadius + buttonRadius + buttonXOffs / 4;

        buttons = new Button[]{
                new PlayButton(centerX, buttonY, buttonRadius, this),
                new SoundButton(centerX + buttonXOffs, buttonY, buttonRadius),
                new LeaderboardButton(centerX - buttonXOffs, buttonY, buttonRadius)
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
        font = new BitmapFont(Gdx.files.internal("fonts/KristenITC.fnt"), Gdx.files.internal("fonts/KristenITC.png"), false);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        progressBarPercent = 0;
        innerRotation = 0;
        outerRotation = 0;

        setScore(0);

        playerX = centerX;
        playerY = innerRadius * scaleFactor + playerRadius + centerY;
        playerVelX = playerVelY = 0;
        playerSpeed = 0.005f * (Gdx.graphics.getWidth() / 480f);

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

    public void render() {
        float dt = Gdx.graphics.getDeltaTime() * 60f;

        updateProgressBarPercent(dt);
        rotateWorld(dt);
        changeMenuBackgroundAlpha(dt);
        shake(dt);

        float xCenter = Util.getAspectRatio() / 2f;
        float dst = Vector2.dst(xCenter, 0.5f, playerX, playerY);
        double dir = Math.atan2(playerY - 0.5f, playerX - xCenter);
        if (dir < 0) dir += Math.PI * 2.0;

        if (!playing && !inMenu && Util.justDown())
            play();

        if (playing) {
            if (Util.justDown() && (onInside || onOutside)) {
                float dirMul = onInside ? 1 : -1;

                playerVelX = (float) Math.cos(dir) * playerSpeed * dirMul;
                playerVelY = (float) Math.sin(dir) * playerSpeed * dirMul;

                lastLoc = onInside;
                onInside = onOutside = false;

                Sounds.playRandom(Sounds.jump);
            }

            changeButtonAlpha(false, dt);
        } else {
            changeButtonAlpha(true, dt);
        }

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
                    setScore(score + 1);
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

                if (playing)
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
        }

        if (onInside || onOutside) {
            if (scaleVelocity != 0)
                System.out.println(scaleVelocity);

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

                        if (Math.abs(spikeAngle - playerDir) < 0.5) continue;

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
            progressBarPercent += 0.008f * dt;

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

            //TODO: Send score to leaderboards
        }

        scaleFactor = 1;
        progressBarPercent = 0;
        playing = false;
        inMenu = true;
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
        sr.arc(xCenter, 0.5f, 1f, 90, 360f * (progressBarPercent), numSegments);

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

        if (playing) {
            sr.setColor(colors[4]);
            float coinDst = (innerRadius + outerRadius) / 2f * scaleFactor;
            float coinRadius = 0.0125f;
            float coinX = (float) (Math.cos(coinAngle) * coinDst + centerX);
            float coinY = (float) (Math.sin(coinAngle) * coinDst + centerY);

            if (Vector2.dst(coinX, coinY, playerX, playerY) <= coinRadius + playerRadius) {
                setScore(score + 5);

                Sounds.playRandom(Sounds.coin);

                while (true) {
                    double newCoinAngle = getRandomRad();

                    if (Math.abs(newCoinAngle - coinAngle) > 0.4) {
                        coinAngle = newCoinAngle;
                        break;
                    }
                }
            }

            sr.circle(coinX, coinY, coinRadius, numSegments);
        }
        sr.end();

        fontBatch.begin();
        font.setColor(colors[4].r, colors[4].g, colors[4].b, 1 - menuBackgroundAlpha);
        font.setScale(0.7f * (Gdx.graphics.getWidth() / 800f));
        BitmapFont.TextBounds bounds = font.getBounds(scoreString);
        font.draw(fontBatch, scoreString, (Gdx.graphics.getWidth() - bounds.width) / 2, (Gdx.graphics.getHeight() + bounds.height) / 2);
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
            String msg = inMenu ? align : tapMessage;

            fontBatch.setColor(1, 1, 1, topTextAlpha);
            font.setColor(colors[4].r, colors[4].g, colors[4].b, topTextAlpha);
            bounds = font.getBounds(msg);
            font.draw(fontBatch, msg, (Gdx.graphics.getWidth() - bounds.width) / 2, (outerRadius + 0.5f) * Gdx.graphics.getHeight() - bounds.height * 1.75f);

            if (playedFirstGame) {
                final float scoreStringScaleFactor = -0.225f;
                font.scale(scoreStringScaleFactor);

                font.setColor(colors[4].r, colors[4].g, colors[4].b, menuBackgroundAlpha);
                bounds = font.getBounds(bestString);
                final float bestYSeperation = bounds.height * 0.075f;
                float yOffs = bounds.height * 1.5f + bestYSeperation;
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

                font.scale(-scoreStringScaleFactor);
            }
        }

        fontBatch.end();

        texBatch.begin();

        if (menuBackgroundAlpha > 0) {
            texBatch.setColor(1, 1, 1, menuBackgroundAlpha);

            for (int i = 0; i < buttons.length; i++)
                 buttons[i].updateAndRender(texBatch, dt, menuBackgroundAlpha == 1 && inMenu);
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
