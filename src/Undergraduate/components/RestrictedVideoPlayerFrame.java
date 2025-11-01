package Undergraduate.components;

import Utils.ThemeManager;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class RestrictedVideoPlayerFrame extends JFrame {

    private final Runnable onFinished;
    private EmbeddedMediaPlayerComponent mediaPlayer;

    static {
        System.setProperty("jna.library.path", "C:\\Program Files\\VideoLAN\\VLC");
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

    private JPanel createControlPanel(EmbeddedMediaPlayer player) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        panel.setBackground(UIManager.getColor("Panel.background"));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Play / Pause Button
        JButton playPause = new JButton("Play");
        ThemeManager.stylePrimaryButton(playPause);
        playPause.setPreferredSize(new Dimension(120, 38));
        playPause.addActionListener(e -> {
            if (player.status().isPlaying()) {
                player.controls().pause();
                playPause.setText("Play");
            } else {
                player.controls().play();
                playPause.setText("Pause");
            }
        });

        // Rewind 10s Button
        JButton rewind = new JButton("Rewind 10s");
        ThemeManager.stylePrimaryButton(rewind);
        rewind.setPreferredSize(new Dimension(130, 38));
        rewind.addActionListener(e -> {
            long time = player.status().time();
            long newTime = Math.max(0, time - 10_000);
            player.controls().setTime(newTime);
        });

        // Stop / Close Button
        JButton stopClose = new JButton("Stop / Close");
        ThemeManager.stylePrimaryButton(stopClose);
        stopClose.setPreferredSize(new Dimension(140, 38));
        stopClose.addActionListener(e -> confirmClose());

        panel.add(playPause);
        panel.add(rewind);
        panel.add(stopClose);

        return panel;
    }

    private void loadAndPlay(String path) {
        File file = resolveFile(path);
        if (file == null || !file.exists()) {
            JOptionPane.showMessageDialog(this, "Video not found: " + path, "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        System.out.println("[VLC] Playing: " + file.getAbsolutePath());
        mediaPlayer.mediaPlayer().media().play(file.getAbsolutePath());

        blockFastForwardAndSpeed(mediaPlayer.mediaPlayer());

        mediaPlayer.mediaPlayer().events().addMediaPlayerEventListener(
                new uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter() {
                    @Override
                    public void finished(uk.co.caprica.vlcj.player.base.MediaPlayer mp) {
                        if (onFinished != null) SwingUtilities.invokeLater(onFinished);
                        SwingUtilities.invokeLater(RestrictedVideoPlayerFrame.this::dispose);
                    }
                }
        );
    }

    private void blockFastForwardAndSpeed(EmbeddedMediaPlayer player) {
        player.events().addMediaPlayerEventListener(
                new uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter() {
                    private long lastAllowedTime = 0;

                    @Override
                    public void playing(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer) {
                        lastAllowedTime = mediaPlayer.status().time();
                    }

                    @Override
                    public void timeChanged(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer, long newTime) {
                        long current = mediaPlayer.status().time();
                        if (current > lastAllowedTime + 2000) {
                            mediaPlayer.controls().setTime(lastAllowedTime);
                        } else {
                            lastAllowedTime = current;
                        }
                    }

                    
                    public void rateChanged(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer, float newRate) {
                        if (newRate != 1.0f) {
                            mediaPlayer.controls().setRate(1.0f);
                        }
                    }
                }
        );
    }

    private void confirmClose() {
        if (JOptionPane.showConfirmDialog(this, "Stop and close video?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    @Override
    public void dispose() {
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
        if (wd.exists()) return wd;
        return null;
    }
}