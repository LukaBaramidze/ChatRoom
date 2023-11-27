import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ChatUser implements java.io.Serializable {
    private static Socket socket;
    private static boolean foundServer = false;
    private static int port;
    private static String address;
    private static ObjectOutputStream output;
    private static ObjectInputStream input;
    public static void main(String[] args) throws IOException, InterruptedException {
        while (!foundServer) {
            foundServer = connectServer();
        }

        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());

        System.out.println("Connected to the server.");

        Thread writer = new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                String message;
                while (!Thread.interrupted()) {
                    message = scanner.nextLine();
                    output.writeObject(message);
                    if (message.equals("/leave")) System.exit(0);
                    output.flush();
                }
            } catch (IOException e) {
                System.out.println("hi");
                e.printStackTrace();
            }
        });

        Thread reader = new Thread(() -> {
            while(true){
                try {
                    while (!Thread.interrupted()) {
                        String receivedMessage = (String) input.readObject();
                        System.out.println(receivedMessage);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    closeSocket();
                    try {
                        reconnectServer();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

            }
        });
        reader.start();
        writer.start();

        reader.join();
        writer.join();
    }
    private static void reconnectServer() throws IOException { // tries to reconnect in 5 min, if timed out the program will close
        long startTime = System.currentTimeMillis();
        boolean connected = false;
        while ((System.currentTimeMillis() - startTime) < 5 * 60 * 1000 && !connected) {
            try {
                System.out.println("Connecting to " + address + ":" + port);
                socket = new Socket(address, port);
                connected = true;
            } catch (IOException e1) {
                System.out.println("Failed connecting to " + address + ":" + port);
                closeSocket();
            }
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        if(!connected){
            System.out.println("Couldn't connect to server, possibly down.");
            System.exit(0);
        }else{
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
        }
    }
    private static boolean connectServer() {
        System.out.print("Enter the server's port:");
        port = new Scanner(System.in).nextInt();
        System.out.print("Enter the server's address:");
        address = new Scanner(System.in).nextLine();
        try {
            System.out.println("Connecting to " + address + ":" + port);
            socket = new Socket(address, port);
            return true;
        } catch (IOException e) {
            System.out.println("Failed connecting to " + address + ":" + port);
            closeSocket();
            return false;
        }
    }
    private static void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}