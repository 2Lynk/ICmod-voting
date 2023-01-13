package icmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

class Vote {
    private static boolean voteActive = false; // false = no vote active, true = vote active
    private static int voteAbout = 0; // 0 = skip night, 1 = set time
    private static String votePlayerStarted = "";
    private static int votesCastInFavor = 0;
    private static int votesCastAgainst = 0;
    private static int voteThreshold = 1;
    private static ArrayList<String> playersVoted = new ArrayList<String>();
    private static ArrayList<String> playersOnlineAtVoteStart = new ArrayList<String>();
    public static boolean returnVoteActive(){
        return voteActive;
    }
    public static void setVoteActive(boolean active) {
        voteActive = active;
        if(!active){
            votePlayerStarted = "";
            votesCastInFavor = 0;
            votesCastAgainst = 0;
            voteThreshold = 1;
            playersVoted.clear();
            playersOnlineAtVoteStart.clear();
        }
    }
    public static int returnVoteAbout(){
        return voteAbout;
    }
    public static void setVoteAbout(int number) {
        voteAbout = number;
    }
    public static String returnVotePlayerStarted(){
        return votePlayerStarted;
    }
    public static void setVotePlayerStarted(String name) {
        votePlayerStarted = name;
    }
    public static int returnVotesCastInFavor(){
        return votesCastInFavor;
    }
    public static void voteCastInFavor(){
        votesCastInFavor = votesCastInFavor + 1;
    }
    public static int returnVotesCastAgainst(){
        return votesCastAgainst;
    }
    public static void voteCastAgainst(){
        votesCastAgainst++;
    }
    public static int returnVoteThreshold(){
        return voteThreshold;
    }
    public static void setVoteThreshold(){
        voteThreshold = playersOnlineAtVoteStart.size() / 2 + 1;
    }
    public static void setPlayersVoted(String playerUuid){
        playersVoted.add(playerUuid);
    }
    public static ArrayList<String> returnPlayersVoted(){
        return playersVoted;
    }
    public static void addPlayersOnlineAtVoteStart(String playerUuid){
        playersOnlineAtVoteStart.add(playerUuid);
    }
    public static ArrayList<String> returnPlayersOnlineAtVoteStart(){
        return playersOnlineAtVoteStart;
    }
}

