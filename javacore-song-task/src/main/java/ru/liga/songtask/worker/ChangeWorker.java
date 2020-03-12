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

import java.util.function.Predicate;

public class ChangeWorker {
    static Logger logger = LoggerFactory.getLogger(ChangeWorker.class);
    Predicate<MidiEvent> isTempo = midiEvent -> midiEvent.getClass().equals(Tempo.class);

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
        MidiFile changedTempoMidiFile = new MidiFile();
        logger.debug("Старый Bpm = {}", SongUtils.getTempo(midiFile).getBpm());
        midiFile.getTracks()
                .stream()
                .map(midiTrack -> changeTempoOfMidiTrack(percentTempo, midiTrack))
                .forEachOrdered(changedTempoMidiFile::addTrack);

        logger.debug("Новай Bpm = {}", SongUtils.getTempo(changedTempoMidiFile).getBpm());
        return changedTempoMidiFile;
    }

    /**
     * <b>Изменить скорость трека</b>
     *
     * @param percentTempo float-множитель скорости 50%=1.5 и т.д.
     * @param midiTrack    изменяемый трек
     * @return новый Midi трек
     */
    private static MidiTrack changeTempoOfMidiTrack(float percentTempo, MidiTrack midiTrack) {
        MidiTrack changedTempoMidiTrack = new MidiTrack();
        midiTrack.getEvents()
                .stream()
                .map(midiEvent -> getChangedTempo(percentTempo, midiEvent))
                .forEachOrdered(changedTempoMidiTrack::insertEvent);
        return changedTempoMidiTrack;
    }

    /**
     * <b>Получить ивент с изменённым темпом</b>
     *
     * @param percentTempo float-множитель скорости 50%=1.5 и т.д.
     * @param midiEvent    предполагаемый к изменению ивент
     * @return если Ивент принадлежал классу Tempo - возвращает изменённый ивент, иначе оставляет его без изменений.
     */
    private static MidiEvent getChangedTempo(float percentTempo, MidiEvent midiEvent) {
        if (midiEvent.getClass().equals(Tempo.class)) {
            return getChangedTempo(percentTempo, (Tempo) midiEvent);
        }
        return midiEvent;
    }

    /**
     * <b>Изменить темп события Tempo</b>
     *
     * @param percentTempo float-множитель скорости 50%=1.5 и т.д.
     * @param midiEvent    изменяемый ивент.
     * @return ивент Tempo с изменённым темпом.
     */
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

    /**
     * <b>Изменить миди трек</b>
     *
     * @param trans     на сколько полутонов изменить высоту.
     * @param midiTrack изменяемый трек.
     * @return новый трек с изменённой высотой нот.
     */
    private static MidiTrack transposeMidiTrack(int trans, MidiTrack midiTrack) {

        MidiTrack transposedMidiTrack = new MidiTrack();
        midiTrack.getEvents()
                .stream()
                .map(midiEvent -> getChangedNote(trans, midiEvent))
                .forEachOrdered(transposedMidiTrack::insertEvent);
        return transposedMidiTrack;
    }

    /**
     * <b>Получить изменённую ноту</b>
     *
     * @param trans     на сколько полутонов менять высоту.
     * @param midiEvent предполагаемый к изменению ивент.
     * @return если ивент класса NoteOn\NoteOff возвращает изменённое соответствующими методами событие
     * иначе возвращает событие без изменений.
     */
    private static MidiEvent getChangedNote(int trans, MidiEvent midiEvent) {
        if (midiEvent.getClass().equals(NoteOn.class)) {
            return getChangedNoteOn(trans, (NoteOn) midiEvent);
        }
        if (midiEvent.getClass().equals(NoteOff.class)) {
            return getChangedNoteOff(trans, (NoteOff) midiEvent);
        }
        return midiEvent;
    }

    /**
     * <b>Изменить высоту мобытия NoteOff</b>
     *
     * @param trans     на сколько полутонов менять высоту.
     * @param midiEvent изменяемое событие NoteOff
     * @return событие класса noteOff с изменённой высотой.
     */
    private static NoteOff getChangedNoteOff(int trans, NoteOff midiEvent) {
        NoteOff off = new NoteOff(midiEvent.getTick(), midiEvent.getDelta(), midiEvent.getChannel(), midiEvent.getNoteValue(), midiEvent.getVelocity());
        off.setNoteValue(off.getNoteValue() + trans);
        return off;
    }

    /**
     * <b>Изменить высоту мобытия NoteOn</b>
     *
     * @param trans     на сколько полутонов менять высоту.
     * @param midiEvent изменяемое событие NoteOn
     * @return событие класса noteOn с изменённой высотой.
     */
    private static NoteOn getChangedNoteOn(int trans, NoteOn midiEvent) {
        NoteOn on = new NoteOn(midiEvent.getTick(), midiEvent.getDelta(), midiEvent.getChannel(), midiEvent.getNoteValue(), midiEvent.getVelocity());
        on.setNoteValue(on.getNoteValue() + trans);
        return on;
    }

}
