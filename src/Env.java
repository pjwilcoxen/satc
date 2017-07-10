import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.csv.*;
import sim.engine.SimState;

/**
 * Main simulation environment 
 */
public class Env extends SimState {

    private static final String USAGE = "Usage: java -jar market.jar <runfile>";

    /**
     * Support -version option
     */
    public static final String VER  = "3.0";

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
    
    private   static String fileConfig ;
    
    /**
     * File name for monte carlo draws
     */
    protected static String fileDraws ;
    
    /**
     * Default transmission cost
     */
    protected static int transCost = 1 ;
    
    /**
     * Default transmission capacity
     */
    protected static int transCap  = 2500 ;
    
    /** 
     * Default number of populations
     */
    protected static int numPop    = 10 ;

    //
    // Handle random number generation centrally so we can
    // set the seed for repeatability in testing.
    //

    private static Random rgen;
    private static long rgen_seed = -1;

    /**
     * Stages per simulation
     */
    public static enum Stage {
       /**
        * Traders send demands up
        * */
       SEND_END,
       /**
        * Mid nodes aggregate and send demand up
        */
       AGG_END, 
       /**
        * Root node aggregates and finds equilibrium
        */
       AGG_MID, 
       /**
        * Root node report price to mid nodes
        */
       REPORT_MID, 
       /**
        * Mid nodes report price to traders
       */
       REPORT_END, 
       /**
        * Traders determine actual loads
        */
       CALC_LOADS 
    };
    
    /**
     * Current stage
     */
    public static Stage stageNow;

    /**
     * Lists of agents to block under DOS runs
     */
    public static final ArrayList<Integer> blockList = new ArrayList<>();
    
    /**
     * Default list of DOS runs
     */
    public static final String dos_runs[] = { "0", "1", "5", "10" };
    
    /**
     * Number of DOS runs to carry out
     */
    public static int nDOS = dos_runs.length;
   
    /** 
     * Current DOS run
     */
    public static String curDOS;

    //
    // Other variables
    //

    static PrintWriter out;
    static PrintWriter net;
    static PrintWriter msg;
    
    /**
     * Printer for log file
     */
    public static PrintWriter log;
    
    /**
     * Printer for the output file
     */
    public static CSVPrinter csvPrinter;
    
    /**
     * Printer for net demands
     */
    public static CSVPrinter loadPrinter;
    static int pop;

    /**
     * Master list of agents
     *
     * Keep as an ArrayList to allow them to be initialized in 
     * a known order.
     */
    static final ArrayList<Agent> listAgent = new ArrayList<>();
   
    /**
     * Master simulation environment 
     * 
     * @param seed Seed for the random number generator
     */
    public Env(long seed) {
        super( rgen_seed != -1 ? rgen_seed : seed );
        try {
            readDraws();
        } catch (IOException e) {
           throw new RuntimeException("Could not read file of draws");
        }
    }

    /**
     * Centralized random number generator
     * 
     * Can be initialized with a specified seed
     * 
     * @return Uniform random number
     */
    public static double runiform() {
       return rgen.nextDouble() ;
    }
    
    /**
     * Get the current population number
     * 
     * @return Number of this population
     */
    static public int getPop() {
        return pop;
    }
    
    /**
     * Look up a property and return an integer
     * 
     * @param prop Properties object
     * @param key  Key to look up
     * @param def  Default to use if key is absent
     */
    private static int getIntProp(Properties prop, String key, String def) {
        String val = prop.getProperty(key,def) ;
        val = val.trim();
        return Integer.parseInt(val);
    }

    /**
     * Find and return an agent given its id
     * 
     * @param own_id ID number of the agent
     * @return Agent's instance
     */
    public static Agent getAgent(int own_id) {
        for(Agent a: listAgent)
            if( a.own_id == own_id )
                return a;
       throw new RuntimeException("No agent with id "+own_id);
    }

    static class Draw {
       int n;
       double load;
       double elast;
    }
    static ArrayList<Draw> drawListD = new ArrayList<>();
    static ArrayList<Draw> drawListS = new ArrayList<>();

