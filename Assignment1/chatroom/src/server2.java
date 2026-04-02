import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;
import java.util.stream.*;

public class server2 {
    private static final Logger logger = Logger.getLogger(server2.class.getName());
    private static final Map<Integer, ChatRoom> chatRooms = new HashMap<>();
    private static final String USER_ID_FILE = "last_user_id.txt";

    public static void main(String[] args) {
        logger.info("Chat server is starting...");

        int[] roomPorts = {12346, 12347, 12348};

        //start chat for every port
        for (int port : roomPorts) {
            ChatRoom chatRoom = new ChatRoom(port);
            chatRooms.put(port, chatRoom);
            new Thread(chatRoom).start(); //each room is a separate thread
        }
    }


    //generate id
    private static synchronized int generateUserId() {
        File idFile = new File(USER_ID_FILE);
        int lastId = 10000; //5 digit id

        try {
            if (idFile.exists()) { //if the file exists, read the last line
                try (BufferedReader reader = new BufferedReader(new FileReader(idFile))) {
                    String line = reader.readLine();
                    if (line != null) {
                        lastId = Integer.parseInt(line.trim()); //last id = last line
                    }
                }
            }

            lastId++; //start new id

            //save id to last_id.txt
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(idFile))) {
                writer.write(String.valueOf(lastId));
            }

