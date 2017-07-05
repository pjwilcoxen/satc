import java.util.ArrayDeque;
import java.util.ArrayList;
import sim.engine.SimState;
import sim.engine.Steppable;

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
    Demand demand;

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
    final ArrayList<Agent> children = new ArrayList<>();

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
        for (int i = 0; i < Env.nDOS ; i++) 
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

    public void appendQueueD(Demand dem, int drop) {
        Bidstep tmp[][];
        tmp = queueD.get(drop);
        tmp[queueSizeD[drop]] = dem.bids;
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

    /**
     * Extract a subset of messages from the input queue
     *
     * @param type Message type to extract
     * @param dos_id DOS run number
     * @return List of messages
     */
    private ArrayList<Msg> getMsgs(Msg.Types type,int dos_id) {
        ArrayList<Msg> selected = new ArrayList<>();

        for(Msg msg: msgs) 
            if( msg.type == type && msg.dos_id == dos_id ) 
                selected.add(msg);
        
        for(Msg msg: selected)
            msgs.remove(msg);
        
        return selected;
    }

    /**
     * Broadcast a price to all children
     */
    private void reportPrice(int price,int dos_id) {
        for (Agent child: children) {
            Msg msg = new Msg(this,child.own_id);
            msg.setPrice(price);
            msg.dos_id = dos_id;
            child.dbus.send(msg);
            }
    }

    /**
     * Send a demand to parent node
     */
    private void reportDemand(Demand dem,int dos_id) {
        Msg msg = new Msg(this,parent.own_id);
        msg.setDemand(dem);
        msg.dos_id = dos_id;
        dbus.send(msg);
    }
 
    public Agent(int type, int up_id, int own_id, String sd_type) {
        super();

        this.type    = type;
        this.sd_type = sd_type;
        this.par_id  = up_id;
        this.own_id  = own_id;
        load         = 0;
        elast        = 0;
        parent       = null;
        
        bl   = new int[Env.nDOS];
        p_c  = new int[Env.nDOS][2];
        cost = Env.transCost ;
        cap  = Env.transCap;

        queueSizeD = new int[Env.nDOS];
      
        queueD = new ArrayList<>();
        aggD   = new ArrayList<>();
        for(int i=0 ; i<Env.nDOS ; i++) {
            queueD.add(new Bidstep[100][Demand.MAXBIDS]);
            aggD.add(new Bidstep[Demand.MAXBIDS]);
        }
        
        clearQueuesD();
    }
    
    /**
     * Build the agent's net demand curve
     */
    private void drawLoad() {
 
        Env.Draw draw;
        int max = 9858; 
        int rand;
        int row;
        
        // generate a random number of input lines to skip
        // max was originally hard-coded and is left that 
        // way for compatibility

        rand = (int)(runiform() * max);
        
        if( sd_type.equals("D") )
           draw = Env.drawListD.get(rand);
        else
           draw = Env.drawListS.get(rand);
        
        load  = draw.load;
        elast = draw.elast;

        //number of steps to create curve
        steps = (int) (runiform() * maxstep + 2);

        //call draw function based on the type of end user
        if (sd_type.equals("D")) 
            demand = Demand.makeDemand(load,elast,steps);
        else 
            demand = Demand.makeSupply(load,elast,steps);

        bids = demand.bids;
    }
     
    /**
     * Aggregate demands from child nodes
     */
    private Demand sumDemands(int dos_id) {
        Demand aggD;
        Demand this_dem;

        for(Msg msg: getMsgs(Msg.Types.DEMAND,dos_id)) 
            appendQueueD(msg.getDemand(),msg.dos_id);

        aggD = new Demand( queueD.get(dos_id)[0] );
        for(int i=1 ; i < queueSizeD[dos_id] ; i++) {
            this_dem = new Demand( queueD.get(dos_id)[i] );
            aggD = aggD.aggregateDemand(this_dem);
        }

        return aggD;
    }

    //change the step prices considering transation cost
    private Demand addCost(Demand dem, int c, int drop) {
        Demand newD;
        Bidstep[] tmp;
        Bidstep[] bids;
        int i;

        newD = new Demand();
        tmp  = newD.bids;
        bids = dem.bids;

        //decrease the price level of steps with positive quantity
        for(i=0 ; (bids[i] != null) && (bids[i].q_min >= 0) ; i++)
            tmp[i] = new Bidstep(bids[i].p-c,bids[i].q_min,bids[i].q_max);
        
        //if there is no step with positive quantity 
        if(i == 0){
            for( ; bids[i] != null ; i++)
                tmp[i] = new Bidstep(bids[i].p+c,bids[i].q_min,bids[i].q_max);
        }
        if(bids[i] != null ){
            //if there is a vertical overlap with y axis
            if(bids[i].q_max == 0){
                //set the two upper and lower limits around the balance price
                int mid = ((bids[i-1].p + bids[i].p)/2);
                //avoid negative value for the lower step
                if((mid-c) < 0)
                    setP_c(0,mid+c, drop);
                else
                    setP_c(mid-c,mid+c, drop);
                //increase the price level of steps with positive quantity
                for(; bids[i] != null ; i++ )
                    tmp[i] = new Bidstep(bids[i].p+c,bids[i].q_min,bids[i].q_max);
            
            //if a horizontal step indicate the balance price
            }else{
                //set the two upper and lower limits around the balance price
                if((bids[i-1].p-c) < 0)
                    setP_c(0,bids[i-1].p+c, drop);
                else
                    setP_c(bids[i-1].p-c,bids[i-1].p+c, drop);

                //divide the middle step into two steps with +c/-c prices
                
                tmp[i]   = new Bidstep(bids[i].p-c,0,bids[i].q_max);
                tmp[i+1] = new Bidstep(bids[i].p+c,bids[i].q_min,0);
                i++;
                
                //increase the price level of steps with positive quantity
                
                for(; bids[i] != null ; i++)
                    tmp[i+1] = new Bidstep(bids[i].p+c,bids[i].q_min,bids[i].q_max);
            }
        }

        return newD;
    }

    
    /**
     * Find the actual price for the end users considering transaction cost and capacity limit
     */
    private int findReportPrice(int drop) {
        Demand dem;
        int pr;
        int pc0;
        int pc1;

        pr  = getBl(drop);
        pc0 = getP_c(drop)[0];
        pc1 = getP_c(drop)[1];
        dem = new Demand( aggD.get(drop) );

        return dem.getP(pr,pc0,pc1,cost,cap);
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
                drawLoad();
                Env.printLoad(this,"base",demand);
                do_send_demands();
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
     * Send demand to parent node
     */
    private void do_send_demands() {
        for(int dos_id=0 ; dos_id<Env.dos_runs.length ; dos_id++) 
            reportDemand(new Demand(bids),dos_id);
    }

    /**
     * Aggregate net demands of leaf nodes
     */
    private void do_agg_end() {

        Demand this_agg;
        Demand tmp;
        
        Env.log.println("node "+own_id);

        for(int dos_id=0 ; dos_id<Env.nDOS ; dos_id++) {

            // do the aggregation and save the result

            this_agg = sumDemands(dos_id) ;
            aggD.set(dos_id,this_agg.bids);
            Env.printLoad(this,Env.dos_runs[dos_id],this_agg);

            // adjust for transmission cost and constraint

            tmp = addCost(this_agg, cost, dos_id);
            tmp = tmp.addCapacity(cap);

            // send to parent

            reportDemand(tmp,dos_id);
        }

        clearQueuesD();

    }

    /**
     * Aggregate net demands of middle nodes
     */
    private void do_agg_mid() {

        Env.log.println("node "+own_id);

        Demand this_agg;
        ArrayList<Bidstep[]> agg;
        Bidstep[] thisD;

        agg = new ArrayList<>();
        for(int dos_id=0 ; dos_id<Env.nDOS ; dos_id++) {
            this_agg = sumDemands(dos_id) ;
            agg.add( this_agg.bids );
            aggD.set(dos_id,this_agg.bids);
            Env.printLoad(this,Env.dos_runs[dos_id],this_agg);
        }

        //find the balance price for each case of dropped nodes
        for (int j = 0; j < Env.nDOS ; j++) {
            int i, bl=0, min=1;

            thisD = agg.get(j);
            for (i=0 ; (thisD[i] != null) && (thisD[i].q_max >= 0) ; i++) {
                bl  = thisD[i].p;
                min = thisD[i].q_min;
            }

            //if there is not any balance point- report -1 as price
            if ((i == 0) || ((thisD[i] == null)&&(min > 0))) {
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
        
        Env.log.println("node "+own_id);

        for(int i=0 ; i<Env.nDOS ; i++ ) {
            reportPrice(getBl(i),i);
        }
    }

    /**
     * Report the balance prices from the middle nodes to leaf nodes
     */
    private void do_report_end() {

        Bidstep[] thisD;
        int child_id;

        for(Msg msg: getMsgs(Msg.Types.PRICE)) 
            setBl(msg.getPrice(),msg.dos_id);

        Env.log.println("node "+own_id);

        //find the balance price for each case of dropped nodes
        for (int j = 0; j < Env.nDOS ; j++) {
            int i, bl = 0;
            thisD = aggD.get(j);
            for(i=0 ; (thisD[i] != null) && (thisD[i].q_max >= 0) ; i++) 
                bl = thisD[i].p;
            Env.log.println("node_id: " + own_id  + " balance price: " + bl);
        }
        
        int[] report = new int[Env.nDOS];
        //set the report values for each case of dropped nodes
        for (int drop = 0; drop < Env.nDOS ; drop++) {
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
            reportPrice(report[i],i);
        }
    }

    /**
     * Calculate actual loads at the leaf nodes
     *
     * Report net demand as 0 if no price was found
     */
    private void do_calc_load() {
        Demand dem;
        int bl;
        int ex;
        String dos;
            
        for(Msg msg: getMsgs(Msg.Types.PRICE)) 
            setBl(msg.getPrice(),msg.dos_id);

        for(int i=0 ; i<Env.dos_runs.length ; i++) {
        
            dem = new Demand(bids);
            bl  = getBl(i);
            ex  = dem.getQ(bl);

            dos = Env.dos_runs[i];
            Env.printResult(this,dos,bl,ex);
        }
    }
}

