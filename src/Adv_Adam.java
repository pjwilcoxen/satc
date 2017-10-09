import java.util.ArrayList;
import sim.engine.SimState;

/**
 * Class that extends Adversary.  Implements an malicious
 * actor that sends false bids indiscriminately.  Does 
 * not hide their own identity
 */
public class Adv_Adam extends Adversary{

    Demand falseBid = new Demand();
    
    /**
     * Constructor
     */
    public Adv_Adam(int own_id) {
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
            
            // Repeat over intel
            for(Intel i: intel ) {
                if(i.send){
                    Msg msg = new Msg(this, i.agent_id);
                    msg.setDemand(falseBid);
                    Channel channel = Channel.find(i.channel);
                    assert channel != null;
                    channel.send(msg);
                }
            }
            break;
            
        default:
            break;
        }
    }
}
