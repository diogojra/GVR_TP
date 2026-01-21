import java.io.*;
import java.text.*;
import java.util.*;

import org.snmp4j.*;
import org.snmp4j.agent.*;
import org.snmp4j.agent.io.*;
import org.snmp4j.agent.io.prop.*;
import org.snmp4j.agent.mo.*;
import org.snmp4j.agent.mo.snmp.*;
import org.snmp4j.cfg.EngineBootsCounterFile;
import org.snmp4j.mp.*;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.*;
import org.snmp4j.util.*;
import org.snmp4j.log.LogFactory;
import org.snmp4j.log.LogAdapter;
import org.snmp4j.log.JavaLogFactory;
import org.snmp4j.agent.mo.util.VariableProvider;
import org.snmp4j.agent.request.SubRequest;
import org.snmp4j.agent.request.RequestStatus;
import org.snmp4j.agent.request.Request;
import org.snmp4j.agent.request.SubRequestIterator;

import javax.swing.*;


public class Agent implements VariableProvider {

    static {
        LogFactory.setLogFactory(new JavaLogFactory());
    }

    private static final String DEFAULT_CL_PARAMETERS = "-c[s{=Agent.cfg}] -bc[s{=Agent.bc}]";
    private static final String DEFAULT_CL_COMMANDS = "#address[s{=udp:127.0.0.1/555}<(udp|tcp):.*[/[0-9]+]?>] ..";

    private final LogAdapter logger = LogFactory.getLogger(Agent.class);

    protected AgentConfigManager agent;
    protected MOServer server;
    private String configFile;
    private File bootCounterFile;
    private GVRTPMIBG1 gvrtpmibg1;
    private static Agent AGENT_INSTANCE;
    protected Modules modules;
    private OctetString localEngineID;


    public Agent(Map args) {

        server = new DefaultMOServer();

        configFile = (String)((List)args.get("c")).get(0);
        bootCounterFile = new File((String)((List)args.get("bc")).get(0));
        MOServer[] moServers = new MOServer[] { server };
        InputStream configInputStream =
                Agent.class.getResourceAsStream("AgentConfig.properties");
        if (configInputStream == null) {
            System.err.println("Unable to load AgentConfig.properties. File not found. Aborting");
            System.exit(1);
        }
        final Properties props = new Properties();
        try {
            props.load(configInputStream);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        MOInputFactory configurationFactory = new MOInputFactory() {
            public MOInput createMOInput() {
                return new PropertyMOInput(props, Agent.this);
            }
        };
        MessageDispatcher messageDispatcher = new MessageDispatcherImpl();
        addListenAddresses(messageDispatcher, (List)args.get("address"));

        this.localEngineID = new OctetString(MPv3.createLocalEngineID());
        agent = new AgentConfigManager(this.localEngineID,
                messageDispatcher,
                null,
                moServers,
                ThreadPool.create("Agent", 3),
                configurationFactory,
                new DefaultMOPersistenceProvider(moServers, configFile),
                new EngineBootsCounterFile(bootCounterFile));
        AGENT_INSTANCE = this;
    }

    public GVRTPMIBG1 getGvrtpmibg1() {
        return gvrtpmibg1;
    }

    protected void addListenAddresses(MessageDispatcher md, List addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return;
        }
        for (Iterator it = addresses.iterator(); it.hasNext();) {
            Address address = GenericAddress.parse((String)it.next());
            TransportMapping tm =
                    TransportMappings.getInstance().createTransportMapping(address);
            if (tm != null) {
                md.addTransportMapping(tm);
            }
            else {
                logger.warn("No transport mapping available for address '"+
                        address+"'.");
            }
        }
    }

    public void run() {
        agent.initialize();

        // Applying the VACM specific configurations
        configureVACM(agent.getVacmMIB());

        // Loading the MIB from the specific files lanes and viasdestino .txt
        registerMIBs();

        // Create the private community string
        addCommunities();

        //Starts listening
        agent.setupProxyForwarder();
        agent.run();
    }

    /**
     * Get the {@link MOFactory} that creates the various MOs (MIB Objects).
     * @return
     *    a {@link DefaultMOFactory} instance by default.
     */
    protected MOFactory getFactory() {
        return DefaultMOFactory.getInstance();
    }

