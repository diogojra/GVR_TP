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


    /**
     * Lógica INTELIGENTE para vias geradoras (Troca Instantânea)
     */
    private void processGeneratorsGroup(List<GVRTPMIBG1.LaneEntryRow> generators, int stepMs) {

        GVRTPMIBG1.LaneEntryRow activeLane = null;

        // 1. Procurar quem está ativo (Verde ou Amarelo)
        for (GVRTPMIBG1.LaneEntryRow lane : generators) {
            int color = lane.getSemaphoreColor().getValue();
            if (color == GREEN || color == YELLOW) {
                activeLane = lane;
                break;
            }
        }

        // Se ninguém estiver ativo (ex: arranque do sistema), forçamos uma eleição imediata
        if (activeLane == null) {
            performElection(generators);
            return;
        }

        // 2. Processar a Via Ativa
        int timeToChange = activeLane.getTimeToChange().getValue();
        int color = activeLane.getSemaphoreColor().getValue();

        int newTime = timeToChange - stepMs;

        // Variáveis para sincronizar as vias em espera (Vermelhas)
        int waitTimeForOthers = 0;
        boolean cycleChanged = false; // Flag para saber se houve troca de sinal

        if (newTime > 0) {
            // A via continua no estado atual
            activeLane.setTimeToChange(new Integer32(newTime));

            // Calculamos quanto tempo as outras têm de esperar
            if (color == GREEN) {
                // Falta o resto do Verde + todo o Amarelo
                waitTimeForOthers = newTime + YELLOW_TIME_MS;
            } else { // YELLOW
                // Falta apenas o resto do Amarelo
                waitTimeForOthers = newTime;
            }

        } else {
            // O tempo acabou! Transição de estado.
            if (color == GREEN) {
                // Verde -> Amarelo
                activeLane.setSemaphoreColor(new Integer32(YELLOW));
                activeLane.setTimeToChange(new Integer32(YELLOW_TIME_MS));

                // As outras esperam agora apenas o tempo do amarelo
                waitTimeForOthers = YELLOW_TIME_MS;

            } else if (color == YELLOW) {
                // Amarelo -> Fim de Ciclo -> Troca Imediata

                // 1. A via atual fecha (Vermelho) e prepara-se para esperar o ciclo completo
                activeLane.setSemaphoreColor(new Integer32(RED));
                activeLane.setTimeToChange(new Integer32(RED_TIME_MS));

                // 2. Realizamos a eleição IMEDIATAMENTE para abrir a próxima
                performElection(generators);
                cycleChanged = true; // Marcamos que já tratámos de tudo na eleição
            }
        }

        // 3. Atualizar as Vias em Espera (apenas se não houve troca de ciclo agora mesmo)
        if (!cycleChanged) {
            for (GVRTPMIBG1.LaneEntryRow lane : generators) {
                if (lane == activeLane) continue; // Ignora a ativa

                // Força estado Vermelho
                if (lane.getSemaphoreColor().getValue() != RED) {
                    lane.setSemaphoreColor(new Integer32(RED));
                }

                // Sincroniza o tempo perfeitamente
                // Se a via 1 tem 5s de Verde, a via 2 mostra 8s (5+3) de Vermelho.
                lane.setTimeToChange(new Integer32(waitTimeForOthers));
            }
        }
    }

    /**
     * Metodo auxiliar para escolher a próxima via Verde
     */
    private void performElection(List<GVRTPMIBG1.LaneEntryRow> generators) {
        GVRTPMIBG1.LaneEntryRow bestCandidate = null;
        double maxOccupancy = -1.0;

        // Calcular quem precisa mais
        for (GVRTPMIBG1.LaneEntryRow lane : generators) {
            // Ignora quem acabou de fechar (ainda está marcado como Amarelo ou acabou de virar Vermelho na lógica acima)
            // Mas num sistema de 2 vias, a que acabou de fechar vai ter ocupação alta se não esvaziou,
            // contudo, a lógica do semáforo deve dar oportunidade à outra.
            // (Neste código simplificado, quem tem mais % ganha, mesmo se for a mesma,
            // mas assumimos que ao ficar verde ela esvaziou e a % baixou).

            int veiculos = (int) lane.getVehicleCount().getValue();
            int capacidade = lane.getCapacity().getValue();
            double occupancy = (capacidade > 0) ? (double) veiculos / capacidade : 0.0;

            if (occupancy > maxOccupancy) {
                maxOccupancy = occupancy;
                bestCandidate = lane;
            } else if (occupancy == maxOccupancy) {
                // Desempate
                if (bestCandidate != null && veiculos > bestCandidate.getVehicleCount().getValue()) {
                    bestCandidate = lane;
                }
            }
        }

        // Se, por acaso, a lista vier vazia ou null, pega o primeiro
        if (bestCandidate == null && !generators.isEmpty()) {
            bestCandidate = generators.get(0);
        }

        // Aplicar as cores pós-eleição
        if (bestCandidate != null) {
            // O vencedor fica VERDE
            bestCandidate.setSemaphoreColor(new Integer32(GREEN));
            bestCandidate.setTimeToChange(new Integer32(GREEN_TIME_MS));

            // Os perdedores ficam VERMELHOS com o tempo total (G+Y)
            for (GVRTPMIBG1.LaneEntryRow lane : generators) {
                if (lane != bestCandidate) {
                    lane.setSemaphoreColor(new Integer32(RED));
                    lane.setTimeToChange(new Integer32(RED_TIME_MS));
                }
            }
        }
    }
}