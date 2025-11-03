package Undergraduate.components;

import Utils.ThemeManager;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Plays course videos with restricted playback.
 * Added timeline (slider + time remaining label) without changing any other functions.
 */
public class RestrictedVideoPlayerFrame extends JFrame {

    private final Runnable onFinished;
    private EmbeddedMediaPlayerComponent mediaPlayer;

    private JSlider timeSlider;
    private JLabel timeLabel;
    private Timer timer;   // updates timeline every second

    static {
        System.setProperty("jna.library.path", "C:\\Program Files (x86)\\VideoLAN\\VLC");
    }

    public RestrictedVideoPlayerFrame(String title, String mediaPath, Runnable onFinished) {
        super("Playing: " + title);
        this.onFinished = onFinished;

        setSize(900, 580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        mediaPlayer = new EmbeddedMediaPlayerComponent();
        add(mediaPlayer, BorderLayout.CENTER);

        JPanel controls = createControlPanel(mediaPlayer.mediaPlayer());
        add(controls, BorderLayout.SOUTH);

        setVisible(true);
        loadAndPlay(mediaPath);
    }

    // ------------------------------------------------------------
    private JPanel createControlPanel(EmbeddedMediaPlayer player) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIManager.getColor("Panel.background"));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- timeline section (top of controls)
        JPanel timelinePanel = new JPanel(new BorderLayout(8, 0));
        timelinePanel.setOpaque(false);

        timeSlider = new JSlider(0, 1000, 0);
        timeSlider.setEnabled(false);
        timelinePanel.add(timeSlider, BorderLayout.CENTER);

        timeLabel = new JLabel("00:00 / 00:00");
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        timelinePanel.add(timeLabel, BorderLayout.EAST);

        panel.add(timelinePanel, BorderLayout.NORTH);

        // --- buttons (bottom)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 6));
        buttonPanel.setOpaque(false);

        JButton playPause = new JButton("Play");
        ThemeManager.stylePrimaryButton(playPause);
        playPause.setPreferredSize(new Dimension(120, 36));
        playPause.addActionListener(e -> {
            if (player.status().isPlaying()) {
                player.controls().pause();
                playPause.setText("Play");
            } else {
                player.controls().play();
                playPause.setText("Pause");
            }
        });

        JButton rewind = new JButton("Rewind 10s");
        ThemeManager.stylePrimaryButton(rewind);
        rewind.setPreferredSize(new Dimension(130, 36));
        rewind.addActionListener(e -> {
            long time = player.status().time();
            long newTime = Math.max(0, time - 10_000);
            player.controls().setTime(newTime);
        });

        JButton stopClose = new JButton("Stop / Close");
        ThemeManager.stylePrimaryButton(stopClose);
        stopClose.setPreferredSize(new Dimension(140, 36));
        stopClose.addActionListener(e -> confirmClose());

        buttonPanel.add(playPause);
        buttonPanel.add(rewind);
        buttonPanel.add(stopClose);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // ------------------------------------------------------------
    private void loadAndPlay(String path) {
        File file = resolveFile(path);
        if (file == null || !file.exists()) {
            JOptionPane.showMessageDialog(this, "Video not found: " + path,
                    "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        System.out.println("[VLC] Playing: " + file.getAbsolutePath());
        mediaPlayer.mediaPlayer().media().play(file.getAbsolutePath());

        blockFastForwardAndSpeed(mediaPlayer.mediaPlayer());

        // when media ends
        mediaPlayer.mediaPlayer().events().addMediaPlayerEventListener(
                new uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter() {
                    @Override
                    public void finished(uk.co.caprica.vlcj.player.base.MediaPlayer mp) {
                        if (timer != null) timer.stop();
                        if (onFinished != null) SwingUtilities.invokeLater(onFinished);
                        SwingUtilities.invokeLater(RestrictedVideoPlayerFrame.this::dispose);
                    }
                }
        );

        // start timeline updater
        startTimelineUpdater();
    }

    // ------------------------------------------------------------
    /** Updates slider + label every second while video plays */
    private void startTimelineUpdater() {
        timer = new Timer(1000, e -> {
            if (mediaPlayer == null) return;
            EmbeddedMediaPlayer player = mediaPlayer.mediaPlayer();
            long length = player.media().info().duration();
            if (length <= 0) return;
            long current = player.status().time();
            int progress = (int) (current * 1000L / length);
            timeSlider.setValue(progress);

            String elapsed = formatTime(current);
            String total = formatTime(length);
            long remaining = Math.max(0, length - current);
            timeLabel.setText(elapsed + " / " + total + "  (‑" + formatTime(remaining) + ")");
        });
        timer.start();
    }

    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0)
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        else
            return String.format("%02d:%02d", minutes, seconds);
    }

    // ------------------------------------------------------------
    private void blockFastForwardAndSpeed(EmbeddedMediaPlayer player) {
        player.events().addMediaPlayerEventListener(
                new uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter() {
                    private long lastAllowedTime = 0;

                    @Override
                    public void playing(uk.co.caprica.vlcj.player.base.MediaPlayer mp) {
                        lastAllowedTime = mp.status().time();
                    }

                    @Override
                    public void timeChanged(uk.co.caprica.vlcj.player.base.MediaPlayer mp, long newTime) {
                        long current = mp.status().time();
                        if (current > lastAllowedTime + 2000) {
                            mp.controls().setTime(lastAllowedTime);
                        } else {
                            lastAllowedTime = current;
                        }
                    }

                    
                    public void rateChanged(uk.co.caprica.vlcj.player.base.MediaPlayer mp, float newRate) {
                        if (newRate != 1.0f) {
                            mp.controls().setRate(1.0f);
                        }
                    }
                });
    }

    // ------------------------------------------------------------
    private void confirmClose() {
        if (JOptionPane.showConfirmDialog(this,
                "Stop and close video?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    @Override
    public void dispose() {
        if (timer != null) timer.stop();
        if (mediaPlayer != null) {
            mediaPlayer.mediaPlayer().controls().stop();
            mediaPlayer.release();
        }
        super.dispose();
    }

    private File resolveFile(String path) {
        File f = new File(path);
        if (f.isAbsolute() && f.exists()) return f;
        File wd = new File(System.getProperty("user.dir"), path);
        return wd.exists() ? wd : null;
    }
}