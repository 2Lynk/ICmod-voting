package icmod.item.custom;

import java.util.concurrent.TimeUnit;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.tick.SimpleTickScheduler;

public class EightBallItem extends Item {
    public EightBallItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(!world.isClient() && hand == Hand.MAIN_HAND){
            // Dit wordt dus alleen op de server uitgevoerd
            int playerCoordinateX = user.getBlockX();
            int playerCoordinateY = user.getBlockY();
            int playerCoordinateZ = user.getBlockZ();

            int iRandomNumber = getRandomNumber();
            int iRandomCoordinateX = getRandomCoordinate(world);
            int iRandomCoordinateZ = getRandomCoordinate(world);
            int iCoordinateY = world.getTopY(Heightmap.Type.WORLD_SURFACE, 100, 100);
            outputResult(user, iRandomNumber, world);
            getHighestBlockToSpawn(world, iRandomCoordinateX, iCoordinateY, iRandomCoordinateZ, user);
            if(iRandomNumber != 7){
                user.teleport(iRandomCoordinateX, getHighestBlockToSpawn(world, iRandomCoordinateX, iCoordinateY, iRandomCoordinateZ, user), iRandomCoordinateZ);
            }
            user.getItemCooldownManager().set(this, 20); // dit zet de cooldown op dit item op 20seconden
            //teleportPlayerBack(Math.toIntExact(world.getTime()), 20, user, world, playerCoordinateX, playerCoordinateY, playerCoordinateZ);

        }
        return super.use(world, user, hand);
    }

    private void outputResult(PlayerEntity player, int iNumber, World world){
        if(iNumber == 7){
            player.sendMessage(Text.literal("You got lucky number 7, teleporting to random place!"));
        }else {
            player.sendMessage(Text.literal("Your number is: " + iNumber));
            player.sendMessage(Text.literal(String.valueOf(world.getTime())));
        }
    }

    private int getRandomNumber(){
        return Random.createLocal().nextInt(10);
    }

    private int getRandomCoordinate(World world){
        return Random.createLocal().nextInt(29999999);
    }

    private int getHighestBlockToSpawn(World world, int iCoordinateX, int iCoordinateY, int iCoordinateZ, PlayerEntity player) {
        boolean blockOneAtPositionIsSolid = world.getBlockState(new BlockPos(iCoordinateX,iCoordinateY,iCoordinateZ)).getMaterial().isSolid();
        boolean blockTwoAtPositionIsSolid = world.getBlockState(new BlockPos(iCoordinateX,iCoordinateY+1,iCoordinateZ)).getMaterial().isSolid();
        if (blockOneAtPositionIsSolid && blockTwoAtPositionIsSolid) {
            iCoordinateY++;
            getHighestBlockToSpawn(world, iCoordinateX, iCoordinateY, iCoordinateZ, player);
        }
        return iCoordinateY;
    }

    private void teleportPlayerBack(int startTick, int amountOfTicks, PlayerEntity player, World world, int playerPreviousLocationX, int playerPreviousLocationY, int playerPreviousLocationZ) {
        while (startTick < Math.toIntExact(world.getTime()) + amountOfTicks) {

        }
        player.teleport(playerPreviousLocationX, playerPreviousLocationY, playerPreviousLocationZ);

    }
}
