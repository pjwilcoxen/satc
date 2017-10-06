/**
 * Message class
 */
public class Msg {

    /**
     * Header for log files
     */
    public static final String LOGHEADER = "pop,dos,sender,from,to,type";

    /**
     * Known types of messages
     */
    public static enum Types {
        /**
         * Unknown message type
         */
        NONE,
        /**
         * Message contains a demand curve
         */
        DEMAND,
        /**
         * Message contains a price
         */
        PRICE
    };
    
    public static enum Security {
        /**
         * Unknown message type
         */
        ENCRYPTED,
        /**
         * Unknown message type
         */
        SIGNED,
        /**
         * Unknown message type
         */
        NONE
    }

    Agent sender;
    int from;
    int to;
    Types type;
    Demand demand;
    int price;
    Security security;
    String privateKey;
    String publicKey;

    /**
     * Message
     * 
     * @param sender Agent sending the message
     * @param to ID of destination agent
     */
    public Msg(Agent sender,int to) {
        this.sender = sender;
        this.from       = sender.own_id;
        this.to         = to;
        this.type       = Types.NONE;
        this.security    = Security.NONE;
    }

    /**
     * Produce a CSV-style log string for the message
     * 
     * @return Log string
     */
    public String logString() {
        String msg;
        msg = ""+Env.pop
            + ","+Env.curDOS
            + ","+sender.own_id
            + ","+from
            + ","+ to
            + ","+type;
        return msg;
    }

    /**
     * Override the from field
     * 
     * @param from ID of apparent sender
     */
    public void setFrom(int from) {
        this.from = from;
    }

    /**
     * Make this a demand message
     * 
     * @param demand Demand curve to send
     */
    public void setDemand(Demand demand) {
        assert type == Types.NONE;
        assert demand != null;
        this.demand = demand;
        type = Types.DEMAND;
    }

    /**
     * Make this a price message
     * 
     * @param price Price to send
     */
    public void setPrice(int price) {
        assert type == Types.NONE;
        this.price = price;
        type = Types.PRICE;
    }

    /**
     * Is this a demand message?
     * 
     * @return True if a demand message
     */
    public boolean isDemand() {
        return type == Types.DEMAND;
    }

    /**
     * Is this a price message?
     * 
     * @return True if a price message
     */
    public boolean isPrice() {
        return type == Types.PRICE;
    }

    /**
     * Get the demand curve from the message
     * 
     * @return Demand
     */
    public Demand getDemand() {
        assert type == Types.DEMAND;
        return demand;
    }

    /** 
     * Get the price from the message
     * 
     * @return Price
     */
    public int getPrice() {
        assert type == Types.PRICE;
        return price;
    }
    
    /**
     * Returns the type of security in the message
     * 
     * @return security enum
     */
    public Security securityLevel() {
        return security;
    }
    
    /**
     * Encrypts the message
     *
     * Uses private and public keys to encrypt the message
     */
    public void encrypt(String pri, String pub) {
        security = Security.ENCRYPTED;
        this.privateKey = pri;
        this.publicKey = pub;
    }
    
    /**
     * Signs the message
     *
     * Uses private key to sign the message
     */
    public void sign(String pri) {
        security = Security.SIGNED;
        this.privateKey = pri;
    }
    
    /**
     * Decrypts the message
     *
     * @return True if decryption was successful
     */
    
    public boolean decrypt(String pub, String pri) {
        
        if (security == Security.ENCRYPTED && Env.resolvePublic(pub) == Env.resolvePrivate(privateKey) && Env.resolvePrivate(pri) == Env.resolvePublic(publicKey)) {
            security = Security.NONE;
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * Verifies the signature of the message
     *
     * @return True if verification was successful
     */
    public boolean verify(String pub) {

        if (security == Security.SIGNED && Env.resolvePublic(pub) == Env.resolvePrivate(privateKey)) {
            security = Security.NONE;
            return true;
        }
        else {
            return false;
        }
    }
}