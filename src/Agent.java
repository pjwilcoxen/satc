//  Agent.java

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.engine.SimState;
import sim.engine.Steppable;

import static java.lang.Integer.min;
import static java.lang.Math.pow;

public class Agent implements Steppable {
     
    public static final String vers = "2.0";

    //indicates the number of each run including difinite number of steps
    static int runTime = 0;

    //indicates the transition cost
    static int cost = 1;
    //indicates the capacity constraint
    static int cap = 2500; //2000;
   
    static int maxstep = 14;
    static int maxbids = 400;

    //inherits from Env class
    Env e;
        
    //indicates local time, which is increased in every step
    int time;
    //indicates the type of node (end user = 3, middle node = 2, root node = 1)
    int type;
    //indicates parents' id
    int up_id;
    //indicates own id
    int own_id;
    //indicates supply or demand type
    int sd_type;
    //indicates initial load for end user extracted from "testdraw.csv"
    double load;
    //indicates the elasticity of end user extracted from "testdraw.csv" 
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
    //indicates the parent node
    Agent Parent;
    //indicates the kid nodes
    ArrayList<Agent> Children;
    //indicates the number of kids
    int childrenSize;

    public Bid[] getAggD(int drop) {
        return aggD.get(drop);
    }

    public void setAggD(Bid[] agg, int drop) {
        this.aggD.set(drop, agg);
    }

    public int[][] getRan() {
        return ran;
    }

    public void setRan(int[][] ran) {
        this.ran = ran;
    }

    public void setParent(Agent Parent) {
        this.Parent = Parent;
        Parent.appendChildren(this);
    }

    public int getBidSize() {
        return bidSize;
    }

    public void setBidSize(int bidSize) {
        this.bidSize = bidSize;
    }


    public Bid[] getBids() {
        return bids;
    }

    public void setBids(Bid[] bids) {
        this.bids = bids;
    }

    public void appendChildren(Agent k) {
        this.Children.add(k);
        this.childrenSize++;
    }

    public Agent getChildren(int i) {
        return this.Children.get(i);
    }

    public void setChildrenSize(int childrenSize) {
        this.childrenSize = childrenSize;
    }

    public int getChildrenSize() {
        return childrenSize;
    }
        
    public int getBl(int drop) {
        return bl[drop];
    }

    public void setBl(int bl, int drop) {
        this.bl[drop] = bl;
    }
        
    public void setQueueSizeD(int queueSize, int drop) {
        this.queueSizeD[drop] = queueSize;
    }

    public int getQueueSizeD(int drop) {
        return queueSizeD[drop];
    }
        
    public Bid[][] getQueueD(int drop) {
        return queueD.get(drop);
    }

    
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
    
    public int getSd_type() {
        return sd_type;
    }

    public void setSd_type(int sd_type) {
        this.sd_type = sd_type;
    }

    public double getLoad() {
        return load;
    }

    public double getElast() {
        return elast;
    }

    public void setLoad(double load) {
        this.load = load;
    }

    public void setElast(double elast) {
        this.elast = elast;
    }
        
    public void setUp_id(int up_id) {
        this.up_id = up_id;
    }

    public int getUp_id() {
        return up_id;
    }

    public void setOwn_id(int down_id) {
        this.own_id = down_id;
    }

    public int getOwn_id() {
        return own_id;
    }

    public int[] getP_c(int drop) {
        return p_c[drop];
    }

    public void setP_c(int p0, int p1, int drop) {
        this.p_c[drop][0] = p0;
        this.p_c[drop][1] = p1;
    }

