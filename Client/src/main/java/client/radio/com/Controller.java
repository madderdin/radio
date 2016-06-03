package client.radio.com;

import javafx.scene.media.MediaPlayer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Michał on 2016-05-04.
 */

@Data
@Slf4j
public class Controller {
    private Receiver receiver;
    private Playlist playlist;
    private Sender sender;
    private StreamPlayer streamPlayer;
    private Recorder recorder;
    private Socket socket;
    private App app;
    private MediaPlayer mediaPlayer;

    /**
     * To prevent from too large packets
     */
    private static long timeToWaitForPacketInSeconds = 35;

    private ExecutorService executor = Executors.newFixedThreadPool(5);
    private Thread receiverThread;
    private Thread senderThread;
    private Thread playerThread;
    private Thread appThread;


    private void startAppThread() {
        try {
            this.getExecutor().execute(appThread);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void startReceiverThread() {
        try {
            this.getExecutor().execute(receiverThread);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void handlePacketsFromReceiver() throws Exception {
        while (true) {
            DataPacket packet = getReceiver().getDataPackets().poll(timeToWaitForPacketInSeconds, TimeUnit.SECONDS);
            if (packet != null) {
                resolveHeaderType(packet);
            } else {
                throw new Exception("Packet too large!");
            }
        }
    }

    private void resolveHeaderType(DataPacket packet) {
        switch (packet.getHeader().getType()) {
            case Header.CONNECT:
                log.info("connect");
                //handleConnectionSignal(packet);
                break;
            case Header.STREAM:
                log.info("stream");
                handleStreamingMusic(packet);
                break;
            case Header.VOTES:
                //playlist.handleVoting(packet);
                break;
            default:
        }
    }

    private void handleConnectionSignal(Header header, byte[] data) {
        if (header.getParameters() == 1) {
            log.info(new String(data));
            log.info("CONNECT SIGNAL RECEIVED");
        }
        //CONNECT(Arrays.copyOfRange(h_data, 7, header.length + 7));
        else if (header.getParameters() == 2) {
            log.info(new String(data));
            log.info("DISCONNECT SIGNAL RECEIVED");
        }
        // disconnect(Arrays.copyOfRange(h_data, 7, header.length + 7));
    }

    private void handleStreamingMusic(DataPacket packet) {
        log.info("MUSIC");
        if (packet.getHeader().getParameters() == 0) {
            log.info("MP3 DATA");
            streamPlayer.handleMusicStream(packet.getMessageByte());
        } else if (packet.getHeader().getParameters() == 2) {
            log.info("NEW SONG");
            streamPlayer.handleNewSong();
        } else if (packet.getHeader().getParameters() == 1) {
            log.info("END OF SONG");
        }
    }


    private void handleVoiceMessage() {
        log.info("VOICE MESSAGE");
    }


    private void handleVoting(Header header, byte[] data) {
        log.info("VOTING");
        log.info(new String(data));
        if (header.getParameters() == 0) {
            log.info("NEW PLAYLIST");
        }
        //handleList(Arrays.copyOfRange(h_data, 7, header.length + 7));
        if (header.getParameters() == 1) {
            log.info("VOTE ACK");
        }
        //ackVote(Arrays.copyOfRange(h_data, 7, header.length + 7));
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        log.info("start");
        Controller controller = new Controller();
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        try {
            controller.setSocket(new Socket(hostName, portNumber));
        } catch (UnknownHostException e) {
            log.error("Unknown host: " + hostName);
            System.exit(1);
        } catch (IOException e) {
            log.error("No I/O");
            System.exit(1);
        }
        DataInputStream in = new DataInputStream(controller.getSocket().getInputStream());

        controller.setStreamPlayer(new StreamPlayer());
        controller.setPlayerThread(new Thread(controller.getStreamPlayer()));
        controller.getPlayerThread().start();


        Playlist playlist = new Playlist();

        Receiver receiver = new Receiver(playlist, controller, in);
        controller.setReceiver(receiver);
        controller.setReceiverThread(new Thread(controller.getReceiver()));
        controller.getReceiverThread().start();

        try {
            controller.handlePacketsFromReceiver();
        } catch (Exception e) {
            e.printStackTrace();
            //controller.closeApp();
        }
        controller.getExecutor().shutdown();
        try {
            controller.getExecutor().awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.info("executor shutdown error");
        }
    }

}
