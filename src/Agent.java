import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    
    public static final String VER = "2.0";

    //indicates the number of each run including definite number of steps
    static int runTime = 0;

    //indicates the transition cost
    int cost;

    //indicates the capacity constraint
    int cap; 
   
    static int maxstep = 14;
    static int maxbids = 400;

    //inherits from Env class
    Env e;
        
    //indicates the type of node (end user = 3, middle node = 2, root node = 1)
    int type;
    
    //indicates parents' id
    int up_id;
    
    //indicates own id
    int own_id;
    
    //indicates supply or demand type
    int sd_type;
    
    //indicates initial load for end user extracted from "testdraw.csv"
    //indicates the elasticity of end user extracted from "testdraw.csv" 
    double load;
    double elast;
    
    //indicates the vector of bids drawn from initial load, elast, and number of steps
    Bid[] bids;
	
    //indicates the total number of bid elements
    int bidSize;
    
	//indicates the three vectors including the id number of droped nodes 
    int[][] ran;
    
	//inidicates the queue variable for Bid type filled by childNodes 
    //Four vactors for different cases of dropped nodes    
    ArrayList<Bid[][]> queueD;
    
	//indicates the size of each vector of queueD
    int[] queueSizeD;
    
	//indicates the aggregated net demands for each case of dropped nodes    
    ArrayList<Bid[]> aggD;
    
	//indicates the balance price for each case of dropped nodes    
    int[] bl;
    
	//indicates the upper and lower level around the balance prices considering transaction cost
    int[][] p_c;

    //  Parent and list of children

    Agent parent;
    ArrayList<Agent> children;


    public Bid[] getAggD(int drop) {
        return aggD.get(drop);
    }

    public void setAggD(Bid[] agg, int drop) {
        aggD.set(drop, agg);
    }

    public int[][] getRan() {
        return ran;
    }

    public void setParent(Agent Parent) {
        this.parent = Parent;
        Parent.children.add(this);
    }

    public void setBidSize(int bidSize) {
        this.bidSize = bidSize;
    }

    public int getBl(int drop) {
        return bl[drop];
    }

    public void setBl(int bl, int drop) {
        this.bl[drop] = bl;
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

    public int getUp_id() {
        return up_id;
    }

    public int[] getP_c(int drop) {
        return p_c[drop];
    }

    public void setP_c(int p0, int p1, int drop) {
        p_c[drop][0] = p0;
        p_c[drop][1] = p1;
    }

    public void appendQueueD(Bid[] bids, int drop) {
        Bid tmp[][];
        tmp = queueD.get(drop);
        tmp[queueSizeD[drop]] = bids;
        queueD.set(drop, tmp);
        queueSizeD[drop]++;
    }

    private double runiform() {
        return Env.runiform() ;
    }

    public Agent(SimState state, Agent mkt, int type, int up_id, int own_id, int sd_type) {
        super();

        this.type = type;
        this.sd_type = sd_type;
        this.up_id = up_id;
        this.own_id = own_id;

        parent   = mkt;
        children = new ArrayList<>();
        
        e    = (Env) state;
        bl   = new int[4];
        ran  = new int[3][];
        p_c  = new int[4][2];
        cost = Env.transCost ;
        cap  = Env.transCap;

        if (mkt != null) {
            mkt.children.add(this);
        }

        queueSizeD = new int[4];
      
        queueD = new ArrayList<>();
        queueD.add(new Bid[100][maxbids]);
        queueD.add(new Bid[100][maxbids]);
        queueD.add(new Bid[100][maxbids]);
        queueD.add(new Bid[100][maxbids]);

        aggD = new ArrayList<>();
        aggD.add(new Bid[maxbids]);
        aggD.add(new Bid[maxbids]);
        aggD.add(new Bid[maxbids]);
        aggD.add(new Bid[maxbids]);

        clearQueuesD();
    }
    
        
    //pull out the index of specific value in the given vector (arr)
    public int getArrayIndex(int[] arr, int value) {
            int k=-1;
            for(int i=0; (arr != null) && (i<arr.length);i++){
                if(arr[i]==value){
                    k=i;
                    break;
                }
            }
            return k;
        }

    //Populate the bids from the input csv file
    public double[] findnewLoad() throws FileNotFoundException, IOException{
  
        BufferedReader br;
	String line;
        String cvsSplitBy = ",";
        
        br = new BufferedReader(Util.openRead(Env.fileDraws));
        br.readLine();
        
        line = br.readLine();
        //skip demand rows if the end user is a seller node
        if(sd_type == 0)
            while (line!=null && !(line.split(cvsSplitBy)[1].equals("\"D\""))){
                line = br.readLine();
            }
        
        //define a random value
        int max = 9858;//based on the length of input file
        int min = 0;
        int random;
        random= (int)(runiform() * max + min);
        
        //skip the input lines before the random id
        int tmp =0;
        for(int i =0 ; i < random; i++)
            line = br.readLine();   
         
        //pull out the elasticity and initial load from the random row
        String[] values = line.split(cvsSplitBy);
        double[] lds = new double[2];
        lds[0] =  Double.parseDouble(values[2]);
        lds[1] =  Double.parseDouble(values[3]);

        br.close();
        return lds;
    }
    
     
    //call aggragate function  on the queue
    public ArrayList<Bid[]> runsim(){

        Bid aggBidD[];
        
        //initiate an arraylist for the aggregated vectors
        ArrayList<Bid[]> agg = new ArrayList<>();
        
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
    private Bid[] addCost(Bid[] bids, int c, int drop) {
        Bid[] tmp = new Bid[maxbids];
        int i;
        //decrease the price level of steps with positive quantity
        for(i = 0; (bids[i] != null) && (bids[i].getQ_min() >= 0); i++ )
            tmp[i] = new Bid(bids[i].getPtice()-c,bids[i].getQ_min(),bids[i].getQ_max());
        
        //if there is no step with positive quantity 
        if(i == 0){
                for(; bids[i] != null ; i++)
                    tmp[i] = new Bid(bids[i].getPtice()+c,bids[i].getQ_min(),bids[i].getQ_max());
        }
        if(bids[i] != null ){
            //if theere is a vertical overlaop with y axis
            if(bids[i].getQ_max() == 0){
                //set the two upper and lower limits around the balance price
                int mid = ((bids[i-1].getPtice()+bids[i].getPtice())/2);
                //avoid negative value for the lower step
                if((mid-c) < 0)
                    setP_c(0,mid+c, drop);
                else
                    setP_c(mid-c,mid+c, drop);
                //increase the price level of steps with positive quantity
                for(; bids[i] != null ; i++ )
                    tmp[i] = new Bid(bids[i].getPtice()+c,bids[i].getQ_min(),bids[i].getQ_max());
            
            //if a horizontal step indicate the balance price
            }else{
                //set the two upper and lower limits around the balance price
                if((bids[i-1].getPtice()-c) < 0)
                    setP_c(0,bids[i-1].getPtice()+c, drop);
                else
                    setP_c(bids[i-1].getPtice()-c,bids[i-1].getPtice()+c, drop);
                //divide the middle step to two steps with +c/-c prices
                tmp[i] = new Bid(bids[i].getPtice()-c,0,bids[i].getQ_max());
                tmp[i+1] = new Bid(bids[i].getPtice()+c,bids[i].getQ_min(),0);
                i++;
                //increase the price level of steps with positive quantity
                for(; bids[i] != null ; i++)
                    tmp[i+1] = new Bid(bids[i].getPtice()+c,bids[i].getQ_min(),bids[i].getQ_max());
            
            }
        }
        return tmp;

        
    }

    //put capacity constrain on the net demand
    private Bid [] addCapacity(Bid[] bids, int cap) {
        Bid[] tmp = new Bid[maxbids];
        int i, j=0;
        
        //skip the steps with more quantity than cap
        for(i =0; bids[i].getQ_min() >= cap; i++);
        if(bids[i] == null)
            return null;
        //set the right corner step
        if(bids[i].getQ_max() > cap){
            tmp[j++]= new Bid(bids[i].getPtice(),bids[i].getQ_min(),cap);
            i++;
        }
        //consider the steps between two capacity limits
        for(;(bids[i] != null) && (bids[i].getQ_min() >= ((-1)*(cap))); i++)
            tmp[j++] = new Bid(bids[i].getPtice(),bids[i].getQ_min(),bids[i].getQ_max());
        if (bids[i] == null) 
            return tmp;
        //set the left corner step
        else
            tmp[j] = new Bid(bids[i].getPtice(),((-1)*(cap)),bids[i].getQ_max());
        
        return tmp;
        
    }
    
    //find the actual price for the end users considering transaction cost and capacity limit
    private int findReportPrice(int drop) {
        int report = 0;
        //if the whole balance price is between local balance price +c/-c
        if ((getBl(drop) >= getP_c(drop)[0]) && (getBl(drop) <= getP_c(drop)[1])) {
            report = ((getP_c(drop)[0] + getP_c(drop)[1]) / 2);
//            Env.log.println("No Capacity limit 0! " + report + " prob: " + drop);
        } else {
            //if the whole balance price is more than local balance price +c
            if (getBl(drop) > getP_c(drop)[1]) {
                int i, p = 0;
                //skip the bids with more quantity than (-1) * cap & less price than the balance price
                for (i = 0; ((getAggD(drop)[i] != null)
                        && (getAggD(drop)[i].getPtice() <= getBl(drop))
                            && (getAggD(drop)[i].getQ_max() >= ((-1) * cap))); i++)
                                p = getAggD(drop)[i].getPtice();

                if (getAggD(drop)[i] == null) {
                    report = p;
                //if the target step passed the cap line
                } else if (getAggD(drop)[i].getQ_max() < ((-1) * cap)) {
                        if(getBl(drop) <= p + cost) {
                            report = getBl(drop) - cost;
//                            Env.log.println("No Capacity limit 1! " + report + " prob: " + drop);
                        }else{
                            report = p;
//                            Env.log.println("Capacity limited 1! " + report + " prob: " + drop);
                        }
                        
                    //put limit equal to cap
                } else {
                        report = getBl(drop) - cost;
//                        Env.log.println("No Capacity limit 1! " + report + " prob: " + drop);
                                                 
                        
                }
                
            //if the whole balance price is less than local balance price +c    
            } else if (getBl(drop) < getP_c(drop)[0]) {
                int i, p = 0;
                //skip the bids with more quantity than cap
                for (i = 0; ((getAggD(drop)[i] != null)
                        && (getAggD(drop)[i].getQ_min() >= cap)); i++)
                             p = getAggD(drop)[i].getPtice();
                if (getAggD(drop)[i] == null) {
                    report = p;
                } else {
                    if (getBl(drop) > getAggD(drop)[i].getPtice() - cost) {
                        report = getBl(drop) + cost;
//                        Env.log.println("No Capacity limit 2! " + report + " prob: " + drop);
                    //put limit equal to cap
                    } else {
                        report = getAggD(drop)[i].getPtice();
//                        Env.log.println("Capacity limited 2! " + report + " prob: " + drop);
                    }
                }

            }

        }
        return report;
    }

    /**
     * Calculate the net demand considering the balance price
     */
     private int findExcessDemand(int price){
        int i;
        for(i = 0;(bids[i] != null)  && (bids[i].getPtice() < price); i++);
          
        if(bids[i] == null) 
             Env.log.println("error price: " + price);
                
        int q;
        if(bids[i].getPtice() == price)  //where the balance price is the same as step price
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
        
            Env.log.println("node "+own_id);
            Env.log.println("initiate random list of users to drop");

            //initiate three vector for the random ids
            int[][] ran = new int[3][];
            
            //initiate the vectors for three case of 1, 5, and 10 drops
            ran[0] = new int[1];
            for (int i = 0; i < 1; i++) {
                ran[0][i] = (int) (runiform() * 100 + 1);
            }
            
            ran[1] = new int[5];
            for (int i = 0; i < 5; i++) {
                int tmp = (int) (runiform() * 100 + 1);
                if (getArrayIndex(ran[1], tmp) < 0) {
                    ran[1][i] = tmp;
                } else {
                    i--;
                }
            }
            
            ran[2] = new int[10];
            for (int i = 0; i < 10; i++) {
                int tmp = (int) (runiform() * 100 + 1);
                if (getArrayIndex(ran[2], tmp) < 0) {
                    ran[2][i] = tmp;
                } else {
                    i--;
                }
            }
            
            //set the random variable
            this.ran = ran;
    }

    /**
     * Create net demand curves for the end users
     */
    private void do_init_load() {

            //initiate the initial load and elasticity
            double tmp[];
            
            //find random values from the input file "testdraw.csv"
            try {
                tmp   = findnewLoad();
                load  = tmp[0];
                elast = tmp[1];
            } catch (IOException ex) {
                Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException("Exception finding load");
            }

            //initiate the number of steps to create curve
            int step = (int) (runiform() * maxstep + 2);

            //call draw function based on the type (demand/supply) of end users
            if (sd_type == 0) {
                bids = drawDemand(load, elast, step);
            } else {
                bids = drawSupply(load, elast, step);
            }

            //set the number of steps in each curve
            setBidSize(3 * step);

            //populate the parents queues for different cases of dropped nodes 
            e.dbus.toQueue(bids, 0, parent.own_id, sd_type);
            if (getArrayIndex(parent.getRan()[0], own_id % 100) < 0) {
                e.dbus.toQueue(bids, 1, parent.own_id, sd_type);
            }
            if (getArrayIndex(parent.getRan()[1], own_id % 100) < 0) {
                e.dbus.toQueue(bids, 2, parent.own_id, sd_type);
            }
            if (getArrayIndex(parent.getRan()[2], own_id % 100) < 0) {
                e.dbus.toQueue(bids, 3, parent.own_id, sd_type);
            }
    }

    private void do_agg_end() {

            Env.log.println("node "+own_id);

            ArrayList<Bid[]> agg;
            Bid tmp[];
            
            //call rusim to aggregate the net demands
            agg = runsim();

            //set the agg vectors as class variable
            for (int i = 0; i < 4; i++) {
                setAggD(agg.get(i), i);
            }

            //call addCost and addCapacity functions to consider transaction costs and capacity constrains
            for (int i = 0; i < 4; i++) {
                tmp = addCost(agg.get(i), cost, i);
                e.dbus.toQueue(addCapacity(tmp, cap), i, parent.own_id, 0);
            }

            clearQueuesD();

    }

    //Fourth cycle: Aggregate the net demand curves of the middle nodes
    private void do_agg_mid() {

            Env.log.println("node "+own_id);

            ArrayList<Bid[]> agg;
            
            //call runsim to aggregate the net demands
            agg = runsim();

            //find the balance price for each case of dropped nodes
            for (int j = 0; j < 4; j++) {
                int i, bl = 0, min =1;
                for (i = 0; ((agg.get(j)[i] != null)
                        && (agg.get(j)[i].getQ_max() >= 0)); i++) {
                    bl = agg.get(j)[i].getPtice();
                    min = agg.get(j)[i].getQ_min();
                }

                //if there is not any balance point- report -1 as price
                if ((i == 0) || ((agg.get(j)[i] == null)&&(min > 0))) {
                    bl = -1;
                    Env.log.println("failed at drop: " + j);
                }

                //set the balance price as the class variable
                setBl(bl, j);
                
                
                //write the balance prices on csv file for each case of dropped nodes
                switch (j) {
                    case 0:
                        Env.out.write("\n" + (runTime + 1) + "," + own_id + ",0,"
                                + getBl(j) + "," + 0);
                        Env.log.println("node_id: " + own_id  +  " Balance Price: " + bl
                                                    + " Prob: 0");
                        break;
                    case 1:
                        Env.out.write("\n" + (runTime + 1) + "," + own_id + ",1,"
                                + getBl(j) + "," + 0);
                        Env.log.println("node_id: " + own_id + " Balance Price: " + bl
                                                    + " Prob: 1");
                        break;
                    case 2:
                        Env.out.write("\n" + (runTime + 1) + "," + own_id + ",5,"
                                + getBl(j) + "," + 0);
                        Env.log.println("node_id: " + own_id + " Balance Price: " + bl
                                                    + " Prob: 5");
                        break;
                    case 3:
                        Env.out.write("\n" + (runTime + 1) + "," + own_id + ",10,"
                                + getBl(j) + "," + 0);
                        Env.log.println("node_id: " + own_id + " Balance Price: " + bl
                                                    + " Prob: 10");
                        break;
                        
                    default:
                        throw new RuntimeException("Unexpected case in do_agg_mid()");
                        
                }

            }

            clearQueuesD();

            //add the population number
            runTime++;
    }

    //Fifth cycle: report the balace price from the root node to middle nodes
    private void do_report_mid() {
        
            int child_id;

            Env.log.println("node "+own_id);

            //report the balance prices to kid nodes

            for (Agent child: children) {
                child_id = child.own_id;
                e.dbus.toQueue(getBl(0), 0, child_id, 0);
                e.dbus.toQueue(getBl(1), 1, child_id, 0);
                e.dbus.toQueue(getBl(2), 2, child_id, 0);
                e.dbus.toQueue(getBl(3), 3, child_id, 0);
            }

    }

    //sixth cycle: report the balance prices from the middle nodes to end users
    private void do_report_end() {

            int child_id;

            Env.log.println("node "+own_id);

            //find the balance price for each case of dropped nodes
            for (int j = 0; j < 4; j++) {
                int i, bl = 0;
                for (i = 0; ((getAggD(j)[i] != null)
                        && (getAggD(j)[i].getQ_max() >= 0)); i++) {
                    bl = getAggD(j)[i].getPtice();
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

            //write the balance prices on csv file for each case of dropped nodes
            Env.out.write("\n" + runTime + "," + own_id + ",0,"
                    + report[0] + "," + 0);
            Env.out.write("\n" + runTime + "," + own_id + ",1,"
                    + report[1] + "," + 0);
            Env.out.write("\n" + runTime + "," + own_id + ",5,"
                    + report[2] + "," + 0);
            Env.out.write("\n" + runTime + "," + own_id + ",10,"
                    + report[3] + "," + 0);


            //set the kid nodes balance prices

            for(Agent child: children) {
                child_id = child.own_id ;
                e.dbus.toQueue(report[0], 0, child_id, 0);
                e.dbus.toQueue(report[1], 1, child_id, 0);
                e.dbus.toQueue(report[2], 2, child_id, 0);
                e.dbus.toQueue(report[3], 3, child_id, 0);
            }

    }

    //seventh cycle: find excess demand for the end users
    private void do_calc_load() {
            
            //initiate the excess demand vector
            int[] ex = new int[4];
            //find the excess demand for the four cases of dropped nodes
            for (int i = 0; i < 4; i++) {
                //report 0 in case of no balance point
                if (getBl(i) <= -1) {
                    ex[i] = 0;
                } else {
                    ex[i] = findExcessDemand(getBl(i));
                }
            }
            
            //report the excess demands for each cased of dropped nodes
            Env.out.write("\n" + runTime + "," + own_id + ",0,"
                    + getBl(0) + "," + ex[0]);

            if (getArrayIndex(parent.getRan()[0], own_id % 100 + 1) < 0) 
                Env.out.write("\n" + runTime + "," + own_id + ",1,"
                        + getBl(1) + "," + ex[1]);
            
            if (getArrayIndex(parent.getRan()[1], own_id % 100 + 1) < 0) 
                Env.out.write("\n" + runTime + "," + own_id + ",5,"
                        + getBl(2) + "," + ex[2]);
            
            if (getArrayIndex(parent.getRan()[2], own_id % 100 + 1) < 0) 
                Env.out.write("\n" + runTime + "," + own_id + ",10,"
                        + getBl(3) + "," + ex[3]);
            

        }

    /**
     * Create demand curves based on initial load, elasticity, and number of steps
     */
    private Bid[] drawDemand(double Q40, double elst, int step){
        Bid [] result = new Bid[maxbids/*step*//*+1*/];
        
        int iniprice= 40 +  (int) (runiform() * 12 - 6);
        int p0 = iniprice/step;
        int p1 = iniprice*2/step;
        int q1= (int) (Q40 * pow((double)p0/iniprice,elst));
        int q2=(int)(Q40 * pow((double)p1/iniprice,elst));
        result[0]  = new Bid(p0, q2, q1);
        
        //create the number of steps below the price=40
        for(int i =1 ; i < step; i ++){
            p1 = iniprice*(i+1)/step;
            q1= q2;
            q2= (int) (Q40 * pow((double)p1/iniprice,elst));
            result[i]  = new Bid(p1, q2, q1);
        }
        
        //create twice the number of steps upper the proce = 40
        for(int i =1 ; i < 2*step; i ++){
            p1 = iniprice + (360*i)/(2*step);
            q1= q2;
            q2= (int) (Q40 * pow((double)p1/iniprice,elst));
            result[step + i - 1]  = new Bid(p1, q2, q1);
        }
        
        return result;
    }
    
    /**
     * Create supply curves with reverse quantities in comparison to demand curve
     */
    private Bid[] drawSupply(double Q40, double elst, int step){
        Bid [] result = new Bid[maxbids/*step*//*+1*/];
        int iniprice= 40 + (int) (runiform() * 12 - 6);
        
        int p0 = iniprice/step;
        int p1 = (iniprice*2)/step;

        int q1=(int)((-1) * (Q40 * pow((double)p0/iniprice,elst)));
        int q2=(int)((-1) * (Q40 * pow((double)p1/iniprice,elst)));
        result[0]  = new Bid(p0, q1, q2);
        
        //create the number of steps below the price=40
        for(int i =1 ; i < step; i ++){
            p1 = iniprice*(i+1)/step;
            q1= q2;
            q2= (int) ((-1) * (Q40 * pow((double)p1/iniprice,elst)));
            result[i]  = new Bid(p1, q1, q2);
        }
        
        //create twice the number of steps upper the proce = 40
        for(int i =1 ; i < 2*step ; i++){
            p1 = iniprice+ 360*i/(2*step);
            q1= q2;
            q2= (int) ((-1) * (Q40 * pow((double)p1/iniprice,elst)));
            result[step + i - 1]  = new Bid(p1, q1, q2);
        }
        
        return result;
    }
    
    //aggregate two net demand curves
    private Bid[] aggregateDemand(Bid[] bid1, Bid[] bid2) {
        Bid [] aggBid = new Bid[maxbids];
        
        int i =0, j =0, k = 0;
        int min = 0, sd;
        
        //screen all the steps in the two input array of bids
        while((bid1[i]!= null)&&(bid2[j]!=null)){
            
            //initiate the aggregate step considering the minimum price level and sum of the right corners as the max quantity
            aggBid[k] = new Bid(minBid(bid1[i], bid2[j]).getPtice()
                                            ,0,bid1[i].getQ_max() + bid2[j].getQ_max());
            
            
            if(bid1[i].getPtice() < bid2[j].getPtice()){
                    //if it is the fist step
                    if(k == 0)
                        aggBid[k].setQ_min(bid1[i].getQ_min() + bid2[j].getQ_max());
                    i++;
                    
            }else if(bid1[i].getPtice() > bid2[j].getPtice()){
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
    
  
    //find the minimum price between two input bids
    public Bid minBid(Bid bid1, Bid bid2) {
        if(bid1.getPtice() < bid2.getPtice())
            return bid1;
        else    
            return bid2;
        
    }
}


