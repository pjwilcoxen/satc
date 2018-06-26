import java.util.ArrayList;
import sim.engine.SimState;

/**
 * Class that extends Adversary.  Implements an malicious
 * actor that sends false bids indiscriminately.  Does 
 * not hide their own identity
 */
public class Adv_Darth extends Adversary{

    Demand falseBid = new Demand();
    
    /**
     * Constructor
     */
    public Adv_Darth(int own_id) {
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
            int traderId = Integer.parseInt(config.get("trader"));
            int targetId = Integer.parseInt(config.get("target"));

            Intel targetIntel = getIntel(targetId);
            int targetPrice = targetIntel.history.p.get(period);
            int targetCost = targetIntel.cost;
            int targetCap = targetIntel.cap;
            History targetHistory = targetIntel.history;


            //if (targetHistory.upD.get(period).getFloorBid(targetPrice - targetCost).q_max == targetCap) {
            if (targetHistory.getConstr(period).equals("D")) {
                System.out.println("Attack Triggered!");

                // Generate false bid
                Intel trader = getIntel(traderId);
                if (trader.compromised) {
                    Demand fakeDemand = new Demand();

                    for (int bidPrice : trader.history.upD.get(period).bids.keySet()) {
                        fakeDemand.add(bidPrice,
                                       trader.history.upD.get(period).getBid(bidPrice).q_min - shift,
                                       trader.history.upD.get(period).getBid(bidPrice).q_max - shift);
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
