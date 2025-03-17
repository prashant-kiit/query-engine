import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) {
        String sourceCode = "query1.sql";
        Path pathToSourceCode = Paths.get("Database/Query", sourceCode);

        try {
            // reading file
            StringBuilder language = getLanguageFromSourceCode(pathToSourceCode);

            // tokenize
            List<String> tokens = getTokenFromLanguage(language);
            // System.out.println("tokens: " + tokens);

            // parse
            Map<String, Object> parseTree = getParseTreeFromTokens(tokens);

            // Print parseTree
            // parseTree.forEach((key, value) -> System.out.println(key + " -> " + value));
            System.out.println(parseTree);
        } catch (Exception e) {
            System.out.println("Error reading file: " + e);
        }
    }

    private static Map<String, Object> getParseTreeFromTokens(List<String> tokens) {
        Map<String, Object> parseTree1 = parseForFirstPass(tokens);

        Map<String, Object> parseTree2 = new LinkedHashMap<>();
        if (parseTree1.get("SELECT") != null)
            parseTree2 = parseForSelectPass(parseTree1);
        if (parseTree1.get("WHERE") != null)
            parseTree2 = parseForWherePass(parseTree1);
        if (parseTree1.get("ORDER_BY") != null)
            parseTree2 = parseForOrderByPass(parseTree1);
        if (parseTree1.get("OFFSET") != null)
            parseTree2 = parseForOffsetPass(parseTree1);
        if (parseTree1.get("LIMIT") != null)
            parseTree2 = parseForLimitPass(parseTree1);
        if (parseTree1.get("INSERT_INTO") != null)
            parseTree2 = parseForInsertPass(parseTree1);

        // System.out.println("parseTree: " + parseTree2);

        return parseTree2;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseForSelectPass(Map<String, Object> parseTree) {
        Map<String, Object> select = new LinkedHashMap<>();
        List<String> selectTokens = (List<String>) parseTree.get("SELECT");
        String finalAggregator = null;
        for (String token : selectTokens) {
            if (!Symbols.AGGREGATORS_KEYWORDS.getSymbols().contains(token.toLowerCase())) {
                Map<String, Object> currentSubMap = new LinkedHashMap<>();
                currentSubMap.put("column", token);
                currentSubMap.put("aggregator", finalAggregator);
                if (finalAggregator != null) {
                    finalAggregator = null;
                }
                select.put("selectable" + currentSubMap.hashCode(), currentSubMap);
            } else {
                finalAggregator = token;
            }
        }

        parseTree.put("SELECT", select);

        return parseTree;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseForLimitPass(Map<String, Object> parseTree) {
        List<String> limitTokens = (List<String>) parseTree.get("LIMIT");

        // Extract limit
        Integer limit = Integer.parseInt(limitTokens.get(0));

        // Construct parse tree
        parseTree.put("LIMIT", limit);

        return parseTree;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseForOffsetPass(Map<String, Object> parseTree) {
        List<String> offsetTokens = (List<String>) parseTree.get("OFFSET");

        // Extract offset
        Integer offset = Integer.parseInt(offsetTokens.get(0));

        // Construct parse tree
        parseTree.put("OFFSET", offset);

        return parseTree;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseForOrderByPass(Map<String, Object> parseTree) {
        List<String> orderByTokens = (List<String>) parseTree.get("ORDER_BY");

        // Extract order by
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

        // Construct parse tree
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

        // Extract where clause
        Map<String, Object> where = new LinkedHashMap<>();
        Map<String, Object> currentSubSubMap = null;
        for (String token : whereTokens) {
            if (!Symbols.OPERATORS.getSymbols().contains(token.toLowerCase())) {
                if (currentSubSubMap == null) {
                    currentSubSubMap = new LinkedHashMap<>();
                    currentSubSubMap.put("not", false);
                }
                if (token.toLowerCase().equals("not")) {
                    currentSubSubMap.put("not", true);
                    continue;
                }
                if (!currentSubSubMap.containsKey("column")) {
                    currentSubSubMap.put("column", token);
                    continue;
                }
                if (!currentSubSubMap.containsKey("operator")) {
                    currentSubSubMap.put("operator", token);
                    continue;
                }
                if (!currentSubSubMap.containsKey("value")) {
                    if (currentSubSubMap.get("operator").toString().toLowerCase().equals("in")) {
                        currentSubSubMap.put("value", new ArrayList<>(Arrays.asList(token)));
                    } else {
                        currentSubSubMap.put("value", token);
                    }
                    continue;
                } else if (currentSubSubMap.get("operator").toString().toLowerCase().equals("in")) {
                    List<String> values = (List<String>) currentSubSubMap.get("value");
                    values.add(token);
                    currentSubSubMap.put("value", values);
                }
            } else {
                where.put("expression" + currentSubSubMap.hashCode(), currentSubSubMap);
                where.put("operation" + currentSubSubMap.hashCode(), token);
                // System.out.println("where" + where);
                currentSubSubMap = null;
            }
        }

        where.put("expression" + currentSubSubMap.hashCode(), currentSubSubMap);

        // Construct parse tree
        parseTree.put("WHERE", where);

        return parseTree;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseForFirstPass(List<String> tokens) {
        List<String> keywords = Symbols.KEYWORDS.getSymbols();
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
}

enum Symbols {
    KEYWORDS(Arrays.asList("select", "from", "where", "order_by", "offset", "limit", "insert_into",
            "values", "delete", "group_by")),
    OPERATORS(Arrays.asList("and", "or")),
    SEPARATORS(Arrays.asList("\n", "\s", ",", "(", ")")),
    ORDERS(Arrays.asList("asc", "desc")),
    AGGREGATORS_KEYWORDS(Arrays.asList("count", "sum", "avg", "min", "max"));

    private final List<String> symbols;

    Symbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public List<String> getSymbols() {
        return symbols;
    }
}