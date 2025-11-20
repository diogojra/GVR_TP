import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;

public class SSFRVisualizer extends JFrame implements ActionListener {

    private final TrafficPanel trafficPanel;
    private final Timer timer;

    public SSFRVisualizer(GVRTPMIBG1 mib) {
        super("Simulação de Tráfego (SSFR MIB)");

        this.trafficPanel = new TrafficPanel(mib);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(trafficPanel);
        pack();
        setLocationRelativeTo(null);

        // Timer para atualizar a visualização a cada 100ms (10 vezes por segundo)
        this.timer = new Timer(100, this);
        this.timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // A cada tick do timer, força o painel a redesenhar
        trafficPanel.repaint();
    }

    public void start() {
        setVisible(true);
    }
}