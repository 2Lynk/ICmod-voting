package icmod_voting.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

class Vote {
    private static boolean voteActive = false; // false = no vote active, true = vote active
    private static long voteTime = 20000L; // 20 seconds - each 1000 is one second
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
    public static long returnVoteTime(){
        return voteTime;
    }
    public static void setVoteTime(long number) {
        voteTime = number;
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

public class Voting extends Thread {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        LiteralCommandNode<ServerCommandSource> register = dispatcher.register(CommandManager.literal("vote")
                .then(CommandManager.literal("skip")
                    .then(CommandManager.literal("night").executes(Voting::voteSkipNight))
                )
                .then(CommandManager.literal("yes").executes(Voting::voteYes))
                .then(CommandManager.literal("no").executes(Voting::voteNo))
//                .then(CommandManager.literal("stop").executes(Voting::voteStop))
        );
    }

    private static void announceVote(MinecraftServer server){
        int voteThreshold = Vote.returnVoteThreshold(); // Get the vote threshold
        int votesToGo = voteThreshold - 1; // set the votes left to pass the vote to the threshold minus one since you voted already
        switch (Vote.returnVoteAbout()){
            case 0: // skip night vote
                broadcast(server, Vote.returnVotePlayerStarted() + " started vote to skip the night.", 0);
                break;
        }
        broadcast(server, Vote.returnVoteTime()/1000 + " seconds to vote!", 2);
        broadcast(server, voteThreshold + " votes needed, only " + votesToGo + " to go", 2);
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
            messagePlayer(server, "A vote is already going on! Wait for it to finish.", 2);
            return 0;
        }

        ///////////////////////////////////////////////////////
        // Guard clause to check if its night or not.
        if(serverWorld.getTimeOfDay() < 12969L && serverWorld.getTimeOfDay() > 23041L ){
            messagePlayer(server, "It isn't night yet, wait for night to start the vote..", 2);
            return 0;
        }

        ////////////////////////////
        // Create a list of all players online at START of the vote. If not enough online, no vote starts
        // Get a list of all players Uuid online at start of the vote and set these in the playersOnlineAtVoteStart
        List<ServerPlayerEntity> playerList = server.getPlayerManager().getPlayerList(); // Get and set list of all players online at time of vote
        if(playerList.size() == 1){ // if there is only 1 person online then no vote allowed
            broadcast(server, "Voting requires two or more people", 2); // No vote allowed message chat
            return 0; // Guard clause, will exit the voting process.
        }

        ////////////////////////////
        // Vote starting can begin!!
        // Set the vote to active
        Vote.setVoteActive(true);
        // Set who started the vote.
        Vote.setVotePlayerStarted(player.getEntityName());
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
        // Start the timer to kill vote after x amount of time
        voteSetTimer(server, true); // true to start it

        // Set first vote
        Vote.voteCastInFavor();
        Vote.setPlayersVoted(player.getUuidAsString());

        // handle vote
        handleVote(server, serverWorld);

        return 0;
    }

    private static void handleVote(MinecraftServer server, ServerWorld serverWorld){
        if(Vote.returnVotesCastInFavor() >= Vote.returnVoteThreshold()){
            broadcast(server,"Vote passed!", 0);
            serverWorld.setTimeOfDay(0L);
            resetVote(server);
        }
        int voteFailThreshold = server.getCurrentPlayerCount() - Vote.returnVoteThreshold();
        if(Vote.returnVotesCastAgainst() > voteFailThreshold){
            broadcast(server,"Vote failed!", 0); // Message to show votes left to pass
            resetVote(server);
        }
    }

    private static void resetVote(MinecraftServer server){
        Vote.setVoteActive(false);
        voteSetTimer(server, false); // false to to stop the timer.
    }

    private static int voteStop(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        World world = context.getSource().getWorld();
        ServerWorld serverWorld = (ServerWorld) world;  // Get server world
        PlayerEntity player = context.getSource().getPlayer();

        if(!Vote.returnVoteActive()){
            messagePlayer(server, "No vote active...", 2);
            return 0; // guard clause
        }

        if(Vote.returnVotePlayerStarted() == player.getEntityName()){
            messagePlayer(server, Vote.returnVotePlayerStarted() + " has stopped the vote.", 2); // Message to show votes left to pass
            resetVote(server);
        }

        return 0;
    }