    /**
     * Register your own MIB modules in the specified context of the agent.
     * The {@link MOFactory} provided to the <code>Modules</code> constructor
     * is returned by {@link #getFactory()}.
     */
    protected void registerMIBs() {
        if (modules == null) {
            modules = new Modules(getFactory());
        }
        try {
            modules.registerMOs(server, null);

            GVRTPMIBG1 mib = modules.getGvrtpmibg1();
            this.gvrtpmibg1 = mib;

            //Initializes the data from lanes and viasdestino .txt
            modules.getGvrtpmibg1().initializeSampleData();

            Thread ssfrThread = new Thread(new SSFR(mib), "SSFR");
            ssfrThread.setDaemon(true);
            ssfrThread.start();

            Thread sdThread = new Thread(new SD(mib), "SD");
            sdThread.setDaemon(true);
            sdThread.start();
        }

        catch (DuplicateRegistrationException drex) {
            logger.error("Duplicate registration: "+drex.getMessage()+"."+
                    " MIB object registration may be incomplete!", drex);
        }
    }

    public Variable getVariable(String name) {
        OID oid;
        OctetString context = null;
        int pos = name.indexOf(':');
        if (pos >= 0) {
            context = new OctetString(name.substring(0, pos));
            oid = new OID(name.substring(pos+1, name.length()));
        }
        else {
            oid = new OID(name);
        }
        final DefaultMOContextScope scope =
                new DefaultMOContextScope(context, oid, true, oid, true);
        MOQuery query = new DefaultMOQuery(scope, false, this);
        ManagedObject mo = server.lookup(query);

        if (mo != null) {
            final VariableBinding vb = new VariableBinding(oid);
            final RequestStatus status = new RequestStatus();
            SubRequest req = new SubRequest() {
                private boolean completed;
                private MOQuery query;

                public boolean hasError() {
                    return false;
                }

                public void setErrorStatus(int errorStatus) {
                    status.setErrorStatus(errorStatus);
                }

                public int getErrorStatus() {
                    return status.getErrorStatus();
                }

                public RequestStatus getStatus() {
                    return status;
                }

                public MOScope getScope() {
                    return scope;
                }

                public VariableBinding getVariableBinding() {
                    return vb;
                }

                public Request getRequest() {
                    return null;
                }

                public Object getUndoValue() {
                    return null;
                }

                public void setUndoValue(Object undoInformation) {
                }

                public void completed() {
                    completed = true;
                }

                public boolean isComplete() {
                    return completed;
                }

                public void setTargetMO(ManagedObject managedObject) {
                }

                public ManagedObject getTargetMO() {
                    return null;
                }

                public int getIndex() {
                    return 0;
                }

                public void setQuery(MOQuery query) {
                    this.query = query;
                }

                public MOQuery getQuery() {
                    return query;
                }

                public SubRequestIterator repetitions() {
                    return null;
                }

                public void updateNextRepetition() {
                }

                public Object getUserObject() {
                    return null;
                }

                public void setUserObject(Object userObject) {
                }

            };
            mo.get(req);
            return vb.getVariable();
        }
        return null;
    }

    public AgentConfigManager getAgentConfigManager() {
        return agent;
    }

    private static void defaultMain(String args[]) {
        ArgumentParser parser =
                new ArgumentParser(DEFAULT_CL_PARAMETERS,
                        DEFAULT_CL_COMMANDS);
        Map commandLineParameters = null;
        try {
            commandLineParameters = parser.parse(args);
            Agent agent = new Agent(commandLineParameters);
            SecurityProtocols.getInstance().addDefaultProtocols();
            agent.run();
        }
        catch (ParseException ex) {
            System.err.println(ex.getMessage());
        }
    }

    /**
     * Runs a sample agent with a default configuration defined by
     * <code>AgentConfig.properties</code>. A sample command line is:
     * <pre>
     * -c Agent.cfg -bc Agent.bc udp:127.0.0.1/4700 tcp:127.0.0.1/4700
     * </pre>
     */
    public static void main(String[] args) {

        defaultMain(args);
        SwingUtilities.invokeLater(() -> {
            // Verifies if MIB was initialized
            if (AGENT_INSTANCE != null && AGENT_INSTANCE.getGvrtpmibg1() != null) {
                GVRTPMIBG1 mib = AGENT_INSTANCE.getGvrtpmibg1();

            } else {
                System.err.println("Erro: MIB não inicializada antes de tentar iniciar o visualizador.");
            }
        });
    }

