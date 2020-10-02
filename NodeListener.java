import java.lang.Thread;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Objects;

public class NodeListener implements Runnable {
    Node node;
    int port;
    private ServerSocket serverSocket;

    public NodeListener(Node node, int port) throws IOException {
        this.node = node;
        this.port = port;
        serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        while (true) {
            System.out.println("listenr thread: " + this.node.termination);
            if (this.node.termination.get() == this.node.nodeNum) {
                System.out.println("Node: " + this.node.nodeID + " is done and receive " + Objects.toString(this.node.nodeNum - 1) + " termination messages");
                //this.node.outputMsgCount(this.node.messageCounter.get());
                return;
            }
            try {
                Socket server = serverSocket.accept();
                ObjectInputStream in = new ObjectInputStream(server.getInputStream());
                Message msg = (Message) in.readObject();
                this.node.receiveMsg(msg);
            } catch (SocketTimeoutException s) {
                System.out.println("Socket timed out!");
            } catch (IOException e) {
                e.printStackTrace();
                break;
            } catch (ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}