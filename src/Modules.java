//--AgentGen BEGIN=_BEGIN
//--AgentGen END
import org.snmp4j.agent.mo.*;
import org.snmp4j.log.LogFactory;
import org.snmp4j.log.LogAdapter;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.smi.OctetString;


//--AgentGen BEGIN=_IMPORT
//--AgentGen END

public class Modules implements MOGroup {

    private static final LogAdapter LOGGER =
            LogFactory.getLogger(Modules.class);

    private GvrTpMibG1 gvrTpMibG1;

    private MOFactory factory;

//--AgentGen BEGIN=_MEMBERS
//--AgentGen END

    public Modules() {
        gvrTpMibG1 = new GvrTpMibG1();
//--AgentGen BEGIN=_DEFAULTCONSTRUCTOR
//--AgentGen END
    }

    public Modules(MOFactory factory) {
        gvrTpMibG1 = new GvrTpMibG1(factory);
//--AgentGen BEGIN=_CONSTRUCTOR
//--AgentGen END
    }

    public void registerMOs(MOServer server, OctetString context)
            throws DuplicateRegistrationException
    {
        gvrTpMibG1.registerMOs(server, context);
//--AgentGen BEGIN=_registerMOs
//--AgentGen END
    }

    public void unregisterMOs(MOServer server, OctetString context) {
        gvrTpMibG1.unregisterMOs(server, context);
//--AgentGen BEGIN=_unregisterMOs
//--AgentGen END
    }

    public GvrTpMibG1 getGvrTpMibG1() {
        return gvrTpMibG1;
    }


//--AgentGen BEGIN=_METHODS
//--AgentGen END

//--AgentGen BEGIN=_CLASSES
//--AgentGen END

//--AgentGen BEGIN=_END
//--AgentGen END

}

