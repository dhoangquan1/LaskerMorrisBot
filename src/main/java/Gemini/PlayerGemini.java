package Gemini;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.apache.http.HttpException;

//Google API Imports
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class PlayerGemini {
    public static State curr_state = new State();
    public static String playerStone = "";
    public static String oppStone = "";
    public static String playerHand = "";
    public static String oppHand = "";
    public static String stoneType = "";

    public static long timeLimit = 50000;
    public static boolean firstRun = true;

    public static void main(String[] args) throws IOException, HttpException, InterruptedException {
        String APIkey = getAPIKey();
        Client client = Client.builder().apiKey(APIkey).build();

        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();

            //If initializing, then set stone and decide to wait or play.
            //Else already playing, then check illegal move and update board.
            if(playerStone.equals("")){
                if (input.equals("orange")){
                    oppStone = "B";
                    oppHand = "h1";
                    playerStone = "O";
                    playerHand = "h2";
                    //timeLimit = 3_950_000_000L;
                    continue;
                }
                playerStone = "B";
                playerHand = "h1";
                oppStone = "O";
                oppHand = "h2";
            }else {
                process_opponent_move(input);
            }

            //Game playing with minimax
            if(!input.startsWith("END")) {
                long startTime = System.currentTimeMillis();
                String bestMove = getGeminiMoves(client);

                if(bestMove.length()!=8){
                    bestMove = getARandomMove();
                }

                long timeLeft = timeLimit - (System.currentTimeMillis() - startTime);
                if (timeLeft > 0){
                    Thread.sleep(timeLeft);
                }
                //Report the move to the referee
                System.out.println(bestMove);
                System.out.flush();

                process_player_move(bestMove);
                //printALLTestsInfo();
            }

            if(!firstRun){
                int winner = checkUtil_WinGame(curr_state);
                if (winner != 0){
                    String winner_stoneType = (winner == 1) ? playerStone : oppStone;
                    System.out.println(winner_stoneType + " wins!");
                    System.out.flush();
                }
            }
        }
    }

    //____________________________________________________________________
    //                    MOVE AND STATE PROCESSING RELATED CODES
    //____________________________________________________________________

    /**
     * Check for illegal moves made by a player
     * @param move: the move to be checked
     * @param player: the player making the move
     * @return true if the move is legal, else false
     */
    public static String checkLegalMove(String move, int player) {
        long startTime = System.nanoTime();
        boolean answer = true;

        String A = move.substring(0,2);
        String B = move.substring(3,5);
        String C = move.substring(6,8);

        //System.out.println(curr_state.board.get(A));

        String tempStoneType = "";

        //Get the correct stone type for the check
        if(player == 1) {
            tempStoneType = oppStone;
        } else {
            tempStoneType = playerStone;
        }

        //Check that the string is in the correct format
        if(!GameConstants.ADJACENT_MOVES.containsKey(A) && !A.equalsIgnoreCase("h1") && !A.equalsIgnoreCase("h2")) {
            //Uncomment in the case printing should occur:
            return ("Incorrect move format, part <x1> is incorrect");
        } else if (!GameConstants.ADJACENT_MOVES.containsKey(B)) {
           //Uncomment in the case printing should occur:
            return ("Incorrect move format, part <y1> is incorrect");
            //System.out.println("Incorrect move format, part B is incorrect");
        } else if (!GameConstants.ADJACENT_MOVES.containsKey(C) && !C.equalsIgnoreCase("r0")) {
            //Uncomment in the case printing should occur:
            return ("Incorrect move format, part <z1> is incorrect");
            //System.out.println("Incorrect move format, part C is incorrect")
        }

        //Checks if a player attempts to place a piece out of the opponent's hand
        String correctHand = (player == 1) ? oppHand : playerHand;
        if((A.equalsIgnoreCase("h1") && playerHand == "h1" && player == 1) || (A.equalsIgnoreCase("h2") && playerHand == "h2" && player == 1) || (A.equalsIgnoreCase("h1") && oppHand == "h1" && player == 0) || (A.equalsIgnoreCase("h2") && oppHand == "h2" && player == 0)) {
            //Uncomment in the case printing should occur:
            return ("Illegal Move Made, attempt to place a stone out of the other player's hand");
            //System.out.println("Illegal Move Made, attempt to place a stone out of the other player's hand");
        }

        //Checks if a piece is attempted to be placed without having any in hand to place
        if(curr_state.stoneHand[player] == 0 && A.contains("h")) {
            //Uncomment in the case printing should occur:
            return ("Illegal Move Made, attempt to place a stone with out having one in hand");
            //             System.out.println("Illegal Move Made, attempt to place a stone with out having one in hand");
        }

        //Check that when placing a piece there is not already one in that position
        if(!curr_state.openSlots.contains(B)) {
            //Uncomment in the case printing should occur:
            return ("Illegal Move Made, attempt to place a stone on a non-empty position");
            //System.out.println("Illegal Move Made, attempt to place a stone on a non-empty position");
        }

       //Check if a piece is attempted to be removed where a piece does not exist
       if(curr_state.openSlots.contains(C)) {
            //Uncomment in the case printing should occur:
           return ("Illegal Move Made, attempt to remove a piece where one does not exist");
            //System.out.println("Illegal Move Made, attempt to remove a piece where one does not exist");
        }

        boolean millFormed = curr_state.checkMoveMadeMillNoUpdate(move, tempStoneType);
        //Check if a piece isn't removed when there is a mill made
        if(millFormed && C.equalsIgnoreCase("r0")) {
            //Uncomment in the case printing should occur:
            return ("Illegal Move Made, didn't remove piece when mill was made");
           //System.out.println("Illegal Move Made, didn't remove piece when mill was made");
        }

        //Check if a piece is removed when there isn't a mill made
        if(!millFormed && !C.equalsIgnoreCase("r0")) {
            //Uncomment in the case printing should occur:
            return ("Illegal Move Made, tried removing a piece when a mill wasn't made");
            //System.out.println("Illegal Move Made, tried removing a piece when a mill wasn't made");
        }

        if(!A.contains("h")) {
            //Checks if a player attempts to move a piece that isn't theirs
            if((curr_state.board.get(A).equalsIgnoreCase(oppStone) && player == 0) || (curr_state.board.get(A).equalsIgnoreCase(playerStone) && player == 1)) {
                //Uncomment in the case printing should occur:
                return ("Illegal Move Made, attempt to move a stone that isn't owned by the player");
                //System.out.println("Illegal Move Made, attempt to move a stone that isn't owned by the player");
           }
        }

        //Check that if the player is in phase 2 that they do not "fly"
        if(curr_state.stoneHand[player] == 0 && curr_state.stonePlaced[player] > 3) {
            boolean isValid = false;
            //Check that B is a possible adjacent move of B
            for(String possibleMove : GameConstants.ADJACENT_MOVES.get(A)) {
                if(possibleMove.equalsIgnoreCase(B)) {
                    isValid = true;
                }
            }

            if(!isValid) {
                //Uncomment in the case printing should occur:
                return ("Illegal Move Made, moved not to an adjacent location");
                //System.out.println("Illegal Move Made, moved not to an adjacent location");
            }
        }
        //System.out.println("Time to validate opp's move: " + (System.nanoTime() - startTime)/1_000_000L);
        return "SUCCESS";
    }

    /**
     * Process the move returned by the ref and update the state
     * @param move the move that was made by the opponent
     */
    public static void process_opponent_move(String move) {
        String[] new_moves = move.split(" ");

        //Check the move is legal
        /*
        boolean isLegal = checkLegalMove(move, 1);
        if(!isLegal) {
            System.out.println("Invalid Move Made, Game is Invalid from this point on");
        }
            */

        if (new_moves[0].startsWith("h")) {
            // placing //
            curr_state.board.put(new_moves[1], oppStone);
            curr_state.stoneHand[1]--;
            curr_state.stonePlaced[1]++;
            curr_state.openSlots.remove(new_moves[1]);
            curr_state.checkMoveMadeMill(new_moves[1], oppStone);
        } else {
            // moving //
            curr_state.board.put(new_moves[0], "");
            curr_state.openSlots.add(new_moves[0]);
            curr_state.board.put(new_moves[1], oppStone);
            curr_state.openSlots.remove(new_moves[1]);
            curr_state.oppMill.removeIf(mill -> mill.contains(new_moves[0]));
            curr_state.checkMoveMadeMill(new_moves[1], oppStone);
        }

        // removing //
        if (!new_moves[2].equals("r0")) {
            curr_state.board.put(new_moves[2], "");
            curr_state.openSlots.add(new_moves[2]);
            curr_state.stonePlaced[0]--;
            for(List<String> mill: curr_state.playerMill){
                curr_state.playerMill.removeIf(m -> m.contains(new_moves[2]));
            }
        }

    }

    /**
     * Process the move returned by the ref and update the state
     * @param move the move that was made by the opponent
     */
    public static void process_player_move(String move) {
        String[] new_moves = move.split(" ");

        //Check the move is legal
        /*
        boolean isLegal = checkLegalMove(move, 1);
        if(!isLegal) {
            System.out.println("Invalid Move Made, Game is Invalid from this point on");
        }
            */

        if (new_moves[0].startsWith("h")) {
            // placing //
            curr_state.board.put(new_moves[1], playerStone);
            curr_state.stoneHand[0]--;
            curr_state.stonePlaced[0]++;
            curr_state.openSlots.remove(new_moves[1]);
            curr_state.checkMoveMadeMill(new_moves[1], playerStone);
        } else {
            // moving //
            curr_state.board.put(new_moves[0], "");
            curr_state.openSlots.add(new_moves[0]);
            curr_state.board.put(new_moves[1], playerStone);
            curr_state.openSlots.remove(new_moves[1]);
            curr_state.playerMill.removeIf(mill -> mill.contains(new_moves[0]));
            curr_state.checkMoveMadeMill(new_moves[1], playerStone);
        }

        // removing //
        if (!new_moves[2].equals("r0")) {
            curr_state.board.put(new_moves[2], "");
            curr_state.openSlots.add(new_moves[2]);
            curr_state.stonePlaced[1]--;
            for(List<String> mill: curr_state.oppMill){
                curr_state.oppMill.removeIf(m -> m.contains(new_moves[2]));
            }
        }

    }

    //Check for win game
    public static int checkUtil_WinGame(State state){
        int playerStoneLeft = state.stoneHand[0] + state.stonePlaced[0];
        int oppStoneLeft = state.stoneHand[1] + state.stonePlaced[1];
        if(playerStoneLeft < 3){
            return -1;
        }else if(oppStoneLeft < 3){
            return 1;
        }

        int playerBlocked = 0;
        int oppBlocked = 0;
        for(String move: GameConstants.ADJACENT_MOVES.keySet()){
            String curr_stone = state.board.get(move);
            if(!curr_stone.equals("")){
                String[] moveSet = GameConstants.ADJACENT_MOVES.get(move);
                boolean blocked = true;
                for(String neighbor: moveSet){
                    if (state.board.get(neighbor).equals("")){
                        blocked = false;
                        break;
                    }
                }
                if(blocked){
                    if(curr_stone.equals(playerStone)){
                        playerBlocked++;
                    }else if(curr_stone.equals(oppStone)){
                        oppBlocked++;
                    }
                }
            }
        }

        if(state.stoneHand[0] == 0 && playerBlocked == state.stonePlaced[0] && playerStoneLeft > 3){
            return -1;
        }else if(state.stoneHand[1] == 0 && oppBlocked == state.stonePlaced[1]  && oppStoneLeft > 3){
            return 1;
        }
        return 0;
    }

    //____________________________________________________________________
    //                    GEMINI API RELATED CODES
    //____________________________________________________________________

    public static String getAPIKey(){
        Properties props = new Properties();

        try (FileInputStream fis = new FileInputStream("config.env")) {
            props.load(fis);
            String apiKey = props.getProperty("GEMINI_API_KEY");

            if (apiKey == null) {
                System.err.println("Error: GEMINI_API_KEY not found in config.env.");
            }
            return apiKey;
        } catch (IOException e) {
            System.err.println("Error reading config.env: " + e.getMessage());
        }

        return "Error";
    }

    public static String getGeminiMoves(Client client) throws IOException, HttpException{
        StringBuilder adjacentMoves = new StringBuilder();
        for (Map.Entry<String, String[]> entry : GameConstants.ADJACENT_MOVES.entrySet()) {
            adjacentMoves.append(entry.getKey()).append(": ");
            adjacentMoves.append(String.join(", ", entry.getValue()));
            adjacentMoves.append("\n");
        }

        StringBuilder boardState = new StringBuilder();
        for (Map.Entry<String, String> entry : curr_state.board.entrySet()) {
            boardState.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }

        StringBuilder millConditions = new StringBuilder();
        for (List<String> entry : GameConstants.MILL_CONDITIONS) {
            millConditions.append(entry).append("\n");
        }

        String prompt = """
                You are tasked with finding the next best move for a Lasker Morris game, according to the current board configuration. It is your turn.
                You move must be valid and satisfy ALL of the game constraints.
                
                Your stone is represented by:
                """ + playerStone + """
                
                Your opponent's stone is represented by:
                """ + oppStone + """
                
                Your hand is represented by:
                """ + playerHand + """
                
                The amount of stones that you have left in your hand is:
                """ + curr_state.stoneHand[0] + """
                
                The amount of stones that your opponent have left in their hand is:
                """ + curr_state.stoneHand[1] + """
                
                The locations on the board that is empty or does not have a stone yet:
                """ + curr_state.openSlots + """
                
                                
                The rule of the game is of follow:
                Players start with 10 stones (also called pieces) in their hand.
                
                You must take an action every turn to continue: You MUST either place a new stone or move an already placed stone.
                    If you choose to place a stone from your hand to the board, then you can place it at any location that is empty, but you must have a stone in your hand.
                    If you choose to move your stone that is already on the board, then you can move it to any location that is empty and adjacent to the original stone's location.
                        These adjacent positions are described in (location: list of adjacent locations):
                        """ + adjacentMoves + """
                    Only and when you have 3 stones left in total, in both your hand and on the board, then you enter the "flying" phase.
                    The "flying" phase is where you can choose to move any of your stones to any empty locations on the board.
                    You MUST only move and place a stone of your type.
                    You MUST only move and place your stone to an empty location (no stone at that location).
                    You MUST only use your own hand and not your opponent's hand (your hand's representation).
                    If you do not have a stone in hand, then you MUST move your stone that is on the board to an empty location adjacent to it.
                
                After every action that you take (placing or moving a stone), you MUST check if you have formed a mill.
                 
                When you placed three stones that have locations contiguous to each other, then you have formed a mill.                
                    The sets of possible mills are:
                    """ + millConditions + """
                    Each set of mill has three locations.
                    You have a mill when your stones are on all three locations of any of those sets.
                    It does not have to be in the exact order.
            
                If after you check and see that you have formed a mill, you MUST always remove a stone that is placed on the board by your opponent.
                    The stone that you will remove must not be part of the opponent's mills.
                    Unless all stones placed by your opponent are part of their mill, then you remove any of their stone on the board.
                    If there is no opponent's stone on the board, then you do not remove any stone.
                    BUT if there is an opponent's stone on the board, then you MUST ALWAYS remove one.
                                
                If a player is reduced to 2 pieces (including hand + deck) then they lose.
                If a player immobilizes an opponent's pieces, preventing them from having any available actions then they win, provided they cannot place another piece.
                
                The board data structure is a grid-like structure with: 
                    the x-axis being letter a-g from left to right
                    the y-axis being number 1-7 from bottom to top
                Each location signifies the coordinate on the board, and it is in the format of "xy", where x is the x-axis in letter and y is the y-axis in number (example: a1).
                Only the location listed on the current board are valid coordinates (You must play the exactly and only coordinates that are listed).
                The connection between coordinates are signify by the list of adjacent locations.
                    If a location has an adjacent locations, then they are connected, and vice versa.
                    Three continuously connected locations are potentials for a mill.
                    Meaning if you have stones of your type on a three continously-connected locations, you have made a mill.
                    
                The current board right now is (format: location = type of stone):
                """ + boardState + """
                If the location is equal to empty, that means there is no stone at that specific location.
                If the location is equal to opponent's stone, that means there is a opponent's stone at that specific location.
                If the location is equal to your stone representation, that means you have a stone at that specific location (that may be able to move).
                You MUST play according to this current board state.
                
                You must give the next best move to be made that will optimally lead you to a win.
                You should consider these strategies when making your moves:
                    Forming your own mill, and remove your opponent's piece.
                    Blocking the opponent's setups so that they cannot form a mill.
                    Check if your opponent about to form a mill so you can potentially block it (IMPORTANT).
                    Preventing yourself from getting immobilized.
                    Immobilizing your opponent.
                    Optimizing moves to allow you to have more stones than your opponent.
                    Setting up configurations where you can form a mill in the future.
                    Closing out a win if you see an opportunity to.
                    Good setup configurations might includes:
                        Your mills share a common piece.
                        Two of your stones are part of a mill setup, and the next location to form that mill is empty
                        Three of your stones are part of a mill setup, and there are two potential locations that you can choose to form a mill
                    ONLY setup appropriately to force your opponent to block you, or if your opponent cannot form a mill in their next turn.
                    If your opponent can also form a mill in the next turn, it might be better to block their potential mill instead of setting up.
                    When you are in flying phase, it is best to make actions that allow you to close out the game and create mills repetitively.
                    
                You MUST ONLY answer in the format of "<x1> <y1> <z1>", where x1, y1 are the locations as described in the game rules and z1 is the piece to be removed. 
            
                If you are placing a piece from your hand, then <x1> is h1 (if your stone is B) and h2 (if your stone is O)
                Else, <x1> is the location of your stone on the board that you want to move.
                You must move your stone on the board if you do not have any stones left in your hand (meaning <x1> cannot be equal to "h1" or "h2").
                
                <y1> is the location of an empty spot that you will move or place your stone to.
                
                If your move have formed a mill this turn, then <z1> is the location of your opponent's stone that you will remove.
                If you have not formed a mill this turn, then you must put "r0" for <z1>.
                Remember to always check if you have made a mill this turn, and ALWAYS remove an opponent's stone if you did formed a mill.
      
                Examples moves:
                 - Move: a7 a4 r0 (Standard move: moving your stone from a7 to a4)
                 - Move: h1 a4 r0 (Placing from hand to a4)
                 - Move: c1 c4 a1 (Forming a mill by moving your stone from c1 to c4, and removing opponent piece at a1)
               
                Say nothing except for the 3 characters (format: "<x1> <y1> <z1>") representing your move
                You must say at least 3 characters (format: "<x1> <y1> <z1>") representing your move
                Your move must always be valid according to the game constraints, and under the circumstances of the current board configuration.
                """;

        String legalCheck = "";
        String redefined = "";
        String attempt = "";
        int tries = 0;
        int maxTries = 5;

        do{
            CompletableFuture<GenerateContentResponse> responseFuture =
                    client.async.models.generateContent(
                            "gemini-2.0-flash-001", redefined + prompt, null);

            CompletableFuture<String> resFuture = new CompletableFuture<>();
            responseFuture
                    .thenAccept(response -> resFuture.complete(response.text().trim()))
                    .exceptionally(ex -> {
                        resFuture.completeExceptionally(ex);
                        return null;
                    })
                    .join();

            try {
                attempt = resFuture.join();
            } catch (Exception e) {
                break;
            }

            legalCheck = checkLegalMove(attempt, 0);
            redefined = "Your previous move: " + attempt + " was invalid, please generate a different legal move. The error was: " + legalCheck + "\n";

            tries++;
        }while (!legalCheck.equals("SUCCESS") && tries < maxTries);

        //System.out.println(prompt);
        return attempt;
    }

    //____________________________________________________________________
    //                    SUCCESSORS RELATED CODES
    //____________________________________________________________________

    /**
     * Get a random move for Gemini in case it fails
     */
    public static String getARandomMove(){
        State rS = getSuccessors(curr_state, 0).get(0);
        return String.format("%s %s %s",rS.moveSet[0], rS.moveSet[1], rS.moveSet[2]);
    }

    /**
     * Get all successors of a state
     * @param state the parent state
     * @param playerType the player that is making the next move
     * @return List of successor states
     */
    public static ArrayList<State> getSuccessors(State state, int playerType){
        ArrayList<State> successors = new ArrayList<>();
        stoneType = (playerType == 0) ? playerStone : oppStone;

        //If there is 3 stone left total
        if (state.stoneHand[playerType] + state.stonePlaced[playerType] <= 3){
            getSuccessors_FlyingtoBoard(state, playerType, successors);
        } else {
            //If there is stone left in hand
            if(state.stoneHand[playerType] > 0){
                getSuccessors_HandtoBoard(state, playerType, successors);
            }
            //If there is stone left in board
            if (state.stonePlaced[playerType] > 0){
                getSuccessors_BoardtoBoard(state, playerType, successors);
            }
        }
        return successors;
    }

    /**
     * Get all successor states that can be made from moving stone from hand to board
     * @param state the parent state
     * @param playerType the player that is making the next move
     * @param successors the list of successors to be added to
     */
    public static void getSuccessors_HandtoBoard(State state, int playerType, ArrayList<State> successors){
        for(String move: state.openSlots){
            State tempS = new State(state);
            tempS.board.put(move, stoneType);
            tempS.stoneHand[playerType]--;
            tempS.stonePlaced[playerType]++;
            tempS.openSlots.remove(move);
            tempS.moveSet[0] = (playerType == 0) ? playerHand : oppHand;
            tempS.moveSet[1] = move;
            if(tempS.checkMoveMadeMill(move, stoneType)){
                getSuccessors_captureStone(tempS, playerType, successors);
            }else {
                successors.add(tempS);
                tempS.moveSet[2] = "r0";
            }
        }
    }

    /**
     * Get all successor states that can be made from moving stone from board to board
     * @param state the parent state
     * @param playerType the player that is making the next move
     * @param successors the list of successors to be added to
     */
    public static void getSuccessors_BoardtoBoard(State state, int playerType, ArrayList<State> successors){
        for(String move: state.board.keySet()) {
            if(state.board.get(move).equals(stoneType)){
                for(String neighbor: GameConstants.ADJACENT_MOVES.get(move)){
                    if(state.board.get(neighbor).equals("")){
                        State tempS = new State(state);
                        tempS.board.put(move, "");
                        tempS.board.put(neighbor, stoneType);
                        tempS.openSlots.add(move);
                        tempS.openSlots.remove(neighbor);
                        tempS.moveSet[0] = move;
                        tempS.moveSet[1] = neighbor;
                        ArrayList<List<String>> tempSMill = (stoneType.equals(playerStone)) ? tempS.playerMill : tempS.oppMill;
                        tempSMill.removeIf(mill -> mill.contains(move));
                        if(tempS.checkMoveMadeMill(neighbor, stoneType)){
                            getSuccessors_captureStone(tempS, playerType, successors);
                        }else {
                            successors.add(tempS);
                            tempS.moveSet[2] = "r0";
                        }
                    }
                }
            }
        }
    }

    /**
     * Get all successor states that can be made from moving stone from flying
     * @param state the parent state
     * @param playerType the player that is making the next move
     * @param successors the list of successors to be added to
     */
    public static void getSuccessors_FlyingtoBoard(State state, int playerType, ArrayList<State> successors){
        for(String move: state.board.keySet()) {
            if(state.board.get(move).equals(stoneType)){
                for(String open: state.openSlots){
                    State tempS = new State(state);
                    tempS.board.put(move, "");
                    tempS.board.put(open, stoneType);
                    tempS.openSlots.add(move);
                    tempS.openSlots.remove(open);
                    tempS.moveSet[0] = move;
                    tempS.moveSet[1] = open;
                    ArrayList<List<String>> tempSMill = (stoneType.equals(playerStone)) ? tempS.playerMill : tempS.oppMill;
                    tempSMill.removeIf(mill -> mill.contains(move));
                    if(tempS.checkMoveMadeMill(open, stoneType)){
                        getSuccessors_captureStone(tempS, playerType, successors);
                    }else {
                        successors.add(tempS);
                        tempS.moveSet[2] = "r0";
                    }
                }
            }
        }
    }

    /**
     * Get all successor states that can be made after moving stone, and can capture an opponent's stone
     * @param state the parent state
     * @param playerType the player that is making the next move
     * @param successors the list of successors to be added to
     */
    public static void getSuccessors_captureStone(State state, int playerType, ArrayList<State> successors){
        String targetStoneType = (playerType == 0) ? oppStone : playerStone;
        ArrayList<List<String>> targetMill = (playerType == 0) ? state.oppMill : state.playerMill;
        int targetType = (playerType == 0) ? 1 : 0;
        boolean allMills = true;

        //Get stones that are not part of mills
        for(String move: state.board.keySet()) {
            if(state.board.get(move).equals(targetStoneType) ){
                for(List<String> mill: targetMill){
                    if(!mill.contains(move)){
                        State tempS = new State(state);
                        tempS.board.put(move, "");
                        tempS.stonePlaced[targetType]--;
                        tempS.openSlots.add(move);
                        tempS.moveSet[2] = move;
                        allMills = false;
                        successors.add(tempS);
                    }
                }
            }
        }

        //If cannot get stones that are not part of mills, then get stones that are part of mills
        if(allMills){
            for(String move: state.board.keySet()) {
                if(state.board.get(move).equals(targetStoneType) ){
                    State tempS = new State(state);
                    tempS.board.put(move, "");
                    tempS.stonePlaced[targetType]--;
                    tempS.openSlots.add(move);
                    tempS.moveSet[2] = move;
                    ArrayList<List<String>> tempSMill = (stoneType.equals(playerStone)) ? tempS.oppMill : tempS.playerMill;
                    tempSMill.removeIf(mill -> mill.contains(move));
                    successors.add(tempS);
                }
            }

        }

    }
    
    //____________________________________________________________________
    //                    TESTINGS RELATED CODES
    //____________________________________________________________________
    public static void printALLTestsInfo(){
        printBoard();
        System.out.println("Player Mills: " + curr_state.playerMill);
        System.out.println("Opp Mills: " + curr_state.oppMill);
        System.out.println("Open Slots: " + curr_state.openSlots);
        System.out.println("Your Hand: " + curr_state.stoneHand[1]);
        System.out.println("BOT Hand: " + curr_state.stoneHand[0]);
        System.out.println();
    }

    public static void printBoard(){
        System.out.println();
        System.out.printf ("%s ---------------- %s ----------------- %s\n",getP("a7"),getP("d7"),getP("g7"));
        System.out.println("|                  |                  |");
        System.out.printf ("|      %s --------- %s --------- %s      |\n",getP("b6"),getP("d6"),getP("f6"));
        System.out.println("|     |            |            |     |");
        System.out.printf ("|     |      %s --- %s --- %s      |     |\n",getP("c5"),getP("d5"),getP("e5"));
        System.out.println("|     |      |           |      |     |");
        System.out.printf ("%s --- %s --- %s            %s --- %s --- %s\n",getP("a4"),getP("b4"),getP("c4"), getP("e4"),getP("f4"),getP("g4"));
        System.out.println("|     |      |           |      |     |");
        System.out.printf ("|     |      %s --- %s --- %s      |     |\n",getP("c3"),getP("d3"),getP("e3"));
        System.out.println("|     |            |            |     |");
        System.out.printf ("|      %s --------- %s --------- %s      |\n",getP("b2"),getP("d2"),getP("f2"));
        System.out.println("|                  |                  |");
        System.out.printf ("%s ---------------- %s ----------------- %s\n",getP("a1"),getP("d1"),getP("g1"));
        System.out.println();
    }

    public static String getP(String key){
        String piece = curr_state.board.get(key);
        if(piece.equals("")){
            piece = "?";
        }else if(piece.equals("B")){
            piece = "\u001B[34m" + piece + "\u001B[0m";
        }else {
            piece = "\u001B[38;5;208m" + piece + "\u001B[0m";
        }
        return piece;
    }

}