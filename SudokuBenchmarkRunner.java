import java.io.*;
import java.util.*;

public class SudokuBenchmarkRunner {

    static final int RUNS_PER_PUZZLE = 10;

    public static void main(String[] args) throws Exception {

        String inputFile = "src/sudoku_150_with_id.csv";
        String outputFile = "results.csv";

        List<Puzzle> puzzles = readCSV(inputFile);

        // -------------------------
        // JVM WARM-UP (100 iterations)
        // -------------------------
        for (int i = 0; i < 100; i++) {
            for (Puzzle p : puzzles) {
                int[][] board = parseBoard(p.puzzle);
                BitmaskSolver.solve(board);
            }
        }

        // -------------------------
        // OUTPUT WRITER
        // -------------------------
        PrintWriter writer = new PrintWriter(new FileWriter(outputFile));
        writer.println("id,source_id,difficulty,algorithm,run,runtime_ms,backtracks,assignments");

        // -------------------------
        // BENCHMARK LOOP
        // -------------------------
        for (Puzzle p : puzzles) {

            for (int run = 1; run <= RUNS_PER_PUZZLE; run++) {

                // -----------------
                // NAIVE SOLVER
                // -----------------
                int[][] board1 = parseBoard(p.puzzle);

                NaiveSolver.resetCounters();

                long start1 = System.nanoTime();
                NaiveSolver.solve(board1);
                long end1 = System.nanoTime();

                double timeMs1 = (end1 - start1) / 1_000_000.0;

                writer.printf("%d,%s,%s,naive,%d,%.3f,%d,%d%n",
                        p.id, p.sourceId, p.difficulty, run,
                        timeMs1,
                        NaiveSolver.backtracks,
                        NaiveSolver.assignments);

                // -----------------
                // BITMASK SOLVER
                // -----------------
                int[][] board2 = parseBoard(p.puzzle);

                BitmaskSolver.resetCounters();

                long start2 = System.nanoTime();
                BitmaskSolver.solve(board2);
                long end2 = System.nanoTime();

                double timeMs2 = (end2 - start2) / 1_000_000.0;

                writer.printf("%d,%s,%s,bitmask,%d,%.3f,%d,%d%n",
                        p.id, p.sourceId, p.difficulty, run,
                        timeMs2,
                        BitmaskSolver.backtracks,
                        BitmaskSolver.assignments);
            }
        }

        writer.close();
        System.out.println("Benchmark complete. Results saved to " + outputFile);
    }

    // -------------------------
    // CSV READER (FIXED FORMAT)
    // -------------------------
    static List<Puzzle> readCSV(String file) throws Exception {
        List<Puzzle> list = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(file));

        String line = br.readLine(); // skip header

        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",", 3);

            int id = Integer.parseInt(parts[0]);
            String difficulty = parts[1];

            // split source_id + puzzle
            String[] puzzleParts = parts[2].split(" ");
            String sourceId = puzzleParts[0];
            String puzzle = puzzleParts[1];

            // sanity check
            if (puzzle.length() != 81) {
                throw new RuntimeException("Invalid puzzle length at id=" + id);
            }

            list.add(new Puzzle(id, sourceId, difficulty, puzzle));
        }

        br.close();
        return list;
    }

    // -------------------------
    // BOARD PARSER
    // -------------------------
    static int[][] parseBoard(String s) {
        int[][] board = new int[9][9];

        for (int i = 0; i < 81; i++) {
            char c = s.charAt(i);
            board[i / 9][i % 9] = (c == '.' || c == '0') ? 0 : c - '0';
        }

        return board;
    }

    // -------------------------
    // PUZZLE CLASS
    // -------------------------
    static class Puzzle {
        int id;
        String sourceId;
        String difficulty;
        String puzzle;

        Puzzle(int id, String sourceId, String difficulty, String puzzle) {
            this.id = id;
            this.sourceId = sourceId;
            this.difficulty = difficulty;
            this.puzzle = puzzle;
        }
    }
}