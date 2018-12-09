package main;

import client.Client;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.start();
    }
}
