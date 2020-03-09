package ru.liga.songtask.worker;

import com.leff.midi.MidiFile;
import com.leff.midi.event.meta.Tempo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liga.songtask.domain.Note;
import ru.liga.songtask.domain.NoteSign;
import ru.liga.songtask.util.SongUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static ru.liga.songtask.util.SongUtils.eventsToNotes;

public class AnalyzeWorker {
    private static Logger logger = LoggerFactory.getLogger(AnalyzeWorker.class);

    /**
     * <b>Получить все треки, приемлемые для исполнения голосом</b>
     *
     * @param midiFile файл, в котором будет осуществляться поиск
     * @return {@code List<List<Note>>}, содержащий все треки для голоса ввиде списков нот.
     */
    public static List<List<Note>> getVoiceTracks(MidiFile midiFile) {
        logger.trace("Процедура получения треков, пригодных для исполнения голосом.");
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
        logger.trace("Процедура извлечерия треков из файла в виде List<Notes>");
        List<List<Note>> allTracks = new ArrayList<>();
        for (int i = 0; i < midiFile.getTracks().size(); i++) {
            allTracks.add(eventsToNotes(midiFile.getTracks().get(i).getEvents()));
        }
        logger.trace("Извлечены все треки {} из файла.", allTracks.size());
        return allTracks;
    }

    /**
     * <b>Поиск треков для голоса</b>
     *
     * @param allTracks все треки миди файла в виде списка списков нот.
     * @return список списков нот, содержащий только треки для голоса.
     */
    private static List<List<Note>> voiceTrackFinder(List<List<Note>> allTracks) {
        logger.trace("Поиск треков пригодных для исполнения голосом.");
        List<List<Note>> voices = new ArrayList<>();
        for (List<Note> track : allTracks) {
            boolean isVoice = isVoice(track);
            if (isVoice && !track.isEmpty()) { //исключаем пустые треки
                voices.add(track);
            }
        }
        logger.trace("Найдено {} треков пригодных для исполнения голосом.", voices.size());
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
        logger.trace("Проверка трека на пригодность для исполнения голосом.");
        long exNoteEndTick = 0;
        for (Note n : track) {
            if (exNoteEndTick > n.startTick()) {
                logger.trace("Непригоден.");
                return false;
            }
            exNoteEndTick = n.startTick() + n.durationTicks();
        }
        logger.trace("Пригоден.");
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
        logger.trace("Поиск экстремумов трека.");
        HashMap<Integer, Note> midiOfNotes = new HashMap<>();
        for (Note n : track) {
            midiOfNotes.put(n.sign().getMidi(), n);
        }
        if (midiOfNotes.size() == 0) {
            logger.trace("Трек пуст, возвращён null");
            return null;
        }
        NoteSign min = midiOfNotes.get(Collections.min(midiOfNotes.keySet())).sign();
        NoteSign max = midiOfNotes.get(Collections.max(midiOfNotes.keySet())).sign();
        logger.trace("Нижний экстремум трека {}, верхний экстремум трека {}", min.fullName(), max.fullName());
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
        logger.trace("Поиск диапазона трека.");
        if (extremeNotes == null) {
            logger.trace("Нет экстремумов (трек пуст), возвращён null");
            return null;
        }
        logger.trace("Найденный диапазон составляет {} полутонов", extremeNotes[1].getMidi() - extremeNotes[0].getMidi());
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
     * <b>Получить HashMap Длительность\число нот</b>
     *
     * @param track    анализируемый трек в виде списка нот.
     * @param midiFile файл, содержащий анализируемый трек
     * @return HashMap, содержащий пары Integer\Integer, где ключ - длительность в ms,
     * а значение - число нот этой длительности.
     */
    public static HashMap<Integer, Integer> getDurationAnalyze(List<Note> track, MidiFile midiFile) {
        logger.trace("Анализ нот трека по длительности.");
        HashMap<Integer, Integer> durationToCount = new HashMap<>();
        if (track == null) {
            logger.trace("Трек пуст, возвращёна пустая HashMap");
            return durationToCount;
        }
        Tempo tempo = SongUtils.getTempo(midiFile);
        float bpm = tempo.getBpm();
        logger.trace("bpm = {}", bpm);
        for (Note n : track) {
            Integer noteMsDuration = SongUtils.tickToMs(bpm, midiFile.getResolution(), n.durationTicks());
            if (durationToCount.containsKey(noteMsDuration)) {
                durationToCount.put(noteMsDuration, durationToCount.get(noteMsDuration) + 1);
            } else {
                durationToCount.put(noteMsDuration, 1);
            }
        }
        logger.trace("Анализ завершён. Найдено {} разных длительностей", durationToCount.size());
        return durationToCount;
    }

    /**
     * <b>Получить количество вхождений нот</b>
     *
     * @param track трек для анализа (в виде списка нот)
     * @return HashMap, где ключ - NoteSign, а значение - количество его появлений в треке.
     */
    public static HashMap<NoteSign, Integer> getNumberOfNotes(List<Note> track) {
        logger.trace("Анализ нот трека по числе вхождений.");
        HashMap<NoteSign, Integer> numberOfNotes = new HashMap<>();
        if (track == null) {
            logger.trace("Трек пуст, возвращёна пустая HashMap");
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
        logger.trace("Анализ завершён. Найдено {} разных длительностей", numberOfNotes.size());
        return numberOfNotes;
    }

}
