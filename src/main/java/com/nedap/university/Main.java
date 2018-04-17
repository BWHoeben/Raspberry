package com.nedap.university;

import java.io.IOException;

public class Main {

    private static boolean keepAlive = true;
    private static boolean running = false;
    private Main() {}

    public static void main(String[] args) {
        running = true;
        print("Hello, Nedap University 3.0!");

        initShutdownHook();

        print("Initializing server...");
        try {
            new Server.ServerThread().start();
        } catch (IOException e) {
            print("IOException.");
            Thread.currentThread().interrupt();
        }

        while (keepAlive) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                print("Interrupted exception.");
                Thread.currentThread().interrupt();
            }
        }

        print("Stopped");
        running = false;
    }

    private static void initShutdownHook() {
        final Thread shutdownThread = new Thread() {
            @Override
            public void run() {
                keepAlive = false;
                while (running) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    private static void print(String msg) {
        System.out.println(msg);
    }
}
