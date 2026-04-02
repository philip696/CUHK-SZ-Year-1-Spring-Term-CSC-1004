import java.util.*;
import javafx.application.*;
import javafx.animation.*;
import javafx.stage.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.util.Duration;
import java.io.*;

public class Main extends Application {
    //sizes
    private static final int BOARD_SIZE = 20;
    private static final int CELL_SIZE = 30;
    private static final int STONE_SIZE = 25;
    private static final int BOARD_PADDING = 20;


    private static class GameState implements Serializable {
        int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
        int[][] blackMoveNumbers = new int[BOARD_SIZE][BOARD_SIZE];
        int[][] whiteMoveNumbers = new int[BOARD_SIZE][BOARD_SIZE];
        boolean blackTurn = true;
        boolean gameOver = false;
        int blackMoveCount = 1;
        int whiteMoveCount = 1;
        int[] winningLine = null;
        int blackWins = 0;
        int whiteWins = 0;
        List<int[]> moveHistory = new ArrayList<>();
        int currentMoveIndex = -1;
        boolean vsAI = false;
        boolean aiIsWhite = true; // AI plays as white by default
    }

    private int[][] board;
    private int[][] blackMoveNumbers;
    private int[][] whiteMoveNumbers;
    private boolean blackTurn = true;
    private boolean gameOver = false;
    private int blackMoveCount = 1;
    private int whiteMoveCount = 1;
    private int[] winningLine;
    private int[] hoverPosition = {-1, -1};

    //game logic assets
    private int blackWins = 0;
    private int whiteWins = 0;
    private Label scoreLabel;
    private Canvas canvas;
    private Stage primaryStage;

    //history assets
    private List<int[]> moveHistory = new ArrayList<>();
    private int currentMoveIndex = -1;

    //image assets
    private Image gameOverImage;
    private Image restartButtonImage;
    private Image exitButtonImage;
    private Image welcomeImage;
    private Image startButtonImage;
    private Image blackWinImage;
    private Image whiteWinImage;
    private Image drawImage;
    private Image menuImage;
    private Image rewindButtonImage;
    private Image forwardButtonImage;
    private Image saveButtonImage;
    private Image loadButtonImage;
    private Image selectGameImage;
    private Image pvpImage;
    private Image WvsAiImage;
    private Image BvsAiImage;
    private Image applyImage;

    //save assets
    private final String SAVE_FOLDER = "gomoku_saves";
    private final int MAX_SAVES = 3;

    //timer assets
    private Timeline gameTimer;
    private int timeLimit = 30; // seconds
    private Label currentTimeLabel;
    private int currentPlayerTimeLeft;

    //ai assets
    private boolean vsAI = false;
    private boolean aiIsWhite = true;

    //animation assets
    private static final int INVALID_MOVE_DURATION = 500; // milliseconds
    private static final int INVALID_MOVE_FLASHES = 3;
    private long animationStartTime;
    private int[] lastInvalidPosition = {-1, -1};
    private Map<String, Double> stoneOpacities = new HashMap<>(); // Key: "x,y", Value: opacity (0.0 to 1.0)

    //launch program
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(true);
        this.primaryStage = primaryStage;

        //ensure 100% exit
        primaryStage.setOnCloseRequest(e -> {
            if (gameTimer != null) gameTimer.stop();
            Platform.exit();
        });

        String path = "/Users/philipdewanto/Downloads/Code/CSC 1004/Gomuku/src/resource/" ; //change accordingly with your directory.

        //get all the image assets from local
        try {
            gameOverImage = new Image(new FileInputStream(path + "game_over.png"));
            restartButtonImage = new Image(new FileInputStream(path + "restart.png"));
            exitButtonImage = new Image(new FileInputStream(path + "exit_game.png"));
            welcomeImage = new Image(new FileInputStream(path + "gomoku.png"));
            startButtonImage = new Image(new FileInputStream(path + "start.png"));
            blackWinImage = new Image(new FileInputStream(path + "black_w.png"));
            whiteWinImage = new Image(new FileInputStream(path + "white_w.png"));
            drawImage = new Image(new FileInputStream(path + "draw.png"));
            menuImage = new Image(new FileInputStream(path + "menu.png"));
            rewindButtonImage = new Image(new FileInputStream(path + "rewind.png"));
            forwardButtonImage = new Image(new FileInputStream(path + "forward.png"));
            saveButtonImage = new Image(new FileInputStream(path + "save.png"));
            loadButtonImage = new Image(new FileInputStream(path + "load.png"));
            selectGameImage = new Image(new FileInputStream(path + "select_game.png"));
            pvpImage = new Image(new FileInputStream(path + "pvp.png"));
            WvsAiImage = new Image(new FileInputStream(path + "WvsAi.png"));
            BvsAiImage = new Image(new FileInputStream(path + "BvsAi.png"));
            applyImage = new Image(new FileInputStream(path + "apply.png"));
        } catch (FileNotFoundException e) {
            System.err.println("Error loading image files: " + e.getMessage());
            System.exit(1);
        }

