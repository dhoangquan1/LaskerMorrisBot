import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
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

    public static long timeLimit = 2_000_000_000L;
    public static boolean firstRun = true;

    public static void main(String[] args) throws IOException, HttpException{
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
                String bestMove = getGeminiMoves(client);
                //System.out.printf(bestMove);
                String[] m = bestMove.split(" ");

                //Increase time limit after the first run
                if(firstRun){
                    timeLimit = 4_900_000_000L;
                    firstRun = false;
                }

                process_player_move(bestMove);
                //printALLTestsInfo();

                //Report the move to the referee
                System.out.printf("%s %s %s\n", m[0], m[1], m[2]);
                printBoard();
                System.out.printf(bestMove);
                System.out.flush();
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

//    /**
//     * Check for illegal moves made by a player
//     * @param move: the move to be checked
//     * @param player: the player making the move
//     * @return true if the move is legal, else false
//     */
//    public static boolean checkLegalMove(String move, int player) {
//        long startTime = System.nanoTime();
//        boolean answer = true;
//
//        String A = move.substring(0,2);
//        String B = move.substring(3,5);
//        String C = move.substring(6,8);
//
//        //System.out.println(curr_state.board.get(A));
//
//        String tempStoneType = "";
//
//        //Get the correct stone type for the check
//        if(player == 1) {
//            tempStoneType = oppStone;
//        } else {
//            tempStoneType = playerStone;
//        }
//
//        //Check that the string is in the correct format
//        if(!GameConstants.ADJACENT_MOVES.containsKey(A) && !A.equalsIgnoreCase("h1") && !A.equalsIgnoreCase("h2")) {
//            //Uncomment in the case printing should occur:
//            System.out.println("Incorrect move format, part A is incorrect");
//            answer = false;
//        } else if (!GameConstants.ADJACENT_MOVES.containsKey(B)) {
//            //Uncomment in the case printing should occur:
//            System.out.println("Incorrect move format, part B is incorrect");
//            answer = false;
//        } else if (!GameConstants.ADJACENT_MOVES.containsKey(C) && !C.equalsIgnoreCase("r0")) {
//            //Uncomment in the case printing should occur:
//            //System.out.println("Incorrect move format, part C is incorrect")
//            answer = false;
//        }
//
//        //Checks if a player attempts to place a piece out of the opponent's hand
//        String correctHand = (player == 1) ? oppHand : playerHand;
//        if((A.equalsIgnoreCase("h1") && playerHand == "h1" && player == 1) || (A.equalsIgnoreCase("h2") && playerHand == "h2" && player == 1) || (A.equalsIgnoreCase("h1") && oppHand == "h1" && player == 0) || (A.equalsIgnoreCase("h2") && oppHand == "h2" && player == 0)) {
//            //Uncomment in the case printing should occur:
//            System.out.println("Illegal Move Made, attempt to place a stone out of the other player's hand");
//            answer = false;
//        }
//
//        //Checks if a piece is attempted to be placed without having any in hand to place
//        if(curr_state.stoneHand[player] == 0 && A.contains("h")) {
//            //Uncomment in the case printing should occur:
//            System.out.println("Illegal Move Made, attempt to place a stone with out having one in hand");
//            answer = false;
//        }
//
//        //Check that when placing a piece there is not already one in that position
//        if(!curr_state.openSlots.contains(B)) {
//            //Uncomment in the case printing should occur:
//            System.out.println("Illegal Move Made, attempt to place a stone on a non-empty position");
//            answer = false;
//        }
//
//        //Check if a piece is attempted to be removed where a piece does not exist
//        if(curr_state.openSlots.contains(C)) {
//            //Uncomment in the case printing should occur:
//            System.out.println("Illegal Move Made, attempt to remove a piece where one does not exist");
//            answer = false;
//        }
//
//        boolean millFormed = curr_state.checkMoveMadeMillNoUpdate(move, tempStoneType);
//        //Check if a piece isn't removed when there is a mill made
//        if(millFormed && C.equalsIgnoreCase("r0")) {
//            //Uncomment in the case printing should occur:
//            System.out.println("Illegal Move Made, didn't remove piece when mill was made");
//            answer = false;
//        }
//
//        //Check if a piece is removed when there isn't a mill made
//        if(!millFormed && !C.equalsIgnoreCase("r0")) {
//            //Uncomment in the case printing should occur:
//            System.out.println("Illegal Move Made, tried removing a piece when a mill wasn't made");
//            answer = false;
//        }
//
//        if(!A.contains("h")) {
//            //Checks if a player attempts to move a piece that isn't theirs
//            if((curr_state.board.get(A).equalsIgnoreCase(oppStone) && player == 0) || (curr_state.board.get(A).equalsIgnoreCase(playerStone) && player == 1)) {
//                //Uncomment in the case printing should occur:
//                System.out.println("Illegal Move Made, attempt to move a stone that isn't owned by the player");
//                answer = false;
//            }
//        }
//
//        //Check that if the player is in phase 2 that they do not "fly"
//        if(curr_state.stoneHand[player] == 0 && curr_state.stonePlaced[player] > 3) {
//            boolean isValid = false;
//            //Check that B is a possible adjacent move of B
//            for(String possibleMove : GameConstants.ADJACENT_MOVES.get(A)) {
//                if(possibleMove.equalsIgnoreCase(B)) {
//                    isValid = true;
//                }
//            }
//
//            if(!isValid) {
//                //Uncomment in the case printing should occur:
//                System.out.println("Illegal Move Made, moved not to an adjacent location");
//                answer = false;
//            }
//        }
//        System.out.println("Time to validate opp's move: " + (System.nanoTime() - startTime)/1_000_000L);
//        return answer;
//    }

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

        try (FileInputStream fis = new FileInputStream("./Gemini/config.env")) {
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
        String prompt = """
                You are tasked with finding the next best moves for a Lasker Morris game, according to a specific board configuration.
                Your stone is represented by:
                """ + playerStone + """
                your hand is represented by
                """ + playerHand + """
                
                The rule of the game is of follow:
                    The rules of Lasker Morris, you must win while following legal moves. Look as many moves ahead as possible.
                    Please understand the rules of Lasker Morris and follow them
                    You cannot place pieces ontop of where one already is
                    Take your time computing, up to a minute, and consider as many steps ahead as possible
                    While you have stones in hand they must be placed before you can move pieces
                    Pieces cannot be placed ontop of each other
                    You can only place pieces on allowed coordinates, as listed below in the current board state
                    Imagine the board as a structure as follows:
                          a7 ---------------- d7 ----------------- g7
                          |                  |                     |
                          |      b6 --------- d6 --------- f6      |
                          |     |            |            |        |
                          |     |      c5 --- d5 --- e5   |        |
                          |     |      |           |      |        |
                          a4 --- b4 --- c4            e4 --- f4 --- g4
                          |     |      |           |      |        |
                          |     |      c3 --- d3 --- e3   |        |
                          |     |            |            |        |
                          |      b2 --------- d2 --------- f2      |
                          |                  |                     |
                          a1 ---------------- d1 ----------------- g1
                    A combination of letter denoting the column and a number denoting the row are used to mark positions
                    The ONLY valid positions are one appearing on the above chart
                    The state of each position is shown below:
                
                The current board right now is (format: location = type of stone):
                """ + "a1 = " + curr_state.board.get("a1") + """
                """ + "d1 = " + curr_state.board.get("d1") + """
                """ + "g1 = " + curr_state.board.get("g1") + """
                """ + "b2 = " + curr_state.board.get("b2") + """
                """ + "d2 = " + curr_state.board.get("d2") + """
                """ + "f2 = " + curr_state.board.get("f2") + """
                """ + "c3 = " + curr_state.board.get("c3") + """
                """ + "d3 = " + curr_state.board.get("d3") + """
                """ + "e3 = " + curr_state.board.get("e3") + """
                """ + "e4 = " + curr_state.board.get("e4") + """
                """ + "f4 = " + curr_state.board.get("f4") + """
                """ + "g4 = " + curr_state.board.get("g4") + """
                """ + "a4 = " + curr_state.board.get("a4") + """
                """ + "b4 = " + curr_state.board.get("b4") + """
                """ + "c4 = " + curr_state.board.get("c4") + """
                """ + "c5 = " + curr_state.board.get("c5") + """
                """ + "d5 = " + curr_state.board.get("d5") + """
                """ + "e5 = " + curr_state.board.get("e5") + """
                """ + "f6 = " + curr_state.board.get("f6") + """
                """ + "d6 = " + curr_state.board.get("d6") + """
                """ + "b6 = " + curr_state.board.get("b6") + """
                """ + "g7 = " + curr_state.board.get("g7") + """
                """ + "d7 = " + curr_state.board.get("d7") + """
                """ + "a7 = " + curr_state.board.get("a7") + """
                If the location is equal to empty, that means there is no stone at that specific location.
                
                You must give the next best move to be made that will ultimately lead you to a win.
                You must only answer in the format of "<x1> <y1> <z1>", where x1, y1, z1, are the locations as described in the game rules.
                
                To place a stone from your hand onto the board use the format:
                
                <your hand> <placement location> r0
                
                Pieces cannot be placed ontop of each other
                
                Say nothing except for the 3 characters representing your move
                """;

        CompletableFuture<GenerateContentResponse> responseFuture =
                client.async.models.generateContent(
                        "gemini-2.0-flash-001", prompt, null);

        CompletableFuture<String> resFuture = new CompletableFuture<>();
        responseFuture
                .thenAccept(
                        response -> {
                            resFuture.complete(response.text());
                        })
                .join();

        return resFuture.join();
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