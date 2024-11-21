package org.example;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.*;

public class Main {
    private static final Map<String, List<String[]>> groups = new HashMap<>(); // FIXME optimize
    private static final Map<Integer, List<Integer>> groupHierarchy = new HashMap<>();
    private static int currentGroupId = 0; // Unique group ID counter
    private static String[] headers;
    private static final List<String[]> csvData = new ArrayList<>(); // Некластеризованные строки


    private static final CSVWriter writer;

    static {
        try {
            writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream("result.csv", false), UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e); // FIXME
            }
        }));


        Scanner scanner = new Scanner(System.in, "UTF-8");

        sout("Введите путь к файлу CSV:");
        String filePath = scanner.nextLine(); /*"proseller.csv";*/

        try {
            loadCSV(filePath);
            sout("Файл успешно загружен!");

            while (true) {
                sout("\nВыберите опцию:");
                sout("1: Создать группу");
                sout("2: Создать подгруппу");
                sout("3: Сохранить CSV");
//                sout("3: Показать CSV");
                sout("0: Выход");
                int choice = parseInt(scanner.nextLine());

                if (choice == 1) createGroup(scanner);
                else if (choice == 2) createSubgroup(scanner);
                else if (choice == 3) saveCSV();
//                else if (choice == 4) displayCSV();
                else if (choice == 0) return;
                else sout("Неверный выбор!");
            }
        } catch (IOException e) {
            System.err.println("Ошибка: " + e.getMessage());
        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
    }

    private static void displayCSV() {
//        sout();
    }

    private static void loadCSV(String filePath) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), UTF_8))) {
            List<String[]> data = reader.readAll();
            if (data.isEmpty()) throw new IOException("Файл пустой!");

            headers = new String[data.get(0).length + 1];
            headers[0] = "Group";
            System.arraycopy(data.get(0), 0, headers, 1, data.get(0).length);
            csvData.addAll(data.subList(1, data.size())); // Сохраняем все строки как некластеризованные
        }
    }

    private static void createGroup(Scanner scanner) {
        sout("Введите регулярное выражение для поиска ключевых слов:");
        String regex = scanner.nextLine();

        Pattern pattern = Pattern.compile(regex);
        List<String[]> matchingKeywords = csvData.stream()
                .filter(row -> pattern.matcher(row[0]).find())
                .collect(Collectors.toList());

        if (matchingKeywords.isEmpty()) {
            sout("Ключевые слова не найдены.");
            return;
        }

        int groupId = ++currentGroupId;
        groups.put(regex + " - " + groupId, matchingKeywords); // Добавляем строки в новую группу
        groupHierarchy.put(groupId, new ArrayList<>());

        csvData.removeAll(matchingKeywords); // Удаляем из некластеризованных

        sout("Группа " + groupId + " создана с " + matchingKeywords.size() + " ключевыми словами.");
    }
    
    private static void sout(String str) {
        System.out.println(new String(str.getBytes(), UTF_8));
    }

    private static void createSubgroup(Scanner scanner) {
        sout("Введите индекс группы для создания подгруппы:");
        String parentId = scanner.nextLine();

        Map.Entry<String, List<String[]>> entry = groups
                .entrySet()
                .stream()
                .filter(t -> t.getKey().split("-")[1].contains(parentId))
                .findFirst()
                .orElse(null);
        if (entry == null) {
            sout("Группа по такому индексу не найден");
            return;
        }
        sout("Введите регулярное выражение для поиска ключевых слов:");
        String regex = scanner.nextLine();

        Pattern pattern = Pattern.compile(regex);
        List<String[]> parentKeywords = entry.getValue();
        List<String[]> matchingKeywords = parentKeywords.stream()
                .filter(row -> pattern.matcher(row[0]).find())
                .collect(Collectors.toList());

        if (matchingKeywords.isEmpty()) {
            sout("Ключевые слова не найдены.");
            return;
        }

        int groupId = ++currentGroupId;
        groups.put(regex + " - " + groupId, matchingKeywords);
//                    groupHierarchy.put(groupId, new ArrayList<>());
//                    groupHierarchy.get(parentId).add(groupId);

        parentKeywords.removeAll(matchingKeywords);

        sout("Подгруппа " + groupId + " создана внутри группы " + parentId + " с " + matchingKeywords.size() + " ключевыми словами");
    }

    private static void saveCSV() {
        List<String[]> newCsvData = new ArrayList<>();
        newCsvData.add(headers);

        for (Map.Entry<String, List<String[]>> entry : groups.entrySet()) {
            String groupPath = /*getGroupPath(entry.getKey());*/ entry.getKey();
            for (String[] keyword : entry.getValue()) {
                String[] temp = new String[keyword.length + 1];
                temp[0] = groupPath;
                System.arraycopy(keyword, 0, temp, 1, keyword.length);
                newCsvData.add(temp);
            }
        }

        for (String[] csvDatum : csvData) {
            String[] temp = new String[csvData.get(0).length + 1];
            temp[0] = "";
            System.arraycopy(csvDatum, 0, temp, 1, csvData.get(0).length);
            newCsvData.add(temp); // Пустая группа для некластеризованных
        }

        writer.writeAll(newCsvData);
        sout("CSV файл успешно обновлен!");
    }

    private static String getGroupPath(int groupId) {
        for (Map.Entry<Integer, List<Integer>> entry : groupHierarchy.entrySet()) {
            if (entry.getValue().contains(groupId)) {
                return getGroupPath(entry.getKey()) + "/" + groupId;
            }
        }
        return String.valueOf(groupId);
    }
}
