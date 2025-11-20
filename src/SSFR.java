
import java.util.*;

import org.snmp4j.agent.mo.MOTable;
import org.snmp4j.agent.mo.MOTableModel;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.TimeTicks;

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

                // 从 MIB 读取步长（ms）
                int stepMs = mib.getSysStepTime().getValue().toInt();
                if (stepMs <= 0) {
                    stepMs = 1000; // 安全兜底：1s
                }
                Thread.sleep(stepMs);
            } catch (InterruptedException e) {
                // 线程被中断就退出循环
                return;
            } catch (Exception e) {
                // 避免因为一次异常整个线程死掉
                e.printStackTrace();
            }
        }
    }

    /**
     * 执行一次仿真步：
     * 1) 根据 RGT 向入口车道注入车辆
     * 2) 对每条 lane，如果灯是绿/黄，按 viasdestino 的 fluxoDest 把车分发到各个目标 lane
     * 3) 更新 VehicleCount / TotalVeh
     * 4) 更新 sysUpTime
     */
    private void runOneStep() {
        // 为了简单起见，这里不做多线程锁。如果以后需要，可以在这里 synchronized(mib)。
        MOTable<?, ?, ?> laneTable = mib.getLaneEntry();
        MOTableModel<?> laneModel = laneTable.getModel();

        MOTable<?, ?, ?> viaTable = mib.getViaDestEntry();
        MOTableModel<?> viaModel = viaTable.getModel();

        // 1) 先把所有 lane 缓存到 map：laneId -> LaneEntryRow
        Map<Integer, GVRTPMIBG1.LaneEntryRow> laneMap = new HashMap<>();
        for (Iterator<?> it = laneModel.iterator(); it.hasNext(); ) {
            GVRTPMIBG1.LaneEntryRow row = (GVRTPMIBG1.LaneEntryRow) it.next();
            int laneId = row.getIndex().last(); // 索引就是 laneID
            laneMap.put(laneId, row);
        }

        // 2) 把 viaDestEntry 也按 origem lane 分组：laneID -> List<ViaDestEntryRow>
        Map<Integer, List<GVRTPMIBG1.ViaDestEntryRow>> destMap = new HashMap<>();
        for (Iterator<?> it = viaModel.iterator(); it.hasNext(); ) {
            GVRTPMIBG1.ViaDestEntryRow row = (GVRTPMIBG1.ViaDestEntryRow) it.next();
            // viaDestEntry 的索引是 (id, laneID)，第二个就是 laneID
            int laneId = row.getIndex().get(1);
            destMap.computeIfAbsent(laneId, k -> new ArrayList<>()).add(row);
        }

        // 3) RGT 注入新车（所有 lane 都可以有 RGT>0）
        for (GVRTPMIBG1.LaneEntryRow lane : laneMap.values()) {
            int vehicleCount = (int) lane.getVehicleCount().getValue();
            int capacity = lane.getCapacity().getValue();
            int rgt = lane.getRgt().getValue(); // RGT: 该步注入多少车

            if (rgt > 0 && vehicleCount < capacity) {
                int freeSpace = capacity - vehicleCount;
                int toAdd = Math.min(rgt, freeSpace);
                if (toAdd > 0) {
                    lane.setVehicleCount(new Gauge32(vehicleCount + toAdd));
                    // 是否把 RGT 注入的车辆计入 totalVeh 看你需求。
                    // 这里把“进入该 lane 的新车”也计入 totalVeh。
                    int totalVeh = lane.getTotalVeh().getValue();
                    lane.setTotalVeh(new Integer32(totalVeh + toAdd));
                }
            }
        }

        // 4) 对每条 origem lane，把车按 fluxoDest 分配到目的 lane
        for (GVRTPMIBG1.LaneEntryRow origin : laneMap.values()) {
            int originId = origin.getIndex().last();

            // 只有在有目的地配置时才需要处理
            List<GVRTPMIBG1.ViaDestEntryRow> dests = destMap.get(originId);
            if (dests == null || dests.isEmpty()) {
                continue;
            }

            // 灯色：1=green, 2=yellow, 3=red
            int color = origin.getSemaphoreColor().getValue();
            boolean open = (color == 1 || color == 2);
            if (!open) {
                // 红灯不允许通行
                continue;
            }

            int originVehicles = (int) origin.getVehicleCount().getValue();
            if (originVehicles <= 0) {
                continue;
            }

            // fluxoDest 作为“分配权重”
            int totalFluxo = 0;
            for (GVRTPMIBG1.ViaDestEntryRow d : dests) {
                int fluxo = d.getFluxoDest().getValue();
                if (fluxo > 0) {
                    totalFluxo += fluxo;
                }
            }
            if (totalFluxo <= 0) {
                // 没有有效 fluxo，啥也不做
                continue;
            }

            int remaining = originVehicles;
            int movedTotal = 0;

            // 为避免整除损失，除了最后一个目的地用权重计算，其余每个按比例分配
            for (int i = 0; i < dests.size(); i++) {
                GVRTPMIBG1.ViaDestEntryRow d = dests.get(i);
                int destId = d.getDestVia().getValue();

                GVRTPMIBG1.LaneEntryRow destLane = laneMap.get(destId);
                if (destLane == null) {
                    // 配置错误：目的 lane 不存在
                    continue;
                }

                int destVehicles = (int) destLane.getVehicleCount().getValue();
                int destCapacity = destLane.getCapacity().getValue();
                int freeSpace = destCapacity - destVehicles;
                if (freeSpace <= 0) {
                    // 目的 lane 满了，不能再进
                    continue;
                }

                int fluxo = d.getFluxoDest().getValue();
                if (fluxo <= 0) {
                    continue;
                }

                int toMove;
                if (i < dests.size() - 1) {
                    // 非最后一个目的地：按权重分配
                    int desired = originVehicles * fluxo / totalFluxo;
                    if (desired <= 0) {
                        desired = 1; // 至少尝试 1 辆，避免一直为 0
                    }
                    toMove = Math.min(desired, Math.min(freeSpace, remaining));
                } else {
                    // 最后一个：把剩下的都塞进去（受容量限制）
                    toMove = Math.min(freeSpace, remaining);
                }

                if (toMove <= 0) {
                    continue;
                }

                // 更新目的 lane 车辆数
                destLane.setVehicleCount(new Gauge32(destVehicles + toMove));

                remaining -= toMove;
                movedTotal += toMove;

                if (remaining <= 0) {
                    break;
                }
            }

            // 更新 origem lane 车辆数和 totalVeh（通过该 semáforo 的车辆）
            if (movedTotal > 0) {
                origin.setVehicleCount(new Gauge32(originVehicles - movedTotal));
                int totalVeh = origin.getTotalVeh().getValue();
                origin.setTotalVeh(new Integer32(totalVeh + movedTotal));
            }
        }

        // 5) 更新 sysUpTime（TimeTicks，单位 1/100s）
        int stepMs = mib.getSysStepTime().getValue().toInt();
        if (stepMs <= 0) {
            stepMs = 1000;
        }
        long deltaTicks = stepMs / 10; // 1 tick = 10ms
        TimeTicks currentTicks = mib.getSysUpTime().getValue();
        long newTicks = currentTicks.toLong() + deltaTicks;
        mib.getSysUpTime().setValue(new TimeTicks(newTicks));
    }
}
