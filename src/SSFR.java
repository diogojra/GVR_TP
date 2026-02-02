import java.util.*;

import org.snmp4j.agent.mo.MOTable;
import org.snmp4j.agent.mo.MOTableModel;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.TimeTicks;

/**
 * SSFR:
 * - finds terminal lanes ( lanes without destination ),
 * - drains the vehicles in the terminal lane
 * - processes the level before and depending on the traffic light and destinationflux it sends the vehicles,
 * - injects the vehicles in the rgt lanes.
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

                // Dynamic simulation speed: fetch the step delay from the MIB (currently its at 1s)
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
        // Fetch current mib state
        MOTable<?, ?, ?> laneTable = mib.getLaneEntry();
        MOTableModel<?> laneModel = laneTable.getModel();
        MOTable<?, ?, ?> viaTable = mib.getViaDestEntry();
        MOTableModel<?> viaModel = viaTable.getModel();

        // Map for a direct lookup for lanes
        Map<Integer, GVRTPMIBG1.LaneEntryRow> laneMap = new HashMap<>();
        for (Iterator<?> it = laneModel.iterator(); it.hasNext(); ) {
            GVRTPMIBG1.LaneEntryRow row = (GVRTPMIBG1.LaneEntryRow) it.next();
            int laneId = row.getIndex().last();
            laneMap.put(laneId, row);
        }

        // Build the Network  Graph (orig -> list(destRow)) e reverse (dest -> list(orig))
        Map<Integer, List<GVRTPMIBG1.ViaDestEntryRow>> destMap = new HashMap<>();
        Map<Integer, List<Integer>> reverseMap = new HashMap<>();
        for (Iterator<?> it = viaModel.iterator(); it.hasNext(); ) {
            GVRTPMIBG1.ViaDestEntryRow row = (GVRTPMIBG1.ViaDestEntryRow) it.next();
            int origLaneId = row.getIndex().get(1);
            destMap.computeIfAbsent(origLaneId, k -> new ArrayList<>()).add(row);

            int destId = row.getDestVia().getValue();
            reverseMap.computeIfAbsent(destId, k -> new ArrayList<>()).add(origLaneId);
        }

        // Identify terminal lane
        Set<Integer> terminals = new HashSet<>();
        for (GVRTPMIBG1.LaneEntryRow lane : laneMap.values()) {
            int isTerminal = lane.getViaterminal().getValue();
            if (isTerminal == 1) {
                int laneId = lane.getIndex().last();
                terminals.add(laneId);
            }
        }

        // Build ascending starting from the end (level 0 = terminals)
        // We assign a "Level" to each lane based on its distance from a terminal.
        // Level 0 = Terminals
        // Level 1 = Lanes directly connected to terminals
        // Level 2 = Lanes connected to Level 1, etc.
        Map<Integer, Set<Integer>> levels = new LinkedHashMap<>();
        Set<Integer> visited = new HashSet<>(); // To avoid reprocessing a node several times (infinite loops)
        Queue<Integer> q = new ArrayDeque<>();

        // Inicializes level 0
        levels.put(0, new LinkedHashSet<>(terminals));
        visited.addAll(terminals);
        q.addAll(terminals);

        // Reverse BFS : pop terminal, add the ones before him (reverseMap) to level+1 if they weren't processed yet
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
                    // Optimization: If we find a shorter path to an exit, update the level
                    int existing = nodeLevel.getOrDefault(pred, Integer.MAX_VALUE);
                    if (curLevel + 1 < existing) {
                        // move to lower level(closer to terminal)
                        levels.get(existing).remove(pred);
                        int newLevel = curLevel + 1;
                        levels.computeIfAbsent(newLevel, k -> new LinkedHashSet<>()).add(pred);
                        nodeLevel.put(pred, newLevel);
                    }
                }
            }
        }


        // Considering solo lanes (not reachable by the terminals) — add them like superior levels isolated
        for (Integer laneId : laneMap.keySet()) {
            if (!nodeLevel.containsKey(laneId)) {
                // atribute an high level (exemple, maxLevel+1) to process later — assuring every lane is considered
                int maxLevel = levels.isEmpty() ? 0 : Collections.max(levels.keySet());
                int assigned = maxLevel + 1;
                levels.computeIfAbsent(assigned, k -> new LinkedHashSet<>()).add(laneId);
                nodeLevel.put(laneId, assigned);
            }
        }

        // PROCESSING: from the bottom (terminals) up
        // Removing everything from terminal lanes
        Set<Integer> level0 = levels.getOrDefault(0, Collections.emptySet());
        for (Integer termId : level0) {
            GVRTPMIBG1.LaneEntryRow termLane = laneMap.get(termId);
            if (termLane == null) continue;
            int color = termLane.getSemaphoreColor().getValue();
            boolean open = (color == 1 || color == 2); // 1=green, 2=yellow
            if (!open) continue;
            int vehicles = (int) termLane.getVehicleCount().getValue();
            if (vehicles <= 0) continue;
            // remove everything
            termLane.setVehicleCount(new Gauge32(0));
            // update totalVeh - every vehicle that passed by this lane
            int totalVeh = termLane.getTotalVeh().getValue();
            termLane.setTotalVeh(new Integer32(totalVeh + vehicles));
        }

        // For each level > 0, from lowest to highest:
        // Moving traffic between levels
        List<Integer> levelKeys = new ArrayList<>(levels.keySet());
        Collections.sort(levelKeys); // assuring the order is correct

        for (int level : levelKeys) {
            if (level == 0) continue; // already processed
            Set<Integer> nodes = levels.getOrDefault(level, Collections.emptySet());
            if (nodes == null) continue;

            for (Integer laneId : nodes) {
                GVRTPMIBG1.LaneEntryRow lane = laneMap.get(laneId);
                if (lane == null) continue;

                // Only sends if it has destinies associated
                List<GVRTPMIBG1.ViaDestEntryRow> outs = destMap.get(laneId);
                if (outs == null || outs.isEmpty()) continue;

                // verifies semaphore color: 1=green,2=yellow,3=red
                int color = lane.getSemaphoreColor().getValue();
                boolean open = (color == 1 || color == 2);
                if (!open) {
                    // if red -> doesnt send traffic
                    continue;
                }

                int originVehicles = (int) lane.getVehicleCount().getValue();
                if (originVehicles <= 0) continue;

                // Verify that at least one destination has "flux" defined
                int totalFluxo = 0;
                for (GVRTPMIBG1.ViaDestEntryRow d : outs) {
                    int fluxo = d.getFluxoDest().getValue();
                    if (fluxo > 0) totalFluxo += fluxo;
                }
                if (totalFluxo <= 0) continue;

                int remaining = originVehicles;
                int movedTotal = 0;

                List<GVRTPMIBG1.ViaDestEntryRow> candidates = new ArrayList<>(outs);
                Map<Integer, Double> weights = new HashMap<>();
                double totalWeight = 0.0;

                // Calculates the weight for every option (Based on free capacity %)
                for (GVRTPMIBG1.ViaDestEntryRow d : candidates) {
                    int destId = d.getDestVia().getValue();
                    GVRTPMIBG1.LaneEntryRow destLane = laneMap.get(destId);

                    // if the lanes doesnt exist we ignore
                    if (destLane == null) continue;

                    double currentVeh = destLane.getVehicleCount().getValue();
                    double capacity = destLane.getCapacity().getValue();

                    // if capacity 0 or if its full the weight is 0
                    if (capacity <= 0 || currentVeh >= capacity) {
                        weights.put(destId, 0.0);
                        continue;
                    }

                    // Calculating the percentage of occupation
                    double occupancyRate = currentVeh / capacity;

                    // The weight is the reverse of occupation (ex: 20% occupied = 80% weight)
                    // Sending more to who has less weight
                    double weight = 1.0 - occupancyRate;

                    weights.put(destId, weight);
                    totalWeight += weight;
                }

                // if it isnt any available option, we dont send traffic
                if (totalWeight > 0) {

                    // Distribute the vehicles we have
                    int vehiclesToDistribute = remaining;

                    for (GVRTPMIBG1.ViaDestEntryRow d : candidates) {
                        // if done distributing we stop
                        if (remaining <= 0) break;

                        int destId = d.getDestVia().getValue();
                        Double weight = weights.get(destId);

                        // if weight is zero we jump to the next
                        if (weight == null || weight <= 0.0) continue;

                        GVRTPMIBG1.LaneEntryRow destLane = laneMap.get(destId);

                        // Calculate the proporcional quota
                        // Ex: if i have 10 cars and the weight is 50% i send 5 cars to that lane
                        double ratio = weight / totalWeight;
                        int targetShare = (int) Math.round(vehiclesToDistribute * ratio);

                        // If the gives zero(due to approximations) but it has a space we still send
                        if (targetShare == 0 && remaining > 0) targetShare = 1;

                        // Respecting the physical capacities of the lanes
                        int destVehicles = (int) destLane.getVehicleCount().getValue();
                        int destCapacity = destLane.getCapacity().getValue();
                        int freeSpace = destCapacity - destVehicles;

                        // What we move is the lowest between:
                        // a) The calculated quota (targetShare)
                        // b) The freespace on the destination lane (freeSpace)
                        // c) What remains on origin (remaining)
                        int toMove = Math.min(targetShare, freeSpace);
                        toMove = Math.min(toMove, remaining);

                        if (toMove > 0) {
                            destLane.setVehicleCount(new Gauge32(destVehicles + toMove));
                            remaining -= toMove;
                            movedTotal += toMove;
                        }
                    }
                }

                // Greedy distribution of the remaining
                // If proportional math left cars behind (rounding errors), push them anywhere that has space
                for (GVRTPMIBG1.ViaDestEntryRow d : candidates) {
                    if (remaining <= 0) break;

                    int destId = d.getDestVia().getValue();
                    GVRTPMIBG1.LaneEntryRow destLane = laneMap.get(destId);
                    if (destLane == null) continue;

                    int destVehicles = (int) destLane.getVehicleCount().getValue();
                    int destCapacity = destLane.getCapacity().getValue();
                    int freeSpace = destCapacity - destVehicles;
                    if (freeSpace <= 0) continue;

                    int toMove = Math.min(freeSpace, remaining);
                    if (toMove <= 0) continue;

                    destLane.setVehicleCount(new Gauge32(destVehicles + toMove));
                    remaining -= toMove;
                    movedTotal += toMove;
                }

                // Updates origin
                if (movedTotal > 0) {
                    lane.setVehicleCount(new Gauge32(originVehicles - movedTotal));
                    int totalVeh = lane.getTotalVeh().getValue();
                    lane.setTotalVeh(new Integer32(totalVeh + movedTotal));
                }
            }
        }

        // Injecting the cars on origin lanes
        // the origin lanes are identified with rgt>0
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

        // Update sysUpTime (TimeTicks, 1/100s)
        // 1sec step time
        int stepMs = mib.getSysStepTime().getValue().toInt();
        if (stepMs <= 0) stepMs = 1000;
        long deltaTicks = stepMs / 10;
        TimeTicks currentTicks = mib.getSysUpTime().getValue();
        long newTicks = currentTicks.toLong() + deltaTicks;
        mib.getSysUpTime().setValue(new TimeTicks(newTicks));
    }
}