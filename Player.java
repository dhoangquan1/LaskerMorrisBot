import java.util.*;

public class Player {
    public static State curr_state = new State();
    public static String playerStone = "";
    public static String oppStone = "";
    public static String playerHand = "";
    public static String oppHand = "";
    public static String stoneType = "";

    public static long timeLimit = 4500;

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
                //Checking for illegal move
                if(checkIllegalMove(input)){
                    System.out.println("The opponent did an illegal move!");
                    break;
                }
                curr_state.board.put(input, oppStone);
            }

            //Game playing with minimax
            if(!input.startsWith("END")) {
                State bestMove = IterativeDeepening();

                //Report the move to the referee
                System.out.println(bestMove);
                System.out.flush();

                //After move, check for win and declare it
                String terminal = checkTerminal(curr_state);
                if(!terminal.equals("None")){
                    if(terminal.equals("Tie")){
                        System.out.println("The game ended in a tie.");
                    }else {
                        System.out.printf("Player %s has won the game!\n", terminal);
                    }
                    break;
                }
            }
        }
    }

    public static State IterativeDeepening() {
        long startTime = System.currentTimeMillis();
        int depth = 1;
        State bestMove = null;

        while (System.currentTimeMillis() - startTime < timeLimit) {
            bestMove = MinimaxDecision(curr_state, startTime, depth);
            depth++;
        }

        return bestMove;
    }

    /**
     * MinimaxDecision is the minimax algorithm with pruning
     * This function decides the next best move
     * @param state the current state configuration
     * @return the String of the next best move
     */
    public static State MinimaxDecision(State state, long startTime, int depth){
        int bestUtil = Integer.MIN_VALUE;
        State bestMove = null;
        int a = Integer.MIN_VALUE;
        int b = Integer.MAX_VALUE;

        for(State tempS: getSuccessors(curr_state, 0)){
            int nextMoveUtil = MinValue(tempS, a, b, depth, startTime);
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
        if (System.currentTimeMillis() - startTime < timeLimit || depth == 0){
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
        if (System.currentTimeMillis() - startTime < timeLimit || depth == 0){
            return checkUtility(state);
        }
        int util = Integer.MAX_VALUE;
        for (State s: getSuccessors(state, 1)){
            util = Math.min(util, MaxValue(s, a, b, depth, startTime));
            if(util <= a){
                return util;
            }
            b = Math.min(b, util);
        }
        return util;
    }

    public static int checkUtility(State state){
        return 0;
    }

    public static String checkGameOver(State state){
        return "None";
    }


    public static String checkTerminal(State state){
        return "";
    }

    /**
     * checkIllegalMove check if the move by opponent is illegal
     * @param move the move that was played
     * @return true if the move is illegal
     */
    public static boolean checkIllegalMove(String move){
        return false;
    }


    //____________________________________________________________________
    //                    SUCCESSORS RELATED CODES
    //____________________________________________________________________

    public static ArrayList<State> getSuccessors(State state, int playerType){
        ArrayList<State> successors = new ArrayList<>();
        stoneType = (playerType == 0) ? playerStone : oppStone;

        //If there is stone left in hand
        if(state.stoneHand[playerType] > 0){
            getSuccessors_HandtoBoard(state, playerType, successors);
        }
        //If there is stone left in board
        if (state.stonePlaced[playerType] > 0){
            getSuccessors_BoardtoBoard(state, playerType, successors);
        }
        //If there is 3 stone left total
        if (state.stoneHand[playerType] + state.stonePlaced[playerType] <= 3){
            getSuccessors_FlyingtoBoard(state, playerType, successors);
        }
        return successors;
    }

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
            }
        }
    }

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
                        if(tempS.checkMoveMadeMill(move, stoneType)){
                            getSuccessors_captureStone(tempS, playerType, successors);
                        }else {
                            successors.add(tempS);
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
                    tempS.board.put(move, "");
                    tempS.board.put(open, stoneType);
                    tempS.openSlots.add(move);
                    tempS.openSlots.remove(open);
                    tempS.moveSet[0] = move;
                    tempS.moveSet[1] = open;
                    if(tempS.checkMoveMadeMill(move, stoneType)){
                        getSuccessors_captureStone(tempS, playerType, successors);
                    }else {
                        successors.add(tempS);
                    }
                }
            }
        }
    }

    public static void getSuccessors_captureStone(State state, int playerType, ArrayList<State> successors){
        String targetStoneType = (stoneType.equals(playerStone)) ? oppStone : playerStone;
        ArrayList<List<String>> targetMill = (stoneType.equals(playerStone)) ? state.oppMill : state.playerMill;
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
                    for(List<String> mill: tempSMill) {
                        if(mill.contains(move)){
                            tempSMill.remove(mill);
                        }
                    }
                    successors.add(tempS);
                }
            }

        }

    }

}