    /* Configuration of view access models

       public:
           gets - can get everything except the sys up time and sys step time
           sets - cant set anything
           walk - can WALK everything except it doesnt receive the sys up time and sys step time

       private:
           gets - can get everything
           sets - can set everything
           walk - can WALK everything

     for this to work there were changes done in AgentConfig.properties (removed default views)
     */
    public void configureVACM(VacmMIB vacm) {

        // OIDs
        OID oidLaneTable = new OID(new int[] { 1, 3, 6, 1, 3, 2025, 1, 1, 1 });
        OID oidViaDestTable = new OID(new int[] { 1, 3, 6, 1, 3, 2025, 1, 2, 1 });
        OID oidGvrtpRoot = new OID(new int[] { 1, 3, 6, 1, 3, 2025 });

        // Scalars to exclude
        OID oidScalar1 = new OID(new int[] { 1, 3, 6, 1, 3, 2025, 1, 3 }); // SysUpTime
        OID oidScalar2 = new OID(new int[] { 1, 3, 6, 1, 3, 2025, 1, 4 }); // SysStepTime

        int securityModel = SecurityModel.SECURITY_MODEL_SNMPv2c;
        int storageType = MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY;

        // Remove defaults vacm
        vacm.removeGroup(securityModel, new OctetString("public"));
        vacm.removeGroup(SecurityModel.SECURITY_MODEL_SNMPv1, new OctetString("public"));

        // Creates the 2 groups public and private
        vacm.addGroup(securityModel, new OctetString("public"), new OctetString("public-group"), storageType);
        vacm.addGroup(securityModel, new OctetString("private"), new OctetString("private-group"), storageType);

        // VIEWS
        // Public: Include Tables, EXCLUDE Scalars
        vacm.addViewTreeFamily(new OctetString("restricted-view"), oidLaneTable, new OctetString(), VacmMIB.vacmViewIncluded, storageType);
        vacm.addViewTreeFamily(new OctetString("restricted-view"), oidViaDestTable, new OctetString(), VacmMIB.vacmViewIncluded, storageType);
        vacm.addViewTreeFamily(new OctetString("restricted-view"), oidScalar1, new OctetString(), VacmMIB.vacmViewExcluded, storageType);
        vacm.addViewTreeFamily(new OctetString("restricted-view"), oidScalar2, new OctetString(), VacmMIB.vacmViewExcluded, storageType);

        // Private: Full View
        vacm.addViewTreeFamily(new OctetString("full-view"), oidGvrtpRoot, new OctetString(), VacmMIB.vacmViewIncluded, storageType);

        // ACCESS
        vacm.addAccess(new OctetString("public-group"), new OctetString(), securityModel, SecurityLevel.NOAUTH_NOPRIV, VacmMIB.vacmPrefixMatch,
                new OctetString("restricted-view"), null, new OctetString("restricted-view"), storageType);

        vacm.addAccess(new OctetString("private-group"), new OctetString(), securityModel, SecurityLevel.NOAUTH_NOPRIV, VacmMIB.vacmPrefixMatch,
                new OctetString("full-view"), new OctetString("full-view"), new OctetString("full-view"), storageType);
    }

    //creates a new private community
    private void addCommunities() {
        SnmpCommunityMIB communityMIB = agent.getSnmpCommunityMIB();
        if (communityMIB == null) return;

        OctetString contextEngineID = this.localEngineID;

        // It prevents the "Old Config vs New Code" conflict with agent.bc by not saving it there
        int storageType = StorageType.volatile_;

        // Remove old private entry if it exists from old config file
        try {
            OID privateIndex = new OctetString("private").toSubIndex(true);
            if (communityMIB.getSnmpCommunityEntry().getModel().containsRow(privateIndex)) {
                communityMIB.getSnmpCommunityEntry().removeRow(privateIndex);
            }
        } catch (Exception e) {}

        communityMIB.addSnmpCommunityEntry(
                new OctetString("private"), // Index
                new OctetString("private"), // Community Name
                new OctetString("private"), // Security Name
                contextEngineID,            // USE LOCAL ENGINE ID
                new OctetString(),          // Context Name
                new OctetString(),          // Transport Tag
                storageType                 // VOLATILE (Do not save to disk)
        );
    }
}
