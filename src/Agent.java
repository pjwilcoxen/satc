import java.util.ArrayDeque;
import java.util.ArrayList;
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

    public void appendQueueD(Demand demand, int drop) {
        Bidstep tmp[][];
        tmp = queueD.get(drop);
        tmp[queueSizeD[drop]] = demand.bids;
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
        if (sd_type.equals("D")) 
            bids = drawDemand();
        else 
            bids = drawSupply();
    }
     
    //call aggragate function  on the queue
    private Demand sumDemands(int dos_id) {

        for(Msg msg: getMsgs(Msg.Types.DEMAND,dos_id)) 
            appendQueueD(msg.getDemand(),msg.dos_id);

        Bidstep[] aggD;
        
        aggD = queueD.get(dos_id)[0];
        for(int i=1 ; i < queueSizeD[dos_id] ; i++)
            aggD = aggregateDemand(aggD, queueD.get(dos_id)[i]);
        
        return new Demand(aggD);
    }

    //change the step prices considering transation cost
    private Demand addCost(Demand demand, int c, int drop) {
        Demand newD;
        Bidstep[] tmp;
        Bidstep[] bids;
        int i;

        newD = new Demand();
        tmp  = newD.bids;
        bids = demand.bids;

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
        dem = new Demand( getAggD(drop) );

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
     * Create net demand curve for the end user
     *
     * Build a random net demand and then send it as a message
     * to this agent's parent node.
     */
    private void do_init_load() {

        drawLoad();

        for(int i=0 ; i<Env.dos_runs.length ; i++) {
            Msg msg = new Msg(this,parent.own_id);
            msg.setDemand(new Demand(bids));
            msg.dos_id = i;
            dbus.send(msg);
        }
    }

    /**
     * Aggregate net demands of leaf nodes
     */
    private void do_agg_end() {

        Demand this_agg;
        Demand tmp;
        
        Env.log.println("node "+own_id);

        for(int dos_id=0 ; dos_id<4 ; dos_id++) {

            // do the aggregation and save the result

            this_agg = sumDemands(dos_id) ;
            aggD.set(dos_id,this_agg.bids);

            // adjust for transmission cost and constraint

            tmp = addCost(this_agg, cost, dos_id);
            tmp = tmp.addCapacity(cap);

            // send to parent

            Msg msg = new Msg(this,parent.own_id);
            msg.setDemand(tmp);
            msg.dos_id = dos_id;
            dbus.send(msg);
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
        
        agg = new ArrayList<>();
        for(int dos_id=0 ; dos_id<4 ; dos_id++) {
            this_agg = sumDemands(dos_id) ;
            agg.add( this_agg.bids );
            aggD.set(dos_id,this_agg.bids);
        }

        //find the balance price for each case of dropped nodes
        for (int j = 0; j < 4; j++) {
            int i, bl = 0, min =1;
            for (i = 0; ((agg.get(j)[i] != null)
                    && (agg.get(j)[i].q_max >= 0)); i++) {
                bl = agg.get(j)[i].p;
                min = agg.get(j)[i].q_min;
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
        
        Env.log.println("node "+own_id);

        for(int i=0 ; i<4 ; i++ ) {
            reportPrice(getBl(i),i);
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
                    && (getAggD(j)[i].q_max >= 0)); i++) {
                bl = getAggD(j)[i].p;
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
            reportPrice(report[i],i);
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
        Demand dem;

        for(i=0 ; i<Env.dos_runs.length ; i++) {
        
            dos = Env.dos_runs[i];
            bl  = getBl(i);

            if (bl <= -1) 
                ex = 0;
            else {
                dem = new Demand(bids);
                ex = dem.getQ(bl);
            }

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
            aggBid[k] = new Bidstep(minBid(bid1[i], bid2[j]).p
                                            ,0,bid1[i].q_max + bid2[j].q_max);
            
            
            if(bid1[i].p < bid2[j].p){
                    //if it is the fist step
                    if(k == 0)
                        aggBid[k].q_min = bid1[i].q_min + bid2[j].q_max;
                    i++;
                    
            }else if(bid1[i].p > bid2[j].p){
                    //initiate the left corner of the first step
                    if(k == 0)
                        aggBid[k].q_min = bid1[i].q_max + bid2[j].q_min;
                    j++;
            }else{ 
                    //initiate the left corner of the first step
                    if(k == 0)
                        aggBid[k].q_min = bid1[i].q_min + bid2[j].q_min;
                    i++;
                    j++;
            }
            //initiate the left corner of each step based on the right corner of the next step
            if(k > 1)
                aggBid[k-1].q_min = aggBid[k].q_max;
            
            k++;
            
          }

        //initiate the last step
        aggBid[k-1].q_min = aggBid[k-1].q_max - 100;
        
        return aggBid;
    }
    
  
    /**
     * Find the minimum price between two input bids
     */
    private Bidstep minBid(Bidstep bid1, Bidstep bid2) {
        if(bid1.p < bid2.p)
            return bid1;
        else    
            return bid2;
        
    }
}


