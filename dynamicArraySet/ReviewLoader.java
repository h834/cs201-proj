import java.io.*;
import java.util.*;

// read CSV and map airlines → users

// public class ReviewLoader {

//     /**
//      * Load reviews from CSV into a map of name -> set of users.
//      * @param filename Path to CSV file
//      * @param type "airline" or "lounge"
//      */
//     public static Map<String, DynamicArraySet<String>> loadReviews(String filename, String type) {
//         Map<String, DynamicArraySet<String>> result = new HashMap<>();

//         File file = new File(filename);
//         if (!file.exists()) {
//             System.out.println("File not found: " + file.getAbsolutePath());
//             return result;
//         }

//         int nameColumn;
//         switch (type.toLowerCase()) {
//             case "airline":
//                 nameColumn = 2; // airline name is in column 2
//                 break;
//             case "lounge":
//                 nameColumn = 2; // lounge name is in column 2
//                 break;
//             default:
//                 throw new IllegalArgumentException("Unknown type: " + type);
//         }

//         try (BufferedReader br = new BufferedReader(new FileReader(file))) {
//             String line = br.readLine(); // skip header

//             while ((line = br.readLine()) != null) {
//                 String[] parts = line.split(",", -1);
//                 if (parts.length < 4) continue; // need at least name + author

//                 String name = parts[nameColumn].trim();
//                 String user = parts[3].trim(); // column 3 = author

//                 if (name.isEmpty() || user.isEmpty()) continue;

//                 // For airlines, remove " customer review" from name
//                 if (type.equalsIgnoreCase("airline") && name.endsWith(" customer review")) {
//                     name = name.replace(" customer review", "");
//                 }

//                 result.putIfAbsent(name, new DynamicArraySet<>());
//                 result.get(name).add(user);
//             }
//         } catch (Exception e) {
//             e.printStackTrace();
//         }

//         return result;
//     }
// }

public class ReviewLoader {

    /**
     * Load reviews from CSV into a map of name -> set of users.
     * @param filename Path to CSV file
     * @param type "airline" or "lounge"
     * @return Map where key = airline/lounge name, value = set of unique users
     */
    public static Map<String, DynamicArraySet<String>> loadReviews(String filename, String type) {
        Map<String, DynamicArraySet<String>> result = new HashMap<>();

        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("File not found: " + file.getAbsolutePath());
            return result;
        }

        int nameColumn;
        switch (type.toLowerCase()) {
            case "airline":
                nameColumn = 2; // airline name column
                break;
            case "lounge":
                nameColumn = 2; // lounge name column
                break;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                List<String> parts = parseCSVLine(line);
                if (parts.size() <= Math.max(nameColumn, 3)) continue; // must have name + author

                String name = parts.get(nameColumn).trim();
                String user = parts.get(3).trim(); // author column is always 3

                if (name.isEmpty() || user.isEmpty()) continue;

                // For airlines, remove " customer review" from name
                if (type.equalsIgnoreCase("airline") && name.endsWith(" customer review")) {
                    name = name.replace(" customer review", "");
                }

                result.putIfAbsent(name, new DynamicArraySet<>());
                result.get(name).add(user);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Parse a CSV line properly handling quotes and commas inside quotes
     */
    private static List<String> parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString()); // add last field
        return result;
    }
}
