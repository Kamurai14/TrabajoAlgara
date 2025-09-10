import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {
    public static void main(String[] args) throws IOException {

        ServerSocket socketEspecial = null;
        try{
            socketEspecial = new ServerSocket(8080);
        } catch (IOException e) {
            System.out.println("Hubo problemas de conexion en la red");
            System.out.println(1);
            throw new RuntimeException(e);
        }
        Socket cliente = null;
        try{
            cliente = socketEspecial.accept();
        } catch (Exception e) {
            System.out.println("Hubo problemas de conexion en la red");
            System.out.println(1);
            throw new RuntimeException(e);
        }

        try{
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
        }catch (IOException e){
            System.out.println("Hubo problemas de conexion con los sockets");
            System.exit(2);
        }
        try{
            cliente.close();
        }catch (IOException e){
            System.out.println("Hubo problemas de conexion en la red");
            System.out.println(1);
        }
    }
}
