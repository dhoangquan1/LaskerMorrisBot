import java.util.*;
import java.util.concurrent.AbstractExecutorService;

public class Player {
    public static State curr_state = new State();
    public static String playerStone = "";
    public static String oppStone = "";
    public static String playerHand = "";
    public static String oppHand = "";
    public static String stoneType = "";

    public static int tempUtil = 0;
    public static long timeLimit = 2_200_000_000L;

    public static void main(String[] args) {
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
                State bestMove = IterativeDeepening();
                String m1 = bestMove.moveSet[0];
                String m2 = bestMove.moveSet[1];
                String m3 = bestMove.moveSet[2];

                curr_state = bestMove;
                //printALLTestsInfo();

                //Report the move to the referee
                System.out.printf("%s %s %s\n", m1,m2,m3);
                System.out.flush();

            }
        }
    }

    public static boolean checkLegalMove(String move, int player) {
        boolean answer = true;

        String A = move.substring(0,2);
        String B = move.substring(3,5);
        String C = move.substring(6, 8);

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
            //System.out.println("Incorrect move format, part A is incorrect")
            answer = false;
        } else if (!GameConstants.ADJACENT_MOVES.containsKey(B)) {
            //Uncomment in the case printing should occur:
            //System.out.println("Incorrect move format, part B is incorrect")
            answer = false;
        } else if (!GameConstants.ADJACENT_MOVES.containsKey(C) && !C.equalsIgnoreCase("r0")) {
            //Uncomment in the case printing should occur:
            //System.out.println("Incorrect move format, part C is incorrect")
            answer = false;
        }

        //Checks if a player attempts to move a piece that isn't theirs
        if((curr_state.board.get(A) == oppStone && player == 0) || (curr_state.board.get(A) == playerStone && player == 1)) {
            //Uncomment in the case printing should occur:
            //System.out.println("Illegal Move Made, attempt to move a stone that isn't owned by the player")
            answer = false;
        }

        //Checks if a player attempts to place a piece out of the opponent's hand
        if((A.equalsIgnoreCase("h1") && playerHand == "h1" && player == 1) || (A.equalsIgnoreCase("h2") && playerHand == "h2" && player == 1) || (A.equalsIgnoreCase("h1") && oppHand == "h1" && player == 0) || (A.equalsIgnoreCase("h2") && oppHand == "h2" && player == 0)) {
            //Uncomment in the case printing should occur:
            //System.out.println("Illegal Move Made, attempt to place a stone out of the other player's hand")
            answer = false;
        }

        //Checks if a piece is attempted to be moved before all pieces are placed
        if(curr_state.stoneHand[player] != 0 && !A.contains("h")) {
            //Uncomment in the case printing should occur:
            //System.out.println("Illegal Move Made, attempted to move a piece when stones were still in hand")
            answer = false;
        }

        //Checks if a piece is attempted to be placed with out having any in hand to place
        if(curr_state.stoneHand[player] == 0 && A.contains("h")) {
            //Uncomment in the case printing should occur:
            //System.out.println("Illegal Move Made, attempt to place a stone with out having one in hand")
            answer = false;
        }

        //Check that when placing a piece there is not already one in that position
        if(!curr_state.openSlots.contains(B)) {
            //Uncomment in the case printing should occur:
            //System.out.println("Illegal Move Made, attempt to place a stone on a non-empty position")
            answer = false;
        }

        //Check if a piece is attempted to be removed where a piece does not exist
        if(curr_state.openSlots.contains(C)) {
            //Uncomment in the case printing should occur:
            //System.out.println("Illegal Move Made, attempt to remove a piece where one does not exist")
            answer = false;
        }

        //Check if a piece isn't removed when there is a mill made
        if(curr_state.checkMoveMadeMillNoUpdate(move, tempStoneType) && C.equalsIgnoreCase("r0")) {
            //Uncomment in the case printing should occur:
            //System.out.println("Illegal Move Made, didn't remove piece when mill was made")
            answer = false;
        }

        //Check if a piece is removed when there isn't a mill made
        if(!curr_state.checkMoveMadeMillNoUpdate(move, tempStoneType) && !C.equalsIgnoreCase("r0")) {
            //Uncomment in the case printing should occur:
            //System.out.println("Illegal Move Made, tried removing a piece when a mill wasn't made")
            answer = false;
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
            //System.out.println("Illegal Move Made, moved not to an adjacent location")
            answer = false;
            }
        }

        return answer;
    }

    public static State IterativeDeepening() {
        long startTime = System.nanoTime();
        int depth = 1;
        State bestMove = null;

        while (System.nanoTime() - startTime < timeLimit) {
            State newMove = MinimaxDecision(startTime, depth);
            if(newMove != null && System.nanoTime() - startTime < timeLimit){
                bestMove = newMove;
            }
            depth++;
        }
        return bestMove;
    }

    /**
     * MinimaxDecision is the minimax algorithm with pruning
     * This function decides the next best move
     * @return the String of the next best move
     */
    public static State MinimaxDecision(long startTime, int depth){
        int bestUtil = Integer.MIN_VALUE;
        State bestMove = null;
        int a = Integer.MIN_VALUE;
        int b = Integer.MAX_VALUE;

        for(State tempS: getSuccessors(curr_state, 0)){
            int nextMoveUtil = MinValue(tempS, a, b, depth-1, startTime);
            if(nextMoveUtil > bestUtil){
                bestUtil = nextMoveUtil;
                bestMove = tempS;
            }
        }
        return bestMove;
    }

    /**
     * MaxValue is a recursive function that maximize the utility for the player
     * @param state the state configuration
     * @param a alpha value
     * @param b beta value
     * @param depth the current depth of the state (for utility check)
     * @return the maximized utility value of this state
     */
    public static int MaxValue(State state, int a, int b, int depth, long startTime){
        if (System.nanoTime() - startTime >= timeLimit) {
            return Integer.MIN_VALUE;
        }
        if(depth == 0){
            return checkUtility(state);
        }
        int util = Integer.MIN_VALUE;
        for (State s: getSuccessors(state, 0)){
            util = Math.max(util, MinValue(s, a, b, depth-1, startTime));
            if(util >= b){
                return util;
            }
            a = Math.max(a, util);
        }
        return util;
    }

    /**
     * MinValue is a recursive function that minimize the utility for the opponent
     * @param state the state configuration
     * @param a alpha value
     * @param b beta value
     * @param depth the current depth of the state (for utility check)
     * @return the minimized utility value of this state
     */
    public static int MinValue(State state, int a, int b, int depth, long startTime){
        if (System.nanoTime() - startTime >= timeLimit) {
            return Integer.MAX_VALUE;
        }
        if(depth == 0){
            return checkUtility(state);
        }
        int util = Integer.MAX_VALUE;
        for (State s: getSuccessors(state, 1)){
            util = Math.min(util, MaxValue(s, a, b, depth-1, startTime));
            if(util <= a){
                return util;
            }
            b = Math.min(b, util);
        }
        return util;
    }

    public static void process_opponent_move(String move) {
        String[] new_moves = move.split(" ");

        //Check the move is legal
        boolean isLegal = checkLegalMove(move, 1);
        if(!isLegal) {
            System.out.println("Invalid Move Made, Game is Invalid from this point on");
        }

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

    //____________________________________________________________________
    //                    HEURISTICS UTILITY RELATED CODES
    //____________________________________________________________________
    public static int checkUtility(State state){
        int eval = 0;
        if(state.phase == 1){
            eval += checkUtil_ClosedMills(state) * 18;
            eval += checkUtil_MillsCount(state) * 26;
            eval += checkUtil_BlockedPieces(state);
            eval += checkUtil_PiecesLeft(state) * 9;
            eval += checkUtil_PiecesConfig(state);
            eval += checkUtil_DoubleMillsCount(state) * 8;
            eval += checkUtil_WinGame(state) * 1050;
        }else if(state.phase == 2){
            eval += checkUtil_ClosedMills(state) * 14;
            eval += checkUtil_MillsCount(state) * 43;
            eval += checkUtil_BlockedPieces(state) * 10;
            eval += checkUtil_PiecesLeft(state) * 11;
            eval += checkUtil_DoubleMillsCount(state) * 8;
            eval += checkUtil_WinGame(state) * 1086;
        }else {
            eval += checkUtil_ClosedMills(state) * 16;
            eval += checkUtil_PiecesConfig(state);
            eval += checkUtil_WinGame(state) * 1190;
        }
        return eval;
    }

    //Heuristic 1: If a mill is last closed by a player, and a stone is captured
    public static int checkUtil_ClosedMills(State state){
        String taker = "";
        if(!state.moveSet[2].equals("r0")){
            taker = state.board.get(state.moveSet[1]);
        }
        if(taker.equals(playerStone)){
            return 1;
        }else if(taker.equals(oppStone)){
            return -1;
        }
        return 0;
    }

    //Heuristic 2: The difference in the number of mills
    public static int checkUtil_MillsCount(State state){
        int playerMillCount = state.playerMill.size();
        int oppMillCount = state.oppMill.size();
        return (playerMillCount - oppMillCount);
    }

    //Heuristic 3: The difference in blocked pieces
    public static int checkUtil_BlockedPieces(State state){
        int playerBlocked = 0;
        int oppBlocked = 0;
        for(String move: GameConstants.ADJACENT_MOVES.keySet()){
            if(!state.board.get(move).equals("")){
                String[] moveSet = GameConstants.ADJACENT_MOVES.get(move);
                boolean blocked = true;
                for(String neighbor: moveSet){
                    if (state.board.get(neighbor).equals("")){
                        blocked = false;
                        break;
                    }
                }
                if(blocked){
                    if(move.equals(playerStone)){
                        playerBlocked++;
                    }else if(move.equals(oppStone)){
                        oppBlocked++;
                    }
                }
            }
        }
        return (oppBlocked - playerBlocked);
    }

    //Heuristic 4: The difference in total pieces left
    public static int checkUtil_PiecesLeft(State state){
        int playerPieces = state.stoneHand[0] + state.stonePlaced[0];
        int oppPieces = state.stoneHand[1] + state.stonePlaced[1];
        return (playerPieces - oppPieces);
    }

    //Heuristic 5-6: Difference in two and three-piece config where 1 more piece is needed to form a mill
    public static int checkUtil_PiecesConfig(State state){
        int playerTwoPieceConfig = 0;
        int oppTwoPieceConfig = 0;

        int playerThreePieceConfig = 0;
        int oppThreePieceConfig = 0;

        HashMap<String, Integer> playerConfig = new HashMap<>();
        HashMap<String, Integer> oppConfig = new HashMap<>();

        for (List<String> c: GameConstants.MILL_CONDITIONS){
            int playerPiece = 0;
            int oppPiece = 0;
            String emptyPieceMove = "";
            for (String move: c){
                String piece = state.board.get(move);
                if (piece.equals(playerStone)){
                    playerPiece++;
                }else if(piece.equals(oppStone)){
                    oppPiece++;
                }else {
                    emptyPieceMove = move;
                }
            }
            if(oppPiece==2 && playerPiece==0){
                for(String move: c){
                    if(!move.equals(emptyPieceMove)){
                        oppConfig.merge(move, 1, Integer::sum);
                        if(oppConfig.get(move) == 2){
                            oppThreePieceConfig++;
                        }
                    }
                }
                oppTwoPieceConfig++;
            }
            if(playerPiece==2 && oppPiece==0){
                for(String move: c){
                    if(!move.equals(emptyPieceMove)){
                        playerConfig.merge(move, 1, Integer::sum);
                        if(playerConfig.get(move) == 2){
                            playerThreePieceConfig++;
                        }
                    }
                }
                playerTwoPieceConfig++;
            }
        }

        int twoMultiplier = 10;
        int threeMultipler = (state.phase == 1) ? 7 : 1;
        int twoPieceEval = (playerTwoPieceConfig - oppTwoPieceConfig) * twoMultiplier;
        int threePieceEval = (playerThreePieceConfig - oppThreePieceConfig) * threeMultipler;
        return twoPieceEval + threePieceEval;
    }

    //Heuristic 7: The difference in the number of mills that share a common piece
    public static int checkUtil_DoubleMillsCount(State state){
        HashMap<String, Integer> playerMillMap = new HashMap<>();
        HashMap<String, Integer> oppMillMap = new HashMap<>();

        int playerDMill = 0;
        int oppDMill = 0;

        for(List<String> c: state.playerMill){
            for(String move: c){
                playerMillMap.merge(move, 1, Integer::sum);
                if(playerMillMap.get(move) == 2){
                    playerDMill++;
                }
            }
        }

        for(List<String> c: state.oppMill){
            for(String move: c){
                oppMillMap.merge(move, 1, Integer::sum);
                if(oppMillMap.get(move) == 2){
                    oppDMill++;
                }
            }
        }

        return (playerDMill - oppDMill);
    }

    //Heuristic 8: The difference in the number of mills that share a common piece
    public static int checkUtil_WinGame(State state){
        int playerStoneLeft = state.stoneHand[0] + state.stonePlaced[0];
        int oppStoneLeft = state.stoneHand[1] + state.stonePlaced[1];
        if(playerStoneLeft < 3){
            return -1;
        }else if(oppStoneLeft < 3){
            return 1;
        }
        return 0;
    }

    //____________________________________________________________________
    //                    SUCCESSORS RELATED CODES
    //____________________________________________________________________

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

    public static void getSuccessors_HandtoBoard(State state, int playerType, ArrayList<State> successors){
        for(String move: state.openSlots){
            State tempS = new State(state);
            tempS.phase = 1;
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

    public static void getSuccessors_BoardtoBoard(State state, int playerType, ArrayList<State> successors){
        for(String move: state.board.keySet()) {
            if(state.board.get(move).equals(stoneType)){
                for(String neighbor: GameConstants.ADJACENT_MOVES.get(move)){
                    if(state.board.get(neighbor).equals("")){
                        State tempS = new State(state);
                        tempS.phase = 2;
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

    public static void getSuccessors_FlyingtoBoard(State state, int playerType, ArrayList<State> successors){
        for(String move: state.board.keySet()) {
            if(state.board.get(move).equals(stoneType)){
                for(String open: state.openSlots){
                    State tempS = new State(state);
                    tempS.phase = 3;
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

    public static void getSuccessors_captureStone(State state, int playerType, ArrayList<State> successors){
        String targetStoneType = (playerType == 0) ? oppStone : playerStone;
        ArrayList<List<String>> targetMill = (playerType == 0) ? state.oppMill : state.playerMill;
        int targetType = (playerType == 0) ? 1 : 0;
        boolean allMills = true;

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
        int util = checkUtility(curr_state);
        System.out.println("Eval Util: " + util);
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