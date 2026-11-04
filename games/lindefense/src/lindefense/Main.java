package lindefense;

import lindefense.ui.LindefenseProgram;

/** Program entry point: launches the ACM GraphicsProgram. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        new LindefenseProgram().start(args);
    }
}
