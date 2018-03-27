import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;
import java.util.HashMap;
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
    //    fileVirt   -- configuration of virtual agents
    //    fileHist   -- previous price and quantity data
    //    transCost  -- transmission cost between nodes
    //    transCap   -- maximum transmission between nodes
    //    seed       -- seed for RNG or else absent or "none"
    //    numPop     -- number of populations to draw
    //
    
    private static String fileConfig ;
    private static String fileVirt ;
    private static String fileHist ;
    
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
        * Service providers send information to clients
        */
       SERVICE_SEND,
       /**
        * Traders send demands up
        * */
       TRADER_SEND,
       /**
        * Hook for virtual agents prior to AGGREGATE for each tier
        */
       PRE_AGGREGATE,
       /**
        * Aggregate and send demand up for each tier
        */
       AGGREGATE, 
       /**
        * Hook for virtual agents prior to REPORT for each tier
        */
       PRE_REPORT,
       /**
        * Report price to child nodes for each tier
        */
       REPORT, 
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
     * List of DOS runs desired
     */
    public static String dos_runs[];
    
    /** 
     * Current DOS run
     */
    public static String curDOS;

    static Properties props;

    //
    // Other variables
    //

    static TreeMap<Integer,String> outMap = new TreeMap<>();
    static boolean outHeader = true;

    static PrintWriter out;
    static PrintWriter net;
    static PrintWriter msg;
   
    static int maxTier;
    static int curTier;
    
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
    
    // Master list of historical information across pop and dos runs
    static final HashMap<String, ArrayList<History>> globalHistory = new HashMap<>();
    
    // Public-Private key list
    static final HashMap<String, Integer> publicKeys = new HashMap<>();
    static final HashMap<String, Integer> privateKeys = new HashMap<>();
    public static final HashMap<Integer, String> availableKeys = new HashMap<>();                                                                          
   
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
    
    /**
     * Find and return intel list given population and dos
     */
    public static ArrayList<History> getHistory(int pop, int dos) {
       
        // Create lookup key
        String key = Integer.toString(pop) + "|" + Integer.toString(dos);
       
        // Returns array if history exists for pop/dos run
        if(globalHistory.containsKey(key)) {
           return globalHistory.get(key);
        }
        else {     
            throw new RuntimeException("No history for population "+pop+" and dos "+dos);
        }
    }
    
    /**
     * Return agent associated with public key
     */
    public static Integer resolvePublic(String key) {
       
        // Returns agent's id if exists
        if(publicKeys.containsKey(key)) {
           return publicKeys.get(key);
        }
        else {     
            throw new RuntimeException("No agent with private key: "+key);
        }
    }
    
    /**
     * Return agent associated with private key
     */
    public static Integer resolvePrivate(String key) {
       
        // Returns agent's id if exists
        if(privateKeys.containsKey(key)) {
           return privateKeys.get(key);
        }
        else {     
            throw new RuntimeException("No agent with public key: "+key);
        }
    }
    
    /**
     * Retrieves an agent's public key
     * 
     * @param agent_id is the other agent's id
     * @return Uniform random number
     */
    private String getPublicKey(int agent_id) {
        return availableKeys.get(agent_id);
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
        double cutoff;
        String dosprop;

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
        fileDraws  = props.getProperty("draws","testdraw.csv") ;
        fileVirt   = props.getProperty("virtualmap","") ;
        fileHist   = props.getProperty("history","") ;
        transCost  = getIntProp(props,"transcost","1");
        transCap   = getIntProp(props,"transcap","2500");
        numPop     = getIntProp(props,"populations","10");
        seed       = props.getProperty("seed","none");
        dosprop    = props.getProperty("dos","0,1,5,10");

        //
        //  Unpack the DOS specification
        //

        dos_runs = dosprop.split(",");
        for(int i=0 ; i<dos_runs.length ; i++)
            dos_runs[i] = dos_runs[i].trim();

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
           "   Seed imposed: "+seed+"\n"+
           "   DOS runs: "+String.join(",",dos_runs)
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
            log.println("\n*** population "+pop);

            // initialize for this population

            for(Agent a: listAgent)
                a.popInit();

            Channel.divert_clear();
            
            // run DOS scenarios

            for(String dos: dos_runs) {

                // initialize for DOS runs; agents figure out if they're blocked

                curDOS = dos;
                blockList.clear();
                for(Agent a: listAgent)
                    a.runInit();

                log.println("dos "+curDOS+" dropped "+blockList);

                // now step through the run

                for( Stage s : Stage.values() ) 
                    switch( s ) {
                        case PRE_AGGREGATE:
                            for(curTier=2 ; curTier<=maxTier ; curTier++ ) {
                                do_stage(enviro,true,Stage.PRE_AGGREGATE);
                                do_stage(enviro,true,Stage.AGGREGATE);
                            }
                            break;

                        case AGGREGATE:
                            break;

                        case PRE_REPORT:
                            for(curTier=maxTier ; curTier>=2 ; curTier-- ) {
                                do_stage(enviro,true,Stage.PRE_REPORT);
                                do_stage(enviro,true,Stage.REPORT);
                            }
                            break;
                        
                        case REPORT:
                            break;

                        default:
                            do_stage(enviro,false,s);
                            break;

                    }
                    
                // write the results

                printResults();
            }
        }
        
        enviro.finish();
    }
    
    /** 
     * Carry out a particular stage and log it in the process
     */
    static void do_stage(Env e, boolean showTier, Stage s) {
        if( showTier) 
            log.println("*** tier "+curTier+" "+s);
        else
            log.println("*** "+s);
        stageNow = s;
        e.schedule.step(e);
    }
    
    /**
     * Create agents based on the input network map
     */
    private void makeAgents(String filename) {
    
        BufferedReader br;
        CSVParser csvReader;
        int cur_id, cur_type, cur_upid, cur_cost, cur_cap, cur_security;
        String cur_sd, cur_chan;
        Agent cur_agent;
        String[] items, cur_secMeasures;
        Channel channel;

        // read the topology of the network and build the list of agents

        try {
            br = new BufferedReader(Util.openRead(filename));
            csvReader = CSVFormat.DEFAULT.withQuote('"').withHeader().withIgnoreHeaderCase().parse(br);

            for(CSVRecord rec: csvReader) {
                cur_id        = Integer.parseInt(rec.get("id"));
                cur_type      = Integer.parseInt(rec.get("type"));
                cur_sd        = rec.get("sd_type");
                cur_upid      = Integer.parseInt(rec.get("up_id"));
                cur_chan      = rec.get("channel");
                cur_cost      = Integer.parseInt(rec.get("cost"));
                cur_cap       = Integer.parseInt(rec.get("cap"));
                cur_security  = Integer.parseInt(rec.get("security"));
                cur_secMeasures  = rec.get("secMeasures").split(",",-1);

                // create the agent

                switch( cur_type ) {
                    case 1: 
                    case 2: 
                        cur_agent = new Market(cur_upid,cur_id);
                        break;
                    case 3: 
                        cur_agent = new Trader(cur_upid,cur_id,cur_sd);
                        break;
                    default:
                        throw new RuntimeException("Unexpected agent type "+cur_type);
                }

                // if this is a Grid agent, save transmission parameters, 
                // overriding with global versions, if they were given
                
                if( cur_agent instanceof Grid ) {
                    Grid grid_agent = (Grid) cur_agent;
                    
                    grid_agent.cost = cur_cost;
                    if( props.getProperty("transcost") != null )
                        grid_agent.cost = transCost ;

                    grid_agent.cap = cur_cap;
                    if( props.getProperty("transcap") != null )
                        grid_agent.cap  = transCap;
                }
                
                // set its channel

                channel = Channel.find(cur_chan);
                if( channel == null )channel = new Channel(cur_chan);
                cur_agent.setChannel(channel);
                
                // set security measures
                cur_agent.security = cur_security;
                //cur_agent.addSecurityMeasures(cur_measures);

                // add it to the list of agents and schedule it for stepping

                listAgent.add(cur_agent);
                schedule.scheduleRepeating(cur_agent);
            } 

        br.close();
        }
        catch (IOException e) {
            throw new RuntimeException("Could not read network file: "+filename);
        }
    }

    /**
     * Configure the grid
     */
    private void buildGrid() {
        ArrayList<Grid> gridList = new ArrayList<>();
        ArrayList<String> Err = new ArrayList<>();
        String where;
        
        for( Agent a : listAgent ) 
            if( a instanceof Grid )
                gridList.add((Grid) a);
        
        // tell parents about their children 

        for (Grid g : gridList) 
            if( g.par_id != 0 ) {
                Agent par = Env.getAgent(g.par_id);
                if( par instanceof Grid )
                    ((Grid) par).children.add(g);
                else
                    Err.add("parent of "+g.own_id+" is not a grid agent");
            }
       
        // set grid tiers for future reference

        maxTier = 0;
        for (Grid g : gridList) {
            curTier = g.getTier();
            if( curTier>maxTier )maxTier = curTier;
        }

        // check configuration

        for(Grid g: gridList) {
            
            if( g instanceof Trader ) {
                where = "trader "+g.own_id+" ";
                if( g.getTier() != 1 )
                    Err.add(where+"has tier "+g.getTier());
                if( !g.children.isEmpty() )
                    Err.add(where+"has child nodes");
                if( g.par_id == 0 )
                    Err.add(where+"has no parent node");
                continue;
            }

            if( g instanceof Market ) {
                where = "market "+g.own_id+" ";
                if( g.getTier() < 2 )
                    Err.add(where+"has tier "+g.getTier());
                if( g.children.isEmpty() )
                    Err.add(where+"has no child nodes");
                continue;
            }

            assert false;
        }
        
        if( !Err.isEmpty() ) {
            for(String s: Err)
                System.out.println("configuration error: "+s);
            throw new RuntimeException("Fatal configuration errors");
        }
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
     * Creates virtual agents from file
     */
     private void makeVirtual(String filename){
        
        BufferedReader br;
        CSVParser csvReader;
        int id, security;
        String type, intelLevel;
        String[] channelList, agentList, intelList, secMeasures, configuration;
        Agent cur_agent, intel_agent;
        Intel cur_intel;
        
        try {
            
            // Configure the csv reader
            br = new BufferedReader(Util.openRead(filename));
            csvReader = CSVFormat.DEFAULT.withQuote('"').withHeader().withIgnoreHeaderCase().parse(br);
            
            // Read in csv records
            for(CSVRecord rec: csvReader) {
                id             = Integer.parseInt(rec.get("id"));
                type           = rec.get("type").toUpperCase();
                configuration  = rec.get("configuration").split(",",-1);
                channelList    = rec.get("channel").split(",",-1);
                agentList      = rec.get("agent").split(",",-1);
                intelLevel     = rec.get("intel_level");
                intelList      = rec.get("intel").split(",",-1);
                security       = Integer.parseInt(rec.get("security"));
                secMeasures    = rec.get("secMeasures").split(",",-1);
                
                // Create the virtual agent
                switch(type) {
                    case "ADV_ADAM": 
                        cur_agent = new Adv_Adam(id);
                        break;
                    default:
                        throw new RuntimeException("Unexpected agent type "+type);
                }
                
                // Load agent's channel list
                for(int i = 0; i < channelList.length; i++){
                    ((Virtual) cur_agent).channels.add(Channel.find(channelList[i]));
                }
                
                // Load agent's access list
                for(int i = 0; i < agentList.length; i++){
                    
                    ((Virtual) cur_agent).agents.add(Integer.parseInt(agentList[i]));
                    
                }
                
                // Load agent's intel list
                switch(intelLevel) {
                    case "full":
                        
                        // Load all from global agent list
                        for(Agent a: listAgent){
                            cur_intel = new Intel(a.own_id, false);
                            ((Virtual) cur_agent).intel.add(cur_intel);
                        }
                        break;
                    case "partial":
                        
                        // Load from intel list in file
                        for(int i = 0; i < intelList.length; i++){
                            intel_agent = Env.getAgent(Integer.parseInt(intelList[i]));
                            cur_intel = new Intel(intel_agent.own_id, false);
                            ((Virtual) cur_agent).intel.add(cur_intel);
                        }
                        break;
                    case "none":
                        break;
                    default:
                        throw new RuntimeException("Unexpected intel type: "+intelLevel);
                }
                
                // Set security level
                cur_agent.security = security;
                
                // Set security measures
                for(int i = 0; i < secMeasures.length; i++){
                    cur_agent.addSecurity(secMeasures[i].toUpperCase());
                }
                
                // Add agent to the schedule for stepping
                listAgent.add(cur_agent);
                schedule.scheduleRepeating(cur_agent);
                
                // Parse agent configuration dictionary
                for(int i = 0; i < configuration.length; i++){
                    String[] config = configuration[i].split(":",-1);
                    ((Virtual) cur_agent).config.put(config[0].toLowerCase(),config[1].toLowerCase());
                }
            }       
        }
        catch (IOException e) {
            System.out.println("Could not read network file: "+filename);
            System.exit(0);
        }
     }
    
    /**
     * Loads global historical data
     */
     private void loadHistory(String filename){
        
        BufferedReader br;
        CSVParser csvReader;
        int pop, dos, id, p, q;
        String key;
        History history;
        ArrayList<History> listHistory;
        
        try {
            
            // Configure csv reader
            br = new BufferedReader(Util.openRead(filename));
            csvReader = CSVFormat.DEFAULT.withQuote('"').withHeader().withIgnoreHeaderCase().parse(br);
            listHistory = new ArrayList<History>();
            
            // Read in csv records
            for(CSVRecord rec: csvReader) {
                pop = Integer.parseInt(rec.get("pop"));
                dos = Integer.parseInt(rec.get("dos"));
                id = Integer.parseInt(rec.get("id"));
                p = Integer.parseInt(rec.get("p"));
                q = Integer.parseInt(rec.get("q"));
                
                /**
                * Store information in new history object
                *
                * Currently there is no field in file for period
                * Temporarily using a default of 1
                */
                history = new History(id);
                history.storePrice(1,p);
                history.storeQuantity(1,q);
                
                // Build key for hashmap
                key = Integer.toString(pop) + "|" + Integer.toString(dos);
                
                
                // Add historical info to global hashmap
                if(globalHistory.containsKey(key)){
                    listHistory.clear();
                    listHistory.addAll(globalHistory.get(key));
                    listHistory.add(history);
                    globalHistory.put(key, listHistory);
                }
                else {
                    listHistory.clear();
                    listHistory.add(history);
                    globalHistory.put(key, listHistory);
                }
            }
        }
        catch (IOException e) {
            System.out.println("Could not read network file: "+filename);
            System.exit(0);
        }
     }
    
    /**
     * Save results for printing out
     * 
     * @param agent Agent 
     * @param p Price
     * @param q Quantity
     */
    public static void saveResult(Agent agent, int p, int q) {
           int key;
           String results;
           int block;
           String draw;

           block = isBlocked(curDOS,agent) ? 1 : 0;
           draw  = String.format("%.1f",agent.rBlock);

           key     = agent.own_id;
           results = draw+","+block+","+p+","+q;

           outMap.put(key,results);
    }

    /**
     * Write the results for a run to the output file
     */
    static void printResults() {
        if( outHeader ) {
            out.println("pop,dos,id,rblock,blocked,p,q");
            outHeader = false;
        }
        for(int key: outMap.keySet() )
            out.println(pop+","+curDOS+","+key+","+outMap.get(key));
        outMap.clear();
    }

    /**
     *  Start the simulation
     */
    @Override
    public void start(){
    super.start();
        makeAgents(fileConfig);
        buildGrid();
        if( !fileVirt.equals("") )makeVirtual(fileVirt);
        if( !fileHist.equals("") )loadHistory(fileHist);
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
    }
 
}
