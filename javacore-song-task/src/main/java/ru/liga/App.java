package ru.liga;


import com.leff.midi.MidiFile;
import com.leff.midi.event.meta.Tempo;
import ru.liga.songtask.domain.Note;
import ru.liga.songtask.util.SongUtils;
import ru.liga.songtask.worker.AnalyzeWorker;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static ru.liga.songtask.util.SongUtils.eventsToNotes;

public class App {

    /**
     * Это пример работы, можете всё стирать и переделывать
     * Пример, чтобы убрать у вас начальный паралич разработки
     * Также посмотрите класс SongUtils, он переводит тики в миллисекунды
     * Tempo может быть только один
     */
    public static void exMain() throws IOException {
        MidiFile midiFile = new MidiFile(new FileInputStream("D:\\Java\\liga-internship\\javacore-song-task\\src\\main\\resources\\Underneath Your Clothes.mid"));
        List<Note> notes = eventsToNotes(midiFile.getTracks().get(3).getEvents());
        Tempo last = (Tempo) midiFile.getTracks().get(0).getEvents().last();
        Note ninthNote = notes.get(8);
        System.out.println("Длительность девятой ноты (" + ninthNote.sign().fullName() + "): " + SongUtils.tickToMs(last.getBpm(), midiFile.getResolution(), ninthNote.durationTicks()) + "мс");
        System.out.println("Все ноты:");
        System.out.println(notes);
    }

    public static void tyem() throws IOException {
        MidiFile midiFile = new MidiFile(new FileInputStream("D:\\Java\\liga-internship\\javacore-song-task\\src\\main\\resources\\Underneath Your Clothes.mid"));
        for (List<Note> i : AnalyzeWorker.getVoiceTracks(midiFile)) {
            System.out.println(i);
        }

    }

    public static void analyze(String path) {

    }

    public static void change(String path, int trans, double tempo) {

    }

    public static void main(String[] args) throws IOException {
        tyem();
        //argsReader(args);
    }

    private static void argsReader(String[] args) {
        String action = args[1].toLowerCase().trim();
        if (action.equals("analyze")) {
            analyze(args[0]);
        }
        if (action.equals("change")) {
            int trans;
            double tempo;
            try {
                trans = Integer.parseInt(args[3]);
            } catch (Exception e) {
                trans = 0;
            }
            try {
                tempo = Double.parseDouble(args[5]);
            } catch (Exception e) {
                tempo = 0;
            }
            change(args[0], trans, tempo);
        }
    }

}
