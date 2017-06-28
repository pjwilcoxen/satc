//  Env.java

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.engine.SimState;
        
public class Env extends SimState {

    public static final String usage = "Usage: java -jar market.jar <runfile>";
    public static final String vers  = "2.0";

    //
    // Information loaded from the run file
    //
    //    fileConfig -- configuration of the network of nodes
    //    fileDraws  -- monte carlo drawings of possible traders
    //    transCost  -- transmission cost between nodes
    //    transCap   -- maximum transmission between nodes
    //    seed       -- seed for RNG or else absent or "none"
    //    numPop     -- number of populations to draw
    //
    
    public static String fileConfig ;
    public static String fileDraws  ;
    public static int    transCost = 1 ;
    public static int    transCap  = 2500 ;
    public static int    numPop    = 10 ;

    //
    // Handle random number generation centrally so we can
    // set the seed for repeatability in testing.
    //

    private static Random rgen;
    private static long rgen_seed = -1;

    //
    // Stages per simulation
    //

    public static enum Stage {
       INIT_DROPS, 
       INIT_LOADS, 
       AGG_END, 
       AGG_MID, 
       REPORT_MID, 
       REPORT_END, 
       CALC_LOADS 
    };
    public static Stage stageNow;

    //
    // Other variables
    //

    public int n = 100; //The number of agents
    double[][] queueS;
    int queueSizeS;
    double[][] queueD;
    int queueSizeD;

    public static PrintWriter out;
    public static PrintWriter log;

    ArrayList<Agent> listAgent;
    DBUS dbus;
   
    //
    //  Env()
    //     Constructor; argument list imposed by MASON
    //
    
    public Env(long seed) {
        super( rgen_seed != -1 ? rgen_seed : seed );
    }

    //
    //  openRead
    //     Open a file and catch exceptions
    //

    public static FileReader openRead(String name) {
        FileReader fr ;
        try {
            fr = new FileReader(name); 
            return fr;
            } 
        catch (IOException ex) {
            System.out.println("Error reading: "+name);
            System.exit(0);
        } 
        return null ;
    }

    // 
    //  openWrite
    //     Open a file for writing and catch exceptions
    //

    public static PrintWriter openWrite(String name) {
        PrintWriter pw;
        try {
            pw = new PrintWriter(name); 
            return pw;
            } 
        catch (IOException ex) {
            System.out.println("Error opening "+name+" for writing");
            System.exit(0);
        } 
        return null ;
    }

    //
    //  runiform()
    //     Centralized random number generator that can be
    //     initialized with a specified seed.
    //

    public static double runiform() {
       return rgen.nextDouble() ;
    }
       
    //
    //  getIntProp()
    //     Look up a property and return an integer
    //

    private static int getIntProp(Properties prop, String key, String def) {
        String val = prop.getProperty(key,def) ;
        val = val.trim();
        return Integer.parseInt(val);
    }

    //
    //  exit()
    //     Close files and tidy up.

    public static void exit() {
       out.close();
       log.close();
       System.exit(0);
    }

    //
    //  main()
    //

    public static void main(String[] args) {
        
        String fileProps;
        String seed;
        String stem;
        int ext;
        Properties props;

        if( args.length != 1 ) {
            System.out.println("Error: expected 1 argument but found "+args.length+".");
            System.out.println(usage);
            System.out.println("Versions: Env="+Env.vers+", Agent="+Agent.vers);
            System.exit(0);
        }

        fileProps = args[0] ;
        props = new Properties() ;
        try {
           props.load(openRead(fileProps)); 
        } catch (IOException ex) {
           System.out.println("Error reading the property file");
           System.exit(0);
        }

        stem = fileProps;
        ext = stem.lastIndexOf(".");
        if( ext>0 )stem = stem.substring(0,ext);

        fileConfig = props.getProperty("netmap","netmap.csv") ;
        fileDraws  = props.getProperty("draws","testdraws,csv") ;
        transCost  = getIntProp(props,"transcost","1");
        transCap   = getIntProp(props,"transcap","2500");
        numPop     = getIntProp(props,"populations","10");
        seed       = props.getProperty("seed","none");

        // 
        //  Set up the random number generator, allowing for a fixed
        //  seed if we want repeatability
        //

        if( seed.equals("none") || seed.equals("") ) {
           rgen_seed = -1; 
           rgen = new Random();
        } else {
           rgen_seed = Long.parseLong(seed);
           rgen = new Random(rgen_seed);
        }

        // 
        //  Open and initialize output and log files
        //

        out = openWrite(stem+"_out.csv");
        out.write("Population,ID,Prob,P,Q");

        log = openWrite(stem+"_log.txt");
        log.println(
           "Scenario Settings:\n" +
           "   Network map: "+fileConfig+"\n"+
           "   Draws of agents: "+fileDraws+"\n"+
           "   Transmission cost: "+transCost+"\n"+
           "   Transmission cap: "+transCap+"\n"+
           "   Populations: "+numPop+"\n"+
           "   Seed imposed: "+seed+"\n"
           );

        // 
        //  Create an instance of the simulator and run the simulation.
        //  Iterate over the number of populations requested and, within
        //  that, over the event stages.
        //

        Env enviro = new Env( System.currentTimeMillis() );

        enviro.start();

        for( int pop=0 ; pop<numPop ; pop++ ) {
           System.out.println("Starting population "+pop);
           log.println("*** population "+pop);

           for( Stage s : Stage.values() ) {
              log.println("*** stage "+s);
              stageNow = s;
              enviro.schedule.step(enviro);
           }
        }
        
        enviro.finish();
	}
    
    //create Agents based on the input csv file
    public void makeAgents() throws FileNotFoundException, IOException{
    
        dbus = new DBUS((Env) this);

        //read the topology of the graph
        BufferedReader br = null;
        String line = "";
            
        int cur_id, cur_type, cur_sd, cur_upid ;
        Agent cur_agent;
        String items[];

        br = new BufferedReader(openRead(fileConfig));
        br.readLine();
        
        //initiate the arraylist of agents
        listAgent = new ArrayList<Agent>();

        line = br.readLine();

        // instantiate the agents based on the input csv file    

        while (line != null) {
            items     = line.split(",");
            cur_id    = Integer.parseInt(items[0]);
            cur_type  = Integer.parseInt(items[1]);
            cur_sd    = Integer.parseInt(items[2]);
            cur_upid  = Integer.parseInt(items[3]);
            cur_agent = new Agent(this,null,cur_type,cur_upid,cur_id,cur_sd);
            listAgent.add(cur_agent);
            line = br.readLine();
        }
        
        // link each agent to its parent now that all are instantiated and
        // then add it to the MASON schedule.

        for (Agent a : listAgent ) {
            if (a.getType() != 1) {
                a.setParent( listAgent.get(a.getUp_id() - 1));
            }
            schedule.scheduleRepeating(a);
        }
}

 public void start(){
	 super.start();
            try {
                makeAgents();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, ex);
            }
 }

 public void finish() {
    System.out.println("Simulation complete");
    exit();
 }
 
}
