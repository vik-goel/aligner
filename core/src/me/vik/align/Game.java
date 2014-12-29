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

//TODO: Enlarge and then shrink back world on jump
//TODO: Camera shake on death
//TODO: Particle effects on death
//TODO: Particle effects on pick up coin
//TODO: Player Motion trail
//TODO: More jump sounds
//TODO: Death sounds
//TODO: Coin pick up sounds
//TODO: Button press sounds
//TODO: Add leaderboards
//TODO: Background music
//TODO: Add banner ads to the top and bottom of the screen
//TODO: Add pause button which is available while playing game
//TODO: Add way to toggle music while playing game
//TODO: Think of a better title than 'Align'
//TODO: Use a different font for drawing text

public class Game extends com.badlogic.gdx.Game {

    private static final Color[] colors = {
        Util.getColor(153, 255, 102), //green
        Util.getColor(153, 204, 255), //blue
        Util.getColor(255, 255, 102), //yellow
        Util.getColor(255, 153, 204), //pink
        Util.getColor(207, 159, 255)  //purple
    };

    public static final String fileOutputName = "me.vik.align";

    private ShapeRenderer sr;
    private SpriteBatch fontBatch, texBatch;
    private BitmapFont font;
    private Random random = new Random();

    private float progressBarPercent;
    private float innerRotation, outerRotation;

    private float playerX, playerY;
    private float velX, velY, speed;
    private Color playerColor = colors[0];
    private int playerColorIndex = 0;
    private boolean onInside, onOutside, lastLoc;

    private int score, highscore;
    private String highscorePrefsString = "highScore";

    private float playerRadius = 0.02f;
    private float innerRadius;
    private float outerRadius;
    private float centerX, centerY;

    private Spike[] innerSpikes, outerSpikes;
    private Vector2[] spikeBuffer = new Vector2[]{new Vector2(), new Vector2(), new Vector2()};

    private double coinAngle;

    private boolean playing = false, inMenu = true;
    private float menuBackgroundAlpha = 1f;
    private float topTextAlpha = 1;
    private Button[] buttons;

    private static final String align = "Align", tapToStart = "Tap to Start", scoreTitleString = "Score: ", bestString = "Best: ";

    public void create() {
        Textures.loadTextures();
        Sounds.init();

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

        coinAngle = random.nextDouble() * Math.PI * 2.0;

        innerSpikes = new Spike[100];
        outerSpikes = new Spike[100];

        //assumes innerspikes and outerspikes arrays are the same length
        for (int i = 0; i < innerSpikes.length; i++) {
            innerSpikes[i] = new Spike();
            outerSpikes[i] = new Spike();
        }

        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, Util.getAspectRatio(), 1);

         sr = new ShapeRenderer();
         sr.setProjectionMatrix(camera.combined);

        texBatch = new SpriteBatch();
        texBatch.setProjectionMatrix(camera.combined);

        fontBatch = new SpriteBatch();
        font = new BitmapFont(Gdx.files.internal("fonts/score_font.fnt"), Gdx.files.internal("fonts/score_font.png"), false);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        progressBarPercent = 0;
        innerRotation = 0;
        outerRotation = 0;

        score = 0;

        playerX = centerX;
        playerY = innerRadius + playerRadius + centerY;
        velX = velY = 0;
        speed = 0.005f;

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

        float xCenter = Util.getAspectRatio() / 2f;
        float dst = Vector2.dst(xCenter, 0.5f, playerX, playerY);
        double dir = Math.atan2(playerY - 0.5f, playerX - xCenter);
        if (dir < 0) dir += Math.PI * 2.0;

        if (!playing && !inMenu && Util.justDown())
            play();

        if (playing) {
            if (Util.justDown() && (onInside || onOutside)) {
                float dirMul = onInside ? 1 : -1;

                velX = (float) Math.cos(dir) * speed * dirMul;
                velY = (float) Math.sin(dir) * speed * dirMul;

                lastLoc = onInside;
                onInside = onOutside = false;
            }

            changeButtonAlpha(false, dt);
        } else {
            changeButtonAlpha(true, dt);
        }

