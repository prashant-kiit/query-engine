import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) {
        String sourceCode = "query1.sql";
        Path pathToSourceCode = Paths.get("Database/Query", sourceCode);
        List<String> keywords = Symbols.KEYWORDS.getSymbols(); //

        try {
            // reading file
            StringBuilder language = getLanguageFromSourceCode(pathToSourceCode);

            // tokenize using comma, white space and new line as delimiters
            List<String> tokens = getTokenFromLanguage(language);

            // parse tokens based on symbols list
            Map<String, Object> parseTree = getParseTreeFromTokens(tokens, keywords);

            // Print parseTree
            parseTree.forEach((key, value) -> System.out.println(key + " -> " + value));
            // printJson(mapToJson(parseTree));
        } catch (Exception e) {
            System.out.println("Error reading file: " + e);
        }
    }

    private static Map<String, Object> getParseTreeFromTokens(List<String> tokens, List<String> keywords) {
        Map<String, Object> parseTree1 = parseForFirstPass(tokens, keywords);

        Map<String, Object> parseTree2 = new LinkedHashMap<>();
        if (parseTree1.get("WHERE") != null)
            parseTree2 = parseForWherePass(parseTree1);
        if (parseTree1.get("ORDER_BY") != null)
            parseForOrderByPass(parseTree1);
        if (parseTree1.get("INSERT_INTO") != null)
            parseTree2 = parseForInsertPass(parseTree1);

        // System.out.println("parseTree: " + parseTree2);

        return parseTree2;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseForOrderByPass(Map<String, Object> parseTree) {
        List<String> orderByTokens = (List<String>) parseTree.get("ORDER_BY");
        List<Map<String, Object>> orderBy = new ArrayList<>();
        
        Map<String, Object> currentSubMap = null;
        for (String orderByToken : orderByTokens) {
            if (!Symbols.ORDERS.getSymbols().contains(orderByToken.toLowerCase())) {
                currentSubMap = new LinkedHashMap<>();
                currentSubMap.put("column", orderByToken);
                currentSubMap.put("order", "asc");
                orderBy.add(currentSubMap);
            } else {
                currentSubMap.put("order", orderByToken);
                orderBy.set(orderBy.size() - 1, currentSubMap);
            }
        }

        parseTree.put("ORDER_BY", orderBy);
        return parseTree;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseForInsertPass(Map<String, Object> parseTree) {
        Map<String, Object> insertInto = new LinkedHashMap<>();

        // Extract table name and columns
        List<String> insertIntoTokens = (List<String>) parseTree.get("INSERT_INTO");
        String table = insertIntoTokens.get(0);
        List<String> columns = insertIntoTokens.stream()
                .skip(1)
                .collect(Collectors.toList());
        insertInto.put("table", table);
        insertInto.put("columns", columns);

        // Extract rows to be inserted
        List<String> valuesTokens = (List<String>) parseTree.get("VALUES");
        Integer chunkSize = columns.size();
        List<List<String>> values = IntStream.range(0, (int) Math.ceil((double) valuesTokens.size() / chunkSize))
                .mapToObj(i -> valuesTokens.subList(i * chunkSize,
                        Math.min(valuesTokens.size(), (i + 1) * chunkSize)))
                .collect(Collectors.toList());

        // Construct parse tree
        parseTree.put("INSERT_INTO", insertInto);
        parseTree.put("VALUES", values);

        return parseTree;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseForWherePass(Map<String, Object> parseTree) {
        List<String> whereTokens = (List<String>) parseTree.get("WHERE");
        List<String> operators = Symbols.OPERATORS.getSymbols(); // ->

        // Extract where clause
        List<List<String>> where = new ArrayList<>();
        List<String> currentSubArray = new ArrayList<>();
        for (String token : whereTokens) {
            if (operators.contains(token)) {
                if (!currentSubArray.isEmpty()) {
                    where.add(new ArrayList<>(currentSubArray));
                    currentSubArray.clear();
                }
                where.add(Collections.singletonList(token));
            } else {
                currentSubArray.add(token);
            }
        }

        if (!currentSubArray.isEmpty()) {
            where.add(currentSubArray);
        }

        // Construct parse tree
        parseTree.put("WHERE", where);

        return parseTree;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseForFirstPass(List<String> tokens, List<String> keywords) {
        Map<String, Object> parseTree = new LinkedHashMap<>();
        String currentKey = "";

        for (String word : tokens) {
            if (keywords.contains(word.toLowerCase())) {
                currentKey = word.toUpperCase();
                parseTree.put(currentKey, new ArrayList<>());
            } else {
                ((List<String>) parseTree.get(currentKey)).add(word);
            }
        }

        return parseTree;
    }

    private static List<String> getTokenFromLanguage(StringBuilder language) {
        String regex = "[" + Symbols.SEPARATORS.getSymbols().stream()
                .map(d -> "\\" + d)
                .collect(Collectors.joining("")) + "]+";
        List<String> tokens = Arrays.asList(language.toString().split(regex)); // ->
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

    private static String mapToJson(Map<String, Object> map) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");

        int size = map.size();
        int count = 0;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            jsonBuilder.append("\"").append(entry.getKey()).append("\":");

            if (entry.getValue() instanceof String) {
                jsonBuilder.append("\"").append(entry.getValue()).append("\"");
            } else {
                jsonBuilder.append(entry.getValue());
            }

            count++;
            if (count < size) {
                jsonBuilder.append(",");
            }
        }

        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }

    private static void printJson(String jsonString) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("jq");
            Process process = processBuilder.start();

            // Write JSON string to process stdin
            OutputStream outputStream = process.getOutputStream();
            outputStream.write(jsonString.getBytes());
            outputStream.close(); // Close stdin after writing

            // Read and print the formatted output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Wait for process to complete
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

enum Symbols {
    KEYWORDS(Arrays.asList("select", "from", "where", "order_by", "offset", "limit", "insert_into",
            "values")),
    OPERATORS(Arrays.asList("AND", "OR", "NOT")),
    SEPARATORS(Arrays.asList("\n", "\s", ",", "(", ")")),
    ORDERS(Arrays.asList("asc", "desc"));

    private final List<String> symbols;

    Symbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public List<String> getSymbols() {
        return symbols;
    }
}