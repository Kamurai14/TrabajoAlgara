import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {
    public static void main(String[] args) throws IOException {
        ServerSocket socketEspecial = new ServerSocket(8080);
        Socket cliente = socketEspecial.accept();

        PrintWriter escritor = new PrintWriter(cliente.getOutputStream(),true);
        BufferedReader lectorSocket = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
        cliente.getInputStream();
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        String entrada;
        String mensaje;
        while((entrada = lectorSocket.readLine()) != null) {
            System.out.println(entrada.toUpperCase());
            mensaje = teclado.readLine();
            escritor.println(mensaje);
            if(mensaje.equalsIgnoreCase("FIN")){
                break;
            }
        }
        cliente.close();
    }
}
