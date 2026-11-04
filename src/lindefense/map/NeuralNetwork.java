package lindefense.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lindefense.sim.Vec;

/**
 * Map 6: The Neural Network - Inside the Machine (Advanced).
 * A layered network: two input neurons on the left, three hidden neurons, two deeper hidden neurons and one output
 * neuron that feeds the exit. Synapse pathways branch and merge at every neuron, giving six distinct routes that all
 * share the output neuron. Each enemy picks one route at spawn, so every branch must be defended.
 *
 * No special mechanics: the challenge is the branching layout.
 */
public final class NeuralNetwork extends GameMap {
    /** Neurons (junctions) by name. */
    public static final Vec IN_A = new Vec(0, 200), IN_B = new Vec(0, 700);
    public static final Vec J1 = new Vec(240, 200), J2 = new Vec(240, 700);
    public static final Vec H1 = new Vec(480, 110), H2 = new Vec(480, 450), H3 = new Vec(480, 790);
    public static final Vec H4 = new Vec(780, 280), H5 = new Vec(780, 620);
    public static final Vec OUT = new Vec(990, 450), EXIT = new Vec(1200, 450);

    private final List<Vec> neurons;

    public NeuralNetwork() {
        super("The Neural Network", "Advanced",
              "Inside the machine. Synapse pathways branch and merge through glowing neurons; six routes converge on a single output.");
        route("Synapse A-1", IN_A, J1, H1, H4);
        route("Synapse A-2", IN_A, J1, H2, H4);
        route("Synapse A-3", IN_A, J1, H2, H5);
        route("Synapse B-1", IN_B, J2, H2, H4);
        route("Synapse B-2", IN_B, J2, H2, H5);
        route("Synapse B-3", IN_B, J2, H3, H5);
        List<Vec> n = new ArrayList<>();
        Collections.addAll(n, J1, J2, H1, H2, H3, H4, H5, OUT);
        neurons = Collections.unmodifiableList(n);
        for (Vec v : neurons) {
            addDecoration(Decoration.oval(v.x - 34, v.y - 34, 68, 68, 0x3a1460, true));
            addDecoration(Decoration.oval(v.x - 34, v.y - 34, 68, 68, 0xff4bd8, false));
        }
    }

    private void route(String name, Vec... via) {
        double[] xy = new double[(via.length + 2) * 2];
        int k = 0;
        for (Vec v : via) { xy[k++] = v.x; xy[k++] = v.y; }
        xy[k++] = OUT.x; xy[k++] = OUT.y;
        xy[k++] = EXIT.x; xy[k] = EXIT.y;
        routes.add(new Path(name, Routes.rounded(28, xy)));
    }

    /** Junction neurons (not the input / exit edges). */
    public List<Vec> getNeurons() { return neurons; }

    @Override public int getBackgroundRgb() { return 0x060818; }
    @Override public int getTrackRgb() { return 0x38e8ff; }
    @Override public String getMechanicSummary() {
        return "No special mechanics. Two inputs branch through the hidden layers into six routes that merge at the output neuron: defend several branches or hold the output.";
    }
}
