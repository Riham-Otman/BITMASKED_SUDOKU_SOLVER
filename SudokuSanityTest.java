import java.io.*;
import java.util.*;

public class SudokuSanityTest {

    public static void main(String[] args) throws Exception {
        String inputFile = "src/sudoku_sanity_subset.csv";
        List<Puzzle> puzzles = readCSV(inputFile);

        for (Puzzle p : puzzles) {
            System.out.println("====================================");
            System.out.println("Puzzle ID: " + p.id + " | " + p.difficulty + " | " + p.sourceId);
            System.out.println("Original:");
            printBoard(parseBoard(p.puzzle));

            int[][] boardNaive = parseBoard(p.puzzle);
            int[][] boardBitmask = parseBoard(p.puzzle);

            NaiveSolver.resetCounters();
            BitmaskSolver.resetCounters();

            boolean solvedNaive = NaiveSolver.solve(boardNaive);
            boolean solvedBitmask = BitmaskSolver.solve(boardBitmask);

            boolean validNaive = isSolved(boardNaive);
            boolean validBitmask = isSolved(boardBitmask);
            boolean sameSolution = boardsEqual(boardNaive, boardBitmask);

            System.out.println("\nNaive solved: " + solvedNaive);
            System.out.println("Naive valid: " + validNaive);
            System.out.println("Naive backtracks: " + NaiveSolver.backtracks);
            System.out.println("Naive assignments: " + NaiveSolver.assignments);

            System.out.println("\nBitmask solved: " + solvedBitmask);
            System.out.println("Bitmask valid: " + validBitmask);
            System.out.println("Bitmask backtracks: " + BitmaskSolver.backtracks);
            System.out.println("Bitmask assignments: " + BitmaskSolver.assignments);

            System.out.println("\nSame final solution: " + sameSolution);

            System.out.println("\nNaive solution:");
            printBoard(boardNaive);

            System.out.println("\nBitmask solution:");
            printBoard(boardBitmask);

            if (!(solvedNaive && solvedBitmask && validNaive && validBitmask && sameSolution)) {
                System.out.println("\nSANITY TEST FAILED for puzzle id " + p.id);
            } else {
                System.out.println("\nSANITY TEST PASSED for puzzle id " + p.id);
            }
        }
    }

    static List<Puzzle> readCSV(String file) throws Exception {
        List<Puzzle> list = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(file));

        String line = br.readLine(); // skip header

        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",", 4);

            int id = Integer.parseInt(parts[0]);
            String difficulty = parts[1];
            String sourceId = parts[2];
            String puzzle = parts[3];

            list.add(new Puzzle(id, difficulty, sourceId, puzzle));
        }

        br.close();
        return list;
    }

    static int[][] parseBoard(String s) {
        int[][] board = new int[9][9];
        for (int i = 0; i < 81; i++) {
            char c = s.charAt(i);
            board[i / 9][i % 9] = (c == '.' || c == '0') ? 0 : c - '0';
        }
        return board;
    }

    static boolean isSolved(int[][] board) {
        for (int i = 0; i < 9; i++) {
            boolean[] rowSeen = new boolean[10];
            boolean[] colSeen = new boolean[10];

            for (int j = 0; j < 9; j++) {
                int rv = board[i][j];
                int cv = board[j][i];

                if (rv < 1 || rv > 9 || rowSeen[rv]) return false;
                if (cv < 1 || cv > 9 || colSeen[cv]) return false;

                rowSeen[rv] = true;
                colSeen[cv] = true;
            }
        }

        for (int br = 0; br < 9; br += 3) {
            for (int bc = 0; bc < 9; bc += 3) {
                boolean[] seen = new boolean[10];
                for (int r = br; r < br + 3; r++) {
                    for (int c = bc; c < bc + 3; c++) {
                        int v = board[r][c];
                        if (v < 1 || v > 9 || seen[v]) return false;
                        seen[v] = true;
                    }
                }
            }
        }

        return true;
    }

    static boolean boardsEqual(int[][] a, int[][] b) {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (a[i][j] != b[i][j]) return false;
            }
        }
        return true;
    }

    static void printBoard(int[][] board) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                System.out.print(board[r][c] + " ");
            }
            System.out.println();
        }
    }

    static class Puzzle {
        int id;
        String difficulty;
        String sourceId;
        String puzzle;

        Puzzle(int id, String difficulty, String sourceId, String puzzle) {
            this.id = id;
            this.difficulty = difficulty;
            this.sourceId = sourceId;
            this.puzzle = puzzle;
        }
    }
}