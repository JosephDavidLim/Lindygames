package lindefense.enemy;

/** Special-mechanic identifiers; the precise rules live in {@link EnemyManager} and {@link Enemy}. */
public enum Mechanic {
    NONE,
    /** Doubly Linked List: +15% speed per hit taken, up to +90%. */
    ACCELERATE_ON_DAMAGE,
    /** Bubble Sort: every 3 s swaps track position with the nearest enemy ahead within 45 px. */
    BUBBLE_SWAP,
    /** Selection Sort: every 2.5 s jumps to the front of the group within 140 px ahead. */
    SELECTION_FRONT,
    /** Insertion Sort: every 2 s leapfrogs the nearest enemy ahead within 70 px. */
    INSERTION_LEAPFROG,
    /** Shell Sort: every 3 s jumps forward by its current gap (140, 70, 35, then 18 px). */
    SHELL_GAP_JUMP,
    /** Heap Sort: enemies up to 90 px behind it on its route take 50% damage. */
    HEAP_SHIELD,
    /** Radix Sort: defensive profile changes every two layers (see DESIGN.md). */
    RADIX_PHASES,
    /** Bucket Sort: absorbs nearby weaker enemies and releases them on death. */
    BUCKET_STORE,
    /** Palantir: multi-phase final boss with Predictive Surveillance. */
    PALANTIR
}
