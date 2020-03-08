package ru.liga.songtask.worker;

import com.leff.midi.MidiFile;
import ru.liga.songtask.domain.Note;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static ru.liga.songtask.util.SongUtils.eventsToNotes;

public class AnalyzeWorker {

    /**
     * <b>Получить все треки, приемлемые для исполнения голосом</b>
     *
     * @param midiFile файл, в котором будет осуществляться поиск
     * @return {@code List<List<Note>>}, содержащий все треки для голоса ввиде списков нот.
     */
    public static List<List<Note>> getVoiceTracks(MidiFile midiFile) {
        List<List<Note>> allTracks = getAllTracksAsNoteLists(midiFile);
        List<List<Note>> voices = voiceTrackFinder(allTracks);

        return voices;
    }

    /**
     * <b>Получить все треки в виде нотных списков</b>
     *
     * @param midiFile файл для извлечения треков
     * @return {@code List<List<Note>>}, содержащий все треки ввиде списков нот.
     */
    public static List<List<Note>> getAllTracksAsNoteLists(MidiFile midiFile) {
        List<List<Note>> allTracks = new ArrayList<>();
        for (int i = 0; i < midiFile.getTracks().size(); i++) {
            allTracks.add(eventsToNotes(midiFile.getTracks().get(i).getEvents()));
        }
        return allTracks;
    }

    /**
     * <b>Поиск треков для голоса</b>
     *
     * @param allTracks все треки миди файла в виде списка списков нот.
     * @return список списков нот, содержащий только треки для голоса.
     */
    private static List<List<Note>> voiceTrackFinder(List<List<Note>> allTracks) {
        List<List<Note>> voices = new ArrayList<>();
        for (List<Note> track : allTracks) {
            boolean isVoice = isVoice(track);
            if (isVoice && !track.isEmpty()) { //исключаем пустые треки
                voices.add(track);
            }
        }
        return voices;
    }

    /**
     * <b>Проверка трека на возможность исполнения голосом</b>
     *
     * @param track трек, представленный в виде списка нот
     * @return {@code false} - если в треке есть хотя бы две ноты звучащие одновременно и
     * {@code true} в остальных случаях
     */
    private static boolean isVoice(List<Note> track) {
        long exNoteEndTick = 0;
        for (Note n : track) {
            if (exNoteEndTick > n.startTick()) {
                return false;
            }
            exNoteEndTick = n.startTick() + n.durationTicks();
        }
        return true;
    }
}
