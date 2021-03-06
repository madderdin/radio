package client.radio.com;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.csv.*;

/**
 * Created by Michał on 2016-04-23.
 */

@Data
@Slf4j
public class Playlist {

    public static char separator = '\n';
    private ConcurrentMap<Integer, Song> currentPlaylist;       //Sprawdzić jak działa
    private CSVParser parser;
    private Vector<ConcurrentMap> mapVector;
    // private StreamPlayer streamPlayer;


    public Playlist() {
        currentPlaylist = new ConcurrentHashMap<Integer, Song>();
        mapVector = new Vector<>();
    }


    public Song getNextSongToStream() {
        List<Song> list = getSongsSorted();
        Iterator<Song> iterator = list.iterator();
        while (true) {
            try {
                Song song = iterator.next();
                if (!song.isPlayed() && !song.isStreamed())
                    return song;
            } catch (NoSuchElementException e) {
                return null;
            }
        }
    }

    public Song getNextSongToPlay() {
        List<Song> list = getSongsSorted();
        Iterator<Song> iterator = list.iterator();
        while (true) {
            try {
                Song song = iterator.next();
                if (!song.isPlayed() && song.isStreamed())
                    return song;
            } catch (NoSuchElementException e) {
                return null;
            }
        }
    }

    public ArrayList<Song> getSongsToDisplay() {
        List<Song> list = new ArrayList<Song>(currentPlaylist.values());
        Comparator<Song> votingComparator = new Comparator<Song>() {
            public int compare(Song song1, Song song2) {
                return song2.getVotesNumber() - song1.getVotesNumber(); // use your logic
            }
        };
        list.sort(votingComparator);
        Iterator<Song> iterator = list.iterator();
        ArrayList<Song> toDisplay = new ArrayList<>();
        ArrayList<Song> streamedSongs = new ArrayList<>();
        streamedSongs.add(new Song());
        Song playedSong = new Song();
        while (iterator.hasNext()) {
            Song song = iterator.next();
            if (!(song.isPlayed() && !song.isStreamed()))
                toDisplay.add(song);
            else {
                if (song.isPlayed())
                    playedSong = song;//("Now Playing: " + song.getVotesNumber() + " " + song.getTitle() + "-" + song.getBand());
                else
                    streamedSongs.add(song);
            }
        }
        if (playedSong.getId() != 0) {
            streamedSongs.add(0, playedSong);
            streamedSongs.addAll(toDisplay);
            return streamedSongs;
        }
        return toDisplay;
    }

    public List<Song> getSongsSorted() {
        List<Song> list = new ArrayList<Song>(currentPlaylist.values());
        Comparator<Song> votingComparator = new Comparator<Song>() {
            public int compare(Song song1, Song song2) {
                return song2.getVotesNumber() - song1.getVotesNumber(); // use your logic
            }
        };
        list.sort(votingComparator);
        return list;
    }

    public void vote(int songId, boolean isGoodSong) {
        try {
            currentPlaylist.get(songId).setVoted(isGoodSong);
            if (isGoodSong) {
                currentPlaylist.get(songId).incVotesNumber();
            } else {
                currentPlaylist.get(songId).decVotesNumber();
            }
        } catch (NullPointerException e) {
            log.info("No such a song");
        }
    }

    private void createPlaylist(List<CSVRecord> list) {
        //log.info(list.get(0).toString());
        Iterator<CSVRecord> iterator = list.iterator();
        while (iterator.hasNext()) {
            CSVRecord record = iterator.next();
            log.info(record.toString());
            Song song = new Song(record.get(1), record.get(2), Integer.valueOf(record.get(0).trim()), Integer.valueOf(record.get(3).trim()));
            updateOrPutSong(song);
        }
        log.info(getSongsSorted().toString());
    }

    public void updateOrPutSong(Song song) {
        // Map<Integer, Song> songsToBePlayed = songsToPlay();
        if (currentPlaylist.containsKey(song.getId())) {
            if (!song.isPlayed() && !song.isStreamed()) {
                song.setVoted(currentPlaylist.get(song.getId()).isVoted());
                currentPlaylist.replace(song.getId(), song);
            }
        } else {
            currentPlaylist.put(song.getId(), song);
        }
    }

    public void deleteRemainingFiles() {
        Iterator<Song> iterator = currentPlaylist.values().iterator();
        while (iterator.hasNext()) {
            Song song = iterator.next();
            if (song.getFileName() != null && song.getFileName().length() != 0) {
                File file = new File(song.getFileName());
                file.delete();
            }
        }
    }

    public void handleNewPlaylist(byte[] data) throws IOException {
        //StringReader buffer= new StringReader(data.toString());
        log.info("Parsing new Playlist");
        Reader targetReader = new InputStreamReader(new ByteArrayInputStream(data));
        parser = new CSVParser(targetReader, CSVFormat.DEFAULT.withRecordSeparator(separator).withDelimiter('|'));

        createPlaylist(parser.getRecords());
        //streamPlayer.getStreamFilesPaths().size();
        targetReader.close();
    }
}