    /** 
     * Add an agent to a block list
     * 
     * @param own_id ID of agent that should be blocked
     */
    public static void setBlock(int own_id) {
       blockList.add(own_id);
    }   

    /**
     * Check whether an agent is on a block list
     * 
     * @param run DOS run of interest
     * @param agent Agent to be checked
     * @return True if the agent is blocked in the indicated DOS run
     */
    public static boolean isBlocked(String run, Agent agent) {
       return blockList.contains(agent.own_id);
    }

    /**
     * Check whether an agent is on a block list
     * 
     * @param from ID of apparent sender
     * @return True if the agent is blocked in the indicated DOS run
     */
    public static boolean isBlocked(int from) {
       return blockList.contains(from);
    }

    /**
     * Entry point for the simulation
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        
        String fileProps;
        String seed;
        String stem;
        int ext;
        Properties props;
        double cutoff;

        if( args.length != 1 ) {
            System.out.println("Error: expected 1 argument but found "+args.length+".");
            System.out.println(USAGE);
            System.out.println("Versions: Env="+Env.VER+", Agent="+Agent.VER);
            System.exit(0);
        }

        fileProps = args[0] ;
        props = new Properties() ;
        try {
           props.load(Util.openRead(fileProps)); 
        } catch (IOException ex) {
           System.out.println("Error reading the property file");
           System.exit(0);
        }

        stem = fileProps;
        ext = stem.lastIndexOf('.');
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

        out = Util.openWrite(stem+"_out.csv");
        net = Util.openWrite(stem+"_net.csv");
        msg = Util.openWrite(stem+"_msg.csv");
        log = Util.openWrite(stem+"_log.txt");

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

        for( pop=1 ; pop<=numPop ; pop++ ) {
            System.out.println("Starting population "+pop);
            log.println("*** population "+pop);

            // initialize for this population

            for(Agent a: listAgent)
                a.popInit();

            // run DOS scenarios

            for(String dos: dos_runs) {

                // initialize for DOS runs; agents figure out if they're blocked

                curDOS = dos;
                blockList.clear();
                for(Agent a: listAgent)
                    a.runInit();

                log.println("dos "+curDOS+" dropped "+blockList);

                // now step through the run

                for( Stage s : Stage.values() ) {
                    log.println("*** stage "+s);
                    stageNow = s;
                    enviro.schedule.step(enviro);
                }
            }
        }
        
        enviro.finish();
	}
    
    /**
     * Create agents based on the input network map
     */
    private void makeAgents() throws FileNotFoundException, IOException {
    
        BufferedReader br;
        CSVParser csvReader;
        int cur_id, cur_type, cur_upid, cur_cost, cur_cap ;
        String cur_sd, cur_dbus;
        Agent cur_agent;
        String items[];
        DBUS dbus;

        //read the topology of the network and build the list of agents

        br = new BufferedReader(Util.openRead(fileConfig));
        csvReader = CSVFormat.DEFAULT.withHeader().withIgnoreHeaderCase().parse(br);

        for(CSVRecord rec: csvReader) {
            cur_id    = Integer.parseInt(rec.get("id"));
            cur_type  = Integer.parseInt(rec.get("type"));
            cur_sd    = rec.get("sd_type");
            cur_upid  = Integer.parseInt(rec.get("up_id"));
            cur_dbus  = rec.get("dbus");
            cur_cost  = Integer.parseInt(rec.get("cost"));    // reserved
            cur_cap   = Integer.parseInt(rec.get("cap"));     // reserved

            // watch for conflicts between type and parent
            
            if( cur_upid == 0 && cur_type != 1 ) {
                log.println("Warning: node "+cur_id+" has type "+cur_type+" but no parent");
                log.println("Warning: will be treated as a root node");
                cur_type = 1;
            }
            
            switch( cur_type ) {
                case 1: 
                    cur_agent = new Root(cur_id);
                    break;

                case 2: 
                    cur_agent = new Mid(cur_upid,cur_id);
                    break;

                case 3: 
                    cur_agent = new Trader(cur_upid,cur_id,cur_sd);
                    break;

                default:
                    throw new RuntimeException("Unexpected agent type "+cur_type);
            }
            
            dbus = DBUS.find(cur_dbus);
            if( dbus == null )dbus = new DBUS(cur_dbus);

            cur_agent.setDBUS(dbus);

            listAgent.add(cur_agent);
            schedule.scheduleRepeating(cur_agent);
        }
                
        // tell parents about their children now that all agents have been instantiated

        for (Agent a : listAgent ) 
            if ( a.par_id != 0 )
                Env.getAgent(a.par_id).children.add(a);
        
        br.close();
    }