    public void appendQueueD(Bid[] bids, int drop) {
        
        Bid[][] tmp = new Bid[maxbids][];
        tmp = this.queueD.get(drop);
        tmp[this.queueSizeD[drop]] = bids;
        this.queueD.set(drop, tmp);
        this.queueSizeD[drop]++;
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

        time = 0;
        childrenSize = 0;
        Parent = mkt;
        Children = new ArrayList<>();
        e = (Env) state;
        bl = new int[4];
        ran = new int[3][];
        p_c = new int[4][2];
        cost = e.transCost ;
        cap  = e.transCap;

        if (mkt != null) {
            mkt.appendChildren(this);
        }

        queueSizeD = new int[4];
      
        this.queueD = new ArrayList<>();
        this.queueD.add(new Bid[100][maxbids]);
        this.queueD.add(new Bid[100][maxbids]);
        this.queueD.add(new Bid[100][maxbids]);
        this.queueD.add(new Bid[100][maxbids]);

        this.aggD = new ArrayList<>();
        this.aggD.add(new Bid[maxbids]);
        this.aggD.add(new Bid[maxbids]);
        this.aggD.add(new Bid[maxbids]);
        this.aggD.add(new Bid[maxbids]);

        for (int i = 0; i < 4; i++) {
            setQueueSizeD(0, i);
        }

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
        ArrayList<double[]> loads = new ArrayList<double[]>();
  
        BufferedReader br = null;
	String line = "";
	String cvsSplitBy = ",";
        
        br = new BufferedReader(Util.openRead(Env.fileDraws));
        br.readLine();
        
        line = br.readLine();
        //skip demand rows if the end user is a seller node
        if(this.sd_type == 0)
            while (!(line.split(cvsSplitBy)[1].equals("\"D\""))){
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

        return lds;
    }
    
     
    //call aggragate function  on the queue
    public ArrayList<Bid[]> runsim(){

        Bid[] aggBidD = new Bid[maxbids];
        
        //initiate an arraylist for the aggregated vectors
        ArrayList<Bid[]> agg = new ArrayList<>();
        
        //call aggregate function on each queue based on number of drops
        for(int j = 0 ; j < 4 ; j ++){
            aggBidD = this.getQueueD(j)[0];
            //call aggregate function by the number of each queue's size
            for(int i=1 ; i < this.getQueueSizeD(j) ; i++)
                aggBidD = aggregateDemand(aggBidD, this.getQueueD(j)[i]);
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
                int mid = (int)((bids[i-1].getPtice()+bids[i].getPtice())/2);
                //avoid negative value for the lower step
                if((mid-c) < 0)
                    this.setP_c(0,mid+c, drop);
                else
                    this.setP_c(mid-c,mid+c, drop);
                //increase the price level of steps with positive quantity
                for(; bids[i] != null ; i++ )
                    tmp[i] = new Bid(bids[i].getPtice()+c,bids[i].getQ_min(),bids[i].getQ_max());
            
            //if a horizontal step indicate the balance price
            }else{
                //set the two upper and lower limits around the balance price
                if((bids[i-1].getPtice()-c) < 0)
                    this.setP_c(0,bids[i-1].getPtice()+c, drop);
                else
                    this.setP_c(bids[i-1].getPtice()-c,bids[i-1].getPtice()+c, drop);
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
        if ((getBl(drop) >= this.getP_c(drop)[0]) && (getBl(drop) <= this.getP_c(drop)[1])) {
            report = (int) ((this.getP_c(drop)[0] + this.getP_c(drop)[1]) / 2);
//            Env.log.println("No Capacity limit 0! " + report + " prob: " + drop);
        } else {
            //if the whole balance price is more than local balance price +c
            if (getBl(drop) > this.getP_c(drop)[1]) {
                int i, p = 0;
                //skip the bids with more quantity than (-1) * cap & less price than the balance price
                for (i = 0; ((this.getAggD(drop)[i] != null)
                        && (this.getAggD(drop)[i].getPtice() <= getBl(drop))
                            && (this.getAggD(drop)[i].getQ_max() >= ((-1) * cap))); i++)
                                p = this.getAggD(drop)[i].getPtice();

                if (this.getAggD(drop)[i] == null) {
                    report = p;
                //if the target step passed the cap line
                } else if (this.getAggD(drop)[i].getQ_max() < ((-1) * cap)) {
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
            } else if (getBl(drop) < this.getP_c(drop)[0]) {
                int i, p = 0;
                //skip the bids with more quantity than cap
                for (i = 0; ((this.getAggD(drop)[i] != null)
                        && (this.getAggD(drop)[i].getQ_min() >= cap)); i++)
                             p = this.getAggD(drop)[i].getPtice();
                if (this.getAggD(drop)[i] == null) {
                    report = p;
                } else {
                    if (getBl(drop) > this.getAggD(drop)[i].getPtice() - cost) {
                        report = getBl(drop) + cost;
//                        Env.log.println("No Capacity limit 2! " + report + " prob: " + drop);
                    //put limit equal to cap
                    } else {
                        report = this.getAggD(drop)[i].getPtice();
//                        Env.log.println("Capacity limited 2! " + report + " prob: " + drop);
                    }
                }

            }

        }
        return report;
    }

    //calculate the net demand considering the balance price
     public int findExcessDemand(int price){
        int i;
        for(i = 0;(getBids()[i] != null)  && (getBids()[i].getPtice() < price); i++);
          
        if(getBids()[i] == null) 
             Env.log.println("error price: " + price);
                
        int q;
        if(getBids()[i].getPtice() == price)  //where the balance price is the same as step price
            q = getBids()[i].getQ_min();
        else                                  //where the balance price is between two steps
            q = getBids()[i].getQ_max();
       
        return q;
        
     }


    //
    //  General step function -- calls subtype specific 
    //  functions in preparation for subclassing
    //

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
                System.out.println("Unexpected agent type in step");
                System.exit(0);
       }

        // increment internal time counter

        time++ ;
    }

    private void step_top() {
        switch (Env.stageNow) {
            case AGG_MID:
                do_agg_mid();
                break;

            case REPORT_MID:
                do_report_mid();
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
            this.setRan(ran);
    }

    //Second step: create net demand curves for the end users
    private void do_init_load() {

            //initiate the initial load and elasticity
            double[] tmp = new double[2];
            try {
                //find random values from the input file "testdraw.csv"
                tmp = findnewLoad();
                setLoad(tmp[0]);
                setElast(tmp[1]);

            } catch (IOException ex) {
                Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(0);
            }

            //initiate the numbber of spteps to create curve
            int step = (int) (runiform() * maxstep + 2);
            //call draw function based on the type (demand/supply) of end users
            if (getSd_type() == 0) {
                setBids(drawDemand(getLoad(), getElast(), step));
            } else {
                setBids(drawSupply(getLoad(), getElast(), step));
            }

            //set the number of steps in each curve
            setBidSize(3 * step);
            //populate the parents queues for different cases of dropped nodes 
            this.e.dbus.toQueue(getBids(), 0, this.Parent.getOwn_id(), this.getSd_type());
            if (getArrayIndex(this.Parent.getRan()[0], this.getOwn_id() % 100) < 0) {
                this.e.dbus.toQueue(getBids(), 1, this.Parent.getOwn_id(), this.getSd_type());
            }
            if (getArrayIndex(this.Parent.getRan()[1], this.getOwn_id() % 100) < 0) {
                this.e.dbus.toQueue(getBids(), 2, this.Parent.getOwn_id(), this.getSd_type());
            }
            if (getArrayIndex(this.Parent.getRan()[2], this.getOwn_id() % 100) < 0) {
                this.e.dbus.toQueue(getBids(), 3, this.Parent.getOwn_id(), this.getSd_type());
            }
    }

    private void do_agg_end() {

            Env.log.println("node "+own_id);

            ArrayList<double[]> dbids = new ArrayList<double[]>();
            ArrayList<Bid[]> agg = new ArrayList<>();
            //call rusim to aggregate the net demands
            agg = runsim();

            //set the agg vectors as class variable
            for (int i = 0; i < 4; i++) {
                this.setAggD(agg.get(i), i);
            }

            Bid[] tmp = null;
            //call addCost and addCapacity functions to consider transaction costs and capacity constrains
            for (int i = 0; i < 4; i++) {
                tmp = this.addCost(agg.get(i), cost, i);
                if (tmp != null) {
                    this.e.dbus.toQueue(this.addCapacity(tmp, cap), i, this.Parent.getOwn_id(), 0);
                }
            }

            //set the size of queues to zeto 
            for (int i = 0; i < 4; i++) {
                this.setQueueSizeD(0, i);
            }

    }

    //Fourth cycle: Aggregate the net demand curves of the middle nodes
    private void do_agg_mid() {

            Env.log.println("node "+own_id);

            ArrayList<Bid[]> agg = new ArrayList<>();
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
                        Env.out.write("\n" + (runTime + 1) + "," + this.getOwn_id() + ",0,"
                                + this.getBl(j) + "," + 0);
                        Env.log.println("node_id: " + this.own_id  +  " Balance Price: " + bl
                                                    + " Prob: 0");
                        break;
                    case 1:
                        Env.out.write("\n" + (runTime + 1) + "," + this.getOwn_id() + ",1,"
                                + this.getBl(j) + "," + 0);
                        Env.log.println("node_id: " + this.own_id + " Balance Price: " + bl
                                                    + " Prob: 1");
                        break;
                    case 2:
                        Env.out.write("\n" + (runTime + 1) + "," + this.getOwn_id() + ",5,"
                                + this.getBl(j) + "," + 0);
                        Env.log.println("node_id: " + this.own_id + " Balance Price: " + bl
                                                    + " Prob: 5");
                        break;
                    case 3:
                        Env.out.write("\n" + (runTime + 1) + "," + this.getOwn_id() + ",10,"
                                + this.getBl(j) + "," + 0);
                        Env.log.println("node_id: " + this.own_id + " Balance Price: " + bl
                                                    + " Prob: 10");
                        break;
                }

            }

            //set the size of queues to zero
            for (int i = 0; i < 4; i++) {
                this.setQueueSizeD(0, i);
            }

            //add the population number
            runTime++;
    }

