import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Servidor {

    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final String ARCHIVO_MENSAJES = "mensajes.txt";

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(8080);
            System.out.println("Servidor listo. Esperando cliente...");

            while (true) {
                Socket cliente = serverSocket.accept();
                System.out.println("Cliente conectado: " + cliente.getInetAddress());

                new Thread(() -> menu(cliente)).start();
            }
        } catch (IOException e) {
            System.out.println("Ocurrió un error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

}