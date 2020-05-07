package net.minelink.ctplus.archonguard;

import net.minelink.ctplus.hook.Hook;
import net.thearchon.guard.ArchonGuardAPI;
import net.thearchon.guard.flags.AGFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public final class ArchonGuardHook implements Hook {
    private ArchonGuardAPI guardAPI = Bukkit.getServicesManager().getRegistration(ArchonGuardAPI.class).getProvider();

    @Override
    public boolean isPvpEnabledAt(Location loc) {
        return guardAPI.getAllowDenyFlagAt(loc, AGFlag.PVP, true).orElse(true); // default pvp allowed
    }
}
