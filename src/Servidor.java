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
private static boolean validPassword(String contrasena) {
    return contrasena != null && !contrasena.trim().isEmpty() && contrasena.length() >= 8;
}
private static boolean verificarUsuarioExiste(String usuario) throws IOException {
    File archivo = new File(ARCHIVO_USUARIOS);
    if (!archivo.exists()) return false;

    try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
        String linea;
        while ((linea = lector.readLine()) != null) {
            String[] partes = linea.split(":");
            if (partes.length > 0 && partes[0].equals(usuario)) {
                return true;
            }
        }
    }
    return false;
}

private static boolean verificarCredenciales(String usuario, String contrasena) throws IOException {
    File archivo = new File(ARCHIVO_USUARIOS);
    if (!archivo.exists()) return false;

    try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
        String linea;
        while ((linea = lector.readLine()) != null) {
            String[] partes = linea.split(":");
            if (partes.length == 2 && partes[0].equals(usuario) && partes[1].equals(contrasena)) {
                return true;
            }
        }
    }
    return false;
}

private static boolean registrarUsuario(String usuario, String contrasena) throws IOException {
    synchronized (Servidor.class) {
        if (verificarUsuarioExiste(usuario)) {
            return false;
        }
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(ARCHIVO_USUARIOS, true))) {
            escritor.write(usuario + ":" + contrasena);
            escritor.newLine();
            return true;
        }
    }
}

private static void mostrarUsuariosRegistrados(PrintWriter escritor) {
    File archivo = new File(ARCHIVO_USUARIOS);
    escritor.println("--- Usuarios Registrados ---");
    if (!archivo.exists()) {
        escritor.println("No hay usuarios registrados.");
    } else {
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length >= 1) {
                    escritor.println("- " + partes[0]);
                }
            }
        } catch (IOException e) {
            escritor.println("Error al leer usuarios.");
        }
    }
    escritor.println("FIN_USUARIOS");
}
    private static void enviarMensaje(String remitente, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("¿Para quién es el mensaje? (nombre de usuario)");
        String destinatario = lector.readLine();
        if (destinatario == null) return;

        if (destinatario.equals(remitente)) {
            escritor.println("Error: No puedes enviarte un mensaje a ti mismo.");
            return;
        }
        if (!verificarUsuarioExiste(destinatario)) {
            escritor.println("Error: El usuario '" + destinatario + "' no existe.");
            return;
        }

        escritor.println("Escribe tu mensaje:");
        String mensaje = lector.readLine();
        if (mensaje == null) return;

        synchronized (Servidor.class) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_MENSAJES, true))) {
                writer.write(destinatario + ":" + remitente + ":" + mensaje); // Formato: destinatario:remitente:mensaje
                writer.newLine();
                escritor.println("Mensaje enviado exitosamente a " + destinatario);
            } catch (IOException e) {
                escritor.println("Error al guardar el mensaje.");
                e.printStackTrace();
            }
        }
    }


    private static void leerMensajes(String usuario, PrintWriter escritor) {
        escritor.println("---MENSAJES---");
        File archivo = new File(ARCHIVO_MENSAJES);
        if (!archivo.exists()) {
            escritor.println("No tienes mensajes.");
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                String linea;
                int contador = 0;
                while ((linea = reader.readLine()) != null) {
                    String[] partes = linea.split(":", 3);
                    if (partes.length == 3 && partes[0].equals(usuario)) {
                        escritor.println("De [" + partes[1] + "]: " + partes[2]);
                        contador++;
                    }
                }
                if (contador == 0) {
                    escritor.println("No tienes mensajes nuevos.");
                }
            } catch (IOException e) {
                escritor.println("Error al leer los mensajes.");
            }
        }
        escritor.println("FIN_MENSAJES");
    }
}