            return lastId;
        } catch (IOException | NumberFormatException e) {
            logger.severe("Failed to generate user ID: " + e.getMessage());
            return -1; //error handle
        }
    }

    private static class ChatRoom implements Runnable {
        private final int port;
        private final Map<Integer, String> onlineUsers = Collections.synchronizedMap(new HashMap<>()); //map of online users id & username
        private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
        private final Logger logger = Logger.getLogger(ChatRoom.class.getName()); //
        private final List<MessageRecord> messageHistory = Collections.synchronizedList(new ArrayList<>());
        private static final Pattern HISTORY_PATTERN = Pattern.compile(
                "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) (.+?) \\(ID: (\\d+)\\): (.+)$" //YYYY-MM-DD HH:MM:SS
        );

        public ChatRoom(int port) {
            this.port = port;
            loadChatHistory(); //load chat history
        }

        private String getHistoryFileName() {
            return "chatHistory_" + port + ".txt";
        }

        private void loadChatHistory() {
            File historyFile = new File(getHistoryFileName());
            if (!historyFile.exists()) return; //skip if no history file exists

            try (BufferedReader reader = new BufferedReader(new FileReader(historyFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = HISTORY_PATTERN.matcher(line);//apply pattern matcher
                    if (matcher.find()) {
                        String timestamp = matcher.group(1);
                        String username = matcher.group(2);
                        int userId = Integer.parseInt(matcher.group(3));
                        String message = matcher.group(4);

                        //add message to history
                        synchronized (messageHistory) {
                            messageHistory.stream()
                                    .filter(record -> record.getMessageText().equals(message))
                                    .findFirst()
                                    .orElseGet(() -> {
                                        MessageRecord newRecord = new MessageRecord(message);
                                        messageHistory.add(newRecord);
                                        return newRecord;
                                    })
                                    .addInstance(username, userId, timestamp);
                        }
                    }
                }
            } catch (IOException | NumberFormatException e) { //error if file cant be read
                logger.severe("Failed to load chat history: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                logger.info("Chat room is now running on port " + port);

                //accept new client
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new ClientHandler(clientSocket).start(); //handle each client in separate thread
                }
            } catch (IOException e) {
                logger.severe("Chat room on port " + port + " encountered an error: " + e.getMessage());
            }
        }

        //1 client handler
        private class ClientHandler extends Thread {
            private final Socket clientSocket;
            private PrintWriter out;
            private String username;
            private int userId;

            public ClientHandler(Socket clientSocket) {
                this.clientSocket = clientSocket;
            }

            @Override
            public void run() {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    this.out = out;

                    out.println("Welcome to the chat! Please enter your username:");
                    boolean validUsername = false;
                    while (!validUsername) {
                        username = in.readLine();
                        userId = server2.generateUserId(); //call generateUserId function to gfenerate 5 digit random id
                        if (userId == -1) {
                            out.println("Error creating user ID. Disconnecting...");
                            return;
                        }
                        synchronized (onlineUsers) {
                            onlineUsers.put(userId, username); //add the user to map
                        }
                        validUsername = true;
                        out.println("Your user ID is: " + userId);
                    }

                    logger.info(username + " (ID: " + userId + ") has joined the chat room on port " + port);

                    //send history to client and mark all as read
                    sendChatHistoryToClient();
                    markAllMessagesAsRead();

                    //add client to room and notify other users
                    synchronized (clients) {
                        clients.add(this);
                        out.println("Online users: " + getOnlineUsers());
                        for (ClientHandler client : clients) {
                            if (client != this) {
                                client.out.println(getCurrentTime() + " " + username + " (ID: " + userId + ") has joined the chat.");
                            }
                        }
                    }

                    //handle messages from the client
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.equalsIgnoreCase("/quit")) {
                            break; // break loop if client /quit
                        }

                        if (message.startsWith("[print-receiver]:")) {
                            String originalMessage = message.substring(message.indexOf(':') + 1).trim();
                            printUserMessages(originalMessage, out);
                        } else { //normal message
                            logger.info(username + " (ID: " + userId + "): " + message);

                            //record and save message
                            recordMessageReceivers(message);
                            saveMessageToChatHistory(message);

                            //broadcast to other clients
                            synchronized (clients) {
                                for (ClientHandler client : clients) {
                                    if (client != this) {
                                        client.out.println(getCurrentTime() + " " + username + " (ID: " + userId + "): " + message);
                                        client.markMessageAsRead(message);
                                    }
                                }
                            }
                        }
                    }

                } catch (IOException e) {
                    logger.severe("Error handling client: " + e.getMessage());
                } finally {
                    //when client disconects
                    synchronized (clients) {
                        clients.remove(this);
                        synchronized (onlineUsers) {
                            onlineUsers.remove(userId);
                        }
                        for (ClientHandler client : clients) {
                            client.out.println(getCurrentTime() + " SERVER: " + username + " (ID: " + userId + ") has left the chat.");
                        }
                    }
                    try {
                        clientSocket.close();
                    } catch (IOException e) {//error if cant exit
                        logger.severe("Error closing client socket: " + e.getMessage());
                    }
                    logger.info(username + " (ID: " + userId + ") has disconnected");
                }
            }

            private void printUserMessages(String originalMessage, PrintWriter out) {
                synchronized (messageHistory) {
                    //filter messages
                    List<MessageRecord> userRecords = messageHistory.stream()
                            .filter(record -> record.getInstances().stream()
                                    .anyMatch(instance -> instance.getSenderUserId() == userId)) //only messages sent by client
                            .collect(Collectors.toList());

                    if (userRecords.isEmpty()) {
                        out.println("You haven't sent any messages yet.");
                        return;
                    }

                    //if a specific message is provided, filter for that message
                    if (originalMessage != null && !originalMessage.isEmpty()) {
                        userRecords = userRecords.stream()
                                .filter(record -> record.getMessageText().equals(originalMessage)) // Filter by message content
                                .collect(Collectors.toList());

                        if (userRecords.isEmpty()) {
                            out.println("No such message found: \"" + originalMessage + "\"");
                            return;
                        }
                    }

                    //display the user message
                    for (MessageRecord record : userRecords) {
                        out.println("Message: \"" + record.getMessageText() + "\"");
                        for (MessageInstance instance : record.getInstances()) {
                            if (instance.getSenderUserId() == userId) { // Ensure only the user's instances are shown
                                out.println("  Sent at: " + instance.getTimestamp());
                                if (instance.getReceiverEvents().isEmpty()) {
                                    out.println("    No read receipts yet");
                                } else {
                                    out.println("    Read by:");
                                    for (ReceiverEvent event : instance.getReceiverEvents()) {
                                        out.println("    - " + event.getReceiver() + " (ID: " + event.getReceiverId() + ")" + " at " + event.getTimeRead());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //sends chat history to client
            private void sendChatHistoryToClient() {
                String historyFile = getHistoryFileName();
                try (BufferedReader reader = new BufferedReader(new FileReader(historyFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.println(line);
                    }
                } catch (FileNotFoundException e) {
                    //no file exist
                } catch (IOException e) {
                    logger.severe("Error reading chat history: " + e.getMessage());
                }
            }

            private void markAllMessagesAsRead() {
                synchronized (messageHistory) {
                    for (MessageRecord record : messageHistory) {
                        for (MessageInstance instance : record.getInstances()) {
                            boolean exists = instance.getReceiverEvents().stream()
                                    .anyMatch(e -> e.getReceiverId().equals(String.valueOf(userId)));

                            if (!exists) {
                                instance.addReceiverEvent(
                                        username,
                                        String.valueOf(userId),
                                        getCurrentTime()
                                );
                            }
                        }
                    }
                }
            }

            //saves message to history file
            private void saveMessageToChatHistory(String message) {
                String historyFile = getHistoryFileName();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(historyFile, true))) {
                    String formattedMessage = getCurrentTime() + " " + username + " (ID: " + userId + "): " + message;
                    writer.write(formattedMessage);
                    writer.newLine();
                } catch (IOException e) { //error handle if cannot save
                    logger.severe("Error saving chat history: " + e.getMessage());
                }
            }

            private String getCurrentTime() {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            }

            private String getOnlineUsers() {
                synchronized (onlineUsers) {
                    if (onlineUsers.isEmpty()) return "No online users";
                    return onlineUsers.entrySet().stream()
                            .map(entry -> entry.getValue() + " (ID: " + entry.getKey() + ")")
                            .collect(Collectors.joining(", "));
                }
            }

            private void recordMessageReceivers(String message) {
                synchronized (messageHistory) {
                    messageHistory.stream()
                            .filter(r -> r.getMessageText().equalsIgnoreCase(message)).findFirst().orElseGet(() -> {
                                MessageRecord newRecord = new MessageRecord(message);
                                messageHistory.add(newRecord);
                                return newRecord;
                            })
                            .addInstance(username, userId, getCurrentTime());
                }
            }

            private void markMessageAsRead(String message) {
                synchronized (messageHistory) {
                    messageHistory.stream().filter(r -> r.getMessageText().equals(message)).findFirst().ifPresent(record -> {
                        MessageInstance latestInstance = record.getLatestInstance();
                        if (latestInstance != null) {
                            boolean alreadyRead = latestInstance.getReceiverEvents().stream().anyMatch(event -> event.getReceiverId().equals(String.valueOf(this.userId)));
                            if (!alreadyRead) {
                                latestInstance.addReceiverEvent(
                                        this.username,
                                        String.valueOf(this.userId),
                                        getCurrentTime()
                                );
                            }
                        }
                    });
                }
            }
        }

        private static class MessageRecord {
            private final String messageText;
            private final List<MessageInstance> instances = new ArrayList<>(); //instances of message being sent

            public MessageRecord(String messageText) {
                this.messageText = messageText;
            }

            public String getMessageText() { return messageText; }
            public List<MessageInstance> getInstances() { return instances; }

            // Adds a new instance of the message being sent
            public void addInstance(String senderUsername, int senderUserId, String timestamp) {
                instances.add(new MessageInstance(senderUsername, senderUserId, timestamp));
            }

            // Returns the latest instance of the message
            public MessageInstance getLatestInstance() {
                return instances.isEmpty() ? null : instances.get(instances.size() - 1);
            }
        }

        //message instance info
        private static class MessageInstance {
            private final String senderUsername;
            private final int senderUserId;
            private final String timestamp;
            private final List<ReceiverEvent> receiverEvents = new ArrayList<>();//message read tracker

            public MessageInstance(String senderUsername, int senderUserId, String timestamp) {
                this.senderUsername = senderUsername;
                this.senderUserId = senderUserId;
                this.timestamp = timestamp;
            }

            public int getSenderUserId() { return senderUserId; }
            public String getTimestamp() { return timestamp; }
            public List<ReceiverEvent> getReceiverEvents() { return receiverEvents; }

            public void addReceiverEvent(String receiver, String receiverId, String timeRead) {
                receiverEvents.add(new ReceiverEvent(receiver, receiverId, timeRead));
            }
        }

        //event where user reads the message
        private static class ReceiverEvent {
            private final String receiver;
            private final String receiverId;
            private final String timeRead;

            public ReceiverEvent(String receiver, String receiverId, String timeRead) {
                this.receiver = receiver;
                this.receiverId = receiverId;
                this.timeRead = timeRead;
            }

            public String getReceiver() { return receiver; }
            public String getReceiverId() { return receiverId; }
            public String getTimeRead() { return timeRead; }
        }
    }
}