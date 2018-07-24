import java.util.ArrayList;
import sim.engine.SimState;

/**
 * Class that extends Adversary.  Implements an malicious
 * actor that sends false bids indiscriminately.  Does 
 * not hide their own identity
 */
public class Adv_Beth extends Adversary{
   
    Demand falseBid = new Demand();

    /**
     * Constructor
     */
    public Adv_Beth(int own_id) {
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
        	
        	ArrayList<Intel[]> list = Intel.getFamily2(intel); //Get list of parent/child pairs
        	if(list.isEmpty())
        		throw new RuntimeException("No intel availible");
        	Intel target = list.get(0)[0]; //Instantiate parent
        	Intel child = list.get(0)[1]; //Instantiate child
        	if (target.send) {
            	Msg msg = new Msg(this, target.agent_id);
                msg.setFrom(child.agent_id);
                msg.setDemand(falseBid);
                Channel channel = Channel.find(target.channel);
                assert channel != null;
                channel.send(msg);
                break;
            }
            break;
            
        default:
            break;
        }
    }
}
