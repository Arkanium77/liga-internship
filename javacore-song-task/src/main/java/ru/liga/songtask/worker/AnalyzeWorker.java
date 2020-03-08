package ru.liga.songtask.worker;

import com.leff.midi.MidiFile;
import com.leff.midi.event.meta.Tempo;
import ru.liga.songtask.domain.Note;
import ru.liga.songtask.domain.NoteSign;
import ru.liga.songtask.util.SongUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    /**
     * <b>Получить верхний и нижний экстремумы трека</b>
     *
     * @param track трек, представленный в виде списка нот
     * @return пару нотных знаков, представляющих нижнюю и верхнюю границу диапазона,
     * в случае, если в треке нет нот - null
     */
    public static NoteSign[] getExtremumNoteSigns(List<Note> track) {
        HashMap<Integer, Note> midiOfNotes = new HashMap<>();
        for (Note n : track) {
            midiOfNotes.put(n.sign().getMidi(), n);
        }
        if (midiOfNotes.size() == 0) {
            return null;
        }
        NoteSign min = midiOfNotes.get(Collections.min(midiOfNotes.keySet())).sign();
        NoteSign max = midiOfNotes.get(Collections.max(midiOfNotes.keySet())).sign();
        return new NoteSign[]{min, max};
    }

    /**
     * <b>Получить диапазон </b> в полутонах
     *
     * @param extremeNotes нижняя и верхняя границы диапазона.
     * @return целое число - количество полутонов в представленном диапазоне.
     * В случае, если в треке нет нот - null
     */
    public static Integer getRange(NoteSign[] extremeNotes) {
        if (extremeNotes == null) {
            return null;
        }
        return extremeNotes[1].getMidi() - extremeNotes[0].getMidi();
    }

    /**
     * <b>Получить диапазон </b> в полутонах
     *
     * @param track трек, представленный в виде списка нот
     * @return целое число - количество полутонов в представленном диапазоне.
     * В случае, если в треке нет нот - null
     */
    public static Integer getRange(List<Note> track) {
        NoteSign[] extremumNotes = getExtremumNoteSigns(track);
        return getRange(extremumNotes);
    }

    /**
     * <b>Получить Tempo-event</b>
     *
     * @param midiFile файл для анализа
     * @return Tempo-event, содержащий информацию о bpm.
     */
    static Tempo getTempo(MidiFile midiFile) {
        Tempo tempo = (Tempo) (midiFile.getTracks().get(0).getEvents()).stream()
                .filter(value -> value instanceof Tempo)
                .findFirst()
                .get();
        return tempo;
    }

    /**
     * <b>Получить HashMap Длительность\число нот</b>
     *
     * @param track    анализируемый трек в виде списка нот.
     * @param midiFile файл, содержащий анализируемый трек
     * @return HashMap, содержащий пары Integer\Integer, где ключ - длительность в ms,
     * а значение - число нот этой длительности.
     */
    public static HashMap<Integer, Integer> getDurationAnalyze(List<Note> track, MidiFile midiFile) {
        HashMap<Integer, Integer> durationToCount = new HashMap<>();
        if (track == null) {
            return durationToCount;
        }
        Tempo tempo = getTempo(midiFile);
        float bpm = tempo.getBpm();

        for (Note n : track) {
            Integer noteMsDuration = SongUtils.tickToMs(bpm, midiFile.getResolution(), n.durationTicks());
            if (durationToCount.containsKey(noteMsDuration)) {
                durationToCount.put(noteMsDuration, durationToCount.get(noteMsDuration) + 1);
            } else {
                durationToCount.put(noteMsDuration, 1);
            }
        }
        return durationToCount;
    }

    /**
     * <b>Получить количество вхождений нот</b>
     *
     * @param track трек для анализа (в виде списка нот)
     * @return HashMap, где ключ - NoteSign, а значение - количество его появлений в треке.
     */
    public static HashMap<NoteSign, Integer> getNumberOfNotes(List<Note> track) {
        HashMap<NoteSign, Integer> numberOfNotes = new HashMap<>();
        if (track == null) {
            return numberOfNotes;
        }

        for (Note n : track) {
            NoteSign current = n.sign();
            if (numberOfNotes.containsKey(current)) {
                numberOfNotes.put(current, numberOfNotes.get(current) + 1);
            } else {
                numberOfNotes.put(current, 1);
            }
        }
        return numberOfNotes;
    }

}
