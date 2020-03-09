package ru.liga;


import com.leff.midi.MidiFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liga.songtask.domain.Note;
import ru.liga.songtask.domain.NoteSign;
import ru.liga.songtask.util.SongUtils;
import ru.liga.songtask.worker.AnalyzeWorker;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class App {
    private static Logger logger = LoggerFactory.getLogger(App.class);

    private static void getRangeWork(NoteSign[] extremum, int range) {
        logger.info("Диапазон:");
        logger.info("    Верхняя нота: " + extremum[1].fullName());
        logger.info("    Нижняя нота: " + extremum[1].fullName());
        logger.info("    Диапазон: " + range);
    }

    private static void getDurationWork(HashMap<Integer, Integer> map) {
        logger.info("Количество нот по длительностям:");
        for (Integer i : map.keySet()) {
            logger.info("    " + i + "ms: " + map.get(i));
        }
    }

    private static void getNumberOfNotesWork(HashMap<NoteSign, Integer> map) {
        logger.info("Список нот с количеством вхождений:");
        for (NoteSign i : map.keySet()) {
            logger.info("    " + i.fullName() + ": " + map.get(i));
        }
    }

    public static void analyze(String path) throws IOException {
        logger.debug("Запущена процедура анализа");
        MidiFile midiFile = new MidiFile(new File(path));
        List<List<Note>> voices = AnalyzeWorker.getVoiceTracks(midiFile);
        if (voices.size() == 0) {
            logger.info("Нет треков пригодных для исполнения голосом.");
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
        logger.debug("Запущена процедура изменения файла {}, " +
                        "с транспонированием на {} полутонов и изменением темпа на {}%"
                , path, trans, tempo
        );

    }

    public static void main(String[] args) throws IOException {
        logger.debug("Программа запущена с параметрами {}", SongUtils.getStringFromArray(args));
        if (args.length > 1) {
            argsReader(args);
        } else {
            warningAboutArguments();
        }
    }

    private static void warningAboutArguments() {
        logger.debug("Недостаточно данных для работы программы");
        logger.info("Для запуска программы введите аргументы командной строки.\nНапример:");
        logger.info("..\\midi-analyzer.jar \"C:\\zombie.mid\" analyze");
        logger.info("..\\midi-analyzer.jar \"C:\\zombie.mid\" change -trans 2 -tempo 20");
    }

    private static void argsReader(String[] args) throws IOException {
        String action = args[1].toLowerCase().trim();
        if (action.equals("analyze")) {
            analyze(args[0]);
        }
        if (action.equals("change") && args.length == 6) {
            int trans = 0;
            double tempo = 0;
            boolean errors = false;
            if (args[2].equals("-tans")) {
                try {
                    trans = Integer.parseInt(args[3]);
                } catch (Exception e) {
                    logger.debug("Ошибка во время парсинга аргумента: {}", e.getMessage());
                    errors = true;
                }
            }
            if (args[2].equals("-tempo")) {
                trans = 0;
                try {
                    tempo = Double.parseDouble(args[5]);
                } catch (Exception e) {
                    logger.debug("Ошибка во время парсинга аргумента: {}", e.getMessage());
                    errors = true;
                }
            }
            if (errors) {
                warningAboutArguments();
                return;
            }
            change(args[0], trans, tempo);
        }
    }

}
