import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Vector;
import javax.swing.Timer;

public class TestAgent2 extends JFrame {

    // --- SNMP CONFIG ---
    private static final String ADDRESS = "127.0.0.1/555";
    private static final String COMMUNITY = "public";
    private static final String BASE_OID = "1.3.6.1.3.2025";
    private static final OID LANE_ENTRY_OID = new OID("1.3.6.1.3.2025.1.1.1");
    private static final OID VIA_DEST_ENTRY_OID = new OID("1.3.6.1.3.2025.1.2.1");

    // --- GUI ---
    private JComboBox<String> cmbOperations;
    private JComboBox<String> cmbVariables;
    private JTextField txtIndex, txtValue;
    private JTextField txtCommunity;
    private JTextArea logArea;
    private JButton btnExecute;
    private LayeredGraphPanel topologyPanel;
    private Timer pollingTimer;
    private final Map<String, MibNode> mibDictionary = new LinkedHashMap<>();

    public TestAgent2() {
        super("Traffic Network Manager - Smart Logging");
        initMibDictionary();
        initUI();
        startPolling();
    }

    // 1. MIB STRUCTURE
    private void initMibDictionary() {
        mibDictionary.put(">> ENTIRE MIB (WALK All) <<", new MibNode("", false, true));
        mibDictionary.put("System: Up Time (.1.3.0)",        new MibNode(".1.3.0", false, false));
        mibDictionary.put("System: Step Time (.1.4.0)",        new MibNode(".1.4.0", false, false));

        String laneBase = ".1.1.1";
        mibDictionary.put(">> [GET/SET] Lane Variable...", new MibNode(laneBase, true, false));
        mibDictionary.put("   Vehicle Count",    new MibNode(laneBase + ".2", true, false));
        mibDictionary.put("   Semaphore Color",  new MibNode(laneBase + ".3", true, false));
        mibDictionary.put("   Time To Change",   new MibNode(laneBase + ".4", true, false));
        mibDictionary.put("   Capacity",         new MibNode(laneBase + ".5", true, false));
        mibDictionary.put("   RGT",              new MibNode(laneBase + ".6", true, false));
        mibDictionary.put("   Total Vehicles",   new MibNode(laneBase + ".7", true, false));
        mibDictionary.put("   Via Terminal",     new MibNode(laneBase + ".8", true, false));

        mibDictionary.put(">> [WALK] ALL Lane Data", new MibNode(laneBase, false, true));
        String viaDestBase = ".1.2.1";
        mibDictionary.put(">> [WALK] ALL Connections", new MibNode(viaDestBase, false, true));
    }

