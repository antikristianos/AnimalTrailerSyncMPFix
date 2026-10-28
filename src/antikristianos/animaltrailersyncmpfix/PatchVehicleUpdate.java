package antikristianos.animaltrailersyncmpfix;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.vehicles.BaseVehicle", methodName = "update", warmUp = true, strictMatch = true)
public final class PatchVehicleUpdate {
    @Patch.OnEnter
    public static void enter(@Patch.This Object vehicle) { GateScope.begin(vehicle); }

    @Patch.OnExit(onThrowable = Throwable.class)
    public static void exit() { GateScope.end(); }
}
