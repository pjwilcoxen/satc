import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import sim.engine.SimState;

/**
 * Class that extends Adversary.  Implements an malicious
 * actor that sends false bids indiscriminately.  Does 
 * not hide their own identity
 */
public class Adv_Elvira extends Adversary{

    Demand falseBid = new Demand();
    
    /**
     * Constructor
     */
    public Adv_Elvira(int own_id) {
        super(own_id);
        falseBid.add(1,-1000,1000);
    }
    
    /** 
     * Initialize for a new population
     */
    @Override 
    public void popInit() {
        super.popInit();
    }

    /** 
     * Reset at the beginning of a DOS run
     */
    @Override
    public void runInit() {
        super.runInit();
    }
    
    /**
     * Actions based on current simulation step
     *
     * @param state Mason state
     */
    @Override
    public void step(SimState state) {

        switch (Env.stageNow) {

        case PRE_AGGREGATE:
            int period = 1;
            int shift = Integer.parseInt(config.get("shift"));
            int targetId = Integer.parseInt(config.get("target"));

            Intel targetIntel = getIntel(targetId);
            int targetPrice = targetIntel.history.p.get(period);
            int targetCost = targetIntel.cost;
            int targetCap = targetIntel.cap;
            History targetHistory = targetIntel.history;

            // Hashmap to store compromised agents' ids
            HashMap<Integer, ArrayList<Integer>> compromisedAgents = new HashMap<>();
            int count = 0;
            int eachShift = 0;
            for (Map.Entry<Integer, Intel> entry : intel.entrySet()) {
                Intel i = entry.getValue();

                if (i.compromised) {
                    ++ count;
                    if (compromisedAgents.get(i.par_id) == null) 
                        compromisedAgents.put(i.par_id, new ArrayList<Integer>(Arrays.asList(i.agent_id)));
                    else
                        compromisedAgents.get(i.par_id).add(i.agent_id);

                }
            }
            eachShift = Math.round(shift / count);

            /*
            System.out.println(eachShift);

            for (Map.Entry<Integer, ArrayList<Integer>> entry : compromisedAgents.entrySet()) {
                System.out.println(entry.getKey() + ":");
                for (Integer id : entry.getValue()) {
                    System.out.println(id);
                }
            }
            */

            if (targetHistory.getConstr(period).equals("D")) {
                System.out.println("Attack Triggered!");

                // Generate false bid
                for (Integer traderId : compromisedAgents.get(targetId)) {
                    Intel trader = getIntel(traderId);
                    Demand fakeDemand = new Demand();

                    for (int bidPrice : trader.history.upD.get(period).bids.keySet()) {
                        fakeDemand.add(bidPrice,
                                       trader.history.upD.get(period).getBid(bidPrice).q_min - eachShift,
                                       trader.history.upD.get(period).getBid(bidPrice).q_max - eachShift);
                    }

                    Msg msg = new Msg(this, targetId);
                    msg.setDemand(fakeDemand);
                    Channel channel = Channel.find(trader.channel);
                    assert channel != null;
                    channel.send(msg);
                }

            } else {
                System.out.println("No Attack Triggered!");
            }


            break;
            
        default:
            break;
        }
    }
}
