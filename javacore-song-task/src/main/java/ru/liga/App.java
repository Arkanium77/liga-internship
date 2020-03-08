package ru.liga;


import com.leff.midi.MidiFile;
import ru.liga.songtask.domain.Note;
import ru.liga.songtask.domain.NoteSign;
import ru.liga.songtask.worker.AnalyzeWorker;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class App {

    private static void getRangeWork(NoteSign[] extremum, int range) {
        System.out.println("Диапазон:");
        System.out.println("\tВерхняя нота: " + extremum[1].fullName());
        System.out.println("\tНижняя нота: " + extremum[1].fullName());
        System.out.println("\tДиапазон: " + range);
    }

    private static void getDurationWork(HashMap<Integer, Integer> map) {
        System.out.println("Количество нот по длительностям");
        for (Integer i : map.keySet()) {
            System.out.println("\t" + i + "ms: " + map.get(i));
        }
    }

    private static void getNumberOfNotesWork(HashMap<NoteSign, Integer> map) {
        System.out.println("Список нот с количеством вхождений:");
        for (NoteSign i : map.keySet()) {
            System.out.println("\t" + i.fullName() + ": " + map.get(i));
        }
    }

    public static void analyze(String path) throws IOException {
        MidiFile midiFile = new MidiFile(new File(path));
        List<List<Note>> voices = AnalyzeWorker.getVoiceTracks(midiFile);
        if (voices.size() == 0) {
            System.out.println("Нет треков пригодных для исполнения голосом.");
            return;
        }
        for (List<Note> track : voices) {
            getRangeWork(
                    Objects.requireNonNull(
                            AnalyzeWorker.getExtremumNoteSigns(track)),
                    Objects.requireNonNull(
                            AnalyzeWorker.getRange(track))
            );
            getDurationWork(AnalyzeWorker.getDurationAnalyze(track, midiFile));
            getNumberOfNotesWork(AnalyzeWorker.getNumberOfNotes(track));
        }
    }

    public static void change(String path, int trans, double tempo) {

    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            argsReader(args);
        } else {
            System.out.println("Для запуска программы введите аргументы командной строки.\nНапример:");
            System.out.println("..\\midi-analyzer.jar \"C:\\zombie.mid\" analyze");
            System.out.println("..\\midi-analyzer.jar \"C:\\zombie.mid\" change -trans 2 -tempo 20");
        }
    }

    private static void argsReader(String[] args) throws IOException {
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
