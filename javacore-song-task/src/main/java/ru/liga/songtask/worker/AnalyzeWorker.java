package ru.liga.songtask.worker;

import com.leff.midi.MidiFile;
import com.leff.midi.event.meta.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liga.songtask.domain.Note;
import ru.liga.songtask.domain.NoteSign;
import ru.liga.songtask.util.SongUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
        List<List<Note>> allTracks = SongUtils.getAllTracksAsNoteLists(midiFile);

        return voiceTrackFinder(allTracks);
    }

    /**
     * <b>Поиск треков для голоса</b>
     *
     * @param allTracks все треки миди файла в виде списка списков нот.
     * @return список списков нот, содержащий только треки для голоса.
     */
    private static List<List<Note>> voiceTrackFinder(List<List<Note>> allTracks) {
        logger.trace("Поиск треков пригодных для исполнения голосом.");
        List<List<Note>> voices;
        voices = allTracks.stream()
                .filter(notes -> isVoice(notes) && !notes.isEmpty()) //исключаем пустые треки
                .collect(Collectors.toList());

        logger.trace("Найдено {} треков пригодных для исполнения голосом.", voices.size());
        return voices;
    }

    /**
     * <b>Поиск трека для голоса по тексту</b>
     *
     * @param midiFile midi-файл для поиска
     * @return трек, соответствующий текстовому сопровождению файла в формате List<Note>
     */
    public static List<Note> getVoiceTrack(MidiFile midiFile) {
        logger.debug("Поиск трека, близкого по длинне к текстовому сопровождению.");
        List<List<Note>> maybe = AnalyzeWorker.getVoiceTracks(midiFile);
        long countOfTextEvents = getCountOfTextEvents(midiFile);
        logger.debug("Всего TextEvent в файле {}", countOfTextEvents);

        List<Long> difference = maybe.stream()
                .map(notes -> Math.abs(notes.size() - countOfTextEvents))
                .collect(Collectors.toList());
        logger.debug("Собран список абсолютной разности между длиной пригодных для " +
                "исполнения голосом треков и числом текстовых ивентов\n{}", difference);

        long minDifference = Collections.min(difference);
        List<Note> result = maybe.get(difference.indexOf(minDifference));
        logger.debug("Список с минимальной разностью и есть трек для голоса. \n{}", result);

        return result;
    }

    /**
     * <b>Подсчитать общее число текстовых событий в midi-файле</b>
     *
     * @param midiFile midi-файл для обработки
     * @return long-число, соответствующее числу midiEvent класса Text.
     */
    private static long getCountOfTextEvents(MidiFile midiFile) {
        return midiFile.getTracks().stream()
                .flatMap(midiTrack -> midiTrack.getEvents().stream())
                .filter(midiEvent -> midiEvent.getClass().equals(Text.class))
                .count();
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
        track.forEach(note -> midiOfNotes.put(note.sign().getMidi(), note)); //Идея говорит stream не нужен.
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

        float bpm = SongUtils.getTempo(midiFile).getBpm();

        logger.trace("bpm = {}", bpm);

        track.stream()
                .map(n -> SongUtils.tickToMs(bpm, midiFile.getResolution(), n.durationTicks())) //получаем длину каждой ноты в милисекундах
                .collect(Collectors.groupingBy(noteDuration -> noteDuration, Collectors.counting())) //считаем количество появлений каждой длительности
                .forEach((integer, aLong) -> durationToCount.put(integer, Math.toIntExact(aLong))); //парсим в инт и закидываем в Hashmap

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

        track.stream()
                .map(Note::sign)
                .collect(Collectors.groupingBy(noteSign -> noteSign, Collectors.counting()))
                .forEach((noteSign, aLong) -> numberOfNotes.put(noteSign, Math.toIntExact(aLong)));
        logger.trace("Анализ завершён. Найдено {} разных длительностей", numberOfNotes.size());
        return numberOfNotes;
    }

}
