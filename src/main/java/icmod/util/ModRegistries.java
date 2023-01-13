package icmod.util;

import icmod.command.Voting;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ModRegistries {
    public static void registerModStuff() {
        registerCommandVoting();
    }

    private static void registerCommandVoting(){
        CommandRegistrationCallback.EVENT.register(Voting::register);
    }
}
