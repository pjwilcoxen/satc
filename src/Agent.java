import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.engine.SimState;
import sim.engine.Steppable;
import static java.lang.Math.pow;

/**
 * General purpose Agent class
 */
public class Agent implements Steppable {
    
    /**
     * Version of the Agent class
     */
    public static final String VER = "2.0";

    //the agent's view of the time
    int myTime = 0;

    //local version of the global transmission cost
    int cost;

    //local version of the global capacity constraint
    int cap; 
   
    /**
     * Maximum number of steps in each generated bid
     */
    static int maxstep = 14;
    
    /**
     * Actual number of steps used, for reference
     */
    int steps;

    // not needed?
    static int maxbids = 400;

    // the overall environment
    Env e;
        
    //indicates the type of node (end user = 3, middle node = 2, root node = 1)
    int type;
    
    //indicates own id
    int own_id;
    
    //indicates parents' id
    int par_id;
    
    //indicates supply or demand type
    String sd_type;
    
    //indicates initial load for end user extracted from "testdraw.csv"
    //indicates the elasticity of end user extracted from "testdraw.csv" 
    double load;
    double elast;
    double blockDraw;

    //vector of bids drawn from initial load, elast, and number of steps
    Bidstep[] bids;
	
    //inidicates the queue variable for Bidstep type filled by childNodes 
    //Four vactors for different cases of dropped nodes    
    ArrayList<Bidstep[][]> queueD;
    
    //indicates the size of each vector of queueD
    int[] queueSizeD;
    
    //indicates the aggregated net demands for each case of dropped nodes    
    ArrayList<Bidstep[]> aggD;
    
    //indicates the balance price for each case of dropped nodes    
    int[] bl;
    
    //indicates the upper and lower level around the balance prices considering transaction cost
    int[][] p_c;

    //data bus this agent uses to communicate with its parent
    DBUS dbus;

    ArrayDeque<Msg> msgs = new ArrayDeque<>();

    //  Parent and list of children

    /**
     * Parent of this agent
     */
    Agent parent;
    
    /**
     * Children of this agent
     */
    ArrayList<Agent> children;

    private Bidstep[] getAggD(int drop) {
        return aggD.get(drop);
    }

    private void setAggD(Bidstep[] agg, int drop) {
        aggD.set(drop, agg);
    }

    public void setParent(Agent Parent) {
        this.parent = Parent;
        Parent.children.add(this);
    }

    private int getBl(int drop) {
        return bl[drop];
    }

    public void setBl(int bl, int drop) {
        this.bl[drop] = bl;
    }
     
    public void setDBUS(DBUS dbus) {
        this.dbus = dbus;
    }
    //
    //  clearQueuesD
    //

    private void clearQueuesD() {
        for (int i = 0; i < 4; i++) 
            queueSizeD[i] = 0;
    }

    public int getType() {
        return type;
    }

    public int getPar_id() {
        return par_id;
    }

    private int[] getP_c(int drop) {
        return p_c[drop];
    }

    private void setP_c(int p0, int p1, int drop) {
        p_c[drop][0] = p0;
        p_c[drop][1] = p1;
    }

    public void appendQueueD(Bidstep[] bids, int drop) {
        Bidstep tmp[][];
        tmp = queueD.get(drop);
        tmp[queueSizeD[drop]] = bids;
        queueD.set(drop, tmp);
        queueSizeD[drop]++;
    }

