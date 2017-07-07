import java.util.ArrayDeque;
import java.util.ArrayList;
import sim.engine.SimState;
import sim.engine.Steppable;

/**
 * General purpose Agent class
 */
public abstract class Agent implements Steppable {
    
    /**
     * Version of the Agent class
     */
    public static final String VER = "2.0";

    //the agent's view of the time
    int myTime = 0;

    //indicates the type of node (end user = 3, middle node = 2, root node = 1)
    int type;
    
    //indicates own id
    int own_id;
    
    //indicates parents' id
    int par_id;
    
    //indicates the balance price for each case of dropped nodes    
    int[] bl;
    
    double blockDraw;

    //data bus this agent uses to communicate with its parent
    DBUS dbus;

    ArrayDeque<Msg> msgs = new ArrayDeque<>();

    //  Parent and list of children

    /**
     * Children of this agent
     */
    final ArrayList<Agent> children = new ArrayList<>();

    public int getBl(int drop) {
        return bl[drop];
    }

    public void setBl(int bl, int drop) {
        this.bl[drop] = bl;
    }
     
    public void setDBUS(DBUS dbus) {
        this.dbus = dbus;
    }

    public int getType() {
        return type;
    }

    public int getPar_id() {
        return par_id;
    }

    /**
     * Add a message to this agent's input queue
     *
     * @param msg Message to add
     */
    public void deliver(Msg msg) {
        msgs.add(msg);
    }

    /**
     * Extract a set of messages from the input queue
     *
     * @param type Message type to extract
     */
    ArrayList<Msg> getMsgs(Msg.Types type) {
        ArrayList<Msg> selected = new ArrayList<>();

        for(Msg msg: msgs) 
            if( msg.type == type ) 
                selected.add(msg);
        
        for(Msg msg: selected)
            msgs.remove(msg);
        
        return selected;
    }

    /**
     * Extract a subset of messages from the input queue
     *
     * @param type Message type to extract
     * @param dos_id DOS run number
     * @return List of messages
     */
    ArrayList<Msg> getMsgs(Msg.Types type,int dos_id) {
        ArrayList<Msg> selected = new ArrayList<>();

        for(Msg msg: msgs) 
            if( msg.type == type && msg.dos_id == dos_id ) 
                selected.add(msg);
        
        for(Msg msg: selected)
            msgs.remove(msg);
        
        return selected;
    }

    /**
     * Send a demand to parent node
     */
    public void reportDemand(Demand dem,int dos_id) {
        Msg msg = new Msg(this,par_id);
        msg.setDemand(dem);
        msg.dos_id = dos_id;
        dbus.send(msg);
    }
 
    public Agent(int up_id, int own_id) {
        super();

        this.type    = 0;
        this.par_id  = up_id;
        this.own_id  = own_id;
        
        bl = new int[Env.nDOS];

    }

}

