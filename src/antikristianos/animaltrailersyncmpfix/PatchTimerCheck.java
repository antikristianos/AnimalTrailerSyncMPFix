package antikristianos.animaltrailersyncmpfix;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.core.utils.UpdateLimit", methodName = "Check", warmUp = true, strictMatch = true)
public final class PatchTimerCheck {
    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.This Object timer) { return GateScope.skip(timer); }

    @Patch.OnExit
    public static void exit(@Patch.This Object timer, @Patch.Return(readOnly = false) boolean result) {
        result = GateScope.complete(timer, result);
    }
}
