import java.util.*;

/**
 * The State class help store the constructor for the board and also can be scaled to the next project
 * It is also easier to declare state than HashMap in the main code because it hides complications
 */
public class State {
    HashMap<String, String> board;

    //Index 0 is for Player, Index 1 is for Opponent
    public int[] stoneHand= new int[2];
    public int[] stonePlaced= new int[2];
    public ArrayList<List<String>> oppMill = new ArrayList<>();
    public ArrayList<List<String>> playerMill = new ArrayList<>();
    public ArrayList<String> openSlots = new ArrayList<>();

    //Storing the move that last made to this board
    public String[] moveSet = new String[3];

    public State(){
        this.board = new HashMap<>();

        board.put("a7", ""); board.put("d7", ""); board.put("g7", "");
        board.put("b6", ""); board.put("d6", ""); board.put("f6", "");
        board.put("c5", ""); board.put("d5", ""); board.put("e5", "");
        board.put("a4", ""); board.put("b4", ""); board.put("c4", ""); board.put("e4", ""); board.put("f4", ""); board.put("g4", "");
        board.put("c3", ""); board.put("d3", ""); board.put("e3", "");
        board.put("b2", ""); board.put("d2", ""); board.put("f2", "");
        board.put("a1", ""); board.put("d1", ""); board.put("g1", "");

        openSlots.add("a7"); openSlots.add("d7"); openSlots.add("g7");
        openSlots.add("b6"); openSlots.add("d6"); openSlots.add("f6");
        openSlots.add("c5"); openSlots.add("d5"); openSlots.add("e5");
        openSlots.add("a4"); openSlots.add("b4"); openSlots.add("c4"); openSlots.add("e4"); openSlots.add("f4"); openSlots.add("g4");
        openSlots.add("c3"); openSlots.add("d3"); openSlots.add("e3");
        openSlots.add("b2"); openSlots.add("d2"); openSlots.add("f2");
        openSlots.add("a1"); openSlots.add("d1"); openSlots.add("g1");

        stoneHand[0] = 10;
        stoneHand[1] = 10;
    }

    //This allows copying board configuration for children without linking the parent
    public State(HashMap<String, String> board, int[] stoneHand, int[] stonePlaced, ArrayList<List<String>> playerMill, ArrayList<List<String>> oppMill, ArrayList<String> openSlots, String[] moveSet){
        this.board = new HashMap<>(board);
        this.stoneHand = stoneHand.clone();
        this.stonePlaced = stonePlaced.clone();
        this.playerMill = new ArrayList<>(playerMill);
        this.oppMill = new ArrayList<>(oppMill);
        this.openSlots = new ArrayList<>(openSlots);

        this.moveSet[0] = moveSet[0];
        this.moveSet[1] = moveSet[1];
        this.moveSet[2] = moveSet[2];
    }

    public State(State state){
        this(state.board, state.stoneHand, state.stonePlaced, state.playerMill, state.oppMill, state.openSlots, state.moveSet);
    }

    /**
     * This check if the recent move formed a mill
     * @param move : the move that was made
     * @param stoneType : the stone type of that move
     * @return true if the move formed a mill
     */
    public Boolean checkMoveMadeMill(String move, String stoneType){
        for (List<String> c: GameConstants.MILL_CONDITIONS){
            if(c.contains(move)){
                if(this.board.get(c.get(0)).equals(stoneType)){
                    String m1 = this.board.get(c.get(0));
                    String m2 = this.board.get(c.get(1));
                    String m3 = this.board.get(c.get(2));
                    if (m1.equals(m2) && m2.equals(m3)){
                        if(stoneType.equals(Player.playerStone)){
                            this.playerMill.add(c);
                        }else{
                            this.oppMill.add(c);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * This check if the recent move formed a mill without updating the state
     * For the purpose of checking moves are legal
     * @param move : the move that was made
     * @param stoneType : the stone type of that move
     * @return true if the move formed a mill
     */
    public Boolean checkMoveMadeMillNoUpdate(String move, String stoneType){
        for (List<String> c: GameConstants.MILL_CONDITIONS){
            if(c.contains(move)){
                if(this.board.get(c.get(0)).equals(stoneType)){
                    String m1 = this.board.get(c.get(0));
                    String m2 = this.board.get(c.get(1));
                    String m3 = this.board.get(c.get(2));
                    if (m1.equals(m2) && m2.equals(m3)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

}