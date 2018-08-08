import sim.engine.SimState;
import java.util.HashMap;
import java.util.ArrayList;


/**
 * Class that extends Adversary.  Implements an malicious
 * actor that sends false bids indiscriminately.  Does 
 * not hide their own identity
 */
public class Adv_Faust extends Adversary{
    private boolean do_attack;
    private int reduction = 0;

    /**
     * Hashmap to store trader-channel relationships
     */
    private final HashMap<Integer, String> channelList = new HashMap<>();

    /**
     * Constructor
     * 
     * @param own_id Agent's id
     */
    public Adv_Faust(int own_id) {
        super(own_id);
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

        int targetId;
        Intel targetIntel;

        reduction   = Integer.parseInt(getConfig("reduction"));
        targetId    = Integer.parseInt(getConfig("target"));
        targetIntel = getIntel(targetId);

        if (targetIntel.compromised) {
            do_attack = true;

            //  Set message diversion
          
            Channel.find(targetIntel.channel).divert_from(targetId, this.own_id);
            channelList.put(targetIntel.par_id, targetIntel.channel);

            //  Store trader's id and channel type into the hashmap

            for (Agent child : ((Grid)Env.getAgent(targetId)).children) {
                channelList.put(child.own_id, child.channel.name);
            }
        }
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
            if (do_attack)
                injectMsg(reduction);
            break;

        case PRE_CALC_LOADS:
            if (do_attack)
                injectMsg(reduction);
            break;

        default:
            break;
        }
    }

    /**
     * Inject diverted messages back to the channel
     *
     * Inject DEMAND message to the channel without any change.
     * Inject PRICE message after decrease the price by reduction%.
     * 
     * @param reduction 
     */
    private void injectMsg(int reduction) {
        for (Msg msg : this.msgs) {
            Util.debug(msg.type + " message injected to " + msg.to);
            if (msg.isPrice()) {
                int oldPrice = msg.getPrice();
                int newPrice = oldPrice * (100 - reduction) / 100;
                msg.setPrice(newPrice);
                Util.debug("Old price: " + oldPrice + ", new price: " + newPrice);
                Channel.find(channelList.get(msg.to)).inject(msg);
            } else {
                Channel.find(channelList.get(msg.to)).inject(msg);
            }
        }
        this.msgs.clear();
    }
}
