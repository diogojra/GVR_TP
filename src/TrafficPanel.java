import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.Integer32;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TrafficPanel extends JPanel {

    private final GVRTPMIBG1 mib;

    // O mapeamento de layout é um placeholder; ajuste se a sua topologia for diferente
    public TrafficPanel(GVRTPMIBG1 mib) {
        this.mib = mib;
        setPreferredSize(new Dimension(550, 450));
        setBackground(new Color(30, 30, 30)); // Fundo escuro
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int laneWidth = 400;

        // 1. Desenhar o Nó Central (Onde as faixas se ligam)
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect((width - laneWidth) / 2, 0, laneWidth, getHeight());

        // 2. Iterar sobre os dados da MIB (laneEntry)
        var laneTable = mib.getLaneEntry();
        var laneModel = laneTable.getModel();

        int yOffset = 50;
        int laneHeight = 30;

        for (Iterator<?> it = laneModel.iterator(); it.hasNext(); ) {
            GVRTPMIBG1.LaneEntryRow lane = (GVRTPMIBG1.LaneEntryRow) it.next();

            int laneID = lane.getIndex().get(0);
            int vehicleCount = lane.getVehicleCount().toInt();
            int semaphoreColor = lane.getSemaphoreColor().toInt();
            int capacity = lane.getCapacity().toInt();
            int timeToChange = lane.getTimeToChange().toInt();

            // === Determinar Posição de Desenho ===
            int x = 50;
            int y = yOffset;

            // === 3. Desenhar a Faixa (a estrada) ===
            g2d.setColor(new Color(100, 100, 100)); // Cor da faixa
            g2d.fillRect(x, y, laneWidth, laneHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(x, y, laneWidth, laneHeight);

            // === 4. Desenhar os Veículos (Barra de Tráfego) ===
            double fillRatio = (double) vehicleCount / capacity;
            int fillLength = (int) (laneWidth * fillRatio);

            Color trafficColor = Color.GREEN.darker();
            if (fillRatio > 0.8) trafficColor = Color.RED.darker();
            else if (fillRatio > 0.5) trafficColor = Color.ORANGE.darker();

            g2d.setColor(trafficColor);
            g2d.fillRect(x, y, fillLength, laneHeight);

            // === 5. Desenhar o Semáforo ===
            Color semaColor = Color.GRAY;
            switch (semaphoreColor) {
                case 1: // GREEN
                    semaColor = Color.GREEN;
                    break;
                case 2: // YELLOW
                    semaColor = Color.YELLOW;
                    break;
                case 3: // RED
                    semaColor = Color.RED;
                    break;
            }

            int semaX = x + laneWidth + 10;
            int semaSize = 25;

            g2d.setColor(semaColor);
            g2d.fillOval(semaX, y + (laneHeight - semaSize) / 2, semaSize, semaSize);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(semaX, y + (laneHeight - semaSize) / 2, semaSize, semaSize);

            // Desenhar texto do tempo
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2d.drawString(String.valueOf(timeToChange / 1000), semaX + 7, y + 27); // Tempo em segundos

            // === 6. Mostrar Dados da MIB ===
            g2d.setColor(Color.CYAN);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
            String data = String.format("Lane %d: Veículos: %d/%d (%.0f%%)",
                    laneID, vehicleCount, capacity, fillRatio * 100);
            g2d.drawString(data, x, y - 5);

            yOffset += 80; // Espaçamento para a próxima faixa
        }
    }
}