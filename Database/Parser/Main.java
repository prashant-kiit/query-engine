import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String fileName = "query1.sql";
        Path pathToFile = Paths.get("Database/Query", fileName);

        // Reading and printing characters using Stream
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(pathToFile)) {
            int ch;
            while ((ch = reader.read()) != -1) {
                content.append((char) ch);
            }
            List<String> tokens = Arrays.asList(content.toString().split("[\\s,\\n]+"));
            System.out.println("Tokens: " + tokens);

            StoreToken storeToken = new StoreToken();
            

        } catch (Exception e) {
            System.out.println("Error reading file: " + e);
        }
    }
}

enum Lexicon {
    SELECT("select"),
    FROM("from"),
    WHERE("where");

    private final String value;

    Lexicon(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
