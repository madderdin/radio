package client.radio.com;


import javazoom.jl.player.Player;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Michał on 2016-04-23.
 */

@Data
@Slf4j
public class StreamPlayer implements Runnable {
    private String fileToPlayPath;
    private String fileToAppendPath;
    private FileInputStream fileInputStream;
    private boolean running = true;
    private Player playMP3;
    private Playlist playlist;
    private Controller controller;

    public StreamPlayer(Playlist playlist) {
        this.playlist = playlist;
    }

    @Override
    public synchronized void run() {
        log.info("player thread start");
        try {
            Song nextSong = null;
            while (playlist.getNextSongToPlay() == null) {
                log.info("czekam");
                try {
                    synchronized (Thread.currentThread()) {
                        Thread.currentThread().wait();
                        nextSong = playlist.getNextSongToPlay();
                    }
                } catch (Exception e) {
                    deleteActiveFile();
                    return;
                }
            }
            controller.updatePlaylist();

            fileToPlayPath = nextSong.getFileName();
            while (running && fileToPlayPath != null) {
                nextSong.setPlayed(true);
                fileInputStream = new FileInputStream(fileToPlayPath);
                log.info("songToPlay:" + fileToPlayPath);
                while (fileInputStream.available() < 100 && running) {
                    fileInputStream = new FileInputStream(fileToPlayPath);
                }
                playMP3 = new Player(fileInputStream);
                playMP3.play();
                while (!playMP3.isComplete() && running) {
                    Thread.sleep(5);
                }
                if (running) {
                    controller.updatePlaylist();
                    playMP3.close();
                    fileInputStream.close();
                    File file = new File(fileToPlayPath);
                    try {
                        file.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    nextSong.setPlayed(false);
                    nextSong.setStreamed(false);
                    nextSong.setVoted(false);
                    nextSong.setVotesNumber(0);
                    log.info("Set Votes to 0");

                    while (playlist.getNextSongToPlay() == null) {
                        synchronized (Thread.currentThread()) {
                            Thread.currentThread().wait();
                            log.info("player thread awaken");
                        }
                    }
                    nextSong = playlist.getNextSongToPlay();
                    if (nextSong != null) {
                        fileToPlayPath = nextSong.getFileName();
                    } else {
                        fileToPlayPath = null;
                    }
                }
            }
        } catch (InterruptedException ie) {
            deleteActiveFile();
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        deleteActiveFile();
        log.info("playerThread done");
        return;

    }

    private void deleteActiveFile() {
        try {
            fileInputStream.close();
            File file = new File(fileToPlayPath);
            file.delete();
        } catch (Exception e) {
            log.info("playerThread done");
            return;
        }
    }

    public void handleNewSong(int songId) {
        Song song = playlist.getCurrentPlaylist().get(songId);
        //getNextSongToStream();
        song.setStreamed(true);
        song.setFileName("stream" + song.getId());
        createNewStreamFile(song.getFileName());
    }

    public void handleMusicStream(byte[] data, int songId) {
        fileToAppendPath = playlist.getCurrentPlaylist().get(songId).getFileName();
        try (FileOutputStream output = new FileOutputStream(fileToAppendPath, true)) {
            output.write(data);
        } catch (Exception e) {
            log.info("BOOM!");
            e.printStackTrace();
        }
    }

    private void createNewStreamFile(String streamFilePath) {
        File newFile = new File(streamFilePath);
        try {
            newFile.createNewFile();
        } catch (IOException e) {
            log.info("Error during new file creation");
        }
        fileToAppendPath = streamFilePath;
    }

    public void stopPlayerThread() {
        if (playMP3 != null) {
            playMP3.close();
        }
        running = false;
        synchronized (controller.getPlayerThread()) {
            controller.getPlayerThread().interrupt();
        }
        playlist.deleteRemainingFiles();    //@TODO Deleting last file
    }
}
