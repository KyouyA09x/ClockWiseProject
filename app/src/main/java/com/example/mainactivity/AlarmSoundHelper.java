package com.example.mainactivity;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

/**
 * Helper class to manage alarm sound playback for notifications
 * Uses Ringtone API for more reliable alarm playback
 */
public class AlarmSoundHelper {
    private static final String TAG = "AlarmSoundHelper";
    private static Ringtone ringtone;
    private static MediaPlayer mediaPlayer;
    
    /**
     * Play default alarm sound continuously (looping)
     * Uses Ringtone API which is more reliable for alarms
     * @param context Application context
     */
    public static void playAlarmSound(Context context) {
        stopAlarmSound(); // Stop any existing alarm first
        
        try {
            // Get default alarm sound
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                // Fallback to notification sound if alarm not available
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            
            if (alarmUri == null) {
                // Last resort - use ringtone
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            
            Log.d(TAG, "Using alarm URI: " + alarmUri);
            
            // Try using Ringtone first (simpler and more reliable)
            try {
                ringtone = RingtoneManager.getRingtone(context, alarmUri);
                
                if (ringtone != null) {
                    // Set audio attributes for alarm - THIS ENSURES ALARM VOLUME STREAM IS USED
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build();
                        ringtone.setAudioAttributes(audioAttributes);
                        Log.d(TAG, "✅ Ringtone audio attributes set to USAGE_ALARM (uses alarm volume)");
                    }

                    // Enable looping on supported devices
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone.setLooping(true);
                        Log.d(TAG, "✅ Ringtone looping enabled");
                    }
                    
                    ringtone.play();
                    Log.d(TAG, "✅ Alarm sound playing via Ringtone API (looping=" + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) + ")");
                    
                    // If Ringtone doesn't support looping on older devices, use MediaPlayer
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        playWithMediaPlayer(context, alarmUri);
                    }
                    return;
                }
            } catch (Exception e) {
                Log.w(TAG, "Ringtone API failed, falling back to MediaPlayer: " + e.getMessage());
            }
            
            // Fallback to MediaPlayer
            playWithMediaPlayer(context, alarmUri);
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing alarm sound: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Play alarm using MediaPlayer (fallback method)
     * Uses ALARM audio stream to ensure alarm volume is used, not media volume
     */
    private static void playWithMediaPlayer(Context context, Uri alarmUri) {
        try {
            Log.d(TAG, "🎵 Initializing MediaPlayer for alarm playback");
            
            mediaPlayer = new MediaPlayer();
            
            // CRITICAL: Set audio stream type to ALARM
            // This ensures the alarm uses the device's "Alarm Volume" not "Media Volume"
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                Log.d(TAG, "✅ MediaPlayer set to STREAM_ALARM (uses alarm volume)");
            }

            // Set audio attributes for alarm (API 21+)
            // USAGE_ALARM ensures alarm volume stream is used
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build();
                mediaPlayer.setAudioAttributes(audioAttributes);
                Log.d(TAG, "✅ MediaPlayer audio attributes set to USAGE_ALARM (uses alarm volume)");
            }

            // Set data source
            mediaPlayer.setDataSource(context, alarmUri);
            
            // Set looping
            mediaPlayer.setLooping(true);
            
            // Set volume to maximum (within the alarm volume stream)
            mediaPlayer.setVolume(1.0f, 1.0f);
            
            Log.d(TAG, "📊 MediaPlayer configuration complete, preparing...");
            
            // Use async preparation to avoid blocking
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "✅ MediaPlayer prepared, starting playback NOW");
                try {
                    mp.start();
                    
                    // Verify it's actually playing
                    if (mp.isPlaying()) {
                        Log.d(TAG, "✅✅✅ ALARM SOUND IS NOW PLAYING via MediaPlayer (looping, using ALARM volume)");
                    } else {
                        Log.e(TAG, "❌ MediaPlayer failed to start - trying to start again");
                        mp.start(); // Try one more time
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error starting MediaPlayer: " + e.getMessage());
                }
            });
            
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "❌ MediaPlayer error: what=" + what + ", extra=" + extra);
                return false; // Allow default error handling
            });
            
            mediaPlayer.setOnInfoListener((mp, what, extra) -> {
                Log.d(TAG, "MediaPlayer info: what=" + what + ", extra=" + extra);
                return false;
            });
            
            mediaPlayer.prepareAsync();
            Log.d(TAG, "⏳ MediaPlayer prepare started (async)...");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error with MediaPlayer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Stop the alarm sound
     */
    public static void stopAlarmSound() {
        // Stop Ringtone
        if (ringtone != null) {
            try {
                if (ringtone.isPlaying()) {
                    ringtone.stop();
                    Log.d(TAG, "Ringtone alarm stopped");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error stopping ringtone: " + e.getMessage());
            } finally {
                ringtone = null;
            }
        }
        
        // Stop MediaPlayer
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    Log.d(TAG, "MediaPlayer alarm stopped");
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping MediaPlayer: " + e.getMessage());
            } finally {
                mediaPlayer = null;
            }
        }
    }
    
    /**
     * Check if alarm is currently playing
     */
    public static boolean isPlaying() {
        boolean ringtonePlaying = ringtone != null && ringtone.isPlaying();
        boolean mediaPlayerPlaying = mediaPlayer != null && mediaPlayer.isPlaying();
        return ringtonePlaying || mediaPlayerPlaying;
    }
}