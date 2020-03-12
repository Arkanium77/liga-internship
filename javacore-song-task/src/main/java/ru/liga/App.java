package ru.liga;


import com.leff.midi.MidiFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liga.songtask.domain.Note;
import ru.liga.songtask.domain.NoteSign;
import ru.liga.songtask.util.SongUtils;
import ru.liga.songtask.worker.AnalyzeWorker;
import ru.liga.songtask.worker.ChangeWorker;

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
        logger.info("    Нижняя нота: " + extremum[0].fullName());
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

    /**
     * Процедура анализа файла
     *
     * @param path путь к анализируемому файлу
     * @throws IOException возникает, в случае ошибочного написания пути
     *                     или попытке доступа к защищённому от чтения файлу.
     */
    public static void analyze(String path) throws IOException {
        logger.debug("Запущена процедура анализа");
        MidiFile midiFile = new MidiFile(new File(path));
        List<Note> track = AnalyzeWorker.getVoiceTrack(midiFile);
        if (track.isEmpty()) {
            logger.info("Нет треков пригодных для исполнения голосом.");
            return;
        }
        track.forEach(note -> System.out.println(note.sign().fullName()));
        getRangeWork(
                Objects.requireNonNull(
                        AnalyzeWorker.getExtremumNoteSigns(track)),
                Objects.requireNonNull(
                        AnalyzeWorker.getRange(track))
        );
        getDurationWork(AnalyzeWorker.getDurationAnalyze(track, midiFile));
        getNumberOfNotesWork(AnalyzeWorker.getNumberOfNotes(track));
    }

    /**
     * Процедура изменения файла
     *
     * @param path  путь к исходному файлу
     * @param trans на сколько полутонов транспонировать
     * @param tempo на сколько ПРОЦЕНТОВ изменить темп
     * @throws IOException возникает в случае ошибочного написания пути или попытке доступа к защищённому файлу\директории
     */
    public static void change(String path, int trans, float tempo) throws IOException {
        logger.info("Запущена процедура изменения файла {}, " +
                        "с транспонированием на {} полутонов и изменением темпа на {}%",
                path, trans, tempo
        );
        File file = new File(path);
        MidiFile midiFile = new MidiFile(file);
        MidiFile newMidi = ChangeWorker.changeMidi(midiFile, trans, tempo);
        String pathNew = getSavePath(trans, tempo, file);
        newMidi.writeToFile(new File(pathNew));
        logger.info("Изменённый файл: {}", pathNew);
    }

    private static String getSavePath(int trans, float tempo, File file) {
        String newName = file.getName().replace(".mid", "") + "-trans" + trans + "-tempo" + tempo + ".mid";
        logger.info("Файл успешно изменён.");
        return file.getParentFile().getAbsolutePath() + File.separator + newName;
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
            return;
        }
        Integer trans = null;
        Float tempo = null;
        boolean errors = false;
        if (action.equals("change") && args.length == 6) {
            if (args[2].equals("-trans")) {
                try {
                    trans = Integer.parseInt(args[3]);
                } catch (Exception e) {
                    logger.debug("Ошибка во время парсинга аргумента: {}", e.getMessage());
                    errors = true;
                }
            }

            if (args[4].equals("-tempo")) {
                try {
                    tempo = Float.parseFloat(args[5]);
                } catch (Exception e) {
                    logger.debug("Ошибка во время парсинга аргумента: {}", e.getMessage());
                    errors = true;
                }
            }

        }
        if (errors || tempo == null || trans == null) {
            warningAboutArguments();
            return;
        }
        change(args[0], trans, tempo);
    }

}
