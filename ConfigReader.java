import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class ConfigReader {
    int nodeNum;
    int myNodeIndex;
    String myHostName;
    int interRequestDelay;
    int csExecutionTime;
    int numRequests;
    ArrayList<HashMap<String, String>> NodeInfoList;
    ArrayList<Integer> outgoingNodeList;

    public ConfigReader() throws UnknownHostException {
        read();
        printConfigInfo(this);
    }

    private void read() throws UnknownHostException {
        InetAddress localHostInfo = InetAddress.getLocalHost();
        this.myHostName = localHostInfo.getHostName().split("\\.")[0];
        boolean firstline = false;
        this.NodeInfoList = new ArrayList<>();
        this.outgoingNodeList = new ArrayList<>();
        System.out.println("current path: " + System.getProperty("user.dir"));
        try (BufferedReader br = new BufferedReader(new FileReader("./config.txt"))) {
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] line_list = line.split("\\s+");
                if (line_list.length == 1 || line_list[0].equals("#")) continue;
                if (!firstline) {
                    this.nodeNum = Integer.parseInt(line_list[0]);
                    this.interRequestDelay = Integer.parseInt(line_list[1]);
                    this.csExecutionTime = Integer.parseInt(line_list[2]);
                    this.numRequests = Integer.parseInt(line_list[3]);
                    firstline = true;
                } else {
                    if (line_list[1].equals(this.myHostName)) {
                        this.myNodeIndex = Integer.parseInt(line_list[0]);
                    } else {
                        this.outgoingNodeList.add(Integer.parseInt(line_list[0]));
                    }
                    HashMap<String, String> temp = new HashMap<>();
                    temp.put("hostname", line_list[1]);
                    temp.put("port", line_list[2]);
                    this.NodeInfoList.add(temp);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return Integer.parseInt(this.NodeInfoList.get(this.myNodeIndex).get("port"));
    }

    private static void printConfigInfo(ConfigReader a) {
        System.out.println("nodeNum: " + a.nodeNum);
        System.out.println("myNodeIndex: " + a.myNodeIndex);
        System.out.println("myHostName: " + a.myHostName);
        System.out.println("interRequestDelay: " + a.interRequestDelay);
        System.out.println("csExecutionTime: " + a.csExecutionTime);
        System.out.println("numRequests: " + a.numRequests);
        ArrayList<HashMap<String, String>> NodeInfoList = a.NodeInfoList;
        ArrayList<Integer> outGoing = a.outgoingNodeList;
        for (Integer temp : outGoing) System.out.println(temp);
    }

    public static void main(String[] args) throws UnknownHostException {
        System.out.println("this is main function of Config Reader class");
        ConfigReader a = new ConfigReader();
        printConfigInfo(a);
    }
}