    /**
     * Read the list of draws of random agent characteristics
     */
    private void readDraws() throws FileNotFoundException, IOException {
        BufferedReader br;
        CSVParser csvReader;
        Draw draw;
        String sd_type;

        br = new BufferedReader(Util.openRead(Env.fileDraws));
        csvReader = CSVFormat.DEFAULT.withHeader().withIgnoreHeaderCase().parse(br);
 
        for(CSVRecord rec: csvReader) {
            draw = new Draw();
            sd_type    = rec.get("type");
            draw.load  = Double.parseDouble(rec.get("load"));
            draw.elast = Double.parseDouble(rec.get("elast"));
            if( sd_type.equals("D") )
               drawListD.add(draw);
            else
               drawListS.add(draw);
        } 

        br.close();
    }

    /**
     * Print out results
     * 
     * @param agent Agent 
     * @param dos DOS run
     * @param p Price
     * @param q Quantity
     */
    public static void printResult(Agent agent, String dos, int p, int q) {

        String header[] = {"pop","dos","id","rblock","blocked","p","q"};
        CSVFormat csvFormat ;
        String draw;
        int block;

        try {
           if( csvPrinter == null ) {
              csvFormat  = CSVFormat.DEFAULT.withHeader(header);
              csvPrinter = new CSVPrinter(out,csvFormat);
           }
           block = isBlocked(dos,agent) ? 1 : 0;
           draw = String.format("%.1f",agent.rBlock);
           csvPrinter.printRecord( pop, dos, agent.own_id, draw, block, p, q );      
        }
        catch (IOException e) {
           throw new RuntimeException("Error writing to output file");
        }
    }

    /**
     * Print out results
     * 
     * @param agent Agent
     * @param casetag Tag indicating DOS case
     * @param dem Demand curve
     */
    public static void printLoad(Agent agent,String casetag, Demand dem) {
        CSVFormat loadFormat ;
        ArrayList<String> header;
        ArrayList<String> values;

        header = new ArrayList<>();
        
        header.add("pop");
        header.add("id");
        header.add("case");
        header.add("sd_type");
        header.add("load");
        header.add("elast");
        for(int i=0 ; i<20 ; i++) {
            header.add("p"+i);
            header.add("q_min"+i);
            header.add("q_max"+i);
        }

        values = new ArrayList<>();

        try {
            if( loadPrinter == null ) {
                loadFormat  = CSVFormat.DEFAULT;
                loadPrinter = new CSVPrinter(net,loadFormat);
                loadPrinter.printRecord(header);
            }
            values.add(Integer.toString(pop));
            values.add(Integer.toString(agent.own_id));
            values.add(casetag);
            if( agent instanceof Trader) {
                Trader trader = (Trader) agent;
                values.add(trader.sd_type);
                values.add(Double.toString(trader.load));
                values.add(Double.toString(trader.elast));
            }
            else {
                values.add("");
                values.add("");
                values.add("");
            }
            for(String str: dem.toStrings() )
                values.add(str);
            loadPrinter.printRecord(values);
        }
        catch (IOException e) {
           throw new RuntimeException("Error writing to load file");
        }
    }

    /**
     *  Start the simulation
     */
    @Override
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

    /**
     *  Finish the simulation
     */
    @Override
    public void finish() {
       System.out.println("Simulation complete");
       out.close();
       log.close();
       net.close();
       System.exit(0);
    }
 
}