    private double runiform() {
        return Env.runiform() ;
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
    private ArrayList<Msg> getMsgs(Msg.Types type) {
        ArrayList<Msg> selected = new ArrayList<>();

        for(Msg msg: msgs) 
            if( msg.type == type ) 
                selected.add(msg);
        
        for(Msg msg: selected)
            msgs.remove(msg);
        
        return selected;
    }

    public Agent(SimState state, Agent mkt, int type, int up_id, int own_id, String sd_type) {
        super();

        this.type = type;
        this.sd_type = sd_type;
        this.par_id = up_id;
        this.own_id = own_id;

        parent   = mkt;
        children = new ArrayList<>();
        
        e    = (Env) state;
        bl   = new int[4];
        p_c  = new int[4][2];
        cost = Env.transCost ;
        cap  = Env.transCap;

        if (mkt != null) {
            mkt.children.add(this);
        }

        queueSizeD = new int[4];
      
        queueD = new ArrayList<>();
        queueD.add(new Bidstep[100][maxbids]);
        queueD.add(new Bidstep[100][maxbids]);
        queueD.add(new Bidstep[100][maxbids]);
        queueD.add(new Bidstep[100][maxbids]);

        aggD = new ArrayList<>();
        aggD.add(new Bidstep[maxbids]);
        aggD.add(new Bidstep[maxbids]);
        aggD.add(new Bidstep[maxbids]);
        aggD.add(new Bidstep[maxbids]);

        clearQueuesD();
    }
    
    /**
     * Build the agent's net demand curve
     */
    private void drawLoad() {
 
        int max = 9858; 
        int rand;
        int row;
        
        // generate a random number of input lines to skip
        // max was originally hard-coded and is left that 
        // way for compatibility

        rand = (int)(runiform() * max);
        
        Env.Draw draw;
        if( sd_type.equals("D") )
           draw = Env.drawListD.get(rand);
        else
           draw = Env.drawListS.get(rand);
        
        load  = draw.load;
        elast = draw.elast;

        //number of steps to create curve
        steps = (int) (runiform() * maxstep + 2);

        //call draw function based on the type of end user
        if (sd_type.equals("D")) {
            bids = drawDemand();
        } else {
            bids = drawSupply();
        }
    }
    
     
    //call aggragate function  on the queue
    private ArrayList<Bidstep[]> runsim(){

        for(Msg msg: getMsgs(Msg.Types.DEMAND)) 
            appendQueueD(msg.getDemand(),msg.dos_id);

        Bidstep aggBidD[];
        
        //initiate an arraylist for the aggregated vectors
        ArrayList<Bidstep[]> agg = new ArrayList<>();
        
        //call aggregate function on each queue based on number of drops
        for(int j = 0 ; j < 4 ; j ++){
            aggBidD = queueD.get(j)[0];
            //call aggregate function by the number of each queue's size
            for(int i=1 ; i < queueSizeD[j] ; i++)
                aggBidD = aggregateDemand(aggBidD, queueD.get(j)[i]);
            //populate the agg arraylist 
            agg.add(aggBidD);
        }
        
        return agg;
       
    }

    //change the step prices considering transation cost
    private Bidstep[] addCost(Bidstep[] bids, int c, int drop) {
        Bidstep[] tmp = new Bidstep[maxbids];
        int i;
        //decrease the price level of steps with positive quantity
        for(i = 0; (bids[i] != null) && (bids[i].getQ_min() >= 0); i++ )
            tmp[i] = new Bidstep(bids[i].getP()-c,bids[i].getQ_min(),bids[i].getQ_max());
        
        //if there is no step with positive quantity 
        if(i == 0){
                for(; bids[i] != null ; i++)
                    tmp[i] = new Bidstep(bids[i].getP()+c,bids[i].getQ_min(),bids[i].getQ_max());
        }
        if(bids[i] != null ){
            //if theere is a vertical overlaop with y axis
            if(bids[i].getQ_max() == 0){
                //set the two upper and lower limits around the balance price
                int mid = ((bids[i-1].getP()+bids[i].getP())/2);
                //avoid negative value for the lower step
                if((mid-c) < 0)
                    setP_c(0,mid+c, drop);
                else
                    setP_c(mid-c,mid+c, drop);
                //increase the price level of steps with positive quantity
                for(; bids[i] != null ; i++ )
                    tmp[i] = new Bidstep(bids[i].getP()+c,bids[i].getQ_min(),bids[i].getQ_max());
            
            //if a horizontal step indicate the balance price
            }else{
                //set the two upper and lower limits around the balance price
                if((bids[i-1].getP()-c) < 0)
                    setP_c(0,bids[i-1].getP()+c, drop);
                else
                    setP_c(bids[i-1].getP()-c,bids[i-1].getP()+c, drop);
                //divide the middle step to two steps with +c/-c prices
                tmp[i] = new Bidstep(bids[i].getP()-c,0,bids[i].getQ_max());
                tmp[i+1] = new Bidstep(bids[i].getP()+c,bids[i].getQ_min(),0);
                i++;
                //increase the price level of steps with positive quantity
                for(; bids[i] != null ; i++)
                    tmp[i+1] = new Bidstep(bids[i].getP()+c,bids[i].getQ_min(),bids[i].getQ_max());
            
            }
        }
        return tmp;

        
    }

    //put capacity constrain on the net demand
    private Bidstep [] addCapacity(Bidstep[] bids, int cap) {
        Bidstep[] tmp = new Bidstep[maxbids];
        int i, j=0;
        
        //skip the steps with more quantity than cap
        for(i =0; bids[i].getQ_min() >= cap; i++);
        if(bids[i] == null)
            return new Bidstep[0];
        //set the right corner step
        if(bids[i].getQ_max() > cap){
            tmp[j++]= new Bidstep(bids[i].getP(),bids[i].getQ_min(),cap);
            i++;
        }
        //consider the steps between two capacity limits
        for(;(bids[i] != null) && (bids[i].getQ_min() >= ((-1)*(cap))); i++)
            tmp[j++] = new Bidstep(bids[i].getP(),bids[i].getQ_min(),bids[i].getQ_max());
        if (bids[i] == null) 
            return tmp;
        //set the left corner step
        else
            tmp[j] = new Bidstep(bids[i].getP(),((-1)*(cap)),bids[i].getQ_max());
        
        return tmp;
        
    }
    
    //find the actual price for the end users considering transaction cost and capacity limit
    private int findReportPrice(int drop) {
        int report = 0;
        int i;
        int p;

        int pr = getBl(drop);
        int pc0 = getP_c(drop)[0];
        int pc1 = getP_c(drop)[1];
        Bidstep[] agg = getAggD(drop);

        //if the whole balance price is between local balance price +c/-c
        if ((pr >= pc0) && (pr <= pc1)) {
            return ((pc0 + pc1) / 2);
        }
		
        //if the whole balance price is more than local balance price +c
        if (pr > pc1) {
            p = 0;

            //skip the bids with more quantity than (-1) * cap & less price than the balance price
            for (i = 0; ((agg[i] != null)
                        && (agg[i].getP() <= pr)
                        && (agg[i].getQ_max() >= ((-1) * cap))); i++)
                            p = agg[i].getP();

            if (agg[i] == null) {
                return p;
            }
            
            //if the target step passed the cap line
            if (agg[i].getQ_max() < ((-1) * cap)) {
                if(pr <= p + cost) 
                    return pr - cost;
                else
                    return p;
            }
            
            //put limit equal to cap
            return pr - cost;
        } 
		
        //if the whole balance price is less than local balance price +c    
		if (pr < pc0) {
            p = 0;

            //skip the bids with more quantity than cap
            for (i = 0; ((agg[i] != null)
                    && (agg[i].getQ_min() >= cap)); i++)
                         p = agg[i].getP();

            if (agg[i] == null) {
                return p;
            } 
            
            if (pr > agg[i].getP() - cost) 
                return pr + cost;

            //put limit equal to cap
            return agg[i].getP();
        }

		assert false;
        return report;
    }

    /**
     * Calculate the net demand considering the balance price
     */
     private int findExcessDemand(int price){
        int i;
        for(i = 0;(bids[i] != null)  && (bids[i].getP() < price); i++);
          
        if(bids[i] == null) 
             Env.log.println("error price: " + price);
                
        int q;
        if(bids[i].getP() == price)  //where the balance price is the same as step price
            q = bids[i].getQ_min();
        else                                  //where the balance price is between two steps
            q = bids[i].getQ_max();
       
        return q; 
     }


    /**
     *  General step function
     * 
     * Calls subtype specific functions in preparation for subclassing
     *
     * @param state Mason state
     */
    @Override
    public void step(SimState state) {
        switch (type) {
            case 1: 
                step_top();
                break;

            case 2:
                step_mid();
                break;

            case 3:
                step_end();
                break;
          
            default:
                throw new RuntimeException("Unexpected agent type in step");
       }
    }

    private void step_top() {
        switch (Env.stageNow) {
            case AGG_MID:
                do_agg_mid();
                break;

            case REPORT_MID:
                do_report_mid();
                break;
                
            default:
                break;
       }
    }

    private void step_mid() {
        switch (Env.stageNow) {
            case INIT_DROPS: 
                do_init_drops();
                break;
                
            case AGG_END:
                do_agg_end();
                break;

            case REPORT_END:
                do_report_end();
                break;
                
            default:
                break;
        }
    }

    private void step_end() {
        switch (Env.stageNow) {
            case INIT_LOADS:
                do_init_load();
                break;

            case CALC_LOADS:
                do_calc_load();
                break;
                
            default:
                break;
        }
    }

    //first cycle: populate the random vectors for dropping end users    

    private void do_init_drops() {
        
        int i,n;
        int rand;
        int block_id;
        double cutoff;
        Agent block_kid;

        Env.log.println("node "+own_id);
        Env.log.println("initialize blocked nodes for DOS runs");

        for(Agent kid: children)
           kid.blockDraw = 100.0*runiform();
           
        for(String dos: Env.dos_runs) {
            cutoff = Double.parseDouble(dos);
            for(Agent kid: children)
                if( kid.blockDraw < cutoff ) 
                    Env.setBlock(dos,kid);
            Env.log.println("dos "+dos+" dropped "+Env.blockList.get(dos));
        }
    }

    /**
     * Create net demand curves for the end users
     */
    private void do_init_load() {

        //build a random net demand for this agent
        drawLoad();

        //populate the parents queues for different cases of dropped nodes 
        for(int i=0 ; i<Env.dos_runs.length ; i++)
            if( ! Env.isBlocked(Env.dos_runs[i],this) ) {
                Msg msg = new Msg(this,parent.own_id);
                msg.setDemand(bids);
                msg.dos_id = i;
                dbus.send(msg);
            }
    }

    /**
     * Aggregate net demands of leaf nodes
     */
    private void do_agg_end() {

        Env.log.println("node "+own_id);

        ArrayList<Bidstep[]> agg;
        Bidstep tmp[];
        
        //call rusim to aggregate the net demands
        agg = runsim();

        //set the agg vectors as class variable
        for (int i = 0; i < 4; i++) {
            setAggD(agg.get(i), i);
        }

        //call addCost and addCapacity functions to consider transaction costs and capacity constrains
        for (int i = 0; i < 4; i++) {
            tmp = addCost(agg.get(i), cost, i);
            tmp = addCapacity(tmp, cap);
            Msg msg = new Msg(this,parent.own_id);
            msg.setDemand(tmp);
            msg.dos_id = i;
            dbus.send(msg);
        }

        clearQueuesD();

    }

    /**
     * Aggregate net demands of middle nodes
     */
    private void do_agg_mid() {

        Env.log.println("node "+own_id);

        ArrayList<Bidstep[]> agg;
        
        //call runsim to aggregate the net demands
        agg = runsim();

        //find the balance price for each case of dropped nodes
        for (int j = 0; j < 4; j++) {
            int i, bl = 0, min =1;
            for (i = 0; ((agg.get(j)[i] != null)
                    && (agg.get(j)[i].getQ_max() >= 0)); i++) {
                bl = agg.get(j)[i].getP();
                min = agg.get(j)[i].getQ_min();
            }

            //if there is not any balance point- report -1 as price
            if ((i == 0) || ((agg.get(j)[i] == null)&&(min > 0))) {
                bl = -1;
                Env.log.println("failed at drop: " + j);
            }

            //set the balance price as the class variable
            setBl(bl, j);
            
            // write the balance prices to the csv file

            String dos = Env.dos_runs[j];
            Env.printResult(this,dos,bl,0);

            // write a log message
            Env.log.println("node_id: "+own_id+" Balance Price: "+bl+" Prob: "+dos);
        }

        clearQueuesD();

        //increment agent's view of time
        myTime++;
    }

    /**
     * Report the balance price from the root node to middle nodes
     */
    private void do_report_mid() {
        
        int child_id;
        DBUS child_bus;

        Env.log.println("node "+own_id);

        //report the balance prices to kid nodes

        for (Agent child: children) {
            child_id = child.own_id;
            for(int i=0 ; i<4 ; i++ ) {
                Msg msg = new Msg(this,child_id);
                msg.setPrice(getBl(i));
                msg.dos_id = i;
                dbus.send(msg);
            }
        }

    }

    /**
     * Report the balance prices from the middle nodes to leaf nodes
     */
    private void do_report_end() {

        int child_id;

        for(Msg msg: getMsgs(Msg.Types.PRICE)) 
            setBl(msg.getPrice(),msg.dos_id);

        Env.log.println("node "+own_id);

        //find the balance price for each case of dropped nodes
        for (int j = 0; j < 4; j++) {
            int i, bl = 0;
            for (i = 0; ((getAggD(j)[i] != null)
                    && (getAggD(j)[i].getQ_max() >= 0)); i++) {
                bl = getAggD(j)[i].getP();
            }
            Env.log.println("node_id: " + own_id  + " balance price: " + bl);
        }
        
        int[] report = new int[4];
        //set the report values for each case of dropped nodes
        for (int drop = 0; drop < 4; drop++) {
            //report -1 in the case of no balance point
            if (getBl(drop) <= -1) {
                report[drop] = -1;
            //call findReportPrice to adjust the repor price by considering the transaction cost and capacity constrains
            } else {
                report[drop] = findReportPrice(drop);
            }
        }

        //write the balance prices on csv file for each case and report to children 

        for(int i=0 ; i<Env.dos_runs.length ; i++) {
            Env.printResult(this,Env.dos_runs[i],report[i],0);
            for(Agent child: children) {
                Msg msg = new Msg(this,child.own_id);
                msg.setPrice(report[i]);
                msg.dos_id = i;
                child.dbus.send(msg);
            }
        }
    }

    /**
     * Calculate actual loads at the leaf nodes
     *
     * Report net demand as 0 if no price was found
     */
    private void do_calc_load() {
            
        for(Msg msg: getMsgs(Msg.Types.PRICE)) 
            setBl(msg.getPrice(),msg.dos_id);

        int ex;
        int bl;
        int i;
        String dos;

        for(i=0 ; i<Env.dos_runs.length ; i++) {
        
            dos = Env.dos_runs[i];
            bl  = getBl(i);

            if (bl <= -1) 
                ex = 0;
            else
                ex = findExcessDemand(bl);

            Env.printResult(this,dos,bl,ex);
        }
    }

    /**
     * Create demand curves based on initial load, elasticity, and number of steps
     */
    private Bidstep[] drawDemand(){
        Bidstep [] result = new Bidstep[maxbids];
        
        int iniprice= 40 +  (int) (runiform() * 12 - 6);
        int p0 = iniprice/steps;
        int p1 = iniprice*2/steps;
        int q1= (int) (load * pow((double)p0/iniprice,elast));
        int q2=(int)(load * pow((double)p1/iniprice,elast));
        result[0]  = new Bidstep(p0, q2, q1);
        
        //create the number of steps below the price=40
        for(int i =1 ; i < steps; i ++){
            p1 = iniprice*(i+1)/steps;
            q1= q2;
            q2= (int) (load * pow((double)p1/iniprice,elast));
            result[i]  = new Bidstep(p1, q2, q1);
        }
        
        //create twice the number of steps upper the proce = 40
        for(int i =1 ; i < 2*steps; i ++){
            p1 = iniprice + (360*i)/(2*steps);
            q1= q2;
            q2= (int) (load * pow((double)p1/iniprice,elast));
            result[steps + i - 1]  = new Bidstep(p1, q2, q1);
        }
        
        return result;
    }
    
    /**
     * Create supply curves with reverse quantities in comparison to demand curve
     */
    private Bidstep[] drawSupply(){
        Bidstep [] result = new Bidstep[maxbids];
        int iniprice= 40 + (int) (runiform() * 12 - 6);
        
        int p0 = iniprice/steps;
        int p1 = (iniprice*2)/steps;

        int q1=(int)((-1) * (load * pow((double)p0/iniprice,elast)));
        int q2=(int)((-1) * (load * pow((double)p1/iniprice,elast)));
        result[0]  = new Bidstep(p0, q1, q2);
        
        //create the number of steps below the price=40
        for(int i =1 ; i < steps; i ++){
            p1 = iniprice*(i+1)/steps;
            q1= q2;
            q2= (int) ((-1) * (load * pow((double)p1/iniprice,elast)));
            result[i]  = new Bidstep(p1, q1, q2);
        }
        
        //create twice the number of steps upper the proce = 40
        for(int i =1 ; i < 2*steps ; i++){
            p1 = iniprice+ 360*i/(2*steps);
            q1= q2;
            q2= (int) ((-1) * (load * pow((double)p1/iniprice,elast)));
            result[steps + i - 1]  = new Bidstep(p1, q1, q2);
        }
        
        return result;
    }
    
    /**
     * Aggregate two net demand curves
     */
    private Bidstep[] aggregateDemand(Bidstep[] bid1, Bidstep[] bid2) {
        Bidstep [] aggBid = new Bidstep[maxbids];
        
        int i =0, j =0, k = 0;
        int min = 0, sd;
        
        //screen all the steps in the two input array of bids
        while((bid1[i]!= null)&&(bid2[j]!=null)){
            
            //initiate the aggregate step considering the minimum price level and sum of the right corners as the max quantity
            aggBid[k] = new Bidstep(minBid(bid1[i], bid2[j]).getP()
                                            ,0,bid1[i].getQ_max() + bid2[j].getQ_max());
            
            
            if(bid1[i].getP() < bid2[j].getP()){
                    //if it is the fist step
                    if(k == 0)
                        aggBid[k].setQ_min(bid1[i].getQ_min() + bid2[j].getQ_max());
                    i++;
                    
            }else if(bid1[i].getP() > bid2[j].getP()){
                    //initiate the left corner of the first step
                    if(k == 0)
                        aggBid[k].setQ_min(bid1[i].getQ_max() + bid2[j].getQ_min());
                    j++;
            }else{ 
                    //initiate the left corner of the first step
                    if(k == 0)
                        aggBid[k].setQ_min(bid1[i].getQ_min() + bid2[j].getQ_min());
                    i++;
                    j++;
            }
            //initiate the left corner of each step based on the right corner of the next step
            if(k > 1)
                aggBid[k-1].setQ_min(aggBid[k].getQ_max());
            
            k++;
            
          }
        //initiate the last step
        aggBid[k-1].setQ_min(aggBid[k-1].getQ_max()-100);
        return aggBid;
    }
    
  
    /**
     * Find the minimum price between two input bids
     */
    private Bidstep minBid(Bidstep bid1, Bidstep bid2) {
        if(bid1.getP() < bid2.getP())
            return bid1;
        else    
            return bid2;
        
    }
}


