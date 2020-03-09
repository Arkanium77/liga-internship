package ru.liga.songtask.util;

import com.leff.midi.MidiFile;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liga.songtask.domain.Note;
import ru.liga.songtask.domain.NoteSign;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

public class SongUtils {
    private static Logger logger = LoggerFactory.getLogger(SongUtils.class);

    /**
     * Перевод тиков в миллисекунды
     *
     * @param bpm          - количество ударов в минуту (темп)
     * @param resolution   - midiFile.getResolution()
     * @param amountOfTick - то что переводим в миллисекунды
     * @return целое число, длительность тиков в мс при заданном bpm
     */
    public static int tickToMs(float bpm, int resolution, long amountOfTick) {
        return (int) (((60 * 1000) / (bpm * resolution)) * amountOfTick);
    }

    /**
     * Этот метод, чтобы вы не афигели переводить эвенты в ноты
     *
     * @param events эвенты одного трека
     * @return список нот
     */
    public static List<Note> eventsToNotes(TreeSet<MidiEvent> events) {
        List<Note> vbNotes = new ArrayList<>();

        Queue<NoteOn> noteOnQueue = new LinkedBlockingQueue<>();
        for (MidiEvent event : events) {
            if (event instanceof NoteOn || event instanceof NoteOff) {
                if (isEndMarkerNote(event)) {
                    NoteSign noteSign = NoteSign.fromMidiNumber(extractNoteValue(event));
                    if (noteSign != NoteSign.NULL_VALUE) {
                        NoteOn noteOn = noteOnQueue.poll();
                        if (noteOn != null) {
                            long start = noteOn.getTick();
                            long end = event.getTick();
                            vbNotes.add(
                                    new Note(noteSign, start, end - start));
                        }
                    }
                } else {
                    try {
                        noteOnQueue.offer((NoteOn) event);
                    } catch (ClassCastException e) {
                        logger.debug(e.getMessage());
                    }
                }
            }
        }
        return vbNotes;
    }

    private static Integer extractNoteValue(MidiEvent event) {
        if (event instanceof NoteOff) {
            return ((NoteOff) event).getNoteValue();
        } else if (event instanceof NoteOn) {
            return ((NoteOn) event).getNoteValue();
        } else {
            return null;
        }
    }

    private static boolean isEndMarkerNote(MidiEvent event) {
        if (event instanceof NoteOff) {
            return true;
        } else if (event instanceof NoteOn) {
            return ((NoteOn) event).getVelocity() == 0;
        } else {
            return false;
        }
    }

    public static String getStringFromArray(Object[] ar) {
        if (ar.length == 0) {
            return "[]";
        }
        if (ar.length == 1) {
            return "[" + ar[0].toString() + "]";
        }
        String s = "[";
        for (Object o : ar) {
            s += o.toString() + ", ";
        }
        s = s.substring(0, s.length() - 2) + "]";
        return s;
    }

    /**
     * <b>Получить Tempo-event</b>
     *
     * @param midiFile файл для анализа
     * @return Tempo-event, содержащий информацию о bpm.
     */
    public static Tempo getTempo(MidiFile midiFile) {
        Tempo tempo = (Tempo) (midiFile.getTracks().get(0).getEvents()).stream()
                .filter(value -> value instanceof Tempo)
                .findFirst()
                .get();
        logger.trace("Извлечён event Tempo={}", tempo);
        return tempo;
    }
}
