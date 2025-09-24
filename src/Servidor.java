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
    private static final String ARCHIVO_BLOQUEADOS = "bloqueados.txt";

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

            String usuarioAutenticado = null;

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
                        borrarMensaje(usuarioAutenticado, lector, escritor);
                        break;
                    case "4":
                        borrarTodosLosMensajes(usuarioAutenticado, lector, escritor);
                        break;
                    case "5":
                        bloquearUsuario(usuarioAutenticado, lector, escritor);
                        break;
                    case "6":
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

    private static void bloquearUsuario(String usuarioBloqueador, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("¿A qué usuario deseas bloquear?");
        String usuarioABloquear = lector.readLine();

        if (usuarioBloqueador.equals(usuarioBloqueador)) {
            escritor.println("No te puedes bloquear a ti mismo");
            return;
        }

        if (!verificarUsuarioExiste(usuarioABloquear)) {
            escritor.println("El usuario '" + usuarioABloquear + "' No existe");
            return;
        }

        if (estaBloqueado(usuarioBloqueador, usuarioABloquear)) {
            escritor.println("Ya has bloqueado a este usuario.");
            return;
        }

        synchronized (Servidor.class) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_BLOQUEADOS, true))) {
                writer.write(usuarioBloqueador + ":" + usuarioABloquear);
                writer.newLine();
                escritor.println("Has bloqueado '" + usuarioABloquear + "' Exitosamente");
                System.out.println("El usuario " + usuarioBloqueador + " bloqueó a '" + usuarioABloquear + "' exitosamente");
            } catch (IOException e) {
                escritor.println("Error al intentar bloquear al usuario");
                e.printStackTrace();
            }
        }

    }

    private static boolean estaBloqueado(String usuario1, String usuario2) throws IOException {
        File archivo = new File(ARCHIVO_BLOQUEADOS);
        if (!archivo.exists()) return false;

        try(BufferedReader reader = new BufferedReader(new FileReader(archivo))){
            String linea;
            while((linea = reader.readLine()) != null){
                String[] partes = linea.split(":");
                if(partes.length == 2){
                    if ((partes[0].equals(usuario1) && partes[1].equals(usuario2)) ||
                    (partes[0].equals(usuario2) && partes[1].equals(usuario1))){
                    return true;
                    }
                }
            }
        }
        return false;
    }

    private static void borrarTodosLosMensajes(String usuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("¿Estás seguro de que deseas borrar TODOS tus mensajes? Esta acción no se puede deshacer. [S/N]");

        String confirmacion = lector.readLine();
        if (confirmacion != null && "S".equalsIgnoreCase(confirmacion.trim())) {
            List<String> mensajesGuardados = new ArrayList<>();
            File archivo = new File(ARCHIVO_MENSAJES);

            if (archivo.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                    String linea;
                    while ((linea = reader.readLine()) != null) {
                        String[] partes = linea.split(":", 3);
                        if (partes.length == 3 && !partes[0].equals(usuario) && !partes[1].equals(usuario)) {
                            mensajesGuardados.add(linea);
                        }
                    }
                }
            }

            synchronized (Servidor.class) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_MENSAJES, false))) {
                    for (String linea : mensajesGuardados) {
                        writer.write(linea);
                        writer.newLine();
                    }
                }
            }
            escritor.println("Todos tus mensajes han sido borrados.");
            System.out.println("El usuario " + usuario + " ha borrado todos sus mensajes.");
        } else {
            escritor.println("Operación cancelada.");
        }
    }

    private static void borrarMensaje(String usuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        List<String> mensajesDelUsuario = new ArrayList<>();
        List<String> otrosMensajes = new ArrayList<>();
        File archivo = new File(ARCHIVO_MENSAJES);

        if (archivo.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    String[] partes = linea.split(":", 3);
                    if (partes.length == 3 && (partes[0].equals(usuario) || partes[1].equals(usuario))) {
                        mensajesDelUsuario.add(linea);
                    } else {
                        otrosMensajes.add(linea);
                    }
                }
            }
        }
        if (mensajesDelUsuario.isEmpty()) {
            escritor.println("No tienes mensajes para borrar.");
            return;
        }

        int tamanoPagina = 10;
        int totalMensajes = mensajesDelUsuario.size();
        int totalPaginas = (int) Math.ceil((double) totalMensajes / tamanoPagina);
        int paginaActual = 1;

        while (true) {
            escritor.println("--- Elige que mensaje quieres borrar (Página " + paginaActual + "/" + totalPaginas + ") ---");
            int inicio = (paginaActual - 1) * tamanoPagina;
            int fin = Math.min(inicio + tamanoPagina, totalMensajes);
            for (int i = inicio; i < fin; i++) {
                String[] partes = mensajesDelUsuario.get(i).split(":", 3);
                String prefijo;
                if (partes[0].equals(usuario)) {
                    prefijo = "Para [" + partes[1] + "]: ";

                } else {
                    prefijo = "De [" + partes[1] + "]: ";
                }
                escritor.println((i - inicio + 1) + "." + prefijo + partes[2]);
            }
            StringBuilder instruccion = new StringBuilder("Elige un mensaje (1-" + (fin-inicio) + ")");
            if(paginaActual < totalPaginas){
                instruccion.append(", [S] Siguiente");
            }
            if(paginaActual > 1){
                instruccion.append(", [A] Anterior");
            }
            instruccion.append(". [C} Cancelar");
            escritor.println(instruccion.toString());
            escritor.println("FIN_LISTA_BORRAR");
            String input = lector.readLine();
            if(input == null) return;
            input = input.trim();
            if("S".equalsIgnoreCase(input) && paginaActual < totalPaginas){
                paginaActual++;
                continue;
            }
            if("A".equalsIgnoreCase(input) && paginaActual > 1){
                paginaActual--;
                continue;
            }
            if("C".equalsIgnoreCase(input)){
                escritor.println("Operacion cancelada.");
            }
            try {
                int seleccionEnPagina = Integer.parseInt(input);
                if (seleccionEnPagina > 0 && seleccionEnPagina <= (fin-inicio)) {
                    int indiceGlobal = inicio + seleccionEnPagina - 1;
                    String mensajeBorrado = mensajesDelUsuario.remove(indiceGlobal);
                    System.out.println("Borrando mensaje: " + mensajeBorrado);
                    synchronized (Servidor.class) {
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_MENSAJES, false))) {
                            for (String linea : otrosMensajes) {
                                writer.write(linea);
                                writer.newLine();
                            }
                            for (String linea : mensajesDelUsuario) {
                                writer.write(linea);
                                writer.newLine();
                            }
                        }
                    }
                    escritor.println("Mensaje borrado exitosamente.");
                    return;
                } else {
                    escritor.println("Número fuera de rango. Operación cancelada.");
                }
            } catch (NumberFormatException e) {
                escritor.println("Entrada no válida. Por favor ingrese un número. Operación cancelada.");
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
                    if (partes.length == 3 && partes[0].equals(usuario)){
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

