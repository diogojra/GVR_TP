//|:AgenPro|=_BEGIN
//|AgenPro:|

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

//|:AgenPro|=import
//|AgenPro:|


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
    // supported MIBs
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
        // initialize agent before registering our own modules
        agent.initialize();
        configureVACM(agent.getVacmMIB());
        // this requires sysUpTime to be available.
        registerMIBs();
        addCommunities();
        // add proxy forwarder
        agent.setupProxyForwarder();
        // now continue agent setup and launch it.
        agent.run();
    }

    /**
     * Get the {@link MOFactory} that creates the various MOs (MIB Objects).
     * @return
     *    a {@link DefaultMOFactory} instance by default.
     */
    protected MOFactory getFactory() {
        //|:AgenPro|=factory

        return DefaultMOFactory.getInstance();
        //|AgenPro:|
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
            // ADD THIS LINE to initialize sample data
            modules.getGvrtpmibg1().initializeSampleData();

            //|:AgenPro:|=registerOtherContexts
            //|AgenPro:|
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
            // Add all available security protocols (e.g. SHA,MD5,DES,AES,3DES,..)
            SecurityProtocols.getInstance().addDefaultProtocols();
            // configure system group:
            // Set system description:
            // sampleAgent.agent.getSysDescr().setValue("My system description".getBytes());
            // Set system OID (= OID of the AGENT-CAPABILITIES statement describing
            // the implemented MIB objects of this agent:
            // sampleAgent.agent.getSysOID().setValue("1.3.1.6.1.4.1....");
            // Set the system services
            // sampleAgent.agent.getSysServices().setValue(72);
            agent.run();
            // Fire a sample named trap generated by AgenPro:
            // agent.modules.getIfMib().fireLinkDownIf10001(agent.server,
            // 	 agent.agent.getNotificationOriginator(), null, null);
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
     *
     * @param args
     *    the command line arguments defining at least the listen addresses.
     *    The format is <code>-c[s{=Agent.cfg}] -bc[s{=Agent.bc}]
     *    #address[s<(udp|tcp):.*[/[0-9]+]?>] ..</code>. For the format
     *    description see {@link ArgumentParser}.
     */
    public static void main(String[] args) {
        //|:AgenPro|=main

        defaultMain(args);
        SwingUtilities.invokeLater(() -> {
            // Verifica se a MIB já foi inicializada (depois de defaultMain)
            if (AGENT_INSTANCE != null && AGENT_INSTANCE.getGvrtpmibg1() != null) {
                GVRTPMIBG1 mib = AGENT_INSTANCE.getGvrtpmibg1();

            } else {
                System.err.println("Erro: MIB não inicializada antes de tentar iniciar o visualizador.");
            }
        });
        //|AgenPro:|
    }


    public void configureVACM(VacmMIB vacm) {
        // OIDs
        OID oidLaneTable = new OID(new int[] { 1, 3, 6, 1, 3, 2025, 1, 1, 1 });
        OID oidViaDestTable = new OID(new int[] { 1, 3, 6, 1, 3, 2025, 1, 2, 1 });
        OID oidGvrtpRoot = new OID(new int[] { 1, 3, 6, 1, 3, 2025 });

        // Scalars to explicitly exclude
        OID oidScalar1 = new OID(new int[] { 1, 3, 6, 1, 3, 2025, 1, 3 }); // SysUpTime
        OID oidScalar2 = new OID(new int[] { 1, 3, 6, 1, 3, 2025, 1, 4 }); // SysStepTime

        int securityModel = SecurityModel.SECURITY_MODEL_SNMPv2c;
        int storageType = MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY;

        // 1. CLEANUP (Remove defaults)
        vacm.removeGroup(securityModel, new OctetString("public"));
        vacm.removeGroup(SecurityModel.SECURITY_MODEL_SNMPv1, new OctetString("public"));

        // 2. GROUPS
        vacm.addGroup(securityModel, new OctetString("public"), new OctetString("public-group"), storageType);
        vacm.addGroup(securityModel, new OctetString("private"), new OctetString("private-group"), storageType);

        // 3. VIEWS
        // Public: Include Tables, EXCLUDE Scalars
        vacm.addViewTreeFamily(new OctetString("restricted-view"), oidLaneTable, new OctetString(), VacmMIB.vacmViewIncluded, storageType);
        vacm.addViewTreeFamily(new OctetString("restricted-view"), oidViaDestTable, new OctetString(), VacmMIB.vacmViewIncluded, storageType);
        vacm.addViewTreeFamily(new OctetString("restricted-view"), oidScalar1, new OctetString(), VacmMIB.vacmViewExcluded, storageType);
        vacm.addViewTreeFamily(new OctetString("restricted-view"), oidScalar2, new OctetString(), VacmMIB.vacmViewExcluded, storageType);

        // Private: Full View
        vacm.addViewTreeFamily(new OctetString("full-view"), oidGvrtpRoot, new OctetString(), VacmMIB.vacmViewIncluded, storageType);

        // 4. ACCESS
        vacm.addAccess(new OctetString("public-group"), new OctetString(), securityModel, SecurityLevel.NOAUTH_NOPRIV, VacmMIB.vacmPrefixMatch,
                new OctetString("restricted-view"), null, new OctetString("restricted-view"), storageType);

        vacm.addAccess(new OctetString("private-group"), new OctetString(), securityModel, SecurityLevel.NOAUTH_NOPRIV, VacmMIB.vacmPrefixMatch,
                new OctetString("full-view"), new OctetString("full-view"), new OctetString("full-view"), storageType);
    }

    // =================================================================================
    //  FIXED COMMUNITY REGISTRATION
    // =================================================================================
    private void addCommunities() {
        SnmpCommunityMIB communityMIB = agent.getSnmpCommunityMIB();
        if (communityMIB == null) return;

        // CRITICAL FIX 1: Use the LOCAL Engine ID (Same as Public).
        // If we use empty context, it mismatches the data registration.
        OctetString contextEngineID = this.localEngineID;

        // CRITICAL FIX 2: Use VOLATILE storage (2).
        // This ensures this row is NEVER saved to Agent.cfg.
        // It prevents the "Old Config vs New Code" conflict forever.
        int storageType = StorageType.volatile_;

        System.out.println("Registering 'private' with Local EngineID: " + contextEngineID);

        // Remove old private entry if it exists (from old config file)
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
