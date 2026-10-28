package antikristianos.animaltrailersyncmpfix;

import java.lang.reflect.Field;
import java.util.ArrayList;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.vehicles.BaseVehicle;

/** Share exactly one native timer decision within one BaseVehicle.update call. */
public final class GateScope {
    private static final class Frame {
        Object gate;
        boolean known, result;
    }
    private static final class State {
        final ArrayList<Frame> frames = new ArrayList<>();
        int depth;
        Frame current() { return depth == 0 ? null : frames.get(depth - 1); }
    }
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static Field timer;
    private static boolean attempted, enabled, reported;

    private GateScope() { }

    public static void begin(Object vehicle) {
        Object gate = null;
        if (GameServer.server && !GameClient.client) {
            initialize();
            if (enabled) {
                try { gate = timer.get(vehicle); }
                catch (ReflectiveOperationException | IllegalArgumentException e) {
                    enabled = false;
                    System.err.println("[AnimalTrailerSyncMPFix] DISABLED: cannot access trailer timer: " + e);
                }
            }
        }
        push(gate);
    }

    // Package-visible test seam; production only supplies the exact private field.
    static void push(Object gate) {
        State state = STATE.get();
        if (state.depth == state.frames.size()) state.frames.add(new Frame());
        Frame frame = state.frames.get(state.depth++);
        frame.gate = gate;
        frame.known = false;
        frame.result = false;
    }

    public static void end() {
        State state = STATE.get();
        if (state.depth > 0) {
            Frame frame = state.frames.get(--state.depth);
            frame.gate = null;
            frame.known = false;
        }
    }

    public static boolean skip(Object gate) {
        Frame frame = STATE.get().current();
        return frame != null && frame.gate == gate && gate != null && frame.known;
    }

    public static boolean complete(Object gate, boolean nativeResult) {
        Frame frame = STATE.get().current();
        if (frame == null || frame.gate != gate || gate == null) return nativeResult;
        if (frame.known) {
            if (frame.result && !reported && enabled && GameServer.server && !GameClient.client) {
                reported = true;
                System.out.println("[AnimalTrailerSyncMPFix] ACTIVE 1.3: multiple trailer animals share the due native timer; client receipt requires an MP test.");
            }
            return frame.result;
        }
        frame.known = true;
        frame.result = nativeResult;
        return nativeResult;
    }

    private static synchronized void initialize() {
        if (attempted) return;
        attempted = true;
        try {
            timer = BaseVehicle.class.getDeclaredField("updateAnimal");
            if (timer.getType() != zombie.core.utils.UpdateLimit.class) throw new IllegalStateException("timer type changed");
            timer.setAccessible(true);
            enabled = true;
            System.out.println("[AnimalTrailerSyncMPFix] Trailer timer resolved; awaiting the native trailer timer hook.");
        } catch (Exception e) {
            System.err.println("[AnimalTrailerSyncMPFix] DISABLED: cannot resolve trailer timer: " + e);
        }
    }

}
