# Experimental Comparison of Sudoku Solvers Using Backtracking and Bitmasked Constraint Techniques

## How to Run

> **Note:** The Java benchmark was run in IntelliJ and the Python analysis in a separate PyCharm environment (Pycharn), as setting up a Python SDK inside the Java project was not feasible.

### Step 1 — Run the Java benchmark
javac SudokuBenchmarkRunner.java
java SudokuBenchmarkRunner
Outputs: `results.csv`

### Step 2 — Copy results.csv to the Python project directory
Move or copy `results.csv` into the same folder as `analysis.py`.

### Step 3 — Run the analysis
Open `analysis.py` in PyCharm (or any Python environment) and run it.  
Outputs: summary statistics, t-test results, and 3 figures.

---

`SudokuSanityTest.java` is included for completeness. It was used to verify correctness before benchmarking and can be disregarded.