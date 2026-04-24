import java.util.ArrayList;
import java.util.List;

class BitmaskSolver {

    public static long backtracks = 0;
    public static long assignments = 0;

    private static final int ALL = 0x3FE; // bits 1..9 set

    public static void resetCounters() {
        backtracks = 0;
        assignments = 0;
    }

    public static boolean solve(int[][] board) {
        int[] rowMask = new int[9];
        int[] colMask = new int[9];
        int[] boxMask = new int[9];
        int[][] candidates = new int[9][9];

        for (int i = 0; i < 9; i++) {
            rowMask[i] = ALL;
            colMask[i] = ALL;
            boxMask[i] = ALL;
        }

        // Initialize masks from given clues
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int val = board[r][c];
                if (val != 0) {
                    int bit = 1 << val;
                    int b = boxIndex(r, c);

                    if ((rowMask[r] & bit) == 0 ||
                            (colMask[c] & bit) == 0 ||
                            (boxMask[b] & bit) == 0) {
                        return false; // invalid puzzle
                    }

                    rowMask[r] &= ~bit;
                    colMask[c] &= ~bit;
                    boxMask[b] &= ~bit;
                }
            }
        }

        // Initialize candidate masks for all cells
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    candidates[r][c] = rowMask[r] & colMask[c] & boxMask[boxIndex(r, c)];
                    if (candidates[r][c] == 0) {
                        return false;
                    }
                } else {
                    candidates[r][c] = 0;
                }
            }
        }

        // Initial propagation of forced singles
        if (!propagate(board, rowMask, colMask, boxMask, candidates)) {
            return false;
        }

        return dfs(board, rowMask, colMask, boxMask, candidates);
    }

    private static boolean dfs(int[][] board, int[] rowMask, int[] colMask, int[] boxMask, int[][] candidates) {
        int bestR = -1;
        int bestC = -1;
        int bestMask = 0;
        int minChoices = 10;

        // MRV: choose empty cell with fewest candidates
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    int mask = candidates[r][c];
                    int count = Integer.bitCount(mask);

                    if (count == 0) {
                        backtracks++;
                        return false;
                    }

                    if (count < minChoices) {
                        minChoices = count;
                        bestR = r;
                        bestC = c;
                        bestMask = mask;

                        if (count == 1) {
                            break;
                        }
                    }
                }
            }
            if (minChoices == 1) {
                break;
            }
        }

        // No empty cells => solved
        if (bestR == -1) {
            return true;
        }

        int options = bestMask;

        while (options != 0) {
            int bit = options & -options;

            int[][] boardCopy = copyBoard(board);
            int[] rowCopy = rowMask.clone();
            int[] colCopy = colMask.clone();
            int[] boxCopy = boxMask.clone();
            int[][] candCopy = copyMatrix(candidates);

            if (assign(bestR, bestC, bit, boardCopy, rowCopy, colCopy, boxCopy, candCopy)) {
                if (dfs(boardCopy, rowCopy, colCopy, boxCopy, candCopy)) {
                    copyInto(boardCopy, board);
                    return true;
                }
            }

            options &= (options - 1);
        }

        backtracks++;
        return false;
    }

    /**
     * Assigns a value to one cell, updates global masks, removes that value from peers,
     * and propagates forced singles.
     */
    private static boolean assign(int r, int c, int bit,
                                  int[][] board,
                                  int[] rowMask,
                                  int[] colMask,
                                  int[] boxMask,
                                  int[][] candidates) {

        int val = Integer.numberOfTrailingZeros(bit);
        int b = boxIndex(r, c);

        // Check if assignment is still legal
        if ((rowMask[r] & bit) == 0 || (colMask[c] & bit) == 0 || (boxMask[b] & bit) == 0) {
            return false;
        }

        board[r][c] = val;
        assignments++;

        rowMask[r] &= ~bit;
        colMask[c] &= ~bit;
        boxMask[b] &= ~bit;
        candidates[r][c] = 0;

        // Forward checking: remove assigned value from all peers
        if (!eliminateFromPeers(r, c, bit, board, candidates)) {
            return false;
        }

        // Recompute affected peer candidate masks using row/col/box masks
        if (!refreshPeers(r, c, board, rowMask, colMask, boxMask, candidates)) {
            return false;
        }

        // Propagate forced singles
        return propagate(board, rowMask, colMask, boxMask, candidates);
    }

    /**
     * Eliminates 'bit' from all peers of (r, c).
     */
    private static boolean eliminateFromPeers(int r, int c, int bit, int[][] board, int[][] candidates) {
        // Row peers
        for (int j = 0; j < 9; j++) {
            if (j != c && board[r][j] == 0) {
                candidates[r][j] &= ~bit;
                if (candidates[r][j] == 0) {
                    return false;
                }
            }
        }

        // Column peers
        for (int i = 0; i < 9; i++) {
            if (i != r && board[i][c] == 0) {
                candidates[i][c] &= ~bit;
                if (candidates[i][c] == 0) {
                    return false;
                }
            }
        }

        // Box peers
        int br = (r / 3) * 3;
        int bc = (c / 3) * 3;
        for (int i = br; i < br + 3; i++) {
            for (int j = bc; j < bc + 3; j++) {
                if ((i != r || j != c) && board[i][j] == 0) {
                    candidates[i][j] &= ~bit;
                    if (candidates[i][j] == 0) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Refreshes candidate masks of peers using current row/col/box availability.
     */
    private static boolean refreshPeers(int r, int c,
                                        int[][] board,
                                        int[] rowMask,
                                        int[] colMask,
                                        int[] boxMask,
                                        int[][] candidates) {

        for (int j = 0; j < 9; j++) {
            if (board[r][j] == 0) {
                candidates[r][j] = rowMask[r] & colMask[j] & boxMask[boxIndex(r, j)];
                if (candidates[r][j] == 0) {
                    return false;
                }
            }
        }

        for (int i = 0; i < 9; i++) {
            if (board[i][c] == 0) {
                candidates[i][c] = rowMask[i] & colMask[c] & boxMask[boxIndex(i, c)];
                if (candidates[i][c] == 0) {
                    return false;
                }
            }
        }

        int br = (r / 3) * 3;
        int bc = (c / 3) * 3;
        for (int i = br; i < br + 3; i++) {
            for (int j = bc; j < bc + 3; j++) {
                if (board[i][j] == 0) {
                    candidates[i][j] = rowMask[i] & colMask[j] & boxMask[boxIndex(i, j)];
                    if (candidates[i][j] == 0) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Propagates forced singles until no more exist.
     */
    private static boolean propagate(int[][] board,
                                     int[] rowMask,
                                     int[] colMask,
                                     int[] boxMask,
                                     int[][] candidates) {

        boolean changed;

        do {
            changed = false;
            List<int[]> singles = new ArrayList<>();

            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (board[r][c] == 0) {
                        int mask = rowMask[r] & colMask[c] & boxMask[boxIndex(r, c)];
                        candidates[r][c] = mask;

                        if (mask == 0) {
                            return false;
                        }

                        if (Integer.bitCount(mask) == 1) {
                            singles.add(new int[]{r, c, mask});
                        }
                    }
                }
            }

            for (int[] s : singles) {
                int r = s[0];
                int c = s[1];
                int bit = s[2];

                if (board[r][c] != 0) {
                    continue;
                }

                int b = boxIndex(r, c);
                if ((rowMask[r] & bit) == 0 || (colMask[c] & bit) == 0 || (boxMask[b] & bit) == 0) {
                    return false;
                }

                int val = Integer.numberOfTrailingZeros(bit);
                board[r][c] = val;
                assignments++;

                rowMask[r] &= ~bit;
                colMask[c] &= ~bit;
                boxMask[b] &= ~bit;
                candidates[r][c] = 0;

                if (!eliminateFromPeers(r, c, bit, board, candidates)) {
                    return false;
                }

                if (!refreshPeers(r, c, board, rowMask, colMask, boxMask, candidates)) {
                    return false;
                }

                changed = true;
            }

        } while (changed);

        return true;
    }

    private static int boxIndex(int r, int c) {
        return (r / 3) * 3 + (c / 3);
    }

    private static int[][] copyBoard(int[][] board) {
        int[][] copy = new int[9][9];
        for (int i = 0; i < 9; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, 9);
        }
        return copy;
    }

    private static int[][] copyMatrix(int[][] matrix) {
        int[][] copy = new int[9][9];
        for (int i = 0; i < 9; i++) {
            System.arraycopy(matrix[i], 0, copy[i], 0, 9);
        }
        return copy;
    }

    private static void copyInto(int[][] from, int[][] to) {
        for (int i = 0; i < 9; i++) {
            System.arraycopy(from[i], 0, to[i], 0, 9);
        }
    }
}