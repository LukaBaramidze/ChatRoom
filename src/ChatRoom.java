import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom {

    private static final Map < Socket, String > ConnectedClients = new ConcurrentHashMap < > ();
    private static final Map < Socket, ObjectOutputStream > ClientOutputs = new ConcurrentHashMap < > (); // stores the ouput for each socket that's connected

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the port number: ");
        int port = scanner.nextInt();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ChatRoom is running on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Received connection from " + clientSocket.getInetAddress().getHostAddress());
                String dfname = uniqueName();

                ClientOutputs.put(clientSocket, new ObjectOutputStream(clientSocket.getOutputStream()));
                ConnectedClients.put(clientSocket, dfname);

                broadcastMessage(clientSocket, "Welcome to the group chat " + dfname + "! (we sell crystal blue meth) \nMember count:" + ConnectedClients.size());
                Thread handler = new Thread(() -> { // handler thread which handles connections
                    System.out.println(Thread.activeCount());
                    try (
                            ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
                    ) {
                        while (clientSocket.isConnected()) {
                            String message = (String) input.readObject();
                            System.out.println("Received message from " + clientSocket.getInetAddress().getHostAddress() + ": " + message);
                            if (message.startsWith("/name:")) { // /name:, what comes afterward is the clients new name
                                String name = message.substring(6);
                                if (findName(name)) ConnectedClients.put(clientSocket, name);
                                else ClientOutputs.get(clientSocket).writeObject("Couldn't change name, Name already in use.");
                            } else if (message.startsWith("/msg:")) {
                                try {
                                    String name = message.substring(5, message.indexOf(' '));
                                    String message1 = message.substring(name.length() + 6);
                                    if (!anonymousMessage(clientSocket, ConnectedClients.get(clientSocket) + ":" + message1, name)) {
                                        ClientOutputs.get(clientSocket).writeObject("Couldn't send message, user doesn't exist.");
                                    }
                                } catch (StringIndexOutOfBoundsException e) {
                                    ClientOutputs.get(clientSocket).writeObject("Couldn't send message, incorrect command syntax");
                                }
                            } else if (message.equals("/leave")) {
                                kickMachine(clientSocket);
                            } else {
                                broadcastMessage(clientSocket, ConnectedClients.get(clientSocket) + ":" + message);
                            }
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void broadcastMessage(Socket clientSocket, String message) { // outputs the message to all connected machines
        ConnectedClients.forEach((a, b) -> {
            try {
                ObjectOutputStream clientOutput1 = ClientOutputs.get(a);
                clientOutput1.writeObject(message);
                clientOutput1.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    private static String uniqueName() {
        String name = "Anonymous#" + String.format("%04d", new Random().nextInt(10000));
        if (!findName(name)) uniqueName();
        return name;
    }
    private static boolean findName(String name) {
        for (Map.Entry < Socket, String > temp: ConnectedClients.entrySet()) {
            System.out.println(temp.getValue());
            if (temp.getValue().equals(name)) return false;
        }
        return true;
    }
    private static boolean anonymousMessage(Socket clientSocket, String message, String name) throws IOException { // outputs the message to one machine
        for (Map.Entry < Socket, String > entry: ConnectedClients.entrySet()) {
            Socket a = entry.getKey();
            String b = entry.getValue();
            if (b.equals(name)) {
                ObjectOutputStream clientOutput1 = ClientOutputs.get(a);
                clientOutput1.writeObject(message);
                clientOutput1.flush();
                return true;
            }
        }
        return false;
    }
    private static void kickMachine(Socket clientSocket) throws IOException {
        int count = ConnectedClients.size() - 1;
        broadcastMessage(clientSocket, ConnectedClients.get(clientSocket) + " has left the chat.\nMember count:" + count);
        clientSocket.close();
        ConnectedClients.remove(clientSocket);
        ClientOutputs.remove(clientSocket);
    }
}