Philip 124040015

Gomuku Game

Features:
1. Preliminary Gomoku Game Rules:
   - The game is played on a 20x20 board.
   - Two players take turns to place a stone on an empty intersection.
   - Black plays first, and the players alternate in placing stones on an empty intersection, i.e. those intersections with a stone on it cannot be placed again.
   - The winner is the first player to get an unbroken row of five stones horizontally, vertically, or diagonally

2. Java GUI: 
   - The game board and the stones placed on it.
   - The current player that has the right to place a stone.
   - The winner and loser (black and while players) once the game is finished.
   - The game settings, including the number of movements and the maximum length of all unbroken row for each user.
   - The game menu with a reset button (i.e. the "start a new game" button) and an exit button.

3. Direct Mouse Control: 
   - The player can place a stone by clicking some intersection on the board. There should be some tolerance for the mouse click, i.e. the player can click on the intersection or the area around it.
   - A player can only place a stone on an empty intersection. If a player clicks on an intersection that already has a stone, the program should reject the action and display a warning message.


Advanced Features:
1. Undo and Redo: 
    - The player can undo the last move by clicking the undo button.
    - The player can redo the last move by clicking the redo button.
    - The player can undo and redo multiple moves.

2. Save and Load: 
   - The player can save the current game status by clicking the save button.
   - The player can load the game status by clicking the load button.
   - The game status should be saved in a file, and the player can choose the file to save or load. The checkpoint file design is up to you, but it'd better be readable rather than binary.

3. Time Limit: 
   - Each player now has a limited time for placing a stone, e.g. 30 seconds.
   - The player can place a stone within the limited time, otherwise he/she will lose the turn of placing a stone immediately, and it would be the other player's turn.

4. AI Player

5. User Friendly Interface: 
   - When the player hovers the mouse over the intersection, the intersection should be highlighted.
   -    When the player mistakenly places a stone on an intersection that has been placed before, there should be some flashing circle (an animation lasting for a short time) around the stone to indicate that it's invalid.
   -  Rendering the board and the stones on it with some animations, e.g. fading in and fading out.


-------- How To Run --------

Change Line 119:

            String path = "(Your Path)/Gomuku/src/resource/" ; //change accordingly with your path in your directory.

Compile in Terminal:

            javac --module-path (Your JavaFX Path)/javafx-sdk-24/lib \
                  --add-modules javafx.controls,javafx.fxml \
                  Main.java

Run in Terminal:

           java --module-path (Your JavaFX Path)/javafx-sdk-24/lib \
                --add-modules javafx.controls,javafx.fxml \
                Main


