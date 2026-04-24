import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from scipy.stats import ttest_rel

# -------------------------
# LOAD DATA
# -------------------------
df = pd.read_csv("results.csv")

# -------------------------
# BASIC CLEANUP
# -------------------------
df['runtime_ms'] = pd.to_numeric(df['runtime_ms'])

# -------------------------
# DIFFICULTY ORDER
# -------------------------
difficulty_order = ['easy', 'medium', 'hard']

# -------------------------
# AGGREGATE PER PUZZLE
# (mean over 10 runs)
# -------------------------
agg = df.groupby(['id', 'difficulty', 'algorithm']).agg({
    'runtime_ms': 'mean',
    'backtracks': 'mean',
    'assignments': 'mean'
}).reset_index()

# -------------------------
# SPLIT NAIVE vs BITMASK
# -------------------------
naive = agg[agg['algorithm'] == 'naive']
bitmask = agg[agg['algorithm'] == 'bitmask']

# merge to align same puzzles
merged = pd.merge(
    naive,
    bitmask,
    on=['id', 'difficulty'],
    suffixes=('_naive', '_bitmask')
)

# -------------------------
# SPEEDUP
# -------------------------
merged['speedup'] = merged['runtime_ms_naive'] / merged['runtime_ms_bitmask']

# -------------------------
# OVERALL PAIRED T-TEST
# -------------------------
t_stat, p_value = ttest_rel(
    merged['runtime_ms_naive'],
    merged['runtime_ms_bitmask']
)
print("\n========== OVERALL PAIRED T-TEST ==========")
print(f"t-statistic: {t_stat:.4f}")
print(f"p-value:     {p_value:.6f}")
if p_value < 0.05:
    print("Result: SIGNIFICANT (p < 0.05)")
else:
    print("Result: NOT significant (p >= 0.05)")

# -------------------------
# PER-DIFFICULTY T-TESTS
# -------------------------
print("\n========== PER-DIFFICULTY PAIRED T-TESTS ==========")
for diff in difficulty_order:
    subset = merged[merged['difficulty'] == diff]
    if len(subset) >= 2:
        t, p = ttest_rel(subset['runtime_ms_naive'], subset['runtime_ms_bitmask'])
        sig = "SIGNIFICANT" if p < 0.05 else "not significant"
        print(f"  {diff.capitalize():8s}: t={t:.4f}, p={p:.6f} => {sig}")
    else:
        print(f"  {diff.capitalize():8s}: not enough data")

# -------------------------
# SUMMARY BY DIFFICULTY
# -------------------------
summary = merged.groupby('difficulty').agg({
    'runtime_ms_naive':    ['mean', 'std'],
    'runtime_ms_bitmask':  ['mean', 'std'],
    'backtracks_naive':    'mean',
    'backtracks_bitmask':  'mean',
    'assignments_naive':   'mean',
    'assignments_bitmask': 'mean',
    'speedup':             'mean'
}).reindex(difficulty_order)

print("\n========== SUMMARY TABLE ==========")
print(summary.to_string())

# save summary
summary.to_csv("summary_table.csv")
print("\nSummary saved to summary_table.csv")

# -------------------------
# GRAPH 1: Runtime Comparison (with error bars)
# -------------------------
runtime_means = merged.groupby('difficulty')[
    ['runtime_ms_naive', 'runtime_ms_bitmask']
].mean().reindex(difficulty_order)

runtime_std = merged.groupby('difficulty')[
    ['runtime_ms_naive', 'runtime_ms_bitmask']
].std().reindex(difficulty_order)

fig, ax = plt.subplots()

x = np.arange(len(difficulty_order))
width = 0.35

naive_means = runtime_means['runtime_ms_naive'].values
naive_std = runtime_std['runtime_ms_naive'].values
bitmask_means = runtime_means['runtime_ms_bitmask'].values
bitmask_std = runtime_std['runtime_ms_bitmask'].values

# Clip lower error so bars never go below 0
naive_lower = np.minimum(naive_std, naive_means)
bitmask_lower = np.minimum(bitmask_std, bitmask_means)

ax.bar(x - width/2, naive_means, width,
       yerr=[naive_lower, naive_std],
       label='Naive', color='steelblue', capsize=4)

ax.bar(x + width/2, bitmask_means, width,
       yerr=[bitmask_lower, bitmask_std],
       label='Bitmask', color='darkorange', capsize=4)

ax.set_title("Mean Runtime by Difficulty")
ax.set_xlabel("Difficulty")
ax.set_ylabel("Runtime (ms)")
ax.set_xticks(x)
ax.set_xticklabels([d.capitalize() for d in difficulty_order])
ax.set_ylim(bottom=0)
ax.legend()
plt.tight_layout()
plt.savefig("runtime_comparison.png", dpi=150)
plt.show()
print("Saved runtime_comparison.png")

# -------------------------
# GRAPH 2: Assignments (log scale)
# -------------------------
assign_means = merged.groupby('difficulty')[
    ['assignments_naive', 'assignments_bitmask']
].mean().reindex(difficulty_order)

fig, ax = plt.subplots()
assign_means.plot(
    kind='bar',
    logy=True,
    ax=ax,
    color=['steelblue', 'darkorange']
)
ax.set_title("Assignments Attempted (Log Scale)")
ax.set_xlabel("Difficulty")
ax.set_ylabel("Assignments (log scale)")
ax.set_xticklabels([d.capitalize() for d in difficulty_order], rotation=0)
ax.legend(['Naive', 'Bitmask'])
plt.tight_layout()
plt.savefig("assignments_log.png", dpi=150)
plt.show()
print("Saved assignments_log.png")

# -------------------------
# GRAPH 3: Speedup
# -------------------------
speedup_means = merged.groupby('difficulty')['speedup'].mean().reindex(difficulty_order)

fig, ax = plt.subplots()
speedup_means.plot(
    kind='bar',
    ax=ax,
    color='seagreen'
)
ax.axhline(y=1, color='red', linestyle='--', linewidth=1, label='Speedup = 1x (no gain)')
ax.set_title("Average Speedup (Naive / Bitmask)")
ax.set_xlabel("Difficulty")
ax.set_ylabel("Speedup")
ax.set_xticklabels([d.capitalize() for d in difficulty_order], rotation=0)
ax.legend()
plt.tight_layout()
plt.savefig("speedup.png", dpi=150)
plt.show()
print("Saved speedup.png")

print("\nAnalysis complete!")
