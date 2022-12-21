package io.github.diontools.donotspeak;

import android.media.AudioManager;

final class AudioFocusUtility {
    private static final String TAG = "AudioFocusUtility";

    private static final AudioManager.OnAudioFocusChangeListener listener = focusChange -> {
    };

    public static void request(AudioManager audioManager, DiagnosticsLogger logger) {
        if (logger != null) logger.Log(TAG, "request audio focus");

        int result = audioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (logger != null) logger.Log(TAG, "audio focus: " + result);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            audioManager.abandonAudioFocus(listener);
        }
    }
}
