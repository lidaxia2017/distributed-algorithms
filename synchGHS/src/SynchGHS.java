import java.io.*;
import java.util.*;

public class SynchGHS {

    public static void main(String[] args) {
        try {
            String configPath = args.length > 0 ? args[0] : "../config.txt";
            String nodeId = args.length > 1 ? args[1] : "-1";
            Logger.setLocalNodeId(Integer.parseInt(nodeId));
            Logger.Info("Init node......");

            Node node = initNode(configPath, nodeId);
            node.startMsgService();

            StringBuilder tmp = new StringBuilder();
            for (int nId : node.getNeighbors().keySet()) {
                tmp.append(nId + "  ");
            }
            Logger.Info("Connecting Node: %s", tmp);

            buildMST(node);

            Logger.Info("Finish");
            //node.updateTree();
            // PriorityQueue<Edge> pq = node.getEdges();
            // while (!pq.isEmpty()) {
            //     Edge edge = pq.poll();
            //     Logger.Debug("Endpoint1: %s, Endpoint2: %s Weight: %s", edge.endpoint1, edge.endpoint2, edge.weight);
            // }

            List<Edge> edgeList = node.getTreeEdges();
            for (Edge edge : edgeList) {
                Logger.Info("Endpoint1: %s, Endpoint2: %s Weight: %s", edge.endpoint1, edge.endpoint2, edge.weight);
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Logger.Error(sw.toString());
        }
    }

    public static Node initNode(String configs, String nodeId) throws Exception {
        Node node = null;
        HashMap<String, String> nodes = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(configs))) {
            String line;
            int lineNum = 0;
            int nodeNum = -1;
            while ((line = br.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (isValidLine(line)) {
                    lineNum++;
                    String readLine = line.split("#")[0].trim();
                    String[] t = readLine.split("\\s+");
                    if (nodeNum == -1) {
                        nodeNum = Integer.parseInt(readLine);
                        lineNum = 0;
                    } else if (lineNum <= nodeNum) {
                        if (t.length != 3) {
                            throw new Exception(String.format("Invalid configs at line %d", lineNum));
                        }
                        nodes.put(t[0], readLine);

                        if (t[0].equals(nodeId)) {
                            node = new Node(Integer.parseInt(t[0]), t[1], Integer.parseInt(t[2]));
                        }
                    } else if (lineNum > nodeNum) {
                        if (t[0].startsWith("(") && t[0].startsWith(")")) {
                            throw new Exception(String.format("Invalid edge format: %s", readLine));
                        }
                        String[] points = t[0].substring(1, t[0].length() - 1).split(",");
                        String id1 = points[0].trim();
                        String id2 = points[1].trim();
                        if (!id1.equals(nodeId) && !id2.equals(nodeId)) {
                            continue;
                        }
                        if (id1.equals(id2)) {
                            throw new Exception(String.format("Invalid edge Id: %s", readLine));
                        }

                        String nbIdStr = id1.equals(nodeId) ? id2 : id1;
                        String[] nbInfo = nodes.get(nbIdStr).split("\\s+");
                        int nId = Integer.parseInt(nbInfo[0]);
                        String nHost = nbInfo[1].trim();
                        int nPort = Integer.parseInt(nbInfo[2]);
                        int weight = Integer.parseInt(t[1].trim());

                        node.setN(nodeNum);
                        node.addNeighbor(nId, nHost, nPort, weight);
                    }
                }
            }
        }
        return node;
    }

    private static boolean isValidLine(String line) {
        return line.length() > 0 && (Character.isDigit(line.charAt(0)) || line.charAt(0) == '(');
    }

    public static void buildMST(Node node) {
        node.initBuildMST();
        //int i = 0;
        //while (i < 2) {
        while (node.getNodeState() != NodeState.TERMINATE) {
            //if (node.getNodeState() != NodeState.JOIN)
            Logger.Debug("Begin a new component search");
            node.searchMWOE();
            Logger.Debug("Search MWOE complete");
            node.selectLocalMWOE();
            Logger.Debug("Select local MWOE complete");
            node.convergeLocalMWOE();
            Logger.Debug("Converge local MWOE complete");
            node.sendMerge();
            node.mergeMWOE();
        }
        Logger.Debug("Finish to create MST");
        Logger.Debug("Final component ID is %s", node.getComponentId());
    }
}