        //show welcome screen first
        showWelcomeScreen();

    }

    //insert image in button with certain width
    private Button createImageButton(Image image, double height) {
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(true);
        Button button = new Button();
        button.setGraphic(imageView);
        button.setStyle("-fx-background-color: transparent; -fx-padding: 5;");
        return button;
    }

    //timer timeout
    private void timeOut() {
        gameTimer.stop();
        System.out.println((blackTurn ? "Black" : "White") + " time out!");

        //switch turn
        blackTurn = !blackTurn;

        //timer for next player
        currentPlayerTimeLeft = timeLimit;
        currentTimeLabel.setText((blackTurn ? "Black: " : "White: ") + currentPlayerTimeLeft + "s");
        gameTimer.play();

        drawBoard(canvas.getGraphicsContext2D());
    }

    //setup board with size asset
    private void initializeBoard() {
        board = new int[BOARD_SIZE][BOARD_SIZE];
        blackMoveNumbers = new int[BOARD_SIZE][BOARD_SIZE];
        whiteMoveNumbers = new int[BOARD_SIZE][BOARD_SIZE];
        winningLine = null;

        // all cells empty
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                board[x][y] = 0;
                blackMoveNumbers[x][y] = 0;
                whiteMoveNumbers[x][y] = 0;
            }
        }
    }

    //welcome screen content
    private void showWelcomeScreen() {
        //setup stage
        Stage welcomeStage = new Stage();
        welcomeStage.initStyle(StageStyle.UNDECORATED);

        //welcome image
        ImageView welcomeView = new ImageView(welcomeImage);
        welcomeView.setFitWidth(400);
        welcomeView.setPreserveRatio(true);

        //start button
        Button startButton = createImageButton(startButtonImage, 30);
        startButton.setOnAction(e -> {
            welcomeStage.close();
            showAISelectionDialog();
        });

        //use vbox to stack image and button
        VBox layout = new VBox(20, welcomeView, startButton);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 30; -fx-background-color: #f8f8f8;");

        //set the scene so it can be shown
        Scene scene = new Scene(layout);
        welcomeStage.setScene(scene);
        welcomeStage.show();
    }

    private void showAISelectionDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Select Game Mode");

        VBox dialogVbox = new VBox(20);
        dialogVbox.setPadding(new Insets(20));

        ImageView selectView = new ImageView(selectGameImage);
        selectView.setFitHeight(40);
        selectView.setPreserveRatio(true);

        Button pvpButton = createImageButton(pvpImage, 25);
        pvpButton.setOnAction(e -> {
            vsAI = false;
            dialog.close();
            initializeGame(); // Call initializeGame instead of resetGame
        });

        Button PvAiButtonBlack = createImageButton(BvsAiImage, 25);
        PvAiButtonBlack.setOnAction(e -> {
            vsAI = true;
            aiIsWhite = true;
            dialog.close();
            initializeGame(); // Call initializeGame instead of resetGame
        });

        Button PvAiButtonWhite = createImageButton(WvsAiImage, 25);
        PvAiButtonWhite.setOnAction(e -> {
            vsAI = true;
            aiIsWhite = false;
            dialog.close();
            initializeGame(); // Call initializeGame instead of resetGame
        });

        dialogVbox.getChildren().addAll(selectView, pvpButton, PvAiButtonBlack, PvAiButtonWhite);
        Scene dialogScene = new Scene(dialogVbox, 750, 400);
        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }

    //setup game with assets
    private void initializeGame() {

        //error handling if board isn't empty
        if (board == null) {
            initializeBoard();
        } else {
            resetGame();
        }

        //score
        scoreLabel = new Label();
        updateScore();
        scoreLabel.setFont(Font.font(16));

        //menu button
        Button menuButton = createImageButton(menuImage, 20);
        menuButton.setOnAction(e -> menuPopUp());

        //rewind button
        Button rewindButton = createImageButton(rewindButtonImage, 15);
        rewindButton.setOnAction(e -> rewindMove());

        //forward button
        Button forwardButton = createImageButton(forwardButtonImage, 15);
        forwardButton.setOnAction(e -> forwardMove());

        //switching time limit based on turn
        currentPlayerTimeLeft = timeLimit;
        currentTimeLabel = new Label((blackTurn ? "Black: " : "White: ") + currentPlayerTimeLeft + "s");
        currentTimeLabel.setFont(Font.font(16));
        gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> { //update every second
            currentPlayerTimeLeft--;
            currentTimeLabel.setText((blackTurn ? "Black: " : "White: ") + currentPlayerTimeLeft + "s"); //display label
            if (currentPlayerTimeLeft <= 0) {
                timeOut();
            }
        }));

        gameTimer.setCycleCount(Timeline.INDEFINITE); //run forever to accomodate the indefinete game time

        //start the timer if it's not running
        if (!gameTimer.getStatus().equals(Timeline.Status.RUNNING)) {
            gameTimer.play();
        }

        //start AI move if its AIs turn first
        if (vsAI && !aiIsWhite) { // If AI is playing black
            Platform.runLater(() ->
                    new Timeline(new KeyFrame(Duration.seconds(0.5),
                            ignored -> makeAIMove())).play());
        }

        //use hbox to stack score, menu button, rewind button, forward button, time horizontally
        HBox topPanel = new HBox(20, scoreLabel, menuButton, rewindButton, forwardButton, currentTimeLabel);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setStyle("-fx-padding: 10;");

        if (canvas == null) { //failsave if canvas doesnt exist
            canvas = new Canvas(
                    (BOARD_SIZE - 1) * CELL_SIZE + 2 * BOARD_PADDING,
                    (BOARD_SIZE - 1) * CELL_SIZE + 2 * BOARD_PADDING
            );
        }

        //stack top panel and canvas vertically
        Pane boardContainer = new Pane(canvas);
        boardContainer.setPadding(new Insets(BOARD_PADDING));
        Pane root = new VBox(10, topPanel, canvas);
        root.setStyle("-fx-background-color: #f0f0f0;");

        //setup css
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawBoard(gc);
        setupMouseHandlers(gc);

        //show the game windows with title
        primaryStage.setTitle("Gomoku with Score Tracking");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();

        //animation for board to fade in
        FadeTransition boardFadeIn = new FadeTransition(Duration.seconds(0.5), canvas);
        boardFadeIn.setFromValue(0.0);
        boardFadeIn.setToValue(1.0);
        boardFadeIn.play();


    }

    //setup board css & animation
    private void drawBoard(GraphicsContext gc) {
        //initial setup
        gc.clearRect(0, 0,
                BOARD_SIZE * CELL_SIZE + 2 * BOARD_PADDING,
                BOARD_SIZE * CELL_SIZE + 2 * BOARD_PADDING + (BOARD_SIZE - 1) * CELL_SIZE
        );

        //fill bg color
        gc.setFill(Color.BURLYWOOD);
        gc.fillRect(0, 0,
                BOARD_SIZE * CELL_SIZE + 2 * BOARD_PADDING,
                BOARD_SIZE * CELL_SIZE + 2 * BOARD_PADDING
        );

        //draw the boxes
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0);
        for (int i = 0; i < BOARD_SIZE; i++) {
            gc.strokeLine(
                    BOARD_PADDING + i * CELL_SIZE,
                    BOARD_PADDING,
                    BOARD_PADDING + i * CELL_SIZE,
                    BOARD_PADDING + (BOARD_SIZE - 1) * CELL_SIZE
            );
            gc.strokeLine(
                    BOARD_PADDING,
                    BOARD_PADDING + i * CELL_SIZE,
                    BOARD_PADDING + (BOARD_SIZE - 1) * CELL_SIZE,
                    BOARD_PADDING + i * CELL_SIZE
            );
        }

        //handle the stone drawing logic
        gc.setFont(Font.font(10));
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                if (board[x][y] == 1) {
                    drawStone(gc, x, y, Color.BLACK, Color.WHITE, blackMoveNumbers[x][y]);
                } else if (board[x][y] == 2) {
                    drawStone(gc, x, y, Color.WHITE, Color.BLACK, whiteMoveNumbers[x][y]);
                }
            }
        }

        //hovering animation logic, get data from setupmousehandlers
        if (!gameOver && hoverPosition[0] >= 0 && board[hoverPosition[0]][hoverPosition[1]] == 0) {
            gc.setFill(blackTurn ? Color.color(0, 0, 0, 0.3) : Color.color(1, 1, 1, 0.3));
            gc.fillOval(
                    BOARD_PADDING + hoverPosition[0] * CELL_SIZE - STONE_SIZE / 2,
                    BOARD_PADDING + hoverPosition[1] * CELL_SIZE - STONE_SIZE / 2,
                    STONE_SIZE, STONE_SIZE
            );
        }

        if (gameOver && winningLine != null) {
            //pulsing effect for winning line
            double pulsePhase = (System.currentTimeMillis() % 1000) / 1000.0;
            double lineWidth = 3 + 2 * Math.sin(pulsePhase * Math.PI * 2); // 3px to 5px

            gc.setStroke(Color.RED);
            gc.setLineWidth(lineWidth);
            gc.strokeLine(
                    BOARD_PADDING + winningLine[0] * CELL_SIZE,
                    BOARD_PADDING + winningLine[1] * CELL_SIZE,
                    BOARD_PADDING + winningLine[2] * CELL_SIZE,
                    BOARD_PADDING + winningLine[3] * CELL_SIZE
            );

            //redraw to keep animation smooth
            Platform.runLater(() -> drawBoard(gc));
        }
    }

    //stone css & animation
    private void drawStone(GraphicsContext gc, int x, int y, Color stoneColor, Color textColor, int moveNumber) {
        String key = x + "," + y;

        //initialize opacity if not present
        if (!stoneOpacities.containsKey(key)) {
            stoneOpacities.put(key, 0.0);
            //start fade animation
            new AnimationTimer() {
                private long startTime = -1;

                @Override
                public void handle(long now) {
                    if (startTime < 0) startTime = now;

                    long elapsed = now - startTime;
                    double progress = Math.min(1.0, elapsed / 300_000_000.0); // 300ms animation
                    stoneOpacities.put(key, progress);

                    // Redraw the board to reflect changes
                    drawBoard(gc);

                    if (progress >= 1.0) {
                        stop();
                    }
                }
            }.start();
        }

        double opacity = stoneOpacities.get(key);
        gc.setGlobalAlpha(opacity);

        //draw stone
        gc.setFill(stoneColor);
        gc.fillOval(
                BOARD_PADDING + x * CELL_SIZE - STONE_SIZE / 2,
                BOARD_PADDING + y * CELL_SIZE - STONE_SIZE / 2,
                STONE_SIZE, STONE_SIZE
        );

        //black outline for white stone
        if (stoneColor.equals(Color.WHITE)) {
            gc.setStroke(Color.BLACK);
            gc.strokeOval(
                    BOARD_PADDING + x * CELL_SIZE - STONE_SIZE / 2,
                    BOARD_PADDING + y * CELL_SIZE - STONE_SIZE / 2,
                    STONE_SIZE, STONE_SIZE
            );
        }

        //draw number of the move
        gc.setFill(textColor);
        String text = String.valueOf(moveNumber);
        double textWidth = gc.getFont().getSize() * text.length() * 0.6;
        gc.fillText(
                text,
                BOARD_PADDING + x * CELL_SIZE - textWidth / 2,
                BOARD_PADDING + y * CELL_SIZE + 5
        );

        gc.setGlobalAlpha(1.0); // Reset for other elements
    }

    //game & mouse logic
    private void setupMouseHandlers(GraphicsContext gc) {
        //gets data whenever mose is moved
        canvas.setOnMouseMoved(e -> {
            if (gameOver) return;

            int[] gridPos = getNearestGridPosition(e.getX(), e.getY());//get data from function & click
            if (gridPos[0] != hoverPosition[0] || gridPos[1] != hoverPosition[1]) {
                hoverPosition = gridPos;
                drawBoard(gc); //get mouse position and feed data into drawboard
            }
        });

        //gets data whenever mose is clicked
        canvas.setOnMouseClicked(e -> {
            if (gameOver) return;
            int[] gridPos = getNearestGridPosition(e.getX(), e.getY()); //get data from function & click
            int x = gridPos[0];
            int y = gridPos[1];

            if (x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE && board[x][y] == 0) { //determine whos turn
                board[x][y] = blackTurn ? 1 : 2;

                addToMoveHistory(x, y, board[x][y]); //store to history

                if (blackTurn) { //add the move count
                    blackMoveNumbers[x][y] = blackMoveCount++;
                } else {
                    whiteMoveNumbers[x][y] = whiteMoveCount++;
                }

                if (checkWin(x, y)) { //check win & update variables
                    gameOver = true;
                    if (blackTurn) blackWins++;
                    else whiteWins++;
                    updateScore();
                    showWinPopup(blackTurn ? "Black" : "White");
                } else if (isBoardFull()) { //draw if board full
                    gameOver = true;
                    showWinPopup("Draw");
                }
                blackTurn = !blackTurn; //change turns after every click
                    currentPlayerTimeLeft = timeLimit;
                    currentTimeLabel.setText((blackTurn ? "Black: " : "White: ") + currentPlayerTimeLeft + "s"); //reset time every turn
                drawBoard(gc);

                if (!gameOver && vsAI) { //player vs AI, call function after 0.2 seconds after click
                    Platform.runLater(() ->
                            new Timeline(new KeyFrame(Duration.seconds(0.2),
                                    ignored -> makeAIMove())).play());
                }
            } else {
                //invalid move animation
                lastInvalidPosition = new int[]{x, y};
                animationStartTime = System.nanoTime();
                invalidMoveAnimation.start();
            }
        });
    }

    //whenever a move is done, record it in list
    private void addToMoveHistory(int x, int y, int player) {
        //remove any moves after current position if were not at the end
        if (currentMoveIndex < moveHistory.size() - 1) {
            moveHistory = moveHistory.subList(0, currentMoveIndex + 1);
        }

        //add the new move
        moveHistory.add(new int[]{x, y, player});
        currentMoveIndex = moveHistory.size() - 1;
    }

    //reset game logic
    private void resetGame() {
        initializeBoard();
        blackMoveCount = 1; //reset moves
        whiteMoveCount = 1;
        gameOver = false;
        blackTurn = true; // black always starts
        moveHistory = new ArrayList<>();
        currentMoveIndex = -1;
        winningLine = null;

        //reset the timer
        if (gameTimer != null) {
            gameTimer.stop();
            currentPlayerTimeLeft = timeLimit;
            currentTimeLabel.setText((blackTurn ? "Black: " : "White: ") + currentPlayerTimeLeft + "s");
        }

        // Restart the timer if the game isn't over
        if (!gameOver && gameTimer != null) {
            gameTimer.play();
        }

        //start AI move if its AIs turn first
        if (vsAI && !aiIsWhite) { // If AI is playing black
            Platform.runLater(() ->
                    new Timeline(new KeyFrame(Duration.seconds(0.5),
                            ignored -> makeAIMove())).play());
        }

        drawBoard(canvas.getGraphicsContext2D());
    }

    //update score if anyone wins
    private void updateScore() {
        scoreLabel.setText(String.format("Black: %d   White: %d", blackWins, whiteWins));
    }

    //logic for stone placement and hovering position logic
    private int[] getNearestGridPosition(double mouseX, double mouseY) {
        double gridX = (mouseX - BOARD_PADDING) / CELL_SIZE;
        double gridY = (mouseY - BOARD_PADDING) / CELL_SIZE;

        int x = (int) Math.round(gridX);
        int y = (int) Math.round(gridY);

        x = Math.max(0, Math.min(BOARD_SIZE - 1, x));
        y = Math.max(0, Math.min(BOARD_SIZE - 1, y));

        return new int[]{x, y};
    }

    //function to check win
    private boolean checkWin(int x, int y) {
        int player = board[x][y]; //get player (1 = black & 2 = white)
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}}; //check all dir

        for (int[] dir : directions) {
            int count = 1;
            int startX = x, startY = y; //start position
            int endX = x, endY = y; //end position

            for (int i = 1; i < 5; i++) { //check positive dir
                int nx = x + i * dir[0];
                int ny = y + i * dir[1];
                if (nx < 0 || nx >= BOARD_SIZE || ny < 0 || ny >= BOARD_SIZE || board[nx][ny] != player) break; //stop if hit boundary / other players stone
                count++;
                endX = nx; //update end
                endY = ny;
            }

            for (int i = 1; i < 5; i++) { //check negative dir
                int nx = x - i * dir[0];
                int ny = y - i * dir[1];
                if (nx < 0 || nx >= BOARD_SIZE || ny < 0 || ny >= BOARD_SIZE || board[nx][ny] != player) break; //stop if hit boundary / other players stone
                count++;
                startX = nx; //update start
                startY = ny;
            }

            if (count >= 5) { //win if count 5+
                winningLine = new int[]{startX, startY, endX, endY};
                return true;
            }
        }
        return false;
    }

    //checks if board is full
    private boolean isBoardFull() {
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                if (board[x][y] == 0) return false;
            }
        }
        return true;
    }

    //rewinds to previous move taken from moveHistory
    private void rewindMove() {
        if (currentMoveIndex < 0 || gameOver) return;

        //get move
        int[] move = moveHistory.get(currentMoveIndex);
        int x = move[0];
        int y = move[1];

        //move -1 & remove the move
        board[x][y] = 0;
        if (move[2] == 1) {
            blackMoveNumbers[x][y] = 0;
            blackMoveCount--;
        } else {
            whiteMoveNumbers[x][y] = 0;
            whiteMoveCount--;
        }

        //switch moves
        currentMoveIndex--;
        blackTurn = !blackTurn;
        gameOver = false;
        winningLine = null;

        drawBoard(canvas.getGraphicsContext2D());
    }

    //forwards the next move (if there is any)
    private void forwardMove() {
        if (currentMoveIndex >= moveHistory.size() - 1 || gameOver) return; //if there is no next move/game is done, function doesnt work

        currentMoveIndex++;

        //get move from moveHistory
        int[] move = moveHistory.get(currentMoveIndex);
        int x = move[0];
        int y = move[1];
        int player = move[2];

        //redo move & move ++
        board[x][y] = player;
        if (player == 1) {
            blackMoveNumbers[x][y] = blackMoveCount++;
        } else {
            whiteMoveNumbers[x][y] = whiteMoveCount++;
        }

        //change players
        blackTurn = !blackTurn;

        //check if move caused a win
        if (checkWin(x, y)) {
            gameOver = true;
            if (player == 1) blackWins++;
            else whiteWins++;
            updateScore();
            showWinPopup(player == 1 ? "Black" : "White");
        }

        drawBoard(canvas.getGraphicsContext2D());
    }

    //menu pop up content
    private void menuPopUp() {
        //inital set up
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UTILITY);
        popup.initOwner(primaryStage);

        //game mode selection
        Label modeLabel = new Label("Game Mode:");
        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton pvpButton = new RadioButton("Player vs Player");
        RadioButton pvaiButton = new RadioButton("Player vs AI");
        pvpButton.setToggleGroup(modeGroup);
        pvaiButton.setToggleGroup(modeGroup);
        pvpButton.setSelected(!vsAI); //unticks AI choosing
        pvaiButton.setSelected(vsAI); //ticks AI choosing

        //AI side selection (only enabled when PvP isnt chosen)
        Label sideLabel = new Label("AI Plays As:");
        ToggleGroup sideGroup = new ToggleGroup();
        RadioButton aiWhiteButton = new RadioButton("White");
        RadioButton aiBlackButton = new RadioButton("Black");
        aiWhiteButton.setToggleGroup(sideGroup);
        aiBlackButton.setToggleGroup(sideGroup);
        aiWhiteButton.setSelected(aiIsWhite);
        aiBlackButton.setSelected(!aiIsWhite);
        aiWhiteButton.disableProperty().bind(pvpButton.selectedProperty()); //disable AI is white when pvp choosing
        aiBlackButton.disableProperty().bind(pvpButton.selectedProperty()); //disable Ai is black when pvp choosing

        //game over image
        ImageView menuView = new ImageView(menuImage);
        menuView.setFitWidth(300);
        menuView.setPreserveRatio(true);

        //apply button
        Button applyButton = createImageButton(applyImage, 30);
        applyButton.setOnAction(e -> {
            vsAI = pvaiButton.isSelected();
            aiIsWhite = aiWhiteButton.isSelected();
            resetGame();
            popup.close();
        });

        //save button
        Button saveButton = createImageButton(saveButtonImage, 20);
        saveButton.setOnAction(e -> {
            showSaveDialog();
            popup.close();
        });

        //load button
        Button loadButton = createImageButton(loadButtonImage, 20);
        loadButton.setOnAction(e -> {
            showLoadDialog();
            popup.close();
        });

        //restart button
        Button restartButton = createImageButton(restartButtonImage, 20);
        restartButton.setOnAction(e -> {
            resetGame();
            popup.close();
        });

        //exit button
        Button exitButton = createImageButton(exitButtonImage, 20);
        exitButton.setOnAction(e -> {
            popup.close();
            primaryStage.close(); //close the program
        });

        //use hbox to stack 2 vboxes horizontally
        VBox modeBox = new VBox(5, modeLabel, pvpButton, pvaiButton);
        VBox sideBox = new VBox(5, sideLabel, aiWhiteButton, aiBlackButton);
        HBox settingsBox = new HBox(20, modeBox, sideBox, applyButton);
        HBox.setHgrow(applyButton, Priority.ALWAYS);

        //below the hbox, add another hbox to stack all the buttons (restart, exit, save, load)
        HBox buttonBox = new HBox(20, restartButton, exitButton, saveButton, loadButton);
        buttonBox.setAlignment(Pos.CENTER);

        //stack all of the hboxes and menu logo with vbox vertically
        VBox layout = new VBox(20, menuView, settingsBox, buttonBox);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 20; -fx-background-color: #f8f8f8;");

        //show the pop up
        popup.setScene(new Scene(layout));
        popup.showAndWait();
    }

    //win pop up content
    private void showWinPopup(String winner) {
        //inital set up
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UTILITY);
        popup.initOwner(primaryStage);

        //game over image
        ImageView gameOverView = new ImageView(gameOverImage);
        gameOverView.setFitWidth(300);
        gameOverView.setPreserveRatio(true);

        //winner indicator image
        ImageView winnerView = new ImageView();
        if (winner.equals("Black")) winnerView.setImage(blackWinImage);
        else if (winner.equals("White")) winnerView.setImage(whiteWinImage);
        else  winnerView.setImage(drawImage);
        winnerView.setFitWidth(150);
        winnerView.setPreserveRatio(true);

        //restart button
        Button restartButton = createImageButton(restartButtonImage, 25);
        restartButton.setOnAction(e -> {
            resetGame();
            popup.close();
        });

        //exit button
        Button exitButton = createImageButton(exitButtonImage, 25);
        exitButton.setOnAction(e -> {
            popup.close();
            primaryStage.close(); //close the program
        });

        //stack the restart & exit buttons horizontally with hbox
        HBox buttonBox = new HBox(20, restartButton, exitButton);
        buttonBox.setAlignment(Pos.CENTER);

        //stack the winner image, "game over" image, and buttons hbox vertically with vbox
        VBox layout = new VBox(20, gameOverView, winnerView, buttonBox);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 20; -fx-background-color: #f8f8f8;");

        //show the pop up
        popup.setScene(new Scene(layout));
        popup.setTitle("Game Over");
        popup.showAndWait();
    }

    //function to save game
    private void saveGame(int slotNumber) {
        try {
            new File(SAVE_FOLDER).mkdirs(); //make new dir

            GameState state = new GameState();
            //deep copy arrays
            for (int x = 0; x < BOARD_SIZE; x++) {
                System.arraycopy(board[x], 0, state.board[x], 0, BOARD_SIZE);
                System.arraycopy(blackMoveNumbers[x], 0, state.blackMoveNumbers[x], 0, BOARD_SIZE);
                System.arraycopy(whiteMoveNumbers[x], 0, state.whiteMoveNumbers[x], 0, BOARD_SIZE);
            }

            state.blackTurn = blackTurn;
            state.gameOver = gameOver;
            state.blackMoveCount = blackMoveCount;
            state.whiteMoveCount = whiteMoveCount;
            state.winningLine = winningLine != null ? winningLine.clone() : null;
            state.blackWins = blackWins;
            state.whiteWins = whiteWins;
            state.moveHistory = new ArrayList<>(moveHistory);
            state.currentMoveIndex = currentMoveIndex;
            state.vsAI = vsAI;
            state.aiIsWhite = aiIsWhite;

            try (ObjectOutputStream oos = new ObjectOutputStream( //saving file to local
                    new FileOutputStream(SAVE_FOLDER + "/save" + slotNumber + ".dat"))) {
                oos.writeObject(state);
                System.out.println("Game saved successfully to slot " + slotNumber);
            }
        } catch (Exception e) { //error handling if game fail to save
            e.printStackTrace();
            System.out.println("Failed to save game.");
        }
    }

    //function to load game
    private void loadGame(int slotNumber) {
        try (ObjectInputStream ois = new ObjectInputStream( //get file from local
                new FileInputStream(SAVE_FOLDER + "/save" + slotNumber + ".dat"))) {

            GameState state = (GameState) ois.readObject();

            //initialize board and arrays if null
            if (state.board == null) state.board = new int[BOARD_SIZE][BOARD_SIZE];
            if (state.blackMoveNumbers == null) state.blackMoveNumbers = new int[BOARD_SIZE][BOARD_SIZE];
            if (state.whiteMoveNumbers == null) state.whiteMoveNumbers = new int[BOARD_SIZE][BOARD_SIZE];
            if (state.moveHistory == null) state.moveHistory = new ArrayList<>();

            //copy all state values
            for (int x = 0; x < BOARD_SIZE; x++) {
                System.arraycopy(state.board[x], 0, board[x], 0, BOARD_SIZE);
                System.arraycopy(state.blackMoveNumbers[x], 0, blackMoveNumbers[x], 0, BOARD_SIZE);
                System.arraycopy(state.whiteMoveNumbers[x], 0, whiteMoveNumbers[x], 0, BOARD_SIZE);
            }

            blackTurn = state.blackTurn;
            gameOver = state.gameOver;
            blackMoveCount = state.blackMoveCount;
            whiteMoveCount = state.whiteMoveCount;
            winningLine = state.winningLine;
            blackWins = state.blackWins;
            whiteWins = state.whiteWins;
            moveHistory = new ArrayList<>(state.moveHistory);
            currentMoveIndex = state.currentMoveIndex;
            vsAI = state.vsAI;
            aiIsWhite = state.aiIsWhite;

            //reset the timer
            if (gameTimer != null) {
                gameTimer.stop();
            }
            currentPlayerTimeLeft = timeLimit;
            currentTimeLabel.setText((blackTurn ? "Black: " : "White: ") + currentPlayerTimeLeft + "s");

            // Restart the timer if the game isn't over
            if (!gameOver && gameTimer != null) {
                gameTimer.play();
            }

            updateScore();
            drawBoard(canvas.getGraphicsContext2D()); //draw board with copied array

        } catch (Exception e) {//error handling if fail toi load game
            e.printStackTrace();
            //initialize fresh game state if load fails
            resetGame();
            System.out.println("Failed to load game. Starting new game.");
        }
    }

    //save game pop up
    private void showSaveDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Save Game");

        VBox dialogVbox = new VBox(20);
        dialogVbox.setPadding(new Insets(20));

        //get existing save timestamps from local
        Map<Integer, String> saveTimestamps = new HashMap<>();
        for (int i = 1; i <= MAX_SAVES; i++) {
            File saveFile = new File(SAVE_FOLDER + "/save" + i + ".dat");
            if (saveFile.exists()) {
                saveTimestamps.put(i, new Date(saveFile.lastModified()).toString());
            }
        }

        //create buttons for each save slot
        for (int i = 1; i <= MAX_SAVES; i++) {
            HBox slotBox = new HBox(10);
            Button saveSlot = new Button("Slot " + i);
            Label timestampLabel = new Label(saveTimestamps.containsKey(i) ?
                    "Last saved: " + saveTimestamps.get(i) : "Empty slot");

            final int slotNumber = i;
            saveSlot.setOnAction(e -> {
                saveGame(slotNumber);
                timestampLabel.setText("Last saved: " + new Date());
                dialog.close();
            });

            slotBox.getChildren().addAll(saveSlot, timestampLabel);
            dialogVbox.getChildren().add(slotBox);
        }

        Scene dialogScene = new Scene(dialogVbox, 350, 200); //wider to fit timestamps
        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }

    //load game pop up
    private void showLoadDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Load Game");

        VBox dialogVbox = new VBox(20);
        dialogVbox.setPadding(new Insets(20));

        //create buttons for each load slot
        for (int i = 1; i <= MAX_SAVES; i++) {
            File saveFile = new File(SAVE_FOLDER + "/save" + i + ".dat");
            Button loadSlot = new Button("Save Slot " + i);
            loadSlot.setDisable(!saveFile.exists()); //diable if there is no file in local

            final int slotNumber = i;
            loadSlot.setOnAction(e -> {
                loadGame(slotNumber);
                dialog.close();
            });
            dialogVbox.getChildren().add(loadSlot);
        }

        Scene dialogScene = new Scene(dialogVbox, 200, 200);
        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }

    //make ai move function
    private void makeAIMove() {
        if (!vsAI || gameOver) return;

        //check if its AIs turn
        if ((aiIsWhite && !blackTurn) || (!aiIsWhite && blackTurn)) {
            int[] move = findBestMove();
            if (move != null) {
                int x = move[0];
                int y = move[1];

                if (board[x][y] == 0) {
                    board[x][y] = blackTurn ? 1 : 2;
                    addToMoveHistory(x, y, board[x][y]); //add move to history

                    if (blackTurn) { //update moves
                        blackMoveNumbers[x][y] = blackMoveCount++;
                    } else {
                        whiteMoveNumbers[x][y] = whiteMoveCount++;
                    }

                    if (checkWin(x, y)) {  //check win every time stone is placed and update values
                        gameOver = true;
                        if (blackTurn) blackWins++;
                        else whiteWins++;
                        updateScore();
                        //show win popup after animation
                        Platform.runLater(() -> showWinPopup(blackTurn ? "White" : "Black"));
                    } else if (isBoardFull()) { //draw if board full
                        gameOver = true;
                        Platform.runLater(() -> showWinPopup("Draw"));
                    }

                    blackTurn = !blackTurn; //change turns every time a stone is placed
                        currentPlayerTimeLeft = timeLimit;
                        currentTimeLabel.setText((blackTurn ? "Black: " : "White: ") + currentPlayerTimeLeft + "s"); //reset time every turn
                    drawBoard(canvas.getGraphicsContext2D());

                    // If game isn't over and next turn is AI's, make another move
                    if (!gameOver && ((aiIsWhite && !blackTurn) || (!aiIsWhite && blackTurn))) {
                        new Timeline(new KeyFrame(Duration.seconds(0.5), e -> makeAIMove())).play();
                    }
                }
            }
        }
    }

    //find best move for ai function
    private int[] findBestMove() {
        //check if AI can win in next move
        int[] winningMove = findWinningMove(blackTurn ? 1 : 2);
        if (winningMove != null) return winningMove;

        //check if opponent can win in next move and block
        int[] blockingMove = findWinningMove(blackTurn ? 2 : 1);
        if (blockingMove != null) return blockingMove;

        //otherwise, find the best strategic move
        return findStrategicMove();
    }

    //find winning move function
    private int[] findWinningMove(int player) {
        for (int x = 0; x < BOARD_SIZE; x++) { //check every x y on baord
            for (int y = 0; y < BOARD_SIZE; y++) {
                if (board[x][y] == 0) { //consider empty cells
                    board[x][y] = player; //place stone
                    if (checkWin(x, y)) {
                        board[x][y] = 0; //undo placement
                        return new int[]{x, y}; //return winning position
                    }
                    board[x][y] = 0; //undo placement
                }
            }
        }
        return null;
    }

    //find strategic move for AI function
    private int[] findStrategicMove() {
        //find position that gives longest line
        int maxScore = -1;
        List<int[]> bestMoves = new ArrayList<>();

        for (int x = 0; x < BOARD_SIZE; x++) {//check every x y on board
            for (int y = 0; y < BOARD_SIZE; y++) {
                if (board[x][y] == 0) {//only consider empty cells
                    int score = evaluatePosition(x, y, blackTurn ? 1 : 2);
                    if (score > maxScore) { //update score anytime one is higher
                        maxScore = score;
                        bestMoves.clear();
                        bestMoves.add(new int[]{x, y}); //add to best movee
                    } else if (score == maxScore) {
                        bestMoves.add(new int[]{x, y});
                    }
                }
            }
        }

        //return random move among best ones
        if (!bestMoves.isEmpty()) {
            return bestMoves.get((int)(Math.random() * bestMoves.size()));
        }

        //fallback: return center if empty, or first available spot
        if (board[BOARD_SIZE/2][BOARD_SIZE/2] == 0) {
            return new int[]{BOARD_SIZE/2, BOARD_SIZE/2};
        }

        //return first empty cell
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                if (board[x][y] == 0) {
                    return new int[]{x, y};
                }
            }
        }

        return null;
    }

    //function to evaluate position based on direction
    private int evaluatePosition(int x, int y, int player) {
        int score = 0;
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}}; //check all dir

        for (int[] dir : directions) {
            score += evaluateDirection(x, y, dir[0], dir[1], player);
        }

        return score;
    }

    //function to evaluate direction (x,y)
    private int evaluateDirection(int x, int y, int dx, int dy, int player) {
        int openEnds = 0;
        int consecutive = 0;

        //check positive dir
        for (int i = 1; i <= 4; i++) {
            int nx = x + i * dx;
            int ny = y + i * dy;

            if (nx < 0 || nx >= BOARD_SIZE || ny < 0 || ny >= BOARD_SIZE) break;//stop at board edge or opponent stone

            if (board[nx][ny] == player) { //count consecutive player stone
                consecutive++;
            } else if (board[nx][ny] == 0) { //if there is empty space add open ends
                openEnds++;
                break;
            } else { //opponent stone
                break;
            }
        }

        //check negative dir
        for (int i = 1; i <= 4; i++) {
            int nx = x - i * dx;
            int ny = y - i * dy;

            if (nx < 0 || nx >= BOARD_SIZE || ny < 0 || ny >= BOARD_SIZE) break;//stop at board edge or opponent stone

            if (board[nx][ny] == player) {
                consecutive++;
            } else if (board[nx][ny] == 0) {//if there is empty space add open ends
                openEnds++;
                break;
            } else { //opponent stone
                break;
            }
        }

        //score based on consecutive stones and open ends
        if (consecutive >= 4) return 1000; //immediate win
        if (consecutive == 3 && openEnds >= 1) return 100; //potential win
        if (consecutive == 2 && openEnds >= 1) return 10; //good position

        return consecutive;
    }

    //setup the red circles animation
    private AnimationTimer invalidMoveAnimation = new AnimationTimer() {
        @Override
        public void handle(long now) {
            //time
            long elapsedNanos = now - animationStartTime;
            double elapsedMillis = elapsedNanos / 1_000_000.0;

            if (elapsedMillis > INVALID_MOVE_DURATION) {
                stop();
                lastInvalidPosition = new int[]{-1, -1};
                drawBoard(canvas.getGraphicsContext2D());
                return;
            }

            //blinking effect
            double flashInterval = INVALID_MOVE_DURATION / INVALID_MOVE_FLASHES;
            boolean visible = ((int)(elapsedMillis / flashInterval) % 2) == 0;

            GraphicsContext gc = canvas.getGraphicsContext2D();
            drawBoard(gc);

            if (visible && lastInvalidPosition[0] != -1) {
                gc.setStroke(Color.RED);
                gc.setLineWidth(3);
                double ovalX = BOARD_PADDING + lastInvalidPosition[0] * CELL_SIZE - STONE_SIZE/2 - 5;
                double ovalY = BOARD_PADDING + lastInvalidPosition[1] * CELL_SIZE - STONE_SIZE/2 - 5;
                gc.strokeOval(ovalX, ovalY, STONE_SIZE + 10, STONE_SIZE + 10);
            }
        }
    };
}