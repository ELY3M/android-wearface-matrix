package net.dheera.wearfacematrix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Random;
import java.util.TimeZone;

public class MatrixWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "MatrixWatchFaceService";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private final int mSettingsNumRows = 23;
        private int mMatrixBaseColor = Color.GREEN;

        private Random random = new Random();

        Paint mAnalogHourPaint;
        Paint mAnalogMinutePaint;
        Paint mAnalogSecondPaint;
        Paint mDigitalActiveTimePaint;
        Paint mDigitalAmbientTimePaint;
        Paint mAnalogTickPaint;
        private Paint[] mMatrixPaints = new Paint[8];

        boolean mMute;
        Time mTime;

        private final String[] matrixChars = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
                "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3", "4",
                "5", "6", "7", "8", "9", "@", "$", "&", "%", "(", ")", "*", "%", "!", "#", "ア", "イ", "ウ", "エ",
                "オ", "カ", "キ", "ク", "ケ", "コ", "サ", "シ", "ス", "セ", "ソ", "タ", "チ", "ツ", "テ", "ト",
                "ナ", "ニ", "ヌ", "ネ", "ノ", "ハ", "ヒ", "フ", "ヘ", "ホ"};

        private String[][] mMatrixValues = new String[mSettingsNumRows][mSettingsNumRows];
        private int[][] mMatrixIntensities = new int[mSettingsNumRows][mSettingsNumRows];

        private int mCharWidth;
        private int mXOffset;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;
        boolean mLowBitAmbient;


        Paint mBackgroundPaint;

        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MatrixWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = MatrixWatchFaceService.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            /*Point size = new Point();
            getWindowManager().getDefaultDisplay().getSize(size);*/
            int screenWidth = 320;

            mCharWidth = screenWidth / mSettingsNumRows + 1;
            mXOffset = (screenWidth - mCharWidth * mSettingsNumRows)/2;

            int i, j;
            for(i = 0; i <=5 ; i++) {
                mMatrixPaints[i] = new Paint();
                mMatrixPaints[i].setColor(Color.rgb(0, i * 32, 0));
                /*mMatrixPaints[i].setColor(
                        (mMatrixBaseColor & 0xFF) / 8 * i +
                        ((mMatrixBaseColor>>8 & 0xFF) / 8 * i) << 8 +
                        ((mMatrixBaseColor>>16 & 0xFF) / 8 * i) << 16 +
                        ((mMatrixBaseColor>>24 & 0xFF) / 8 * i) << 24
                ); */
                mMatrixPaints[i].setTextSize(mCharWidth - 1);
                mMatrixPaints[i].setAntiAlias(false);
            }

            mMatrixPaints[6] = new Paint();
            mMatrixPaints[6].setColor(Color.rgb(63, 255, 63));
            mMatrixPaints[6].setTextSize(mCharWidth - 1);
            mMatrixPaints[6].setAntiAlias(false);
            mMatrixPaints[7] = new Paint();
            mMatrixPaints[7].setColor(Color.rgb(191, 255, 191));
            mMatrixPaints[7].setTextSize(mCharWidth - 1);
            mMatrixPaints[7].setAntiAlias(false);

            mDigitalActiveTimePaint = new Paint();
            mDigitalActiveTimePaint.setColor(Color.rgb(255, 255, 255));
            mDigitalActiveTimePaint.setTextSize((int) (screenWidth / 3.5));
            mDigitalActiveTimePaint.setTextAlign(Paint.Align.CENTER);
            mDigitalActiveTimePaint.setAntiAlias(true);
            // mDigitalActiveTimePaint.setStyle(Paint.Style.STROKE);
            // mDigitalActiveTimePaint.setTypeface(Typeface.createFromFile(new File(R.raw.orbitron_medium)));
            mDigitalActiveTimePaint.setTypeface(Typeface.createFromAsset(resources.getAssets(), "miltown2.ttf"));
            mDigitalActiveTimePaint.setShadowLayer(20f, 0f, 0f, Color.GREEN);

            mDigitalAmbientTimePaint = new Paint();
            mDigitalAmbientTimePaint.setColor(Color.rgb(255, 255, 255));
            mDigitalAmbientTimePaint.setTextSize((int) (screenWidth / 3.5));
            mDigitalAmbientTimePaint.setTextAlign(Paint.Align.CENTER);
            mDigitalAmbientTimePaint.setAntiAlias(true);
            mDigitalAmbientTimePaint.setTypeface(Typeface.createFromAsset(resources.getAssets(), "miltown2.ttf"));

            for(i = 0; i < mSettingsNumRows; i++) {
                for (j = 0; j < mSettingsNumRows; j++) {
                    mMatrixValues[i][j] = matrixChars[random.nextInt(matrixChars.length)];
                    mMatrixIntensities[i][j]=0;
                }
            }

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setARGB(255, 0, 0, 0);

            mAnalogHourPaint = new Paint();
            mAnalogHourPaint.setARGB(255, 200, 200, 200);
            mAnalogHourPaint.setStrokeWidth(5.f);
            mAnalogHourPaint.setAntiAlias(true);
            mAnalogHourPaint.setStrokeCap(Paint.Cap.ROUND);

            mAnalogMinutePaint = new Paint();
            mAnalogMinutePaint.setARGB(255, 200, 200, 200);
            mAnalogMinutePaint.setStrokeWidth(3.f);
            mAnalogMinutePaint.setAntiAlias(true);
            mAnalogMinutePaint.setStrokeCap(Paint.Cap.ROUND);

            mAnalogSecondPaint = new Paint();
            mAnalogSecondPaint.setARGB(255, 255, 0, 0);
            mAnalogSecondPaint.setStrokeWidth(2.f);
            mAnalogSecondPaint.setAntiAlias(true);
            mAnalogSecondPaint.setStrokeCap(Paint.Cap.ROUND);

            mAnalogTickPaint = new Paint();
            mAnalogTickPaint.setARGB(100, 255, 255, 255);
            mAnalogTickPaint.setStrokeWidth(2.f);
            mAnalogTickPaint.setAntiAlias(true);

            mTime = new Time();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mAnalogHourPaint.setAntiAlias(antiAlias);
                mAnalogMinutePaint.setAntiAlias(antiAlias);
                mAnalogSecondPaint.setAntiAlias(antiAlias);
                mAnalogTickPaint.setAntiAlias(antiAlias);
            }
            invalidate();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                mAnalogHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mAnalogMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mAnalogSecondPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "onDraw");
            }
            long now = System.currentTimeMillis();
            mTime.set(now);
            int milliseconds = (int) (now % 1000);

            int width = bounds.width();
            int height = bounds.height();

            if (!isInAmbientMode()) {
                // Draw the background, scaled to fit.
                if (mBackgroundScaledBitmap == null
                        || mBackgroundScaledBitmap.getWidth() != width
                        || mBackgroundScaledBitmap.getHeight() != height) {
                    mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                            width, height, true /* filter */);
                }
                canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
            } else {
                canvas.drawRect(0,0,width,height, mBackgroundPaint);
            }

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;

            /*
            // Draw the ticks.
            float innerTickRadius = centerX - 10;
            float outerTickRadius = centerX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(centerX + innerX, centerY + innerY,
                        centerX + outerX, centerY + outerY, mAnalogTickPaint);
            }

            float seconds = mTime.second + milliseconds / 1000f;
            float secRot = seconds / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f ) * (float) Math.PI;

            float secLength = centerX - 20;
            float minLength = centerX - 40;
            float hrLength = centerX - 80;

            if (!isInAmbientMode()) {
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mAnalogSecondPaint);
            }

            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mAnalogMinutePaint);

            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mAnalogHourPaint);
            */

            // Draw the matrix background

            if (!isInAmbientMode()) {
                int i, j;
                for (i = 0; i < mSettingsNumRows; i++) {
                    for (j = mSettingsNumRows - 1; j > 0; j--) {
                        if (mMatrixIntensities[i][j] == 7 || (j < 5 && random.nextInt(24) == 0)) {
                            canvas.drawText(mMatrixValues[i][j], mXOffset + i * mCharWidth, j * mCharWidth, mMatrixPaints[7]);
                            if (random.nextInt(2) == 0) {
                                if (j < mSettingsNumRows - 1) {
                                    mMatrixIntensities[i][j + 1] = 7;
                                    mMatrixValues[i][j + 1] = matrixChars[random.nextInt(matrixChars.length)];
                                }
                                mMatrixIntensities[i][j] = 6;
                            }
                        } else {
                            if (mMatrixIntensities[i][j] > 0) {
                                canvas.drawText(mMatrixValues[i][j], mXOffset + i * mCharWidth, j * mCharWidth, mMatrixPaints[mMatrixIntensities[i][j]]);
                            }
                            mMatrixIntensities[i][j] += random.nextInt(5) - 3;
                            if (mMatrixIntensities[i][j] < 0) mMatrixIntensities[i][j] = 0;
                            if (mMatrixIntensities[i][j] >= 7) mMatrixIntensities[i][j] = 6;
                        }
                    }
                }
            }

            if (!isInAmbientMode()) {
                String timeString = String.format("%02d:%02d", mTime.hour, mTime.minute);
                canvas.drawText(timeString, centerX, centerY + mDigitalActiveTimePaint.getTextSize() / 3, mDigitalActiveTimePaint);
            } else {
                String timeString = String.format("%02d:%02d", mTime.hour, mTime.minute);
                canvas.drawText(timeString, centerX, centerY + mDigitalAmbientTimePaint.getTextSize() / 3, mDigitalAmbientTimePaint);
            }

            // Draw every frame as long as we're visible and in interactive mode.
            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
                invalidate();
            } else {
                unregisterReceiver();
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MatrixWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MatrixWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }
    }

}