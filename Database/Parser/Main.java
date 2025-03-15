import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String sourceCode = "query1.sql";
        Path pathToSourceCode = Paths.get("Database/Query", sourceCode);
        List<String> symbols = Arrays.asList("select", "from", "where", "order_by", "limit"); 

        try {
            // reading file
            StringBuilder language = getLanguageFromSourceCode(pathToSourceCode);

            // tokenize using comma, white space and new line as delimiters
            List<String> tokens = getTokenFromString(language);

            // parse tokens based on symbols list
            Map<String, List<String>> parseTree = getParseTreeFromTokens(tokens, symbols);

            // Print parseTree
            parseTree.forEach((key, value) -> System.out.println(key + " -> " + value));
        } catch (Exception e) {
            System.out.println("Error reading file: " + e);
        }
    }

    private static Map<String, List<String>> getParseTreeFromTokens(List<String> tokens, List<String> symbols) {
        Map<String, List<String>> parseTree = new LinkedHashMap<>();
        String currentKey = "";

        for (String word : tokens) {
            if (symbols.contains(word.toLowerCase())) {
                currentKey = word.toUpperCase();
                parseTree.put(currentKey, new ArrayList<>());
            } else {
                parseTree.get(currentKey).add(word);
            }
        }

        return parseTree;
    }

    private static List<String> getTokenFromString(StringBuilder language) {
        List<String> tokens = Arrays.asList(language.toString().split("[\\s,\\n]+"));
        return tokens;
    }

    private static StringBuilder getLanguageFromSourceCode(Path pathToSourceCode) throws IOException {
        BufferedReader reader = Files.newBufferedReader(pathToSourceCode);
        StringBuilder language = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            language.append((char) ch);
        }
        reader.close();

        return language;
    }
}
