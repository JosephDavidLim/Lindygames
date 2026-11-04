package entities;

/**
 * Shows a letter grade that drops from A to F as it takes damage. At F it
 * panics and runs 1.5 times faster.
 */
public class ReportCard extends Enemy {
    /**
     * Creates a report card.
     *
     * @param type the enemy type
     * @param row lane index
     * @param x starting position in cell units
     */
    public ReportCard(EnemyType type, int row, double x) {
        super(type, row, x);
    }

    /**
     * Gets the current letter grade.
     *
     * @return 'A' through 'D' while healthy, 'F' when nearly dead
     */
    public char getGrade() {
        double fraction = health / maxHealth;
        if (fraction > 0.8) {
            return 'A';
        } else if (fraction > 0.6) {
            return 'B';
        } else if (fraction > 0.4) {
            return 'C';
        } else if (fraction > 0.2) {
            return 'D';
        }
        return 'F';
    }

    @Override
    protected double currentSpeed() {
        double speed = super.currentSpeed();
        return getGrade() == 'F' ? speed * 1.5 : speed;
    }
}
