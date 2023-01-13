package icmod;

import icmod.block.ModBlocks;
import icmod.command.Voting;
import icmod.item.ModItems;
import icmod.util.ModRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class main implements ModInitializer {
	public static final String MOD_ID = "icmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

//		ModItems.registerModItems();
//		ModBlocks.registerModBlocks();

		ModRegistries.registerModStuff();

	}
}