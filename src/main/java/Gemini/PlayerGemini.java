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
    public static String lastMove = "";

    public static long timeLimit = 4000;
    public static int maxTries = 5;
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
                lastMove = input;
            }

            //Game playing with minimax
            if(!input.startsWith("END")) {
                String bestMove = getGeminiMoves(client);

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
        if((A.equalsIgnoreCase("h1") && playerHand.equals("h1") && player == 1) || (A.equalsIgnoreCase("h2") && playerHand.equals("h2") && player == 1) || (A.equalsIgnoreCase("h1") && oppHand == "h1" && player == 0) || (A.equalsIgnoreCase("h2") && oppHand == "h2" && player == 0)) {
            //Uncomment in the case printing should occur:
            return ("Illegal Move Made, attempt to place a stone out of the other player's hand. Please play only from your hand");
            //System.out.println("Illegal Move Made, attempt to place a stone out of the other player's hand");
        }

        //Checks if a piece is attempted to be placed without having any in hand to place
        if(curr_state.stoneHand[player] == 0 && A.contains("h")) {
            //Uncomment in the case printing should occur:
            return ("Illegal Move Made, attempt to place a stone with out having one in hand. Please move your stone from the board instead.");
            //             System.out.println("Illegal Move Made, attempt to place a stone with out having one in hand");
        }

        //Check that when placing a piece there is not already one in that position
        if(!curr_state.openSlots.contains(B)) {
            //Uncomment in the case printing should occur:
            return ("Illegal Move Made, attempt to place a stone on a non-empty position. Please choose your <y1> location to be an EMPTY location on the board.");
            //System.out.println("Illegal Move Made, attempt to place a stone on a non-empty position");
        }

        //Check if a piece is attempted to be removed where a piece does not exist
        if(!C.equals("r0") && curr_state.openSlots.contains(C)) {
            //Uncomment in the case printing should occur:
            return ("Illegal Move Made, attempt to remove a piece where one does not exist. Please remove a piece from a correct location of the board.");
            //System.out.println("Illegal Move Made, attempt to remove a piece where one does not exist");
        }


        boolean millFormed = curr_state.checkMoveMadeMillNoUpdate(B, tempStoneType);

        //Check if a piece isn't removed when there is a mill made
        if(millFormed && C.equalsIgnoreCase("r0")) {
            //Uncomment in the case printing should occur:
            //System.out.println("Illegal Move Made, didn't remove piece when mill was made");
            return ("Illegal Move Made, didn't remove piece when mill was made. Please choose your <z1> to be a location of the opponent's stone that you want to take (NOT r0)");

        }

        //Check if a piece is removed when there isn't a mill made
        if(!millFormed && !C.equalsIgnoreCase("r0")) {
            //Uncomment in the case printing should occur:
            return ("Illegal Move Made, tried removing a piece when a mill wasn't made. Please change your <z1> to be r0");
            //System.out.println("Illegal Move Made, tried removing a piece when a mill wasn't made");
        }

        if(!A.contains("h")) {
            //Checks if a player attempts to move a piece that isn't theirs
            if((curr_state.board.get(A).equalsIgnoreCase(oppStone) && player == 0) || (curr_state.board.get(A).equalsIgnoreCase(playerStone) && player == 1)) {
                //Uncomment in the case printing should occur:
                return ("Illegal Move Made, attempt to move a stone that isn't owned by the player. Please change your <x1> to a location of stone that is owned by you.");
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
                    break;
                }
            }

            if(!isValid) {
                //Uncomment in the case printing should occur:
                return ("Illegal Move Made, moved not to an adjacent location. Please choose <x1> to be a different stone to move or choose <y1> to be an empty adjacent location.");
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

    public static String getGeminiMoves(Client client) throws IOException, HttpException, InterruptedException{
        StringBuilder adjacentMoves = new StringBuilder();
        for (Map.Entry<String, String[]> entry : GameConstants.ADJACENT_MOVES.entrySet()) {
            adjacentMoves.append(entry.getKey()).append(": ");
            adjacentMoves.append(String.join(", ", entry.getValue()));
            adjacentMoves.append("\n");
        }

        StringBuilder boardState = new StringBuilder();
        for (Map.Entry<String, String> entry : curr_state.board.entrySet()) {
            if(entry.getValue().equals("")){
                boardState.append(entry.getKey()).append(" = ").append("EMPTY").append("\n");
            }else {
                boardState.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }

        }

        StringBuilder millConditions = new StringBuilder();
        for (List<String> entry : GameConstants.MILL_CONDITIONS) {
            millConditions.append(entry).append("\n");
        }

        String prompt =
                "You are playing Lasker Morris according to the current board configuration. It is your turn.\n" +
                "You must make a valid move according to ALL the rules.\n" +
                "\n" +
                "Your stone is: " + playerStone + "\n" +
                "Your opponent's stone is: " + oppStone + "\n" +
                "Your hand is represented by: " + playerHand + "\n" +
                "\n" +
                "Game Rules:\n" +
                "\n" +
                "Board:\n" +
                "- Sparse grid, x-axis: a-g, y-axis: 1-7.\n" +
                "- Locations: (number-letter) format (e.g., a1).\n" +
                "\n" +
                "Basic Restrictions:\n" +
                "- Move/place only your stones.\n" +
                "- Target location must be empty.\n" +
                "- Start with 10 stones in hand.\n" +
                "- If no stones in hand, move a stone on the board (but you can move stone on board even when you have stone in hand).\n" +
                "\n" +
                "Turn Actions:\n" +
                "- Place a stone from hand (your stone type) to an EMPTY location.\n" +
                "- Move a stone on the board: to an EMPTY adjacent location.\n" +
                "  - Adjacent locations: " + adjacentMoves + "\n" +
                "- After placing/moving, ALWAYS check if you have formed a mill. If your action led you to form a mill this turn, then you MUST take action regarding removing an opponent's piece (check Mill Removals rules below)\n" +
                "\n" +
                "Flying Phase:\n" +
                "- If your total stones from your (board + hand) == 3, you can move any of your stone to any empty location.\n" +
                "\n" +
                "Mills Conditions:\n" +
                "- Three of your stones contiguously adjacent to each other form a mill.\n" +
                "- Possible mills: \n" +
                millConditions + "\n" +
                "- For example: If you have a stone on `a1` and `d1`, and you plan to place a stone to `g1`, that means you have formed a mill. \n" +
                "Current Board:\n" +
                boardState + "\n" +
                "The board format is: location = Type of stone on that location. \n" +
                "If the location = EMPTY, then there is no stone on that location. \n" +
                "If the location = Your stone type, then you have a stone on that location. \n" +
                "If the location = Opponent's stone type, then opponent has a stone on that location. \n" +
                "The move that you are about to do will be updated to this board, so ALWAYS consider your own move this turn as if it is updated on here. \n" +
                "\n" +
                "Game State:\n" +
                "- The number of your stones in hand: " + curr_state.stoneHand[0] + "\n" +
                "- The number of your stones on board: " + (10 - curr_state.stoneHand[0]) + "\n" +
                "- The number of opponent's stones in hand: " + curr_state.stoneHand[1] + "\n" +
                "- The number of opponent's stones on board: " + (10 - curr_state.stoneHand[1]) + "\n" +
                "- Empty locations on the board: " + curr_state.openSlots + "\n" +
                "- The last move that was made by your opponent is: " + lastMove + "\n" +
                "- Remember that the move you are calculating for this turn will update this board state, so check if the move you about to do will lead you to form a mill or not." + "\n" +
                "\n" +
                "Mill Removal:\n" +
                "- If you form a mill this turn, you MUST ABSOLUTELY remove ONE opponent's stone from the board.\n" +
                "- Remove an opponent's stone (e.g., a1).\n" +
                "- Do not remove opponent's stones from their mills; unless all opponent stones are in mills, then remove any of their stones.\n" +
                "- If no opponent stones, do not remove.\n" +
                "- Always check if the move you are about to take will lead you to form a mill.\n" +
                "\n" +
                "Win Conditions:\n" +
                "- Opponent has 2 or fewer stones.\n" +
                "- Opponent is immobilized (Opponent cannot move any of their stones and they cannot place anymore stones).\n" +
                "\n" +
                "\n" +
                "Strategy:\n" +
                "   1. Prioritized Mill Check: After EACH of your potential moves, perform the following steps:\n" +
                "       a. Temporarily update the current board state as if the move has been made. \n" +
                "       b. Implement the following algorithm to check for mills:\n" +
                "           i. Iterate through EACH of the possible mill formations (provided in `Mills Conditions`).\n" +
                "           ii. For EACH mill formation, check if ALL three positions in that formation are now occupied by your stones in the *simulated* board state.\n" +
                "           iii. If a mill is found, record the mill formations and proceed to step 2.\n" +
                "       c. Undo the temporary board state update.\n" +
                "   2. Mill Closure (If Mill Formed): If a mill was formed in step 1, select an opponent's stone to remove (according to `Mill Removal` rules). \n" +
                "   3. Move Selection: \n" +
                "       a. Game-Ending Moves: If there are any moves that will lead you to immediately win the game, such as immobilizing ALL of your opponent's stones or reduces ALL of their total stones to 2, then ALWAYS go for this move. \n" +
                "       b. Mill-Closing Moves: If there are any moves that create a mill (identified in step 1), always prioritize those moves over any other moves (except for Game-Ending Moves). \n" +
                "       c. Other Moves: If no mill-closing moves are available, consider other strategic moves (blocking, setting up potential mills, etc.). \n" +
                "   4. Output:  Provide the move in the specified format.  Ensure <z1> is correctly set to the removed stone (if a mill was formed) or \"r0\" (if no mill).\n" +
                "   It is important that your move should always prioritized forming a mill this turn over setting up. Meaning you should form a mill at any chance you get.\n" +
                "\n" +
                "Output Format:\n" +
                "- <x1> <y1> <z1>\n" +
                "- <x1>: place from hand (h1 if you are B, h2 if you are O) or stone location (move).\n" +
                "- <y1>: Empty target location.\n" +
                "- <z1>: Location of opponent's stone to remove (if mill), or r0 (no mill).\n" +
                "- Ensure <z1> is r0 ONLY if NO mill was formed this turn. r0 is not a location on the board and is just a placeholder to signify not removing any stones" + "\n" +
                "- Recheck your <y1> to make sure that it is empty (Check the Game State: <y1> must be within that list)" + "\n" +
                "- Recheck your <x1> and <y1> move against the current board and the mill conditions to see if you have created a mill" +
                "\n" +
                "Examples moves:\n" +
                "   - Move: a7 a4 r0 (Standard move: moving your stone from a7 to a4)\n" +
                "   - Move: h1 a4 r0 (Placing from hand to a4)\n" +
                "   - Move: c1 c4 a1 (Forming a mill by moving your stone from c1 to c4, and removing opponent piece at a1) \n" +
                "\n" +
                "Provide your move in the specified format. Do not say anything more than just the formatted answer.\n";

        String legalCheck = "";
        StringBuilder redefined = new StringBuilder();
        String attempt = "";
        int tries = 0;

        do{
            long startTime = System.currentTimeMillis();

            CompletableFuture<GenerateContentResponse> responseFuture =
                    client.async.models.generateContent(
                            "gemini-2.0-flash-001", (prompt + redefined.toString()), null);

            CompletableFuture<String> resFuture = new CompletableFuture<>();
            responseFuture
                    .thenAccept(response -> resFuture.complete(response.text().trim()))
                    .join();

            try {
                attempt = resFuture.join();
            } catch (Exception e) {
                tries++;
                continue;
            }

            legalCheck = checkLegalMove(attempt, 0);
            if (!legalCheck.equals("SUCCESS")) {
                redefined.append("Your previous move: ").append(attempt)
                        .append(" was invalid, please generate a different legal move. The error was: ")
                        .append(legalCheck).append("\n");
            }

            tries++;
            long timeLeft = timeLimit - (System.currentTimeMillis() - startTime);
            if (timeLeft > 0){
                Thread.sleep(timeLeft);
            }
            //System.out.println(redefined + prompt);
        }while (!legalCheck.equals("SUCCESS") && tries < maxTries);

        if (!legalCheck.equals("SUCCESS")) {
            return getARandomMove();
        }


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