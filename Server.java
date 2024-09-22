import java.io.*;
import java.net.*;
import java.nio.channels.*;

public class Server {

    public static int clientNo;
    private ServerSocket serverSocket;
    private ServerSocketChannel serverChannel;
    private final String folder = "./files/";
    private final String IpAddress = "192.168.56.1";
    private final int port = 5555;
    private final int portChannel = 5556;
    private final File[] fileList;

    public Server() {
        fileList = new File(folder).listFiles(File::isFile);
        connectionHandle();
    }

    public final void connectionHandle() {
        try {
            serverSocket = new ServerSocket(port);
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(IpAddress, portChannel));
            while (true) {
                Socket socketClient = serverSocket.accept();
                DataInputStream fromClient = new DataInputStream(socketClient.getInputStream());
                DataOutputStream toClient = new DataOutputStream(socketClient.getOutputStream());
                SocketChannel socketChannel = serverChannel.accept();
                System.out.println("Client " + (clientNo + 1) + " is connected");
                ClientHandle ch = new ClientHandle(socketClient, fromClient, toClient, socketChannel,
                        ++clientNo, fileList);
                ch.start();
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    class ClientHandle extends Thread {

        private final int clientNo;
        private final Socket socket;
        private final DataInputStream fromClient;
        private final DataOutputStream toClient;
        private final SocketChannel socketChannel;
        private final File[] fileList;

        public ClientHandle(Socket socket, DataInputStream fromClient, DataOutputStream toClient,
                SocketChannel socketChannel, int clientNo, File[] fileList) {
            this.socket = socket;
            this.fromClient = fromClient;
            this.toClient = toClient;
            this.socketChannel = socketChannel;
            this.clientNo = clientNo;
            this.fileList = fileList;
        }

        public final void sendFileList() {
            try {
                for (File file : fileList)
                    toClient.writeUTF(file.getName());
                toClient.writeUTF("/EOF");
                toClient.flush();
            } catch (IOException e) {
                System.out.println(e);
            }
        }

        @Override
        public void run() {
            sendFileList();
            try {
                while (true) {
                    int index = fromClient.readInt();
                    String type = fromClient.readUTF();
                    String filePath = fileList[index].getAbsolutePath();
                    long size = fileList[index].length();
                    toClient.writeLong(size);
                    System.out.println("Client " + clientNo + "need to " + (!type.equals("1") ? "zero " : "")
                            + "copy file :" + fileList[index].getName());
                    if (type.equals("1")) {
                        copy(filePath, size);
                    } else if (type.equals("2")) {
                        zeroCopy(filePath, size);
                    }
                }
            } catch (IOException ex) {
                System.out.println("Client " + clientNo + " is disconnected");
            }
        }

        public void copy(String filePath, long size) {
            FileInputStream readfile = null;
            try {
                readfile = new FileInputStream(filePath);
                byte[] buffer = new byte[1024];
                int read;
                long currentRead = 0;
                while (currentRead < size && (read = readfile.read(buffer)) != -1) {
                    toClient.write(buffer, 0, read);
                    currentRead += read;
                }
            } catch (IOException e) {
            } finally {
                try {
                    if (readfile != null)
                        readfile.close();
                } catch (IOException e) {
                    System.out.println("Client " + clientNo + " is disconnected");
                }
            }
        }

        public void zeroCopy(String filePath, long size) {
            FileChannel source = null;
            try {
                source = new FileInputStream(filePath).getChannel();
                long currentRead = 0;
                long read;
                while (currentRead < size
                        && (read = source.transferTo(currentRead, size - currentRead, socketChannel)) != -1) {
                    currentRead += read;
                }
            } catch (IOException e) {
            } finally {
                try {
                    if (source != null)
                        source.close();
                } catch (IOException e) {
                    System.out.println("Client " + clientNo + " is disconnected");
                }
            }
        }
    }
    public static void main(String[] args) {
        Server host = new Server();
    }
}