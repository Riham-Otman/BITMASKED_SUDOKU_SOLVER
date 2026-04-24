class NaiveSolver {

    public static long backtracks = 0;
    public static long assignments = 0;

    public static void resetCounters() {
        backtracks = 0;
        assignments = 0;
    }

    public static boolean solve(int[][] board) {
        return dfs(board);
    }

    private static boolean dfs(int[][] board) {

        int r = -1, c = -1;

        for (int i = 0; i < 9 && r == -1; i++) {
            for (int j = 0; j < 9; j++) {
                if (board[i][j] == 0) {
                    r = i;
                    c = j;
                    break;
                }
            }
        }

        if (r == -1) return true;

        for (int val = 1; val <= 9; val++) {

            if (isValid(board, r, c, val)) {

                board[r][c] = val;
                assignments++;

                if (dfs(board)) return true;

                board[r][c] = 0;
            }
        }

        backtracks++;
        return false;
    }

    static boolean isValid(int[][] board, int r, int c, int val) {

        for (int i = 0; i < 9; i++) {
            if (board[r][i] == val || board[i][c] == val)
                return false;
        }

        int br = (r / 3) * 3;
        int bc = (c / 3) * 3;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[br + i][bc + j] == val)
                    return false;
            }
        }

        return true;
    }
}