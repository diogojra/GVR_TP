import org.snmp4j.smi.Integer32;
import java.util.*;

public class SD implements Runnable {

    private final GVRTPMIBG1 mib;

    // Definição das cores
    private static final int GREEN = 1;
    private static final int YELLOW = 2;
    private static final int RED = 3;

    // --- CONFIGURAÇÃO DE TEMPOS ---
    private static final int YELLOW_TIME_MS = 3000;
    private static final int GREEN_TIME_MS = 8000;

    // O Vermelho é calculado (G + Y)
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
     * Ciclo principal
     */
    private void runOneStep() {
        var laneTable = mib.getLaneEntry();
        var laneModel = laneTable.getModel();

        // Precisamos da tabela de conexões para saber quem intercepta quem
        var viaDestTable = mib.getViaDestEntry();
        var viaDestModel = viaDestTable.getModel();

        int stepMs = mib.getSysStepTime().getValue().toInt();
        if (stepMs <= 0) stepMs = 1000;

        // 1. Mapear Topologia: Qual Lane vai para Qual Destino?
        // Map<SourceLaneID, DestinationLaneID>
        Map<Integer, Integer> laneToDestMap = new HashMap<>();

        for (Iterator<?> it = viaDestModel.iterator(); it.hasNext(); ) {
            GVRTPMIBG1.ViaDestEntryRow link = (GVRTPMIBG1.ViaDestEntryRow) it.next();
            int sourceId = link.getIndex().get(1); // O índice da tabela é composto, parte 1 é a lane origem
            int destId = link.getDestVia().getValue();

            // Assumimos que a lane geradora principal conflita no seu PRIMEIRO destino encontrado.
            // (Em cenários simples, uma lane geradora entra em apenas 1 via ou interseção imediata)
            laneToDestMap.putIfAbsent(sourceId, destId);
        }

        // 2. Agrupar Geradores por Destino Comum (Interseção)
        // Map<IntersectionID, List<LaneObj>>
        Map<Integer, List<GVRTPMIBG1.LaneEntryRow>> conflictGroups = new HashMap<>();

        // Lista auxiliar para lanes que não são geradoras
        List<GVRTPMIBG1.LaneEntryRow> normalLanes = new ArrayList<>();

        for (Iterator<?> it = laneModel.iterator(); it.hasNext(); ) {
            GVRTPMIBG1.LaneEntryRow lane = (GVRTPMIBG1.LaneEntryRow) it.next();
            int rgt = lane.getRgt().getValue();

            if (rgt > 0) {
                // É geradora. Descobre para onde ela vai.
                int laneId = lane.getIndex().last();
                Integer destination = laneToDestMap.get(laneId);

                if (destination != null) {
                    // Adiciona ao grupo daquela interseção (destino)
                    conflictGroups
                            .computeIfAbsent(destination, k -> new ArrayList<>())
                            .add(lane);
                } else {
                    // Se a lane geradora não tem destino configurado na ViaDestEntry,
                    // ela é isolada. Criamos um grupo só para ela (ID negativo para não colidir).
                    conflictGroups
                            .computeIfAbsent(-laneId, k -> new ArrayList<>())
                            .add(lane);
                }
            } else {
                normalLanes.add(lane);
            }
        }

        // 3. Processar Vias Normais (sem lógica de conflito)
        for (GVRTPMIBG1.LaneEntryRow lane : normalLanes) {
            processNormalLane(lane, stepMs);
        }

        // 4. Processar Grupos de Conflito (Interseções) INDEPENDENTEMENTE
        for (List<GVRTPMIBG1.LaneEntryRow> group : conflictGroups.values()) {
            processGeneratorsGroup(group, stepMs);
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
                nextTime = YELLOW_TIME_MS;
                break;
            case YELLOW:
                nextColor = RED;
                nextTime = RED_TIME_MS;
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
     * Lógica INTELIGENTE para um GRUPO ESPECÍFICO de geradores
     * Se o grupo tiver apenas 1 lane, ela ficará sempre Verde (ou ciclará sozinha).
     * Se tiver 3 lanes, elas disputarão entre si.
     */
    private void processGeneratorsGroup(List<GVRTPMIBG1.LaneEntryRow> generators, int stepMs) {

        if (generators.isEmpty()) return;

        // Se houver apenas 1 geradora neste grupo (isolada), forçamos Verde Constante
        // para otimizar fluxo, ou deixamos ciclar se preferir realismo visual.
        // Aqui: Deixamos ela ganhar a eleição sempre, mantendo-a verde.

        GVRTPMIBG1.LaneEntryRow activeLane = null;

        // 1. Procurar quem está ativo neste grupo
        for (GVRTPMIBG1.LaneEntryRow lane : generators) {
            int color = lane.getSemaphoreColor().getValue();
            if (color == GREEN || color == YELLOW) {
                activeLane = lane;
                break;
            }
        }

        // Caso especial: Inicialização ou falha anterior -> Força eleição
        if (activeLane == null) {
            performElection(generators);
            return;
        }

        // 2. Processar a Via Ativa
        int timeToChange = activeLane.getTimeToChange().getValue();
        int color = activeLane.getSemaphoreColor().getValue();
        int newTime = timeToChange - stepMs;

        int waitTimeForOthers = 0;
        boolean cycleChanged = false;

        if (newTime > 0) {
            // Continua no estado atual
            activeLane.setTimeToChange(new Integer32(newTime));

            if (color == GREEN) {
                waitTimeForOthers = newTime + YELLOW_TIME_MS;
            } else { // YELLOW
                waitTimeForOthers = newTime;
            }
        } else {
            // O tempo acabou! Transição.
            if (color == GREEN) {
                // Verde -> Amarelo
                activeLane.setSemaphoreColor(new Integer32(YELLOW));
                activeLane.setTimeToChange(new Integer32(YELLOW_TIME_MS));
                waitTimeForOthers = YELLOW_TIME_MS;
            } else if (color == YELLOW) {
                // Amarelo -> Fim -> Vermelho
                activeLane.setSemaphoreColor(new Integer32(RED));
                activeLane.setTimeToChange(new Integer32(RED_TIME_MS));

                // Eleição IMEDIATA para abrir a próxima
                performElection(generators);
                cycleChanged = true;
            }
        }

        // 3. Atualizar as Vias em Espera (apenas se não houve troca de ciclo agora)
        if (!cycleChanged) {
            for (GVRTPMIBG1.LaneEntryRow lane : generators) {
                if (lane == activeLane) continue;

                // Força Vermelho nas outras
                if (lane.getSemaphoreColor().getValue() != RED) {
                    lane.setSemaphoreColor(new Integer32(RED));
                }

                // Sincroniza tempo de espera
                lane.setTimeToChange(new Integer32(waitTimeForOthers));
            }
        }
    }

    private void performElection(List<GVRTPMIBG1.LaneEntryRow> generators) {
        GVRTPMIBG1.LaneEntryRow bestCandidate = null;
        double maxOccupancy = -1.0;

        // Calcular quem precisa mais
        for (GVRTPMIBG1.LaneEntryRow lane : generators) {
            int veiculos = (int) lane.getVehicleCount().getValue();
            int capacidade = lane.getCapacity().getValue();
            double occupancy = (capacidade > 0) ? (double) veiculos / capacidade : 0.0;

            if (occupancy > maxOccupancy) {
                maxOccupancy = occupancy;
                bestCandidate = lane;
            } else if (occupancy == maxOccupancy) {
                // Desempate por quantidade absoluta
                if (bestCandidate != null && veiculos > bestCandidate.getVehicleCount().getValue()) {
                    bestCandidate = lane;
                }
            }
        }

        // Se der null (ex: lista vazia), pega o primeiro
        if (bestCandidate == null && !generators.isEmpty()) {
            bestCandidate = generators.get(0);
        }

        if (bestCandidate != null) {
            // O vencedor fica VERDE
            bestCandidate.setSemaphoreColor(new Integer32(GREEN));
            bestCandidate.setTimeToChange(new Integer32(GREEN_TIME_MS));

            // Os perdedores (do mesmo grupo) ficam VERMELHOS
            for (GVRTPMIBG1.LaneEntryRow lane : generators) {
                if (lane != bestCandidate) {
                    lane.setSemaphoreColor(new Integer32(RED));
                    lane.setTimeToChange(new Integer32(RED_TIME_MS));
                }
            }
        }
    }
}