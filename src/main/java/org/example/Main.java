package org.example;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

public class Main {
    private static final Map<String, List<String>> groups = new LinkedHashMap<>(); // FIXME optimize
    private static final Map<Integer, List<Integer>> groupHierarchy = new HashMap<>();
    private static int currentGroupId = 0; // Unique group ID counter
    private static List<String[]> csvData = new ArrayList<>();
    private static String[] headers;

    private static final CSVWriter writer;

    static {
        try {
            writer = new CSVWriter(new FileWriter("result.csv"));
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


        Scanner scanner = new Scanner(System.in);

        System.out.println("Введите путь к файлу CSV:");
        String filePath = /*scanner.nextLine();*/ "proseller.csv";

        try {
            loadCSV(filePath);
            System.out.println("Файл успешно загружен!");

            while (true) {
                System.out.println("\nВыберите опцию:");
                System.out.println("1: Создать группу");
                System.out.println("2: Создать подгруппу");
                System.out.println("3: Сохранить CSV");
                System.out.println("0: Выход");
                int choice = parseInt(scanner.nextLine());

                if (choice == 1) createGroup(scanner);
                else if (choice == 2) createSubgroup(scanner);
                else if (choice == 3) saveCSV();
                else if (choice == 0) return;
                else System.out.println("Неверный выбор!");
            }
        } catch (IOException e) {
            System.err.println("Ошибка: " + e.getMessage());
        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadCSV(String filePath) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> data = reader.readAll();
            if (data.isEmpty()) throw new IOException("Файл пустой!");

            headers = data.getFirst();
            csvData = data.subList(1, data.size());
        }
    }

    private static void createGroup(Scanner scanner) {
        System.out.println("Введите регулярное выражение для поиска ключевых слов:");
        String regex = scanner.nextLine();

        Pattern pattern = Pattern.compile(regex);
        List<String[]> matchingKeywords = csvData.stream()
                .filter(row -> pattern.matcher(row[0]).find())
                .collect(Collectors.toList());

        if (matchingKeywords.isEmpty()) {
            System.out.println("Ключевые слова не найдены.");
            return;
        }

        int groupId = ++currentGroupId;
        groups.put(regex + " - " + groupId, matchingKeywords.stream().map(row -> row[0]).collect(Collectors.toList()));
        groupHierarchy.put(groupId, new ArrayList<>());

        csvData.removeAll(matchingKeywords);

        System.out.println("Группа " + groupId + " создана с " + matchingKeywords.size() + " ключевыми словами.");
        saveCSV();
    }

    private static void createSubgroup(Scanner scanner) {
        System.out.println("Введите индекс группы для создания подгруппы:");
        int parentId = parseInt(scanner.nextLine());

        // FIXME
        if (!groups.containsKey(parentId)) {
            System.out.println("Группа с таким индексом не найдена.");
            return;
        }

        System.out.println("Введите регулярное выражение для поиска ключевых слов:");
        String regex = scanner.nextLine();

        Pattern pattern = Pattern.compile(regex);
        List<String> parentKeywords = groups.get(parentId);
        List<String> matchingKeywords = parentKeywords.stream()
                .filter(keyword -> pattern.matcher(keyword).find())
                .collect(Collectors.toList());

        if (matchingKeywords.isEmpty()) {
            System.out.println("Ключевые слова не найдены.");
            return;
        }

        int groupId = ++currentGroupId;
        groups.put(regex + " - " + groupId, matchingKeywords);
        groupHierarchy.put(groupId, new ArrayList<>());
        groupHierarchy.get(parentId).add(groupId);

        parentKeywords.removeAll(matchingKeywords);

        System.out.println("Подгруппа " + groupId + " создана внутри группы " + parentId);
    }

    private static void saveCSV() {
        List<String[]> newCsvData = new ArrayList<>();
        newCsvData.add(headers);

        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            String groupPath = /*getGroupPath(entry.getKey());*/ entry.getKey();
            for (String keyword : entry.getValue()) {
                newCsvData.add(new String[]{groupPath, keyword});
            }
        }

        writer.writeAll(newCsvData);
        System.out.println("CSV файл успешно обновлен!");
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
