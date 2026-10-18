package com.marwan.mediaplayer;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

@UnstableApi
public class MainActivity extends ComponentActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private FrameLayout playerFrame;
    private LinearLayout home;
    private TextView status;
    private Button fileButton;
    private boolean playingScreen = false;
    private boolean muted = false;
    private float previousVolume = 1f;
    private boolean cropToFill = true;
    private final float[] speeds = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f};
    private int speedIndex = 2;

    private final ActivityResultLauncher<String[]> pickMedia =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                playUri(uri);
            }
        });

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(10, 13, 22));
        getWindow().setNavigationBarColor(Color.rgb(10, 13, 22));
        buildHome();
        initializePlayer();
        setContentView(home);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(42, 112, 220)));
        return button;
    }

    private void buildHome() {
        home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.setGravity(Gravity.CENTER);
        home.setPadding(dp(28), dp(24), dp(28), dp(24));
        home.setBackgroundColor(Color.rgb(10, 13, 22));

        TextView logo = text("▶", 42, Color.rgb(100, 160, 255));
        logo.setGravity(Gravity.CENTER);
        home.addView(logo, new LinearLayout.LayoutParams(-1, -2));

        TextView heading = text("Media Player", 26, Color.WHITE);
        heading.setGravity(Gravity.CENTER);
        heading.setPadding(0, dp(8), 0, dp(8));
        home.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = text("Choose a video or audio file to begin", 14, Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, dp(22));
        home.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        fileButton = button("📁   File");
        fileButton.setTextSize(18);
        LinearLayout.LayoutParams fileParams = new LinearLayout.LayoutParams(-1, dp(58));
        fileParams.setMargins(0, dp(4), 0, dp(8));
        home.addView(fileButton, fileParams);
        fileButton.setOnClickListener(v -> openFilePicker());

        // The launch screen intentionally contains only the File action.
    }

    private void openFilePicker() {
        pickMedia.launch(new String[]{"video/*", "audio/*", "application/octet-stream", "*/*"});
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView = new PlayerView(this);
        playerView.setPlayer(player);
        playerView.setUseController(true);
        playerView.setControllerAutoShow(true);
        playerView.setControllerHideOnTouch(true);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        playerView.setBackgroundColor(Color.BLACK);
        playerView.setKeepContentOnPlayerReset(true);

        playerFrame = new FrameLayout(this);
        playerFrame.setBackgroundColor(Color.BLACK);
        playerFrame.addView(playerView, new FrameLayout.LayoutParams(-1, -1));

        Button menu = button("⋮");
        menu.setTextSize(24);
        menu.setPadding(0, 0, 0, 0);
        menu.setMinWidth(dp(44));
        menu.setMinimumWidth(dp(44));
        menu.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x990A0D16));
        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.END);
        menuParams.setMargins(dp(8), dp(8), dp(8), dp(8));
        playerFrame.addView(menu, menuParams);
        menu.setOnClickListener(v -> showOptions(menu));

        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) setStatus("Buffering…");
                else if (state == Player.STATE_READY) setStatus(player.isPlaying() ? "Playing" : "Ready");
                else if (state == Player.STATE_ENDED) setStatus("Finished");
            }

            @Override public void onPlayerError(PlaybackException error) {
                setStatus("This file could not be played.");
                Toast.makeText(MainActivity.this,
                    "Can't play this file. Try a supported MP4, WebM, MP3, or M4A file.",
                    Toast.LENGTH_LONG).show();
            }

            @Override public void onIsPlayingChanged(boolean isPlaying) {
                setStatus(isPlaying ? "Playing" : "Paused");
            }
        });
    }

    private void setStatus(String message) {
        if (status != null) status.setText(message);
    }

    private void showOptions(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("Playback speed: " + speeds[speedIndex] + "×");
        popup.getMenu().add(muted ? "Unmute" : "Mute");
        popup.getMenu().add(cropToFill ? "Fit video (show whole frame)" : "Fill screen (crop edges)");
        popup.getMenu().add("Choose another file");
        popup.setOnMenuItemClickListener(item -> {
            String choice = item.getTitle().toString();
            if (choice.startsWith("Playback speed")) {
                speedIndex = (speedIndex + 1) % speeds.length;
                player.setPlaybackSpeed(speeds[speedIndex]);
                Toast.makeText(this, "Speed: " + speeds[speedIndex] + "×", Toast.LENGTH_SHORT).show();
            } else if (choice.equals("Mute")) {
                previousVolume = player.getVolume();
                player.setVolume(0f);
                muted = true;
            } else if (choice.equals("Unmute")) {
                player.setVolume(previousVolume <= 0f ? 1f : previousVolume);
                muted = false;
            } else if (choice.startsWith("Fit video")) {
                cropToFill = false;
                playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            } else if (choice.startsWith("Fill screen")) {
                cropToFill = true;
                playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            } else if (choice.equals("Choose another file")) {
                openFilePicker();
            }
            return true;
        });
        popup.show();
    }

    private void playUri(Uri uri) {
        playingScreen = true;
        String name = uri.getLastPathSegment();
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(name == null ? "Now playing" : name);
        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();
        player.play();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(playerFrame);
        enterImmersiveFullscreen();
    }

    private void enterImmersiveFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void leavePlayer() {
        playingScreen = false;
        if (player != null) player.pause();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        setContentView(home);
    }

    @Override public void onBackPressed() {
        if (playingScreen) leavePlayer();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
