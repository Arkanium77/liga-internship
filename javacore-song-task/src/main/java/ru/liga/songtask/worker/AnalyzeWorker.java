package ru.liga.songtask.worker;

import com.leff.midi.MidiFile;
import ru.liga.songtask.domain.Note;
import ru.liga.songtask.domain.NoteSign;

import java.util.*;

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

        return voiceTrackFinder(allTracks);
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

    public static NoteSign[] getExtremumNoteSigns(List<Note> track) {
        HashMap<Integer, Note> midiOfNotes = new HashMap<>();
        for (Note n : track) {
            midiOfNotes.put(n.sign().getMidi(), n);
        }
        if(midiOfNotes.size()==0){
            return null;
        }
        NoteSign min = midiOfNotes.get(Collections.min(midiOfNotes.keySet())).sign();
        NoteSign max = midiOfNotes.get(Collections.max(midiOfNotes.keySet())).sign();
        return new NoteSign[]{min, max};
    }
    public static Integer getRange(NoteSign [] extremeNotes){
        if(extremeNotes==null){
            return null;
        }
        return extremeNotes[1].getMidi()-extremeNotes[0].getMidi();
    }

    public static Integer getRange(List<Note> track){
        NoteSign [] extremumNotes= getExtremumNoteSigns(track);
        return getRange(extremumNotes);
    }

}
