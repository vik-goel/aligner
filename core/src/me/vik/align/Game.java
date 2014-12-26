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
    private SpriteBatch batch;
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

    private double[] innerSpikes, outerSpikes;
    private Vector2[] spike = new Vector2[]{new Vector2(), new Vector2(), new Vector2()};

    private double coinAngle;

    public void create() {
        coinAngle = 2.1;

        innerSpikes = new double[100];
        outerSpikes = new double[100];

        for (int i = 0; i < innerSpikes.length; i++) {
            innerSpikes[i] = outerSpikes[i] = Double.MAX_VALUE;
        }

        /*innerSpikes[0] = Math.PI / 4.0;
        innerSpikes[1] = 3.0 * Math.PI / 4.0;
        innerSpikes[2] = 5.0 * Math.PI / 4.0;
        innerSpikes[3] = 7.0 * Math.PI / 4.0;

        outerSpikes[0] = Math.PI / 4.0;
        outerSpikes[1] = 3.0 * Math.PI / 4.0;
        outerSpikes[2] = 5.0 * Math.PI / 4.0;
        outerSpikes[3] = 7.0 * Math.PI / 4.0;*/

        centerX = Util.getAspectRatio() / 2f;
        centerY = 0.5f;
        innerRadius = centerX * 0.3f;
        outerRadius = centerX * 0.9f;

        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, Util.getAspectRatio(), 1);

        sr = new ShapeRenderer();
        sr.setProjectionMatrix(camera.combined);

        batch = new SpriteBatch();
        font = new BitmapFont(Gdx.files.internal("score_font.fnt"), Gdx.files.internal("score_font.png"), false);
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
    }

    public void render() {
        float dt = Gdx.graphics.getDeltaTime() * 60f;

        if (onOutside || onInside)
            progressBarPercent += 0.008f * dt;

        if (progressBarPercent >= 1) {
            lose();
        }

        float innerRotationAmt = -2.05f * dt;
        float outerRotationAmt = 1.35f * dt;

        innerRotation += innerRotationAmt;

        //TODO: combine pasted code below into one function
        for (int i = 0; i < innerSpikes.length; i++) {
            if (innerSpikes[i] != Double.MAX_VALUE) {
                innerSpikes[i] -= Math.toRadians(innerRotationAmt);
                innerSpikes[i] %= Math.PI * 2.0;
                if (innerSpikes[i] < 0) innerSpikes[i] += Math.PI * 2.0;
            }
        }

        for (int i = 0; i < outerSpikes.length; i++) {
            if (outerSpikes[i] != Double.MAX_VALUE) {
                outerSpikes[i] -= Math.toRadians(outerRotationAmt);
                outerSpikes[i] %= Math.PI * 2.0;
                if (outerSpikes[i] < 0) outerSpikes[i] += Math.PI * 2.0;
            }
        }

        if (onInside)
            rotatePlayer(innerRotationAmt);

        outerRotation += outerRotationAmt;
        if (onOutside) {
            rotatePlayer(outerRotationAmt);
        }

        outerRotation %= 360f;
        innerRotation %= 360f;

        float xCenter = Util.getAspectRatio() / 2f;
        float dst = Vector2.dst(xCenter, 0.5f, playerX, playerY);
        double dir = Math.atan2(playerY - 0.5f, playerX - xCenter);
        if (dir < 0) dir += Math.PI * 2.0;

        if (Gdx.input.justTouched() && (onInside || onOutside)) {
            float dirMul = onInside ? 1 : -1;

            velX = (float) Math.cos(dir) * speed * dirMul;
            velY = (float) Math.sin(dir) * speed * dirMul;

            lastLoc = onInside;
            onInside = onOutside = false;
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

                int numSpikes = onOutside ? random.nextInt(5) + 2 : random.nextInt(3) + 1;

                double[] spikes = onOutside ? outerSpikes : innerSpikes;

                for (int i = 0; i < spikes.length; i++)
                    spikes[i] = Double.MAX_VALUE;

                //TODO: Prevent impossible scenarios (too many spikes in a given quadrant)
                Outer: for (int i = 0; i < numSpikes; i++) {
                    Inner: for (int j = 0; j < spikes.length; j++) {
                       if (j == spikes.length - 1)
                           break Outer;

                       if (spikes[j] == Double.MAX_VALUE) {
                           FindAngle: while (true) {
                               double spikeAngle = random.nextDouble() * Math.PI * 2.0;
                               spikes[j] = spikeAngle;

                               float spikeQuad = getAdjustedAngle(spikeAngle, onOutside) / 90f;
                               if (spikeQuad - (int)spikeQuad < 0.125f)
                                   continue;

                               for (int k = 0; k < spikes.length; k++) {
                                   if (k == j) continue;
                                   if (spikes[k] == Double.MAX_VALUE) continue;
                                   if (Math.abs(spikes[k] - spikes[j]) < 0.125) continue FindAngle;
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

        drawScreen();
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
        score = 0;
        progressBarPercent = 0;
    }

    private void rotatePlayer(float degrees) {
        //TODO: Dst and dir may have already been calculated elsewhere, no need to redo this
        float dst = Vector2.dst(centerX, centerY, playerX, playerY);
        double dir = Math.atan2(playerY - centerY, playerX - centerX) - Math.toRadians(degrees);

        playerX = (float)(Math.cos(dir) * dst) + centerX;
        playerY = (float)(Math.sin(dir) * dst) + centerY;
    }

    private void drawScreen() {
        Color bgColor = Color.BLACK;
        Color textColor = Color.WHITE;

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
        float coinRadius = 0.01f;
        float coinX = (float)(Math.cos(coinAngle) * coinDst + centerX);
        float coinY = (float)(Math.sin(coinAngle) * coinDst + centerY);

        if (Vector2.dst(coinX, coinY, playerX, playerY) <= coinRadius + playerRadius) {
            score += 5;

            while (true) { //TODO: Find a cleaner way to write this
                double newCoinAngle = Math.PI * 2.0 * random.nextDouble();

                if (Math.abs(newCoinAngle - coinAngle) > 0.4) {
                    coinAngle = newCoinAngle;
                    break;
                }
            }
        }

        sr.circle(coinX, coinY, coinRadius, numSegments);

        sr.end();

        batch.begin();
        font.setColor(colors[4]);
        font.setScale(0.8f * (Gdx.graphics.getWidth() / 800f));

        String scoreString = String.valueOf(score); //TODO: Check if allocating memory for this every frame is a problem

        BitmapFont.TextBounds bounds = font.getBounds(scoreString);

        font.draw(batch, scoreString, (Gdx.graphics.getWidth() - bounds.width) / 2, (Gdx.graphics.getHeight() + bounds.height) / 2);
        batch.end();
    }

    //TODO: Might want to sort spikes by color for performance
    private void drawSpikes(double[] spikes, boolean outside) {
        for (int i = 0; i < spikes.length; i++) {
            double spikeAngle = spikes[i] + Math.PI / 2.0;

            if (spikeAngle == Double.MAX_VALUE)
                continue;

            final float sideLength = 0.02f;

            float spikeOffs = outside ? 0.018f : 0.005f;
            float spike0YOffs = outside ? -sideLength : 0;
            float spike2YOffs = outside ? sideLength : -sideLength;
            float radius = outside ? outerRadius : innerRadius;

            spike[0].set(centerX - sideLength / 2f, centerY - radius + spikeOffs + spike0YOffs);
            spike[1].set(spike[0].x + sideLength, spike[0].y);
            spike[2].set((spike[0].x + spike[1].x) / 2f, spike[0].y + spike2YOffs);

            rotateSpike(spike[0], spikeAngle);
            rotateSpike(spike[1], spikeAngle);
            rotateSpike(spike[2], spikeAngle);

            for (int j = 0; j < 3; j++)
                if (Vector2.dst(playerX, playerY, spike[j].x, spike[j].y) < playerRadius)
                    lose();

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

}
