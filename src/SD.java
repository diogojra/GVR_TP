import org.snmp4j.smi.Integer32;

import java.util.Iterator;

public class SD implements Runnable {

    private final GVRTPMIBG1 mib;

    // 灯色定义（与你 SSFR 一样）
    private static final int GREEN = 1;
    private static final int YELLOW = 2;
    private static final int RED = 3;

    // 这里是默认灯色时长（毫秒）
    // 老师作业没有强制时长数值，你可以自由调
    private static final int GREEN_TIME_MS = 8000;
    private static final int YELLOW_TIME_MS = 3000;
    private static final int RED_TIME_MS = 8000;

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

    /** 进行一次 SD 计算周期 */
    private void runOneStep() {
        var laneTable = mib.getLaneEntry();
        var laneModel = laneTable.getModel();

        for (Iterator<?> it = laneModel.iterator(); it.hasNext(); ) {

            GVRTPMIBG1.LaneEntryRow lane = (GVRTPMIBG1.LaneEntryRow) it.next();

            int color = lane.getSemaphoreColor().getValue();
            int timeToChange = lane.getTimeToChange().getValue();

            // step 时间
            int stepMs = mib.getSysStepTime().getValue().toInt();
            if (stepMs <= 0) stepMs = 1000;

            // 1) 减计时器
            int newTime = timeToChange - stepMs;
            if (newTime > 0) {
                lane.setTimeToChange(new Integer32(newTime));
                continue; // 灯色不变
            }

            // 2) 计时器到 0 或以下：改变灯色
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

            // 3) 写回 MIB（非常重要）
            lane.setSemaphoreColor(new Integer32(nextColor));
            lane.setTimeToChange(new Integer32(nextTime));
        }
    }
}