    //Fifth cycle: report the balace price from the root node to middle nodes
    private void do_report_mid() {
        
            Env.log.println("node "+own_id);

            //report the balance prices to kid nodes
            for (int i = 0; i < getChildrenSize(); i++) {
                this.e.dbus.toQueue(getBl(0), 0, this.getChildren(i).getOwn_id(), 0);
                this.e.dbus.toQueue(getBl(1), 1, this.getChildren(i).getOwn_id(), 0);
                this.e.dbus.toQueue(getBl(2), 2, this.getChildren(i).getOwn_id(), 0);
                this.e.dbus.toQueue(getBl(3), 3, this.getChildren(i).getOwn_id(), 0);
            }

    }

    //sixth cycle: report the balance prices from the middle nodes to end users
    private void do_report_end() {

            Env.log.println("node "+own_id);

            //find the balance price for each case of dropped nodes
            for (int j = 0; j < 4; j++) {
                int i, bl = 0, min =1;
                for (i = 0; ((getAggD(j)[i] != null)
                        && (getAggD(j)[i].getQ_max() >= 0)); i++) {
                    bl = getAggD(j)[i].getPtice();
                    min = getAggD(j)[i].getQ_min();
                }
                Env.log.println("node_id: " + this.own_id  + " balance price: " + bl);
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
            Env.out.write("\n" + runTime + "," + this.getOwn_id() + ",0,"
                    + report[0] + "," + 0);
            Env.out.write("\n" + runTime + "," + this.getOwn_id() + ",1,"
                    + report[1] + "," + 0);
            Env.out.write("\n" + runTime + "," + this.getOwn_id() + ",5,"
                    + report[2] + "," + 0);
            Env.out.write("\n" + runTime + "," + this.getOwn_id() + ",10,"
                    + report[3] + "," + 0);


            //set the kid nodes balance prices
            for (int i = 0; i < getChildrenSize(); i++) {
                this.e.dbus.toQueue(report[0], 0, this.getChildren(i).getOwn_id(), 0);
                this.e.dbus.toQueue(report[1], 1, this.getChildren(i).getOwn_id(), 0);
                this.e.dbus.toQueue(report[2], 2, this.getChildren(i).getOwn_id(), 0);
                this.e.dbus.toQueue(report[3], 3, this.getChildren(i).getOwn_id(), 0);
            }

    }

    //seventh cycle: find excess demand for the end users
    private void do_calc_load() {
            
            //initiate the excess demand vector
            int[] ex = new int[4];
            //find the excess demand for the four cases of dropped nodes
            for (int i = 0; i < 4; i++) {
                //report 0 in case of no balance point
                if (this.getBl(i) <= -1) {
                    ex[i] = 0;
                } else {
                    ex[i] = findExcessDemand(getBl(i));
                }
            }
            
            //report the excess demands for each cased of dropped nodes
            Env.out.write("\n" + runTime + "," + this.getOwn_id() + ",0,"
                    + this.getBl(0) + "," + ex[0]);

            if (getArrayIndex(this.Parent.getRan()[0], this.getOwn_id() % 100 + 1) < 0) 
                Env.out.write("\n" + runTime + "," + this.getOwn_id() + ",1,"
                        + this.getBl(1) + "," + ex[1]);
            
            if (getArrayIndex(this.Parent.getRan()[1], this.getOwn_id() % 100 + 1) < 0) 
                Env.out.write("\n" + runTime + "," + this.getOwn_id() + ",5,"
                        + this.getBl(2) + "," + ex[2]);
            
            if (getArrayIndex(this.Parent.getRan()[2], this.getOwn_id() % 100 + 1) < 0) 
                Env.out.write("\n" + runTime + "," + this.getOwn_id() + ",10,"
                        + this.getBl(3) + "," + ex[3]);
            

        }

    //create demand curves based on initial load, elasticity, and number of steps
    public Bid[] drawDemand(double Q40, double elst, int step){
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
    
    //create supply curves with reverse quantities in comparison to demand curve
    public Bid[] drawSupply(double Q40, double elst, int step){
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
    public Bid[] aggregateDemand(Bid[] bid1, Bid[] bid2) {
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