    private static int voteYes(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        World world = context.getSource().getWorld();
        ServerWorld serverWorld = (ServerWorld) world;  // Get server world
        PlayerEntity player = context.getSource().getPlayer();

        if(!Vote.returnVoteActive()){
            messagePlayer(server, "No vote active...",2 );
            return 0; // guard clause
        }

        if(Vote.returnPlayersVoted().contains(player.getUuidAsString())){
            messagePlayer(server, "You have already voted...", 2);
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

        if(!Vote.returnVoteActive()){
            messagePlayer(server, "No vote active...",2 );
            return 0; // guard clause
        }

        if(Vote.returnPlayersVoted().contains(player.getUuidAsString())){
            messagePlayer(server, "You have already voted...", 2);
            return 0; // guard clause
        }

        Vote.voteCastAgainst();
        Vote.setPlayersVoted(player.getUuidAsString());
        handleVote(server, serverWorld);
        return 0;
    }

    private static void setWorldTime(Voting voting, PlayerEntity player){

    }

    private static void broadcast(MinecraftServer server, String message, int style){
        message = "{SERVER}: " + message;
        switch (style){
            case 0: // post to HUD and chat
                server.getPlayerManager().broadcast(Text.of(message), true); // post to HUD
                server.getPlayerManager().broadcast(Text.of(message), false); // post to chat
                break;
            case 1: // post to HUD
                server.getPlayerManager().broadcast(Text.of(message), true); // post to HUD
                break;
            case 2: // post to chat
                server.getPlayerManager().broadcast(Text.of(message), false); // post to HUD
                break;
        }
    }
    private static void messagePlayer(MinecraftServer server, String message, int style){
        message = "{SERVER}: " + message;
        switch (style){
            case 0: // post to HUD and chat
                server.getPlayerManager().broadcast(Text.of(message), true); // post to HUD
                server.getPlayerManager().broadcast(Text.of(message), false); // post to chat
            case 1: // post to HUD
                server.getPlayerManager().broadcast(Text.of(message), true); // post to HUD
            case 2: // post to chat
                server.getPlayerManager().broadcast(Text.of(message), false); // post to HUD
        }
    }

    public static void voteSetTimer(MinecraftServer server, boolean start){
        TimerTask voteEndingThree = new TimerTask() {
            @Override
            public void run() {
                if(Vote.returnVoteActive()){
                    broadcast(server, "Vote ending in 3...", 2);
                }

            }
        };
        TimerTask voteEndingTwo = new TimerTask() {
            @Override
            public void run() {
                if(Vote.returnVoteActive()){
                    broadcast(server, "Vote ending in 2...", 2);
                }

            }
        };
        TimerTask voteEndingOne = new TimerTask() {
            @Override
            public void run() {
                if(Vote.returnVoteActive()){
                    broadcast(server, "Vote ending in 1...", 2);
                }

            }
        };
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(Vote.returnVoteActive()){
                    broadcast(server, "Vote ended, not enough people voted in time!", 2);
                    resetVote(server);
                }

            }
        };
        Timer timerMain = new Timer("Timer main");
        Timer timerOneSec = new Timer("Timer one sec");
        Timer timerTwoSec = new Timer("Timer two sec");
        Timer timerThreeSec = new Timer("Timer three sec");

        if(start){
            timerMain.schedule(task, Vote.returnVoteTime());
            timerOneSec.schedule(voteEndingOne, Vote.returnVoteTime()-1000L);
            timerTwoSec.schedule(voteEndingTwo, Vote.returnVoteTime()-2000L);
            timerThreeSec.schedule(voteEndingThree, Vote.returnVoteTime()-3000L);
        }else{
            timerMain.cancel();
            timerMain.purge();
            timerMain.cancel();
            timerOneSec.purge();
            timerTwoSec.cancel();
            timerTwoSec.purge();
            timerThreeSec.cancel();
            timerThreeSec.purge();
        }

    };
}
