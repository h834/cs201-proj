import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Main class to run the data structures experiment.
 *
 * This experiment is designed to solve:
 * "Find the total overlapping unique authors between the airline.csv and lounge.csv files."
 *
 * It reads *all* authors from each file into two master sets,
 * then runs statistical analysis on the intersection of those two sets.
 */
public class ExperimentRunner {

    // --- Configuration ---
    private static final String AIRLINE_CSV_FILE = "airline.csv";
    private static final String LOUNGE_CSV_FILE = "lounge.csv";

    // --- Column Names from your error logs ---
    
    // This is correct for airline.csv per your log
    private static final String AIRLINE_AUTHOR_COL = "author";
    private static final String AIRLINE_DATE_COL = "date";
    
    // ***********************************
    // ***** THIS IS THE CORRECTED CODE ****
    // ***********************************
    // These now match your lounge.csv error log
    private static final String LOUNGE_AUTHOR_COL = "author";
    private static final String LOUNGE_DATE_COL = "date_visit";

    // --- Set to "" to read ALL reviews from all years ---
    // Or set to "2024", "2023", etc. to filter by year.
    private static final String TIME_WINDOW = "";

    /**
     * Main entry point for the experiment.
     */
    public static void main(String[] args) {
        System.out.println("Starting Data Structures Experiment: Total Overlap Detection");
        System.out.println("Reading Airline File: " + AIRLINE_CSV_FILE);
        System.out.println("Reading Lounge File: " + LOUNGE_CSV_FILE);

        if (TIME_WINDOW.isEmpty()) {
            System.out.println("\nReading ALL reviews from all years.");
        } else {
            System.out.println("\nFiltering reviews for time window: '" + TIME_WINDOW + "'");
        }

        // Define the three Set implementations to test
        List<Supplier<ISet<String>>> setFactories = new ArrayList<>();
        setFactories.add(() -> new HashSetSeparateChaining<>());
        setFactories.add(() -> new HashSetLinearProbing<>());
        setFactories.add(() -> new HashSetQuadraticProbing<>());

        for (Supplier<ISet<String>> factory : setFactories) {
            String setTypeName = factory.get().getClass().getSimpleName();
            System.out.println("\n-----------------------------------------");
            System.out.println("Testing Set Type: " + setTypeName);

            try {
                // --- 1. Build Phase: Read files and build the master sets ---
                long buildStart = System.nanoTime();

                ISet<String> airlineAuthors = buildMasterSet(AIRLINE_CSV_FILE, factory, AIRLINE_AUTHOR_COL, AIRLINE_DATE_COL);
                ISet<String> loungeAuthors = buildMasterSet(LOUNGE_CSV_FILE, factory, LOUNGE_AUTHOR_COL, LOUNGE_DATE_COL);

                long buildEnd = System.nanoTime();
                System.out.printf("...Build Phase Complete. Time: %.2f ms\n", (buildEnd - buildStart) / 1_000_000.0);

                System.out.println("Found " + airlineAuthors.size() + " total unique airline authors.");
                System.out.println("Found " + loungeAuthors.size() + " total unique lounge authors.");

                // --- 2. Intersection Phase: Time the core operation ---
                long intersectStart = System.nanoTime();
                ISet<String> overlapSet = airlineAuthors.intersection(loungeAuthors);
                long intersectEnd = System.nanoTime();

                System.out.println("...Intersection Phase Complete.");
                System.out.printf("Total Intersection Time: %.6f ms\n", (intersectEnd - intersectStart) / 1_000_000.0);
                System.out.println("Total Unique Overlapping Authors Found: " + overlapSet.size());

            } catch (IOException e) {
                System.err.println("Error reading files: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("An unexpected error occurred: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-----------------------------------------");
        }
    }

    /**
     * Reads a CSV file and builds a "master set" of all unique authors.
     *
     * @param filename The CSV file to read.
     * @param factory A supplier for creating the new ISet.
     * @param authorColName The exact name of the author column.
     * @param dateColName The exact name of the date column.
     * @return A Set containing all unique authors from the file.
     * @throws IOException If the file cannot be read or headers are missing.
     */
    private static ISet<String> buildMasterSet(String filename, Supplier<ISet<String>> factory, String authorColName, String dateColName) throws IOException {
        ISet<String> masterSet = factory.get();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("File is empty: " + filename);
            }

            // --- Find Column Indices ---
            // Read headers, convert to lowercase, trim, and remove quotes for reliable matching
            List<String> headers = new ArrayList<>();
            for (String h : headerLine.split(",")) {
                headers.add(h.trim().replace("\"", "").toLowerCase());
            }
            
            int authorCol = headers.indexOf(authorColName.toLowerCase());
            int dateCol = headers.indexOf(dateColName.toLowerCase());

            // --- Validate Headers ---
            List<String> missing = new ArrayList<>();
            if (authorCol == -1) missing.add(authorColName);
            if (dateCol == -1) missing.add(dateColName);

            if (!missing.isEmpty()) {
                throw new IOException("Missing columns in " + filename + ": " + missing + ". Found headers: " + headers);
            }

            System.out.println("Found columns in " + filename + ": " + authorColName + "(" + authorCol + "), " + dateColName + "(" + dateCol + ")");

            // --- Process Data Rows ---
            String line;
            while ((line = br.readLine()) != null) {
                // This regex is a simple CSV parser.
                String[] values = line.split(",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                if (values.length <= Math.max(authorCol, dateCol)) {
                    continue; // Skip malformed lines
                }

                String author = values[authorCol].trim().replace("\"", "");
                String date = values[dateCol].trim().replace("\"", "");

                // Filter by time window and valid data
                // NOTE: We use .contains() so "2024" will match "1st May 2024"
                if (date.contains(TIME_WINDOW) && !author.isEmpty()) {
                    masterSet.add(author);
                }
            }
        }
        return masterSet;
    }
}