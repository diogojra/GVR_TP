import org.snmp4j.smi.Integer32;
import java.util.*;

/**
   logic:
   independent lanes follow a timed cycle (independent lanes are the ones with no interceptions)
   intersections: Use a algorithm to choose a lane to prioritize certain lanes (most occupied)
*/
public class SD implements Runnable {

    private final GVRTPMIBG1 mib;

    private static final int GREEN = 1;
    private static final int YELLOW = 2;
    private static final int RED = 3;

    // --- traffic light time configuration ---
    private static final int YELLOW_TIME_MS = 3000;
    private static final int GREEN_TIME_MS = 8000;

    // Red needs to be a full cycle of green and yellow
    private static final int RED_TIME_MS = GREEN_TIME_MS + YELLOW_TIME_MS;

    public SD(GVRTPMIBG1 mib) {
        this.mib = mib;
    }

    @Override
    public void run() {
        while (true) {
            try {
                runOneStep();

                // Dynamic simulation speed: fetch the step delay from the MIB (currently its at 1000 ms)
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
     * Seperates independent lanes from the ones with interceptions
     */
    private void runOneStep() {
        var laneTable = mib.getLaneEntry();
        var laneModel = laneTable.getModel();
        var viaDestTable = mib.getViaDestEntry();
        var viaDestModel = viaDestTable.getModel();

        int stepMs = mib.getSysStepTime().getValue().toInt();
        if (stepMs <= 0) stepMs = 1000;

        // Mapping of topology
        // if multiple lanes feed into the same destination, an interception is formmed
        Map<Integer, Integer> laneToDestMap = new HashMap<>();

        for (Iterator<?> it = viaDestModel.iterator(); it.hasNext(); ) {
            GVRTPMIBG1.ViaDestEntryRow link = (GVRTPMIBG1.ViaDestEntryRow) it.next();
            int sourceId = link.getIndex().get(1);
            int destId = link.getDestVia().getValue();
            laneToDestMap.putIfAbsent(sourceId, destId);
        }

        // Grouping lanes based on destination
        Map<Integer, List<GVRTPMIBG1.LaneEntryRow>> conflictGroups = new HashMap<>();
        List<GVRTPMIBG1.LaneEntryRow> normalLanes = new ArrayList<>();

        for (Iterator<?> it = laneModel.iterator(); it.hasNext(); ) {
            GVRTPMIBG1.LaneEntryRow lane = (GVRTPMIBG1.LaneEntryRow) it.next();
            int rgt = lane.getRgt().getValue();

            //identifying generating lanes
            if (rgt > 0) {
                int laneId = lane.getIndex().last();
                Integer destination = laneToDestMap.get(laneId);

                if (destination != null) {
                    // add lane to the group of interceptions for that destination
                    conflictGroups
                            .computeIfAbsent(destination, k -> new ArrayList<>())
                            .add(lane);
                } else {
                    conflictGroups
                            .computeIfAbsent(-laneId, k -> new ArrayList<>())
                            .add(lane);
                }
            } else {
                normalLanes.add(lane);
            }
        }

        // Process normal lanes (fixed times)
        for (GVRTPMIBG1.LaneEntryRow lane : normalLanes) {
            processNormalLane(lane, stepMs);
        }

        // Process interceptions
        for (List<GVRTPMIBG1.LaneEntryRow> group : conflictGroups.values()) {
            processGeneratorsGroup(group, stepMs);
        }
    }

    /**
     * Fixed cycle for normal lanes:
     * Green -> Yellow -> Red -> Green
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
     * Adaptive cycles for interception
     * After a Green light time expires it goes trough a selection to see who needs the green light the most
     */
    private void processGeneratorsGroup(List<GVRTPMIBG1.LaneEntryRow> generators, int stepMs) {
        if (generators.isEmpty()) return;

        // Identifies the current green light
        GVRTPMIBG1.LaneEntryRow activeLane = null;
        for (GVRTPMIBG1.LaneEntryRow lane : generators) {
            int color = lane.getSemaphoreColor().getValue();
            if (color == GREEN || color == YELLOW) {
                activeLane = lane;
                break;
            }
        }

        // If any of the lanes isnt green it goes directly to a selection process
        if (activeLane == null) {
            performElection(generators);
            return;
        }

        int timeToChange = activeLane.getTimeToChange().getValue();
        int color = activeLane.getSemaphoreColor().getValue();
        int newTime = timeToChange - stepMs;

        int waitTimeForOthers = 0;
        boolean cycleChanged = false;

        if (newTime > 0) {
            // Mantain current state
            activeLane.setTimeToChange(new Integer32(newTime));

            if (color == GREEN) {
                waitTimeForOthers = newTime + YELLOW_TIME_MS;
            } else {
                waitTimeForOthers = newTime;
            }
        } else {
            // Transition state
            if (color == GREEN) {
                // Green -> Yellow
                activeLane.setSemaphoreColor(new Integer32(YELLOW));
                activeLane.setTimeToChange(new Integer32(YELLOW_TIME_MS));
                waitTimeForOthers = YELLOW_TIME_MS;
            } else if (color == YELLOW) {
                // Yellow -> Red
                activeLane.setSemaphoreColor(new Integer32(RED));
                activeLane.setTimeToChange(new Integer32(RED_TIME_MS));

                // Cycle ended, perform new election
                performElection(generators);
                cycleChanged = true;
            }
        }

        // Enforce the selection on all lanes
        if (!cycleChanged) {
            for (GVRTPMIBG1.LaneEntryRow lane : generators) {
                if (lane == activeLane) continue;

                // Red for any lane that wasnt selected
                if (lane.getSemaphoreColor().getValue() != RED) {
                    lane.setSemaphoreColor(new Integer32(RED));
                }
                lane.setTimeToChange(new Integer32(waitTimeForOthers));
            }
        }
    }
    /**
     * The Election Algorithm:
     * Calculates occupancy (Vehicles / Capacity) for all competing lanes.
     * The lane with the highest saturation ratio wins the Green light.
    */
     private void performElection(List<GVRTPMIBG1.LaneEntryRow> generators) {
        GVRTPMIBG1.LaneEntryRow bestCandidate = null;
        double maxOccupancy = -1.0;

        for (GVRTPMIBG1.LaneEntryRow lane : generators) {
            int veiculos = (int) lane.getVehicleCount().getValue();
            int capacidade = lane.getCapacity().getValue();
            // Saturation metric = V / C
            double occupancy = (capacidade > 0) ? (double) veiculos / capacidade : 0.0;

            if (occupancy > maxOccupancy) {
                maxOccupancy = occupancy;
                bestCandidate = lane;
            } else if (occupancy == maxOccupancy) {
                // If both have equal saturations then its decided based on vehicle count
                if (bestCandidate != null && veiculos > bestCandidate.getVehicleCount().getValue()) {
                    bestCandidate = lane;
                }
            }
        }


        if (bestCandidate != null) {
            // The lane selected goes Green
            bestCandidate.setSemaphoreColor(new Integer32(GREEN));
            bestCandidate.setTimeToChange(new Integer32(GREEN_TIME_MS));

            // The not chosen ones go Red
            for (GVRTPMIBG1.LaneEntryRow lane : generators) {
                if (lane != bestCandidate) {
                    lane.setSemaphoreColor(new Integer32(RED));
                    lane.setTimeToChange(new Integer32(RED_TIME_MS));
                }
            }
        }
    }
}