        if (!onInside && !onOutside) {
            final float minDst = innerRadius + playerRadius;
            final float maxDst = outerRadius - playerRadius;

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
                    score++;
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

                if (Sounds.isEnabled()) {
                    Sounds.hit[random.nextInt(Sounds.hit.length)].play();
                }

                if (playing)
                    spawnSpikes(dir);
            } else {
                playerX += velX * dt;
                playerY += velY * dt;
            }
        }

        changeSpikeProtrusions(innerSpikes, dt);
        changeSpikeProtrusions(outerSpikes, dt);
        drawScreen(dt);
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

                        double spikeAngle = random.nextDouble() * Math.PI * 2.0;

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

            Preferences prefs = Gdx.app.getPreferences(Game.fileOutputName);
            prefs.putInteger(highscorePrefsString, highscore);
            prefs.flush();

            //TODO: Send score to leaderboards
        }

        progressBarPercent = 0;
        playing = false;
        inMenu = true;
        coinAngle = random.nextDouble() * Math.PI * 2.0;
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
        sr.arc(xCenter, 0.5f, outerRadius * 1.08f, -outerRotation, 90f, numSegments);

        sr.setColor(colors[1]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.08f, 90 - outerRotation, 90f, numSegments);

        sr.setColor(colors[2]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.08f, 180 - outerRotation, 90f, numSegments);

        sr.setColor(colors[3]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.08f, 270 - outerRotation, 90f, numSegments);

        sr.setColor(bgColor);
        sr.arc(xCenter, 0.5f, 1, 90, 360f * (/*1 - */progressBarPercent), numSegments);

        sr.setColor(colors[0]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.035f, -outerRotation, 90f, numSegments);

        sr.setColor(colors[1]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.035f, 90 - outerRotation, 90f, numSegments);

        sr.setColor(colors[2]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.035f, 180 - outerRotation, 90f, numSegments);

        sr.setColor(colors[3]);
        sr.arc(xCenter, 0.5f, outerRadius * 1.035f, 270 - outerRotation, 90f, numSegments);

        sr.setColor(bgColor);
        sr.circle(xCenter, 0.5f, outerRadius, numSegments);

        sr.setColor(colors[0]);
        sr.arc(xCenter, 0.5f, innerRadius, -innerRotation, 90f, numSegments);

        sr.setColor(colors[1]);
        sr.arc(xCenter, 0.5f, innerRadius, 90 - innerRotation, 90f, numSegments);

        sr.setColor(colors[2]);
        sr.arc(xCenter, 0.5f, innerRadius, 180 - innerRotation, 90f, numSegments);

        sr.setColor(colors[3]);
        sr.arc(xCenter, 0.5f, innerRadius, 270 - innerRotation, 90f, numSegments);

        sr.setColor(bgColor);
        sr.circle(xCenter, 0.5f, innerRadius * 0.9f, numSegments);

        sr.setColor(playerColor);
        sr.circle(playerX, playerY, playerRadius, numSegments);

        drawSpikes(innerSpikes, false);
        drawSpikes(outerSpikes, true);

        if (playing) {
            sr.setColor(colors[4]);
            float coinDst = (innerRadius + outerRadius) / 2f;
            float coinRadius = 0.0125f;
            float coinX = (float) (Math.cos(coinAngle) * coinDst + centerX);
            float coinY = (float) (Math.sin(coinAngle) * coinDst + centerY);

            if (Vector2.dst(coinX, coinY, playerX, playerY) <= coinRadius + playerRadius) {
                score += 5;

                while (true) {
                    double newCoinAngle = Math.PI * 2.0 * random.nextDouble();

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
        font.setScale(0.75f * (Gdx.graphics.getWidth() / 800f));
        String scoreString = String.valueOf(score);
        BitmapFont.TextBounds bounds = font.getBounds(scoreString);
        font.draw(fontBatch, scoreString, (Gdx.graphics.getWidth() - bounds.width) / 2, (Gdx.graphics.getHeight() + bounds.height) / 2);
        fontBatch.end();

        Gdx.gl20.glEnable(GL20.GL_BLEND);
        Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        final float menuCol = 0f;
        sr.setColor(menuCol, menuCol, menuCol, menuBackgroundAlpha * 0.5f);
        sr.rect(0f, 0f, Util.getAspectRatio(), 1f);
        sr.end();

        fontBatch.begin();

        if (topTextAlpha > 0) {
            String msg = inMenu ? align : tapToStart;

            fontBatch.setColor(1, 1, 1, topTextAlpha);
            font.setColor(colors[4].r, colors[4].g, colors[4].b, topTextAlpha);
            bounds = font.getBounds(msg);
            font.draw(fontBatch, msg, (Gdx.graphics.getWidth() - bounds.width) / 2, (outerRadius + 0.5f) * Gdx.graphics.getHeight() - bounds.height * 2);

            final float scaleFactor = -0.3f;
            final float scoreStringXOffset = Gdx.graphics.getWidth() / 25f;
            font.scale(scaleFactor);
            font.setColor(colors[4].r, colors[4].g, colors[4].b, menuBackgroundAlpha);
            bounds = font.getBounds(bestString);
            float xPos = (Gdx.graphics.getWidth() - bounds.width) / 2 - scoreStringXOffset;
            float yPos = Gdx.graphics.getHeight() / 2f + bounds.height;
            font.draw(fontBatch, bestString, xPos, yPos);
            font.draw(fontBatch, String.valueOf(highscore), xPos + bounds.width, yPos);
            bounds = font.getBounds(scoreTitleString);
            yPos = Gdx.graphics.getHeight() / 2f - bounds.height;
            font.draw(fontBatch, scoreTitleString, xPos, yPos);
            font.draw(fontBatch, scoreString, xPos + bounds.width, yPos);
            font.scale(-scaleFactor);
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
            float radius = outside ? outerRadius : innerRadius;

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
        score = 0;
        if (inMenu) {
            inMenu = false;
            return;
        }

        assert(playing);
        playing = true;
    }

    class Spike {
        public double angle;
        public boolean active = false;
        public double protrudePercent = 0;
        public boolean movingIn = false, movingOut = false;
        public boolean collidable = true;
    }

}