public class Voting {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        LiteralCommandNode<ServerCommandSource> register = dispatcher.register(CommandManager.literal("vote")
                .then(CommandManager.literal("nightskip").executes(Voting::voteSkipNight))
                .then(CommandManager.literal("yes").executes(Voting::voteYes))
                .then(CommandManager.literal("no").executes(Voting::voteNo))
                .then(CommandManager.literal("stop").executes(Voting::voteStop))
        );
    }

    private static void announceVote(MinecraftServer server){
        int voteThreshold = Vote.returnVoteThreshold(); // Get the vote threshold
        int votesToGo = voteThreshold - 1; // set the votes left to pass the vote to the threshold minus one since you voted already
        switch (Vote.returnVoteAbout()){
            case 0: // skip night vote
                Text message = Text.of(Vote.returnVotePlayerStarted() + " started vote to skip the night."); // create the message
                server.getPlayerManager().broadcast(message, true); // Send it to the player in the HUD
                server.getPlayerManager().broadcast(message, false); // Send it to all the players in chat
                break;
        }

        Text message = Text.of(voteThreshold + " votes needed, only " + votesToGo + " to go"); // Message to show votes left to pass
        server.getPlayerManager().broadcast(message, false); // Send it to all chat only
    }

    public static int voteSkipNight(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Init all the global stuff
        MinecraftServer server = context.getSource().getServer(); // Get server
        World world = context.getSource().getWorld();   // Get world
        ServerWorld serverWorld = (ServerWorld) world;  // Get server world
        PlayerEntity player = context.getSource().getPlayer();  // get the player who started the vote.

        ///////////////////////////////////////////////////////
        // Guard clause to check if a vote is not already active!
        if(Vote.returnVoteActive()) {
            Text message = Text.of("A vote is already going on! Wait for it to finish.");
            player.sendMessage(message, false);
            return 0;
        }

        ///////////////////////////////////////////////////////
        // Guard clause to check if its night or not.
        if(serverWorld.getTimeOfDay() > 12969L && serverWorld.getTimeOfDay() < 23041L ){
            Text message = Text.of("It isn't night yet, wait for night to start the vote..");
            player.sendMessage(message, false);
            return 0;
        }

        ////////////////////////////
        // Create a list of all players online at START of the vote. If not enough online, no vote starts
        // Get a list of all players Uuid online at start of the vote and set these in the playersOnlineAtVoteStart
        List<ServerPlayerEntity> playerList = server.getPlayerManager().getPlayerList(); // Get and set list of all players online at time of vote
        if(playerList.size() == 1){ // if there is only 1 person online then no vote allowed
            Text message = Text.of( "Voting requires two or more people"); // No vote allowed message chat
            server.getPlayerManager().broadcast(message, false); // Send it to player in chat, not in hud, hence the false
            return 0; // Guard clause, will exit the voting process.
        }

        ////////////////////////////
        // Vote starting can begin!!
        // Set the vote to active
        Vote.setVoteActive(true);
        // Set who started the vote.
        Vote.setVotePlayerStarted(player.getName().toString());
        // Set what the vote is about
        Vote.setVoteAbout(0); // 0 is to set the vote about skip night
        // Add the players online at start of vote to the vote list.
        for (ServerPlayerEntity serverPlayerEntity : playerList) {
            Vote.addPlayersOnlineAtVoteStart(serverPlayerEntity.getUuidAsString());
        }
        // Set the threshold
        Vote.setVoteThreshold();
        // Anounce the vote
        announceVote(server);

        // Set first vote
        Vote.voteCastInFavor();
        Vote.setPlayersVoted(player.getUuidAsString());

        // handle vote
        handleVote(server, serverWorld);

        return 0;
    }

    private static void handleVote(MinecraftServer server, ServerWorld serverWorld){
        if(Vote.returnVotesCastInFavor() >= Vote.returnVoteThreshold()){
            Text message = Text.of("Vote passed!"); // Message to show votes left to pass
            server.getPlayerManager().broadcast(message, true); // Send it to all players hud
            server.getPlayerManager().broadcast(message, false); // Send it to all chat
            serverWorld.setTimeOfDay(0L);
            resetVote();
        }
        int voteFailThreshold = server.getCurrentPlayerCount() - Vote.returnVoteThreshold();
        if(Vote.returnVotesCastAgainst() > voteFailThreshold){
            Text message = Text.of("Vote failed!"); // Message to show votes left to pass
            server.getPlayerManager().broadcast(message, true); // Send it to all players hud
            server.getPlayerManager().broadcast(message, false); // Send it to all chat
            resetVote();
        }
    }

    private static void resetVote(){
        Vote.setVoteActive(false);
    }

    private static int voteStop(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        World world = context.getSource().getWorld();
        ServerWorld serverWorld = (ServerWorld) world;  // Get server world
        PlayerEntity player = context.getSource().getPlayer();

        if(Vote.returnVotePlayerStarted() == player.getName().toString()){
            Text message = Text.of(Vote.returnVotePlayerStarted() + " has stopped the vote."); // Message to show votes left to pass
            server.getPlayerManager().broadcast(message, false); // Send it to all players chat
            resetVote();
        }

        return 0;
    }

    private static int voteYes(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        World world = context.getSource().getWorld();
        ServerWorld serverWorld = (ServerWorld) world;  // Get server world
        PlayerEntity player = context.getSource().getPlayer();

        if(Vote.returnPlayersVoted().contains(player.getUuidAsString())){
            Text message = Text.of("You have already voted...");
            player.sendMessage(message, true); // send message to the hud of the player
            return 0; // guard clause
        }

        Vote.voteCastInFavor();
        Vote.setPlayersVoted(player.getUuidAsString());
        handleVote(server, serverWorld);
        return 0;
    }

    private static int voteNo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        World world = context.getSource().getWorld();
        ServerWorld serverWorld = (ServerWorld) world;  // Get server world
        PlayerEntity player = context.getSource().getPlayer();

        if(Vote.returnPlayersVoted().contains(player.getUuidAsString())){
            Text message = Text.of("You have already voted...");
            player.sendMessage(message, true); // send message to the hud of the player
            return 0; // guard clause
        }

        Vote.voteCastAgainst();
        Vote.setPlayersVoted(player.getUuidAsString());
        handleVote(server, serverWorld);
        return 0;
    }

    private static void setWorldTime(Voting voting, PlayerEntity player){

    }
}
