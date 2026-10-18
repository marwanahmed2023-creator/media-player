package com.marwan.mediaplayer;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

@UnstableApi
public class MainActivity extends ComponentActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private TextView title, status;
    private LinearLayout rootLayout;
    private boolean fullscreen = false;
    private boolean muted = false;
    private float volumeBeforeMute = 1f;
    private final float[] speeds = {0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f};
    private int speedIndex = 3;

    private final ActivityResultLauncher<String[]> pickMedia =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (Exception ignored) {}
                playUri(uri);
            }
        });

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(12, 15, 25));
        getWindow().setNavigationBarColor(Color.rgb(12, 15, 25));
        buildUi();
        initializePlayer();
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(42, 53, 78))); return b;
    }
    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        rootLayout = root;
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16), dp(14), dp(16), dp(12));
        root.setBackgroundColor(Color.rgb(12, 15, 25));
        TextView brand = text("MEDIA PLAYER", 13, Color.rgb(108, 185, 255)); brand.setLetterSpacing(0.12f); root.addView(brand);
        title = text("Choose a video or audio file", 20, Color.WHITE); title.setPadding(0, dp(8), 0, dp(12)); root.addView(title);
        playerView = new PlayerView(this); playerView.setUseController(true);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING); playerView.setBackgroundColor(Color.BLACK);
        root.addView(playerView, new LinearLayout.LayoutParams(-1, 0, 1f));
        status = text("Ready · Select a file to start", 13, Color.LTGRAY); status.setPadding(0, dp(10), 0, dp(4)); root.addView(status);
        Button open = button("＋  Open video or audio"); open.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(42, 112, 220)));
        open.setOnClickListener(v -> pickMedia.launch(new String[]{"video/*", "audio/*"}));
        root.addView(open, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout row1 = row(root);
        Button playPause = button("▶ / Ⅱ  Play");
        playPause.setOnClickListener(v -> { if (player == null) return; if (player.isPlaying()) player.pause(); else player.play(); });
        row1.addView(playPause, weight());
        Button back = button("↶ 10s");
        back.setOnClickListener(v -> { if (player != null) player.seekTo(Math.max(0, player.getCurrentPosition() - 10000)); });
        row1.addView(back, weight());
        Button forward = button("10s ↷");
        forward.setOnClickListener(v -> { if (player != null) player.seekTo(player.getCurrentPosition() + 10000); });
        row1.addView(forward, weight());
        LinearLayout row2 = row(root);
        Button mute = button("🔊  Mute");
        mute.setOnClickListener(v -> {
            if (player == null) return;
            muted = !muted;
            if (muted) { volumeBeforeMute = player.getVolume(); player.setVolume(0f); mute.setText("🔇  Unmute"); }
            else { player.setVolume(volumeBeforeMute <= 0f ? 1f : volumeBeforeMute); mute.setText("🔊  Mute"); }
        });
        row2.addView(mute, weight());
        Button speed = button("Speed 1.0×");
        speed.setOnClickListener(v -> {
            speedIndex = (speedIndex + 1) % speeds.length;
            float value = speeds[speedIndex]; if (player != null) player.setPlaybackSpeed(value);
            speed.setText("Speed " + value + "×");
        });
        row2.addView(speed, weight());
        Button fullscreenButton = button("⛶  Fullscreen");
        fullscreenButton.setOnClickListener(v -> {
            if (!fullscreen) {
                fullscreen = true;
                rootLayout.removeView(playerView);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                setContentView(playerView);
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            } else {
                fullscreen = false;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                setContentView(rootLayout);
                rootLayout.addView(playerView, 2, new LinearLayout.LayoutParams(-1, 0, 1f));
            }
        });
        row2.addView(fullscreenButton, weight());
        TextView hint = text("Use the timeline in the video controls to jump to any point.", 12, Color.rgb(155, 166, 190));
        hint.setPadding(0, dp(8), 0, 0); root.addView(hint);
        setContentView(root);
    }
    private LinearLayout row(LinearLayout root) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER); root.addView(row); return row;
    }
    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f); p.setMargins(dp(2), dp(3), dp(2), dp(3)); return p;
    }
    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build(); playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) status.setText("Buffering…");
                else if (state == Player.STATE_READY) status.setText(player.isPlaying() ? "Playing" : "Ready");
                else if (state == Player.STATE_ENDED) status.setText("Finished");
            }
            @Override public void onPlayerError(PlaybackException error) {
                status.setText("Can't play this file or codec.");
                Toast.makeText(MainActivity.this, "Playback failed: format or codec may not be supported.", Toast.LENGTH_LONG).show();
            }
            @Override public void onIsPlayingChanged(boolean playing) { status.setText(playing ? "Playing" : "Paused"); }
        });
    }
    private void playUri(Uri uri) {
        String name = uri.getLastPathSegment(); title.setText(name == null ? "Now playing" : name);
        player.setMediaItem(MediaItem.fromUri(uri)); player.prepare(); player.play(); status.setText("Loading media…");
    }
    @Override protected void onDestroy() {
        super.onDestroy(); if (player != null) { player.release(); player = null; }
    }
}
