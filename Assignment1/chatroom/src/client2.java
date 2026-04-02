import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

public class client2 {
    private static final Logger logger = Logger.getLogger(client2.class.getName());
    private static final List<String> chatHistory = new ArrayList<>();
    private static String username;
    private static int userId;  //get user id from server
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // call getValidPort function
        int roomPort = getValidPort(scanner);

        try (
                Socket socket = new Socket("localhost", roomPort);
                BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));//send message to server
                PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true)//get message from server
        ) {
            in = serverIn;
            out = serverOut;

            //handle username
            System.out.println(serverIn.readLine()); // welcome message, get from server
            username = scanner.nextLine().trim();
            if (username.isEmpty()) {
                username = "Anonymous";  //if user doenst input, assign anonymous
            }
            out.println(username);

            // Receive the user ID from the server
            String confirmationMessage = serverIn.readLine();  //server sends user id
            if (confirmationMessage.startsWith("Your user ID is: ")) {
                userId = Integer.parseInt(confirmationMessage.split(": ")[1]);  //get user id from server
                System.out.println("Your assigned user ID is: " + userId);
            }

            //listen for messages from the server
            Thread messageListenerThread = new MessageListenerThread(serverIn);
            messageListenerThread.start();

            //chat loop
            while (true) {
                System.out.print(""); //media for input
                String input = scanner.nextLine().trim();//read user input

                if (input.equalsIgnoreCase("/quit")) {//if user /quit, break loop
                    out.println("/quit");
                    System.out.println("You have left the chat.");
                    break;
                }

                if (input.startsWith("/search")) {//if user /search, search message
                    String query = input.substring(7).trim();
                    if (query.isEmpty()) { //if there is no iunput, handle error
                        System.out.println("No search query provided.");
                    } else {
                        boolean found = false;
                        for (String message : chatHistory) {
                            //get the message by skipping formatted part
                            String[] messageParts = message.split(": ", 2); //split when it encounters :
                            if (messageParts.length > 1) {
                                String messageContent = messageParts[1]; //message
                                if (messageContent.contains(query)) {
                                    System.out.println("Found: " + message);
                                    found = true;
                                }
                            }
                        }
                        if (!found) {//handle error if not found
                            System.out.println("No messages found for: " + query);
                        }
                    }
                } else if (!input.isEmpty()) {//if input has a message, format, save it and send it.
                    String userMessage = getFormattedMessage(userId, username, input);
                    chatHistory.add(userMessage);
                    out.println(input);
                    System.out.println(userMessage);
                }
            }
        } catch (IOException e) {// error handling loop if cannot connect to server
            logger.severe("Could not connect to server: " + e.getMessage());
        }
    }

    //format mesage
    private static String getFormattedMessage(int userId, String sender, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        return timestamp + " " + sender + " (ID: " + userId + "): " + message;
    }

    //listen for message
    private static class MessageListenerThread extends Thread {
        private final BufferedReader in;
        //recursive loop to read message from server and format it
        public MessageListenerThread(BufferedReader in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    chatHistory.add(serverMessage);  //store all messages received from the server
                    System.out.println(serverMessage); //display the message with timestamp, ID, and username
                }
            } catch (IOException e) {//error handling if server cannot be read
                logger.severe("Error reading from server: " + e.getMessage());
            }
        }
    }

    //check user port input
    private static int getValidPort(Scanner scanner) {
        int roomPort = -1;
        while (roomPort == -1) {
            try {
                System.out.print("Enter the chat room port (12346, 12347, 12348): ");
                String input = scanner.nextLine().trim();

                if (input.matches("\\d+")) {//user input
                    roomPort = Integer.parseInt(input);

                    // Check if it's a valid port
                    if (roomPort == 12346 || roomPort == 12347 || roomPort == 12348) {
                        return roomPort; // Return the valid port
                    } else {//error handling if user input is wrong number
                        System.out.println("Invalid port. Please enter a valid port (12346, 12347, or 12348).");
                    }
                } else {//error handling if user input isnt number
                    System.out.println("Invalid port. Please enter a valid port (12346, 12347, or 12348).");
                }

            } catch (NumberFormatException e) {//error handling
                System.out.println("Invalid port. Please enter a valid port (12346, 12347, or 12348).");
            }
        }
        return roomPort;
    }
}