    private void initUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 950);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(340);

        // === TOP PANEL: CONTROLS ===
        JPanel topContainer = new JPanel(new BorderLayout());
        JPanel pnlConn = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlConn.setBackground(new Color(230, 240, 250));
        pnlConn.add(new JLabel("Target: " + ADDRESS));
        pnlConn.add(new JLabel(" | Community: "));
        txtCommunity = new JTextField("public", 10); // Default value "public"
        pnlConn.add(txtCommunity);
        topContainer.add(pnlConn, BorderLayout.NORTH);

        JPanel pnlControls = new JPanel(new GridBagLayout());
        pnlControls.setBorder(new TitledBorder("SNMP Manager Controls"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx=0; gbc.gridy=0; pnlControls.add(new JLabel("Operation:"), gbc);
        cmbOperations = new JComboBox<>(new String[]{"GET", "SET", "WALK", "GETNEXT"});
        gbc.gridx=1; pnlControls.add(cmbOperations, gbc);

        gbc.gridx=2; pnlControls.add(new JLabel("Target:"), gbc);
        cmbVariables = new JComboBox<>(new Vector<>(mibDictionary.keySet()));
        gbc.gridx=3; pnlControls.add(cmbVariables, gbc);

        gbc.gridx=0; gbc.gridy=1; pnlControls.add(new JLabel("Index (ID):"), gbc);
        txtIndex = new JTextField("1");
        gbc.gridx=1; pnlControls.add(txtIndex, gbc);

        gbc.gridx=2; pnlControls.add(new JLabel("Value (Set):"), gbc);
        txtValue = new JTextField(); txtValue.setEnabled(false);
        gbc.gridx=3; pnlControls.add(txtValue, gbc);

        btnExecute = new JButton("EXECUTE COMMAND");
        btnExecute.setBackground(new Color(180, 220, 180));
        gbc.gridx=1; gbc.gridy=2; gbc.gridwidth=3;
        pnlControls.add(btnExecute, gbc);

        topContainer.add(pnlControls, BorderLayout.CENTER);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setBorder(new TitledBorder("Output Log"));
        scrollLog.setPreferredSize(new Dimension(800, 140));
        topContainer.add(scrollLog, BorderLayout.SOUTH);

        // === BOTTOM PANEL: VISUALIZATION ===
        topologyPanel = new LayeredGraphPanel();
        splitPane.setTopComponent(topContainer);
        splitPane.setBottomComponent(topologyPanel);
        add(splitPane);

        // Events
        cmbOperations.addActionListener(e -> txtValue.setEnabled("SET".equals(cmbOperations.getSelectedItem())));
        btnExecute.addActionListener(e -> executeSNMP());
        cmbVariables.setSelectedIndex(0);
    }

    // --- MANUAL SNMP EXECUTION ---
    private void executeSNMP() {
        new Thread(() -> {
            String op = (String) cmbOperations.getSelectedItem();
            String key = (String) cmbVariables.getSelectedItem();
            MibNode node = mibDictionary.get(key);

            if (key.startsWith(">>") && !key.contains("WALK")) {
                log("Please select a valid option.");
                return;
            }

            String fullOID = BASE_OID + node.suffix;
            if (node.requiresIndex && !"WALK".equals(op)) {
                String idx = txtIndex.getText().trim();
                if (idx.isEmpty()) { log("Error: Index required."); return; }
                fullOID += "." + idx;
            }

            log("\n>> Executing " + op + " on " + key);
            try { performSnmpRequest(op, fullOID); }
            catch (Exception ex) { log("Error: " + ex.getMessage()); }
        }).start();
    }

    private void performSnmpRequest(String operation, String oidStr) throws IOException {
        TransportMapping<?> t = new DefaultUdpTransportMapping(); Snmp s = new Snmp(t); t.listen();
        CommunityTarget target = createTarget();
        try {
            OID oid = new OID(oidStr);
            if ("WALK".equals(operation)) {
                walkTableLog(s, target, oid);
            } else {
                PDU p = new PDU(); p.add(new VariableBinding(oid));
                if ("SET".equals(operation)) {
                    p.setType(PDU.SET); p.set(0, new VariableBinding(oid, new Integer32(Integer.parseInt(txtValue.getText()))));
                } else if ("GET".equals(operation)) p.setType(PDU.GET);
                else p.setType(PDU.GETNEXT);

                ResponseEvent r = s.send(p, target);
                if (r != null && r.getResponse() != null) {
                    // Smart Log: Format the OID to be readable
                    VariableBinding vb = r.getResponse().get(0);
                    String readable = formatOID(vb.getOid());
                    log("Result: " + readable + " = " + vb.getVariable());
                } else log("Timeout.");
            }
        } finally { s.close(); }
    }

    private void walkTableLog(Snmp snmp, Target target, OID rootOID) throws IOException {
        OID currentOID = new OID(rootOID);
        while (true) {
            PDU pdu = new PDU(); pdu.add(new VariableBinding(currentOID)); pdu.setType(PDU.GETNEXT);
            ResponseEvent resp = snmp.send(pdu, target);
            if (resp == null || resp.getResponse() == null) break;
            VariableBinding vb = resp.getResponse().get(0);
            if (!vb.getOid().startsWith(rootOID)) break;

            // Smart Log
            String readable = formatOID(vb.getOid());
            log(readable + " = " + vb.getVariable());

            currentOID = vb.getOid();
        }
        log("End of Walk.");
    }

    // --- OID TRANSLATOR (The "Easy to Read" Fix) ---
    private String formatOID(OID oid) {
        String oidStr = oid.toString();
        // Check for Lane Table
        if (oidStr.startsWith("1.3.6.1.3.2025.1.1.1")) {
            try {
                int[] raw = oid.toIntArray();
                if (raw.length < 2) return oidStr;
                int col = raw[raw.length - 2];
                int idx = raw[raw.length - 1];

                String colName = switch (col) {
                    case 2 -> "Vehicle Count";
                    case 3 -> "Semaphore Color";
                    case 4 -> "Time To Change";
                    case 5 -> "Capacity";
                    case 6 -> "RGT";
                    case 7 -> "Total Vehicles";
                    case 8 -> "Via Terminal";
                    default -> "Col " + col;
                };
                return "[Lane " + idx + " " + colName + "] " + oidStr;
            } catch (Exception e) { return oidStr; }
        }
        // Check for ViaDest Table
        if (oidStr.startsWith("1.3.6.1.3.2025.1.2.1")) {
            try {
                int[] raw = oid.toIntArray();
                if (raw.length < 3) return oidStr;
                int col = raw[raw.length - 3];
                int laneID = raw[raw.length - 1];
                String colName = (col == 2) ? "DestVia" : (col == 3 ? "Fluxo" : "Col " + col);
                return "[ViaDest L" + laneID + " " + colName + "] " + oidStr;
            } catch (Exception e) { return oidStr; }
        }
        // Scalars
        if (oidStr.equals("1.3.6.1.3.2025.1.3.0")) return "[Sys UpTime] " + oidStr;
        if (oidStr.equals("1.3.6.1.3.2025.1.4.0")) return "[Sys StepTime] " + oidStr;

        return oidStr; // Fallback
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // --- VISUALIZER POLLING ---
    private void startPolling() {
        pollingTimer = new Timer(1000, e -> fetchData());
        pollingTimer.start();
    }

    private void fetchData() {
        new Thread(() -> {
            try {
                Map<Integer, LaneData> lanes = networkWalkLanes();
                List<Connection> connections = networkWalkConnections();
                SwingUtilities.invokeLater(() -> topologyPanel.updateGraph(lanes, connections));
            } catch (Exception ex) { /* Silent fail */ }
        }).start();
    }

    private Map<Integer, LaneData> networkWalkLanes() throws IOException {
        Map<Integer, LaneData> result = new HashMap<>();
        TransportMapping<?> t = new DefaultUdpTransportMapping(); Snmp s = new Snmp(t); t.listen();
        try {
            CommunityTarget target = createTarget();
            OID oid = LANE_ENTRY_OID;
            while(true) {
                PDU pdu = new PDU(); pdu.add(new VariableBinding(oid)); pdu.setType(PDU.GETNEXT);
                ResponseEvent r = s.send(pdu, target);
                if(r==null || r.getResponse()==null) break;
                VariableBinding vb = r.getResponse().get(0);
                if(!vb.getOid().startsWith(LANE_ENTRY_OID)) break;
                int[] raw = vb.getOid().toIntArray();
                int id = raw[raw.length-1];
                int col = raw[raw.length-2];
                LaneData d = result.getOrDefault(id, new LaneData(id));
                if(col==2) d.vehicleCount = vb.getVariable().toInt();
                if(col==3) d.semaphoreColor = vb.getVariable().toInt();
                if(col==4) d.timeToChange = vb.getVariable().toInt();
                if(col==5) d.capacity = vb.getVariable().toInt();
                if(col==8) d.isTerminal = (vb.getVariable().toInt() == 1);
                result.put(id, d);
                oid = vb.getOid();
            }
        } finally { s.close(); }
        return result;
    }

    private List<Connection> networkWalkConnections() throws IOException {
        List<Connection> list = new ArrayList<>();
        TransportMapping<?> t = new DefaultUdpTransportMapping(); Snmp s = new Snmp(t); t.listen();
        try {
            CommunityTarget target = createTarget();
            OID oid = VIA_DEST_ENTRY_OID;
            while(true) {
                PDU pdu = new PDU(); pdu.add(new VariableBinding(oid)); pdu.setType(PDU.GETNEXT);
                ResponseEvent r = s.send(pdu, target);
                if(r==null || r.getResponse()==null) break;
                VariableBinding vb = r.getResponse().get(0);
                if(!vb.getOid().startsWith(VIA_DEST_ENTRY_OID)) break;
                int[] raw = vb.getOid().toIntArray();
                int col = raw[raw.length-3];
                int laneID = raw[raw.length-1];
                if(col==2) list.add(new Connection(laneID, vb.getVariable().toInt()));
                oid = vb.getOid();
            }
        } finally { s.close(); }
        return list;
    }

    private CommunityTarget createTarget() {
        Address addr = new UdpAddress(ADDRESS);
        CommunityTarget t = new CommunityTarget();
        t.setCommunity(new OctetString(txtCommunity.getText()));
        t.setAddress(addr);
        t.setRetries(0); t.setTimeout(800); t.setVersion(SnmpConstants.version2c);
        return t;
    }

    // =============================================================
    // === LAYERED GRAPH ENGINE (Visualizer) ===
    // =============================================================

    private class LayeredGraphPanel extends JPanel {
        private Map<Integer, LaneData> lanes = new HashMap<>();
        private List<VisualLane> visualLanes = new ArrayList<>();

        public LayeredGraphPanel() { setBackground(Color.WHITE); }

        public void updateGraph(Map<Integer, LaneData> lanes, List<Connection> connections) {
            this.lanes = lanes;
            calculateLayeredLayout(connections);
            repaint();
        }

        private void calculateLayeredLayout(List<Connection> connections) {
            visualLanes.clear();
            Map<Integer, List<Integer>> adj = new HashMap<>();
            Map<Integer, Integer> inDegree = new HashMap<>();
            for (int id : lanes.keySet()) { adj.put(id, new ArrayList<>()); inDegree.put(id, 0); }
            for (Connection c : connections) {
                if(lanes.containsKey(c.from) && lanes.containsKey(c.to)) {
                    adj.get(c.from).add(c.to); inDegree.put(c.to, inDegree.get(c.to) + 1);
                }
            }

            Map<Integer, Integer> laneLayers = new HashMap<>();
            Queue<Integer> queue = new LinkedList<>();
            for (int id : lanes.keySet()) if (inDegree.get(id) == 0) { laneLayers.put(id, 0); queue.add(id); }

            int maxLayer = 0;
            while (!queue.isEmpty()) {
                int curr = queue.poll();
                int currLayer = laneLayers.get(curr);
                maxLayer = Math.max(maxLayer, currLayer);
                for (int neighbor : adj.get(curr)) {
                    if (!laneLayers.containsKey(neighbor) || laneLayers.get(neighbor) < currLayer + 1) {
                        laneLayers.put(neighbor, currLayer + 1); queue.add(neighbor);
                    }
                }
            }
            for(int id : lanes.keySet()) laneLayers.putIfAbsent(id, 0);

            Map<Integer, List<Integer>> lanesByLayer = new TreeMap<>();
            for (Map.Entry<Integer, Integer> entry : laneLayers.entrySet()) {
                lanesByLayer.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
            }

            int marginX = 100; int width = getWidth();
            int layerWidth = (width - 2 * marginX) / Math.max(1, maxLayer + 1);
            int height = getHeight();

            Map<Integer, Point> startPoints = new HashMap<>();
            Map<Integer, Point> endPoints = new HashMap<>();

            for (Map.Entry<Integer, List<Integer>> entry : lanesByLayer.entrySet()) {
                int layerIdx = entry.getKey();
                List<Integer> layerLanes = entry.getValue();
                Collections.sort(layerLanes);

                int laneHeight = height / (layerLanes.size() + 1);

                for (int i = 0; i < layerLanes.size(); i++) {
                    int id = layerLanes.get(i);
                    int xStart = marginX + (layerIdx * layerWidth);
                    int xEnd = marginX + ((layerIdx + 1) * layerWidth);
                    int yCenter = laneHeight * (i + 1);
                    Point pStart = new Point(xStart, yCenter);
                    Point pEnd;

                    List<Integer> targets = adj.get(id);
                    if (targets.isEmpty()) {
                        pEnd = new Point(xEnd, yCenter);
                    } else {
                        double targetYSum = 0; int targetCount = 0;
                        int minTargetLayer = Integer.MAX_VALUE;
                        for(int tID : targets) minTargetLayer = Math.min(minTargetLayer, laneLayers.get(tID));
                        for(int tID : targets) {
                            int tLayer = laneLayers.get(tID);
                            List<Integer> tLayerList = lanesByLayer.get(tLayer);
                            if(tLayerList != null) {
                                Collections.sort(tLayerList);
                                int tIndex = tLayerList.indexOf(tID);
                                int tHeight = height / (tLayerList.size() + 1);
                                targetYSum += (tHeight * (tIndex + 1));
                                targetCount++;
                            }
                        }
                        int targetYAvg = (targetCount > 0) ? (int)(targetYSum / targetCount) : yCenter;
                        int targetXStart = marginX + (minTargetLayer * layerWidth);
                        pEnd = new Point(targetXStart, targetYAvg);
                    }
                    startPoints.put(id, pStart); endPoints.put(id, pEnd);
                }
            }

            for(int id : lanes.keySet()) {
                List<Integer> feeders = new ArrayList<>();
                for(Map.Entry<Integer, List<Integer>> e : adj.entrySet()) if(e.getValue().contains(id)) feeders.add(e.getKey());
                if(!feeders.isEmpty()) {
                    double feedY = 0; for(int f : feeders) feedY += endPoints.get(f).y;
                    int newY = (int)(feedY / feeders.size());
                    Point oldStart = startPoints.get(id); startPoints.put(id, new Point(oldStart.x, newY));
                }
            }

            for(int id : lanes.keySet()) visualLanes.add(new VisualLane(id, startPoints.get(id), endPoints.get(id)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if(visualLanes.isEmpty()) {
                g2.setColor(Color.GRAY); g2.setFont(new Font("SansSerif", Font.BOLD, 20));
                g2.drawString("Waiting for Data...", getWidth()/2 - 100, getHeight()/2); return;
            }

            // Pass 1: Roads
            for(VisualLane vl : visualLanes) {
                LaneData data = lanes.get(vl.id);
                if(data == null) data = new LaneData(vl.id);
                drawLaneBase(g2, vl, data);
            }

            // Pass 2: Overlays
            for(VisualLane vl : visualLanes) {
                LaneData data = lanes.get(vl.id);
                if(data == null) data = new LaneData(vl.id);
                drawOverlays(g2, vl, data);
            }
        }

        private void drawLaneBase(Graphics2D g2, VisualLane vl, LaneData data) {
            g2.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(50, 50, 50));
            g2.draw(new Line2D.Float(vl.p1, vl.p2));

            int cap = data.capacity > 0 ? data.capacity : 50;
            float ratio = Math.min(1.0f, (float)data.vehicleCount / cap);
            Color fillColor = ratio > 0.8 ? Color.RED : (ratio > 0.5 ? Color.ORANGE : Color.GREEN);

            if (data.vehicleCount > 0) {
                double totalLen = vl.p1.distance(vl.p2);
                float fillLen = (float)(totalLen * ratio);
                float emptyLen = (float)(totalLen * 1.5);
                g2.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[]{fillLen, emptyLen}, 0));
                g2.setColor(fillColor);
                g2.draw(new Line2D.Float(vl.p1, vl.p2));
            }

            drawArrow(g2, vl.p1, vl.p2);
        }

        private void drawOverlays(Graphics2D g2, VisualLane vl, LaneData data) {
            int cap = data.capacity > 0 ? data.capacity : 50;

            Color semColor = Color.GRAY;
            if (data.semaphoreColor == 1) semColor = new Color(0, 255, 0);
            else if (data.semaphoreColor == 2) semColor = Color.ORANGE;
            else if (data.semaphoreColor == 3) semColor = Color.RED;

            double ang = Math.atan2(vl.p2.y - vl.p1.y, vl.p2.x - vl.p1.x);
            int backoff = 25;
            int sx = (int)(vl.p2.x - Math.cos(ang)*backoff);
            int sy = (int)(vl.p2.y - Math.sin(ang)*backoff);

            g2.setColor(semColor); g2.fillOval(sx - 12, sy - 12, 24, 24);
            g2.setStroke(new BasicStroke(2)); g2.setColor(Color.WHITE); g2.drawOval(sx - 12, sy - 12, 24, 24);

            double t = 0.6;
            int lx = (int)(vl.p1.x + t * (vl.p2.x - vl.p1.x));
            int ly = (int)(vl.p1.y + t * (vl.p2.y - vl.p1.y));

            drawBadge(g2, "L" + vl.id, lx, ly - 20, new Color(240, 240, 255));
            drawBadge(g2, "V: " + data.vehicleCount + "/" + cap, lx, ly + 20, new Color(200, 255, 255));

            // Offset timer 75% down
            double tTimer = 0.75;
            int tx = (int)(vl.p1.x + tTimer * (vl.p2.x - vl.p1.x));
            int ty = (int)(vl.p1.y + tTimer * (vl.p2.y - vl.p1.y));
            drawBadge(g2, (data.timeToChange/1000) + "s", tx, ty - 25, Color.YELLOW);
        }

        private void drawBadge(Graphics2D g2, String txt, int x, int y, Color bg) {
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(txt) + 12; int h = fm.getHeight() + 4;
            g2.setColor(bg); g2.fillRoundRect(x - w/2, y - h/2, w, h, 6, 6);
            g2.setColor(Color.BLACK); g2.setStroke(new BasicStroke(1)); g2.drawRoundRect(x - w/2, y - h/2, w, h, 6, 6);
            g2.setColor(Color.BLACK); g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString(txt, x - w/2 + 6, y + h/2 - 5);
        }

        private void drawArrow(Graphics2D g2, Point p1, Point p2) {
            double ang = Math.atan2(p2.y-p1.y, p2.x-p1.x);
            int dist = 45;
            int tx = p2.x - (int)(Math.cos(ang)*dist);
            int ty = p2.y - (int)(Math.sin(ang)*dist);
            Polygon p = new Polygon();
            p.addPoint(tx, ty);
            p.addPoint(tx-(int)(Math.cos(ang-0.5)*12), ty-(int)(Math.sin(ang-0.5)*12));
            p.addPoint(tx-(int)(Math.cos(ang+0.5)*12), ty-(int)(Math.sin(ang+0.5)*12));
            g2.setColor(Color.WHITE); g2.fillPolygon(p);
        }
    }

    private static class LaneData {
        int id; int vehicleCount=0; int semaphoreColor=3; int timeToChange=0; int capacity=50; boolean isTerminal;
        LaneData(int id) { this.id = id; }
    }
    private static class Connection {
        int from, to;
        Connection(int f, int t) { from=f; to=t; }
    }
    private static class VisualLane {
        int id; Point p1, p2;
        VisualLane(int id, Point p1, Point p2) { this.id=id; this.p1=p1; this.p2=p2; }
    }
    private static class MibNode {
        String suffix; boolean requiresIndex; boolean forceWalk;
        MibNode(String s, boolean r, boolean f) { suffix = s; requiresIndex = r; forceWalk = f; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TestAgent2().setVisible(true));
    }
}