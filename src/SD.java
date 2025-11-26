import org.snmp4j.smi.Integer32;
import java.util.*;

public class SD implements Runnable {

    private final GVRTPMIBG1 mib;

    // Definição das cores
    private static final int GREEN = 1;
    private static final int YELLOW = 2;
    private static final int RED = 3;

    // --- CONFIGURAÇÃO DE TEMPOS ---

    // 1. O Amarelo é fixo (ex: 3 segundos)
    private static final int YELLOW_TIME_MS = 3000;

    // 2. O Verde é definido aqui (ex: 8 segundos)
    private static final int GREEN_TIME_MS = 8000;

    // 3. O Vermelho é CALCULADO: Soma do Verde + Amarelo (ex: 8000 + 3000 = 11000ms)
    // Isto garante que o vermelho dura exatamente o ciclo da via oposta.
    private static final int RED_TIME_MS = GREEN_TIME_MS + YELLOW_TIME_MS;


    public SD(GVRTPMIBG1 mib) {
        this.mib = mib;
    }

    @Override
    public void run() {
        while (true) {
            try {
                runOneStep();

                int stepMs = mib.getSysStepTime().getValue().toInt();
                if (stepMs <= 0) stepMs = 1000;
                Thread.sleep(stepMs);

            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Realiza um ciclo de cálculo dos semáforos
     */
    private void runOneStep() {
        var laneTable = mib.getLaneEntry();
        var laneModel = laneTable.getModel();

        int stepMs = mib.getSysStepTime().getValue().toInt();
        if (stepMs <= 0) stepMs = 1000;

        List<GVRTPMIBG1.LaneEntryRow> generators = new ArrayList<>();

        for (Iterator<?> it = laneModel.iterator(); it.hasNext(); ) {
            GVRTPMIBG1.LaneEntryRow lane = (GVRTPMIBG1.LaneEntryRow) it.next();

            // Verifica se é via geradora (RGT > 0)
            int rgt = lane.getRgt().getValue();

            if (rgt > 0) {
                generators.add(lane);
            } else {
                // VIAS NORMAIS
                processNormalLane(lane, stepMs);
            }
        }

        // Processar o grupo de vias geradoras
        if (!generators.isEmpty()) {
            processGeneratorsGroup(generators, stepMs);
        }
    }

    /**
     * Lógica para vias normais
     */
    private void processNormalLane(GVRTPMIBG1.LaneEntryRow lane, int stepMs) {
        int color = lane.getSemaphoreColor().getValue();
        int timeToChange = lane.getTimeToChange().getValue();

        int newTime = timeToChange - stepMs;

        if (newTime > 0) {
            lane.setTimeToChange(new Integer32(newTime));
            return;
        }

        int nextColor;
        int nextTime;

        switch (color) {
            case GREEN:
                nextColor = YELLOW;
                nextTime = YELLOW_TIME_MS; // Tempo Fixo
                break;
            case YELLOW:
                nextColor = RED;
                nextTime = RED_TIME_MS;    // Tempo Calculado (G + Y)
                break;
            case RED:
            default:
                nextColor = GREEN;
                nextTime = GREEN_TIME_MS;
                break;
        }

        lane.setSemaphoreColor(new Integer32(nextColor));
        lane.setTimeToChange(new Integer32(nextTime));
    }

    /** Lógica INTELIGENTE para vias geradoras */
    /**
     * Lógica INTELIGENTE para vias geradoras
     */
    /**
     * Lógica INTELIGENTE para vias geradoras
     */
    private void processGeneratorsGroup(List<GVRTPMIBG1.LaneEntryRow> generators, int stepMs) {

        GVRTPMIBG1.LaneEntryRow activeLane = null;

        // 1. Identificar se alguém tem o sinal aberto (Verde ou Amarelo)
        for (GVRTPMIBG1.LaneEntryRow lane : generators) {
            int color = lane.getSemaphoreColor().getValue();
            if (color == GREEN || color == YELLOW) {
                activeLane = lane;
                break;
            }
        }

        // --- CENÁRIO A: Existe uma via ativa (o sistema está ocupado) ---
        if (activeLane != null) {

            // 1. Atualizar a Via Ativa (Verde/Amarelo)
            int timeToChange = activeLane.getTimeToChange().getValue();
            int color = activeLane.getSemaphoreColor().getValue();
            int newTime = timeToChange - stepMs;

            if (newTime > 0) {
                activeLane.setTimeToChange(new Integer32(newTime));
            } else {
                // Transição de estado da via ativa
                if (color == GREEN) {
                    activeLane.setSemaphoreColor(new Integer32(YELLOW));
                    activeLane.setTimeToChange(new Integer32(YELLOW_TIME_MS));
                } else if (color == YELLOW) {
                    activeLane.setSemaphoreColor(new Integer32(RED));
                    // Agora entra em período de segurança (Vermelho)
                    activeLane.setTimeToChange(new Integer32(RED_TIME_MS));
                }
            }

            // 2. Atualizar as Outras Vias (Vermelhas) - Para não parecerem congeladas!
            for (GVRTPMIBG1.LaneEntryRow lane : generators) {
                if (lane == activeLane) continue; // Pula a ativa

                // Garante que está vermelha
                int laneColor = lane.getSemaphoreColor().getValue();
                if (laneColor != RED) {
                    lane.setSemaphoreColor(new Integer32(RED));
                }

                // Decrementa o tempo de espera (efeito visual de contagem)
                int t = lane.getTimeToChange().getValue();
                if (t > 0) {
                    int nextT = t - stepMs;
                    if (nextT < 0) nextT = 0; // Não deixa ir negativo
                    lane.setTimeToChange(new Integer32(nextT));
                }
                // Se chegar a 0, fica em 0 à espera da sua vez (comportamento correto)
            }

        }
        // --- CENÁRIO B: Ninguém ativo (Tudo Vermelho -> Eleição ou Segurança) ---
        else {

            // 1. Verificar tempos de segurança ("All Red")
            boolean waitingForSafetyTime = false;

            for (GVRTPMIBG1.LaneEntryRow lane : generators) {
                int t = lane.getTimeToChange().getValue();

                // Se alguma via ainda tem tempo de vermelho > 0, decrementamos e esperamos
                if (t > 0) {
                    int nextT = t - stepMs;
                    if (nextT < 0) nextT = 0;
                    lane.setTimeToChange(new Integer32(nextT));

                    if (nextT > 0) waitingForSafetyTime = true;
                }
            }

            // Se ainda estamos no tempo de segurança de alguém, não fazemos eleição
            if (waitingForSafetyTime) return;

            // --- ELEIÇÃO: Escolher a próxima via a abrir ---
            GVRTPMIBG1.LaneEntryRow bestCandidate = null;
            double maxOccupancy = -1.0;

            for (GVRTPMIBG1.LaneEntryRow lane : generators) {
                int veiculos = (int) lane.getVehicleCount().getValue();
                int capacidade = lane.getCapacity().getValue();

                double occupancy = 0.0;
                if (capacidade > 0) {
                    occupancy = (double) veiculos / capacidade;
                }

                if (occupancy > maxOccupancy) {
                    maxOccupancy = occupancy;
                    bestCandidate = lane;
                } else if (occupancy == maxOccupancy) {
                    // Desempate: quem tiver mais veículos absolutos
                    if (bestCandidate != null && veiculos > bestCandidate.getVehicleCount().getValue()) {
                        bestCandidate = lane;
                    }
                }
            }

            // Vencedor ganha o Verde
            if (bestCandidate != null) {
                bestCandidate.setSemaphoreColor(new Integer32(GREEN));
                bestCandidate.setTimeToChange(new Integer32(GREEN_TIME_MS));

                // As outras reiniciam o seu timer de "paciência" (opcional, ou mantêm-se a 0)
                // Aqui mantemos a 0 ou resetamos para RED_TIME_MS se quisermos simular um ciclo completo.
                // Mas deixar a 0 indica "Estou pronta".
            }
        }
    }
}