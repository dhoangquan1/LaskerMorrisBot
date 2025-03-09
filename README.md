# Lasker Morris Bot
*This project is a demo for educational purposes as part of course*

Lasker Morris (also known as Ten Men's Morris) is a variant of Nine Men's Morris, where each player get ten pieces and their pieces can be moved any time in the first phase.

There are two different bots that we implemented: Gemini API Bot and PLAYER Bot
The **PLAYER Bot** uses a Minimax Algorithm with Alpha-Beta Pruning in order to seek the next best move within limitted time. To make sure the bot always have a respond by the end of the timer, we use Iterative Deepening algorithm to get the best moves in incrementing depths.

The **Gemini API Bot** uses prompt to get moves. This bot is a demo for using Large Language Model in Game-Playing, and it is not supposed to make optimal moves to win the game. The Bot makes a random move within a time limit if it is uncapable of generating a valid move set.

------------------------
## ⚙️ Technologies Used
-----------------------
[![My Skills](https://skillicons.dev/icons?i=java,maven)](https://skillicons.dev)

- Others: Google Gemini API 0.1.0

------------------------
## 🎯 Program I/O
-----------------------
### Color declaration:
- Tell the bot which pieces color it will be: **blue** or **orange**
- **Blue** goes first, and its hand will be represented as **"h1"**
- **Orange** goes after, and its hand will be represented as **"h2"**
    ```
    blue
    ```

### I/O format:
- Tell the bot which move set you will be making, or the bot will tell you what move it will make
- The move format is: "x1 y1 z1"
- **x1**: The location where the stone move from (hand or location of the board)
- **y1**: The location where the stone move to (location of the board)
- **z1**: The location of the stone to capture if mill formed (r0 if mill is not formed)
    ```
    h1 a7 r0
    ```

### Print Board:
- At the end of the program, there is functions to print the board
- The board is in a grid stucture
- The x-axis is a-g from left to right
- The y-axis is 1-7 from bottom to top
    ```
          a7 ---------------- d7 ---------------- g7
          |                   |                    |
          |      b6 --------- d6 --------- f6      |
          |     |             |              |     |
          |     |      c5 --- d5 --- e5      |     |
          |     |      |              |      |     |
          a4 --- b4 --- c4            e4 --- f4 --- g4
          |     |      |              |      |     |
          |     |      c3 --- d3 --- e3      |     |
          |     |             |              |     |
          |      b2 --------- d2 --------- f2      |
          |                   |                    |
          a1 ---------------- d1 ---------------- g1

    ```

### Interact with Referee (AutoPlay):
- The program can interact with the referee program, implemented by SA Jake Molnia in CS4341
- You can read more about the referee here:
[Lasker Morris Referee](https://github.com/jake-molnia/CS4341-referee)

------------------------
## 🧠 Algorithms
-----------------------
### PLAYER Bot:
- **Minimax**: is an algorithm that minimize the worst-case potential loss.
- **Alpha-Beta Pruning**: allows to "prune" out branches that is worse than the previously assessed branches, in order to save search time.
- **Utility Evaluation**: allows to evaluate a value of specific board configuration (whether if it is in favor of the player, and by how much).

### Gemini API Bot:
- **Prompting**: simple prompting and reprompt the Google Gemini API to return moves.

------------------------
## 🧮 Attributes
-----------------------
These are attributes that you can change in order to control the program/algorithm

### Program Attributes

| Name | Description | Value |
|------|-------------|---------|
| timeLimit |The time limit to run the program | 2s initially (4.9s after) |

### Heuristic Evaluation Attributes

| Name | Description | Value (Phase 1 - Phase 2 - Phase 3) |
|------|-------------|---------|
| ClosedMills | Utility if the mill was last closed by a player, and a stone is captured | 50-55-40 |
| MillsCount | Utility for difference in the number of mills | 40-70-0 |
| PiecesLeft | Utility for difference in the number of pieces left | 10-15-0 |
| BlockedPieces | Utility for difference in the number of blocked pieces | 3-10-0 |
| PiecesConfig | Utility for difference in configuration that only needs 1 stone to form a mill. (2-pieces config, 3-pieces config) | (10,7)-(0,0)-(10,1) |
| DoubleMillsCount | Utility for difference in the number of mills that share a common stone | 0-0-8 |
| WinGame | Utility for game-ending state | 5000-5000-5000 |

------------------------
## 🚀 Getting Started
-----------------------
### Requirements:
- Apache Maven 3.9.9
- Java 18+

### Set up dependencies (if running Gemini):
- Create a python environment:
    ```
    python -m venv venv
    ```
- Activate the python environment:
    ```
    .\venv\Scripts\activate
    ```
- Download dependencies:
    ```
    mvn clean install
    ```

------------------------
## 🖥️ Running the program
-----------------------
### To initialize the programs:
- Compile all Maven programs with the following command:
    ```
    mvn compile
    ```

### To run the Bots:
- Start PLAYER with the following command:
    ```
    Minimax.bat
    ```
- Start Gemini API with the following command:
    ```
    Gemini.bat
    ```

------------------------
## 📚 Documentations
-----------------------
For more information on the Bot design, check out the documentations in /documents to see heuristic explaination, program capabilties, and detailed instructions.

------------------------
## 🙏 Acknowledgements
-----------------------
- Professor Ruiz - CS4341: Introduction to AI
- Contributors/Team Members 
    <div style="display: flex; gap: 10px; margin-top: 10px;">
        <a href="https://github.com/dhoangquan1">
            <img src="https://github.com/dhoangquan1.png" width="50" style="border-radius: 25px; overflow: hidden;">
        </a>
        <a href="https://github.com/ElijahWPI">
            <img src="https://github.com/ElijahWPI.png" width="50" style="border-radius: 25px; overflow: hidden;">
        </a>
        <a href="https://github.com/jpisano05">
            <img src="https://github.com/jpisano05.png" width="50" style="border-radius: 25px; overflow: hidden;">
        </a>
    </div>