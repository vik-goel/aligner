package me.vik.align;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.Random;

public class Game extends com.badlogic.gdx.Game {

    private static final Color[] colors = {
        Util.getColor(153, 255, 102), //green
        Util.getColor(153, 204, 255), //blue
        Util.getColor(255, 255, 102), //yellow
        Util.getColor(255, 153, 204), //pink
        Util.getColor(207, 159, 255)  //purple
    };

    private ShapeRenderer sr;
    private SpriteBatch fontBatch, texBatch;
    private BitmapFont font;
    private Random random = new Random();

    private float progressBarPercent;
    private float innerRotation, outerRotation ;

    private float playerX, playerY;
    private float velX, velY, speed;
    private Color playerColor = colors[0];
    private int playerColorIndex = 0;
    private boolean onInside, onOutside, lastLoc;

    private int score;

    private float playerRadius = 0.02f;
    private float innerRadius;
    private float outerRadius;
    private float centerX, centerY;

    private Spike[] innerSpikes, outerSpikes;
    private Vector2[] spike = new Vector2[]{new Vector2(), new Vector2(), new Vector2()};

    private double coinAngle;

    private boolean playing = false, inMenu = true;
    private float menuBackgroundAlpha = 1f;
    private float topTextAlpha = 1;
    private Button[] buttons;

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

        coinAngle = 2.1;

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

        rotatePlayer(45f);

        onInside = lastLoc =  true;
        onOutside = false;

        //TODO: Spawn spikes at startup
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

                int numSpikes = onOutside ? random.nextInt(5) + 2 : random.nextInt(3) + 1;

                Spike[] spikes = onOutside ? outerSpikes : innerSpikes;

                for (int i = 0; i < spikes.length; i++) {
                    if (spikes[i].active) {
                        spikes[i].movingIn = true;
                    }
                }

                //TODO: Prevent impossible scenarios (too many spikes in a given quadrant)
                //TODO: Fix bug which causes spikes to spawn on colour boundaries
                Outer: for (int i = 0; i < numSpikes; i++) {
                    Inner: for (int j = 0; j < spikes.length; j++) {
                       if (j == spikes.length - 1)
                           break Outer;

                       if (!spikes[j].active) {
                           FindAngle: while (true) {
                               double spikeAngle = random.nextDouble() * Math.PI * 2.0;
                               spikes[j].angle = spikeAngle;
                               spikes[j].active = spikes[j].movingOut = true;
                               spikes[j].movingIn = false;
                               spikes[j].protrudePercent = 0;
                               spikes[j].collidable = true;

                               float spikeQuad = getAdjustedAngle(spikeAngle, onOutside) / 90f;
                               if (spikeQuad - (int)spikeQuad < 0.125f)
                                   continue;

                               for (int k = 0; k < spikes.length; k++) {
                                   if (k == j) continue;
                                   if (!spikes[k].active) continue;
                                   if (Math.abs(spikes[k].angle - spikes[j].angle) < 0.125) continue FindAngle;
                               }

                               if (Math.abs(spikeAngle - dir) > 0.5) {
                                   //System.out.printf("Spike Angle: %f, Player Angle: %f, Difference: %f\n", (float)spikeAngle, (float)dir, (float)Math.abs(spikeAngle - dir) );
                                   break Inner;
                               }
                           }
                       }
                    }
                }
            } else {
                playerX += velX * dt;
                playerY += velY * dt;
            }
        }

        changeSpikeProtrusions(innerSpikes, dt);
        changeSpikeProtrusions(outerSpikes, dt);
        drawScreen(dt);
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

                if (spikes[i].movingIn && spikes[i].movingOut)
                    throw new IllegalStateException("A spike cannot be both moving in and moving out at the same time!");

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
        int quadrant = (int)(getAdjustedAngle(rad, outside) / 90f);

        return quadrant;
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
        //score = 0;
        progressBarPercent = 0;
        playing = false;
        inMenu = true;
    }

    private void rotatePlayer(float degrees) {
        //TODO: Dst and dir may have already been calculated elsewhere, no need to redo this
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

        //sr.setColor(colors[4]);
        //sr.circle(xCenter, 0.5f, outerRadius * 0.99f, numSegments);

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

        sr.setColor(colors[4]);
        float coinDst = (innerRadius + outerRadius) / 2f;
        float coinRadius = 0.0125f;
        float coinX = (float)(Math.cos(coinAngle) * coinDst + centerX);
        float coinY = (float)(Math.sin(coinAngle) * coinDst + centerY);

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

        sr.end();

        fontBatch.begin();
        fontBatch.setColor(Color.WHITE);
        font.setColor(colors[4]);
        font.setScale(0.8f * (Gdx.graphics.getWidth() / 800f));
        String scoreString = String.valueOf(score); //TODO: Check if allocating memory for this every frame is a problem
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
            String msg = inMenu ? "Align" : "Tap to Start";

            fontBatch.setColor(1, 1, 1, topTextAlpha);
            font.setColor(colors[4].r, colors[4].g, colors[4].b, topTextAlpha);
            bounds = font.getBounds(msg);
            font.draw(fontBatch, msg, (Gdx.graphics.getWidth() - bounds.width) / 2, Gdx.graphics.getHeight() - 75); //TODO: Move this into normalized coordinates
        }

        fontBatch.end();

        texBatch.begin();

        if (menuBackgroundAlpha > 0) {
            texBatch.setColor(1, 1, 1, menuBackgroundAlpha);

            for (int i = 0; i < buttons.length; i++)
                buttons[i].updateAndRender(texBatch, dt);
        }

        texBatch.end();
    }

    //TODO: Might want to sort spikes by color for performance
    //TODO: Ensure inner and outer spikes are the same size
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

            spike[0].set(centerX - sideLength / 2f, centerY - radius + spikeOffs + spike0YOffs);
            spike[1].set(spike[0].x + sideLength, spike[0].y);
            spike[2].set((spike[0].x + spike[1].x) / 2f, spike[0].y + spike2YOffs);

            for (int j = 0; j < 3; j++)
                rotateSpike(spike[j], spikeAngle);

            if (spikes[i].collidable) {
                for (int j = 0; j < 3; j++) {
                    if (Vector2.dst(playerX, playerY, spike[j].x, spike[j].y) < playerRadius) {
                        spikes[i].collidable = false;
                        lose();
                    }
                }
            }

            int quadrant = getQuadrant(spikeAngle, outside) - 1;
            if (quadrant < 0) quadrant = 3;
            sr.setColor(colors[quadrant]);

            sr.triangle(spike[0].x, spike[0].y, spike[1].x, spike[1].y, spike[2].x, spike[2].y);
        }
    }

    private void rotateSpike(Vector2 vec, double rad) {
        vec.sub(centerX, centerY);
        vec.rotateRad((float) rad);
        vec.add(centerX, centerY);
    }

    public void play() {
        if (inMenu) {
            inMenu = false;
            return;
        }

        if (playing)
            throw new IllegalStateException("Already playing!");
        playing = true;
        score = 0;
    }

}

class Spike {
    public double angle;
    public boolean active = false;
    public double protrudePercent = 0;
    public boolean movingIn = false, movingOut = false;
    public boolean collidable = true;
}
