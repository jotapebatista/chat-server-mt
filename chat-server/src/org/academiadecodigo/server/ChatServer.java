package org.academiadecodigo.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Logger.*;

public class ChatServer {

    public static void main(String[] args) throws IOException {

        ChatServer chatServer = new ChatServer();
        chatServer.startServer();

    }

    private static final int serverPort = 6667;
    private static final Logger logger = getLogger(ChatServer.class.getName());
    static int i = 0;
    Vector<ClientHandler> clientArr = null;
    ServerSocket serverSocket = null;
    ExecutorService es;

    public ChatServer(){

        try {
            serverSocket = new ServerSocket(serverPort);
            es = Executors.newCachedThreadPool();
            clientArr = new Vector<>();
            logger.log(Level.INFO, "Chat server started on port " + serverPort);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }
    }

    public void startServer() throws IOException {

        while (true){

            Socket clientSocket = serverSocket.accept();
            logger.log(Level.INFO, "New client connected. IP: " + clientSocket.getInetAddress() + " Port: " + clientSocket.getPort());

            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());

            ClientHandler ch = new ClientHandler("client " + i, this, clientSocket);

            es.submit(ch); //sends client handler to thread pool

            clientArr.add(i, ch); //Add new connection to Vector

            logger.log(Level.INFO, "Client: " + ch.getName() +  " entered the chat.");

            i++; //increments i to keep track of the number of users connected.

        }
    }

    /*
    * Synchronized method invoked by the ClientHandler Object.
    * This means that other clients cannot simultaneously access it.
    * */
    public synchronized void broadCastMessage(String message, ClientHandler sender){

        logger.log(Level.INFO, "Starting message broadcast from: " + sender.getName());

        for (ClientHandler client: clientArr) {

            if (message.equals("quit")){
                client.sendMessage(sender.id + " leaved the chat.");
                logger.log(Level.INFO, sender.id + " leaved the chat");
                clientArr.remove(sender.id); //removes client from the vector list
                i--;
            }

            if (client != sender){
                client.sendMessage(sender.name + "# " + message + "\n");
            }

        }
    }

    /*
    *
    * Nested class that implements an abstract "client"
    *
    * */
    public static class ClientHandler implements Runnable {

        private final String name;
        private int id;
        private final DataOutputStream dos;
        private final DataInputStream dis;
        private final Socket clientSocket;
        private final ChatServer chatServer;

        public ClientHandler(String name, ChatServer chatServer, Socket clientSocket) throws IOException {
            this.name = name;

            this.dos = new DataOutputStream(clientSocket.getOutputStream());
            this.dis = new DataInputStream(clientSocket.getInputStream());

            this.chatServer = chatServer;
            this.clientSocket = clientSocket;
        }

        public String getName(){
            return name;
        }

        public void sendMessage(String message){
            try {
                dos.writeUTF(message);
                dos.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {

            while (!clientSocket.isClosed()){
                try {
                    String receivedMsg;
                    while ((receivedMsg = dis.readLine()) != null){
                        if (receivedMsg.equals("quit")){
                            clientSocket.close();
                        }
                        chatServer.broadCastMessage(receivedMsg, this);
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }

        }
    }

}


