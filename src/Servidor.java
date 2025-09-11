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
private static void menu(Socket cliente) {
    try (
            PrintWriter escritor = new PrintWriter(cliente.getOutputStream(), true);
            BufferedReader lector = new BufferedReader(new InputStreamReader(cliente.getInputStream()))
    ) {
        escritor.println("Bienvenido. ¿Deseas [1] Iniciar sesión, [2] Registrarte o [3] Ver usuarios registrados?");
        String opcion = lector.readLine();

        String usuarioAutenticado = null; // Guarda el nombre del usuario que inició sesión

        if ("3".equals(opcion)) {
            mostrarUsuariosRegistrados(escritor);
            cliente.close();
            return;
        }

        escritor.println("Usuario:");
        String usuario = lector.readLine();
        escritor.println("Contraseña:");
        String contrasena = lector.readLine();

        if ("1".equals(opcion)) {
            if (verificarCredenciales(usuario, contrasena)) {
                escritor.println("Autenticación exitosa");
                usuarioAutenticado = usuario;
            } else {
                escritor.println("Credenciales inválidas");
            }
        } else if ("2".equals(opcion)) {
            if (!validPassword(contrasena)) {
                escritor.println("Contraseña no válida. Debe tener al menos 8 caracteres y no puede estar vacía.");
            } else {
                if (registrarUsuario(usuario, contrasena)) {
                    escritor.println("Usuario registrado exitosamente");
                    usuarioAutenticado = usuario;
                } else {
                    escritor.println("El usuario ya existe");
                }
            }
        } else {
            escritor.println("Opción no válida");
        }

        if (usuarioAutenticado == null) {
            cliente.close();
            return;
        }

        String opcionMenu;
        while ((opcionMenu = lector.readLine()) != null) {
            System.out.println("Opción recibida del cliente '" + usuarioAutenticado + "': '" + opcionMenu + "'");

            switch (opcionMenu) {
                case "1":
                    enviarMensaje(usuarioAutenticado, lector, escritor);
                    break;
                case "2":
                    leerMensajes(usuarioAutenticado, escritor);
                    break;
                case "3":
                    System.out.println("Cliente " + usuarioAutenticado + " ha cerrado sesión.");
                    cliente.close();
                    return;
                default:
                    escritor.println("Opción de menú no válida.");
                    break;
            }
        }

    } catch (IOException e) {
        System.out.println("Error con el cliente: " + e.getMessage());
    } finally {
        try {
            if (cliente != null && !cliente.isClosed()) {
                cliente.close();
            }
        } catch (IOException e) {
            System.out.println("Error al cerrar el socket del cliente: " + e.getMessage());
        }
    }
}