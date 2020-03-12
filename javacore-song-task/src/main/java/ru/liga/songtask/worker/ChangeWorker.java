package ru.liga.songtask.worker;

import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liga.songtask.util.SongUtils;

public class ChangeWorker {
    static Logger logger = LoggerFactory.getLogger(ChangeWorker.class);

    /**
     * <b>Изменить мидифайл</b>
     *
     * @param midiFile изменяемый файл
     * @param trans    на сколько полутонов изменить высоту.
     * @param tempo    на сколько ПРОЦЕНТОВ изменить скорость.
     * @return новый Midi файл
     */
    public static MidiFile changeMidi(MidiFile midiFile, int trans, float tempo) {
        float percentTempo = 1 + tempo / 100;
        logger.trace("Получен float множитель тема = {}", percentTempo);
        MidiFile newMidi = changeTempo(midiFile, percentTempo);
        newMidi = transposeMidi(newMidi, trans);
        logger.trace("Изменение завершено");
        return newMidi;
    }

    /**
     * <b>Изменить скорость мидифайла</b>
     *
     * @param midiFile     изменяемый файл
     * @param percentTempo float-множитель скорости 50%=1.5 и т.д.
     * @return новый Midi файл
     */
    public static MidiFile changeTempo(MidiFile midiFile, float percentTempo) {
        MidiFile midiFile1 = new MidiFile();
        logger.debug("Старый Bpm = {}", SongUtils.getTempo(midiFile).getBpm());
        midiFile.getTracks()
                .stream()
                .map(midiTrack -> changeTempoOfMidiTrack(percentTempo, midiTrack))
                .forEachOrdered(midiFile1::addTrack);

        logger.debug("Новай Bpm = {}", SongUtils.getTempo(midiFile1).getBpm());
        return midiFile1;
    }

    private static MidiTrack changeTempoOfMidiTrack(float percentTempo, MidiTrack midiTrack) {
        MidiTrack midiTrack1 = new MidiTrack();
        for (MidiEvent midiEvent : midiTrack.getEvents()) {
            if (midiEvent.getClass().equals(Tempo.class)) {
                Tempo tempo = getChangedTempo(percentTempo, (Tempo) midiEvent);
                midiTrack1.getEvents().add(tempo);
            } else {
                midiTrack1.getEvents().add(midiEvent);
            }
        }
        return midiTrack1;
    }

    private static Tempo getChangedTempo(float percentTempo, Tempo midiEvent) {
        Tempo tempo = new Tempo(midiEvent.getTick(), midiEvent.getDelta(), midiEvent.getMpqn());
        tempo.setBpm(tempo.getBpm() * percentTempo);
        return tempo;
    }

    /**
     * <b>Изменить мидифайл</b>
     *
     * @param midiFile изменяемый файл
     * @param trans    на сколько полутонов изменить высоту.
     * @return новый Midi файл
     */
    public static MidiFile transposeMidi(MidiFile midiFile, int trans) {
        MidiFile midiFile1 = new MidiFile();
        midiFile.getTracks()
                .stream()
                .map(midiTrack -> transposeMidiTrack(trans, midiTrack))
                .forEachOrdered(midiFile1::addTrack);

        logger.debug("Транспонирование на {} полутонов. Первая нота старого трека:{} -> Первая нота нового трека {}",
                trans,
                SongUtils.getAllTracksAsNoteLists(midiFile).get(0).get(0),
                SongUtils.getAllTracksAsNoteLists(midiFile1).get(0).get(0)
        );
        return midiFile1;
    }

    private static MidiTrack transposeMidiTrack(int trans, MidiTrack midiTrack) {
        MidiTrack midiTrack1 = new MidiTrack();
        for (MidiEvent midiEvent : midiTrack.getEvents()) {
            if (midiEvent.getClass().equals(NoteOn.class)) {
                NoteOn on = getChangedNoteOn(trans, (NoteOn) midiEvent);
                midiTrack1.getEvents().add(on);
            } else if (midiEvent.getClass().equals(NoteOff.class)) {
                NoteOff off = getChangedNoteOff(trans, (NoteOff) midiEvent);
                midiTrack1.getEvents().add(off);
            } else {
                midiTrack1.getEvents().add(midiEvent);
            }
        }
        return midiTrack1;
    }

    private static NoteOff getChangedNoteOff(int trans, NoteOff midiEvent) {
        NoteOff off = new NoteOff(midiEvent.getTick(), midiEvent.getDelta(), midiEvent.getChannel(), midiEvent.getNoteValue(), midiEvent.getVelocity());
        off.setNoteValue(off.getNoteValue() + trans);
        return off;
    }

    private static NoteOn getChangedNoteOn(int trans, NoteOn midiEvent) {
        NoteOn on = new NoteOn(midiEvent.getTick(), midiEvent.getDelta(), midiEvent.getChannel(), midiEvent.getNoteValue(), midiEvent.getVelocity());
        on.setNoteValue(on.getNoteValue() + trans);
        return on;
    }

}
