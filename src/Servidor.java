import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class Servidor {
    public static void main(String[] args) throws IOException {
        try {
            ServerSocket socketEspecial = new ServerSocket(8080);
            System.out.println("Servidor listo. Esperando cliente...");
            Socket cliente = socketEspecial.accept();
            System.out.println("Cliente conectado.");

            PrintWriter escritor = new PrintWriter(cliente.getOutputStream(), true);
            BufferedReader lectorSocket = new BufferedReader(new InputStreamReader(cliente.getInputStream()));

            // Número aleatorio entre 1 y 10
            Random random = new Random();
            int numeroSecreto = random.nextInt(10) + 1;

            int intentos = 0;
            boolean adivinado = false;
            String entrada;

            while (intentos < 3 && (entrada = lectorSocket.readLine()) != null) {
                int intentoCliente;
                try {
                    intentoCliente = Integer.parseInt(entrada);
                } catch (NumberFormatException e) {
                    escritor.println("Entrada inválida. Por favor, ingresa un número entre 1 y 10.");
                    continue;
                }
                intentos++;

                if (intentoCliente == numeroSecreto) {
                    escritor.println("¡Felicidades! Has adivinado el número.");
                    adivinado = true;
                    break;
                } else if (intentoCliente < numeroSecreto) {
                    escritor.println("El número es mayor.");
                } else {
                    escritor.println("El número es menor.");
                }
            }

            if (!adivinado) {
                escritor.println("Se acabaron los intentos. El número era: " + numeroSecreto);
            }

            cliente.close();
            socketEspecial.close();
            System.out.println("Conexión cerrada.");

        } catch (IOException e) {
            System.out.println("Ocurrió un error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
