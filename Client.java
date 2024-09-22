import java.io.*;
import java.nio.channels.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private Socket socket;
    private DataInputStream fromServer;
    private DataOutputStream toServer;
    private SocketChannel socketChannel;
    private final String folder = "./output/";
    private final String IpAddress = "192.168.56.1";
    private final int port = 5555;
    private final int portChannel = 5556;
    private final String[] fileList;

    public Client() {
        connection();
        fileList = getFileList();
        requestServer();
    }

    public final void connection() {
        try {
            socket = new Socket(IpAddress, port);
            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());
            socketChannel = SocketChannel.open(new InetSocketAddress(IpAddress, portChannel));
        } catch (IOException e) {
        }
    }

    public final String[] getFileList() {
        try {
            String read, file = "";
            while (!(read = fromServer.readUTF()).equalsIgnoreCase("/EOF"))
                file += read + "/";
            return file.split("/");  
        } catch (IOException e) {
            System.out.println(e);
            return null;
        }
    }
    
    public final void printFile() {
        System.out.println();
        System.out.println("  + Select number + ");
        for (int i = 0; i < fileList.length; i++)
            System.out.println(" [" + (i + 1) + "] " + fileList[i]);
        System.out.println();
        System.out.println("  * Print [EXIT] for Disconnected * ");
        System.out.println(" ");
    }

    public final void requestServer() {
        printFile();
        Scanner scan = new Scanner(System.in);
        while (true) {
            System.out.print("-> Select File No. -> ");
            String request = scan.next();
            if (request.equalsIgnoreCase("EXIT")){
                System.out.println("--Disconnected--");
                break;
            }
            try {
                int index = Integer.parseInt(request) - 1;
                if (index < 0 || index >= fileList.length) {
                    System.out.println("**** No information ****\n");
                    continue;
                }
                System.out.println("Choose one type for send\n1.Copy\n2.zero copy");
                System.out.print("Select type -->");
                String type = scan.next();
                if(!type.equals("1") && !type.equals("2")) {
                    System.out.println("Invalid type\n");
                    printFile();
                    continue;
                }
                toServer.writeInt(index);
                toServer.writeUTF(type);
                long size = fromServer.readLong();
                long start = System.currentTimeMillis();
                String filePath = folder + fileList[index];
                if(type.equals("1")) {
                    copy(filePath, size, index);
                } else if(type.equals("2")){
                    zeroCopy(filePath, size, index);
                }
                long end = System.currentTimeMillis();
                long timeElaspe = end-start;
                System.out.println("Time Elaspe: "+timeElaspe+" ms\n");
                printFile();
            } catch (NumberFormatException e) {
                System.out.println("**** No information ****\n");
            } catch (IOException ex) {
            }
        }
    }

    public void copy(String filePath, long size,int index) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filePath);
            byte[] buffer = new byte[1024];
            int read;
            long currentRead = 0;
            while (currentRead < size && (read = fromServer.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                currentRead += read;
            }
            System.out.println(">>> import "+ fileList[index] +" Success <<<");
        } catch (IOException e) {
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public final void zeroCopy(String filePath, long size, int index){
        FileChannel destination = null;
        try{
            destination = new FileOutputStream(filePath).getChannel();
            long currentRead = 0;
            long read;
            while(currentRead < size && (read = destination.transferFrom(socketChannel, currentRead, size - currentRead)) != -1)
                currentRead += read;
            System.out.println(">>> import "+ fileList[index] +" Success <<<");
        } catch (IOException e){}
        finally{
            try{
                if(destination != null)
                    destination.close();
            } catch (IOException e){
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
    }
}