package com.vinaychandra.HttpServer;

import java.io.*;
import java.net.*;

public class HttpServer{

    public static void main(String[] args) {
        int port;
        try {
            port = Integer.parseInt(args[0]);
        }catch (Exception e){
            port = 20000;
        }

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
            while(true)
                try {
                    System.out.println("Listening...");
                    Socket server = serverSocket.accept();
                    System.out.println("Just connected to " + server.getRemoteSocketAddress());
                    new Thread(new thread_main(server)).start();
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
        }catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }
    }
}
