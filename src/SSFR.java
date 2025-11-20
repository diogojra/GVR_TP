import java.util.*;

import org.snmp4j.agent.mo.MOTable;
import org.snmp4j.agent.mo.MOTableModel;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.TimeTicks;

/**
 * SSFR reformulado:
 * - encontra vias terminais (vias sem destinos),
 * - escoa completamente veículos nas terminais,
 * - processa níveis ascendentes (predecessores) e propaga tráfego conforme semáforo + fluxoDest,
 * - injeta RGT nas vias origem no fim.
 */
public class SSFR implements Runnable {

    private final GVRTPMIBG1 mib;

    public SSFR(GVRTPMIBG1 mib) {
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

    private void runOneStep() {
        // Carregar tabelas
        MOTable<?, ?, ?> laneTable = mib.getLaneEntry();
        MOTableModel<?> laneModel = laneTable.getModel();

        MOTable<?, ?, ?> viaTable = mib.getViaDestEntry();
        MOTableModel<?> viaModel = viaTable.getModel();

        // 1) Mapear lanes: id -> LaneEntryRow
        Map<Integer, GVRTPMIBG1.LaneEntryRow> laneMap = new HashMap<>();
        for (Iterator<?> it = laneModel.iterator(); it.hasNext(); ) {
            GVRTPMIBG1.LaneEntryRow row = (GVRTPMIBG1.LaneEntryRow) it.next();
            int laneId = row.getIndex().last();
            laneMap.put(laneId, row);
        }

        // 2) Construir grafo de destinos (orig -> list(destRow)) e grafo reverso (dest -> list(orig))
        Map<Integer, List<GVRTPMIBG1.ViaDestEntryRow>> destMap = new HashMap<>();
        Map<Integer, List<Integer>> reverseMap = new HashMap<>(); // destId -> list(origId)
        for (Iterator<?> it = viaModel.iterator(); it.hasNext(); ) {
            GVRTPMIBG1.ViaDestEntryRow row = (GVRTPMIBG1.ViaDestEntryRow) it.next();
            int origLaneId = row.getIndex().get(1);
            destMap.computeIfAbsent(origLaneId, k -> new ArrayList<>()).add(row);

            int destId = row.getDestVia().getValue();
            reverseMap.computeIfAbsent(destId, k -> new ArrayList<>()).add(origLaneId);
        }

        // 3) Identificar vias terminais: lanes que NÃO têm entradas em destMap (ou seja, sem destinos)
        Set<Integer> terminals = new HashSet<>();
        for (Integer laneId : laneMap.keySet()) {
            if (!destMap.containsKey(laneId) || destMap.get(laneId).isEmpty()) {
                terminals.add(laneId);
            }
        }

        // 4) Construir níveis ascendentes a partir das terminais (level 0 = terminais)
        //    level -> set of laneIds
        Map<Integer, Set<Integer>> levels = new LinkedHashMap<>();
        Set<Integer> visited = new HashSet<>(); // para evitar processar um nó várias vezes (evita ciclos infinitos)
        Queue<Integer> q = new ArrayDeque<>();

        // inicializa nível 0
        levels.put(0, new LinkedHashSet<>(terminals));
        visited.addAll(terminals);
        q.addAll(terminals);

        // BFS reverso: pop terminal, adicionar seus predecessores (reverseMap) ao nível+1 se não visitados
        Map<Integer, Integer> nodeLevel = new HashMap<>(); // laneId -> level
        for (Integer t : terminals) nodeLevel.put(t, 0);

        while (!q.isEmpty()) {
            int current = q.poll();
            int curLevel = nodeLevel.get(current);
            List<Integer> preds = reverseMap.getOrDefault(current, Collections.emptyList());
            for (Integer pred : preds) {
                if (!visited.contains(pred)) {
                    int nextLevel = curLevel + 1;
                    levels.computeIfAbsent(nextLevel, k -> new LinkedHashSet<>()).add(pred);
                    visited.add(pred);
                    nodeLevel.put(pred, nextLevel);
                    q.add(pred);
                } else {
                    // se já visitado, mas podemos querer garantir que nodeLevel guarda o menor nível
                    int existing = nodeLevel.getOrDefault(pred, Integer.MAX_VALUE);
                    if (curLevel + 1 < existing) {
                        // mover para nível inferior (mais próximo das terminais)
                        levels.get(existing).remove(pred);
                        int newLevel = curLevel + 1;
                        levels.computeIfAbsent(newLevel, k -> new LinkedHashSet<>()).add(pred);
                        nodeLevel.put(pred, newLevel);
                    }
                }
            }
        }

        // Observação: algumas lanes podem não estar conectadas às terminais — decidir o que fazer:
        // vamos considerar também lanes isoladas (não alcançáveis pelas terminais) — adicioná-las como níveis superiores isolados
        for (Integer laneId : laneMap.keySet()) {
            if (!nodeLevel.containsKey(laneId)) {
                // atribui um nível alto (por exemplo, maxLevel+1) para processar depois — isto garante que todas as lanes são consideradas
                int maxLevel = levels.isEmpty() ? 0 : Collections.max(levels.keySet());
                int assigned = maxLevel + 1;
                levels.computeIfAbsent(assigned, k -> new LinkedHashSet<>()).add(laneId);
                nodeLevel.put(laneId, assigned);
            }
        }

        // 5) PROCESSAMENTO: do fundo (terminais) para cima

        // 5a) Escoar totalmente as vias terminais (level 0)
        Set<Integer> level0 = levels.getOrDefault(0, Collections.emptySet());
        for (Integer termId : level0) {
            GVRTPMIBG1.LaneEntryRow termLane = laneMap.get(termId);
            if (termLane == null) continue;
            int vehicles = (int) termLane.getVehicleCount().getValue();
            if (vehicles <= 0) continue;
            // escoar tudo
            termLane.setVehicleCount(new Gauge32(0));
            // atualizar totalVeh - assumimos que totalVeh conta os veículos que saíram por esta via
            int totalVeh = termLane.getTotalVeh().getValue();
            termLane.setTotalVeh(new Integer32(totalVeh + vehicles));
        }

        // 5b) Para cada nível > 0, do nível baixo para o alto:
        // processamos níveis em ordem crescente (1,2,3,...), cada nível envia tráfego para os seus destinos
        List<Integer> levelKeys = new ArrayList<>(levels.keySet());
        Collections.sort(levelKeys); // já devem estar em ordem, mas garantimos

        for (int level : levelKeys) {
            if (level == 0) continue; // já processado
            Set<Integer> nodes = levels.getOrDefault(level, Collections.emptySet());
            if (nodes == null) continue;

            for (Integer laneId : nodes) {
                GVRTPMIBG1.LaneEntryRow lane = laneMap.get(laneId);
                if (lane == null) continue;

                // Somente se tiver destinos configurados (se não tiver, nada faz)
                List<GVRTPMIBG1.ViaDestEntryRow> outs = destMap.get(laneId);
                if (outs == null || outs.isEmpty()) continue;

                // verifica cor do semáforo: 1=green,2=yellow,3=red
                int color = lane.getSemaphoreColor().getValue();
                boolean open = (color == 1 || color == 2);
                if (!open) {
                    // semáforo fechado -> não envia tráfego
                    continue;
                }

                int originVehicles = (int) lane.getVehicleCount().getValue();
                if (originVehicles <= 0) continue;

                // calcular soma de fluxos válidos
                int totalFluxo = 0;
                for (GVRTPMIBG1.ViaDestEntryRow d : outs) {
                    int fluxo = d.getFluxoDest().getValue();
                    if (fluxo > 0) totalFluxo += fluxo;
                }
                if (totalFluxo <= 0) continue;

                int remaining = originVehicles;
                int movedTotal = 0;

                // distribuir para cada destino proporcionalmente ao fluxo
                for (int i = 0; i < outs.size(); i++) {
                    GVRTPMIBG1.ViaDestEntryRow d = outs.get(i);
                    int destId = d.getDestVia().getValue();
                    GVRTPMIBG1.LaneEntryRow destLane = laneMap.get(destId);
                    if (destLane == null) {
                        // destino inválido - ignora
                        continue;
                    }

                    int destVehicles = (int) destLane.getVehicleCount().getValue();
                    int destCapacity = destLane.getCapacity().getValue();
                    int freeSpace = destCapacity - destVehicles;
                    if (freeSpace <= 0) continue;

                    int fluxo = d.getFluxoDest().getValue();
                    if (fluxo <= 0) continue;

                    int toMove;
                    if (i < outs.size() - 1) {
                        int desired = originVehicles * fluxo / totalFluxo;
                        if (desired <= 0) desired = 1;
                        toMove = Math.min(desired, Math.min(freeSpace, remaining));
                    } else {
                        // último destino recebe o que resta
                        toMove = Math.min(freeSpace, remaining);
                    }

                    if (toMove <= 0) continue;

                    // atualiza destino
                    destLane.setVehicleCount(new Gauge32(destVehicles + toMove));

                    remaining -= toMove;
                    movedTotal += toMove;

                    if (remaining <= 0) break;
                }

                // atualizar origem (reduzir veículos que saíram) e totalVeh da origem
                if (movedTotal > 0) {
                    lane.setVehicleCount(new Gauge32(originVehicles - movedTotal));
                    int totalVeh = lane.getTotalVeh().getValue();
                    lane.setTotalVeh(new Integer32(totalVeh + movedTotal));
                }
            }
        }

        // 6) Finalmente, injetar RGT nas vias origem (aquelas com rgt>0)
        //    Observação: "vias origem" podem ser todas as lanes ou só as identificadas com RGT>0. Aqui usamos rgt>0.
        for (GVRTPMIBG1.LaneEntryRow lane : laneMap.values()) {
            int rgt = lane.getRgt().getValue();
            if (rgt <= 0) continue;

            int vehicleCount = (int) lane.getVehicleCount().getValue();
            int capacity = lane.getCapacity().getValue();
            if (vehicleCount >= capacity) continue;

            int freeSpace = capacity - vehicleCount;
            int toAdd = Math.min(rgt, freeSpace);
            if (toAdd > 0) {
                lane.setVehicleCount(new Gauge32(vehicleCount + toAdd));
                int totalVeh = lane.getTotalVeh().getValue();
                lane.setTotalVeh(new Integer32(totalVeh + toAdd));
            }
        }

        // 7) Atualizar sysUpTime (TimeTicks, 1/100s)
        int stepMs = mib.getSysStepTime().getValue().toInt();
        if (stepMs <= 0) stepMs = 1000;
        long deltaTicks = stepMs / 10;
        TimeTicks currentTicks = mib.getSysUpTime().getValue();
        long newTicks = currentTicks.toLong() + deltaTicks;
        mib.getSysUpTime().setValue(new TimeTicks(newTicks));
    }
}