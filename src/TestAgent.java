import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.*;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import java.io.IOException;

public class TestAgent {
    public static void main(String[] args) throws IOException {
        // Configurar o SNMP
        TransportMapping transport = new DefaultUdpTransportMapping();
        Snmp snmp = new Snmp(transport);
        transport.listen();

        // Endereço do agente
        Address targetAddress = new UdpAddress("127.0.0.1/555");
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);

        System.out.println("=== TESTANDO MIB GvrTpMibG1 ===");

        // Testar OIDs específicos
        testOID(snmp, target, "1.3.6.1.3.2025.1.1.1.2.1", "VehicleCount lane 1");
        testOID(snmp, target, "1.3.6.1.3.2025.1.1.1.3.1", "SemaphoreColor lane 1");
        testOID(snmp, target, "1.3.6.1.3.2025.1.1.1.2.2", "VehicleCount lane 2");

        // Fazer walk na tabela laneEntry
        System.out.println("\n=== TABELA laneEntry ===");
        walkTable(snmp, target, "1.3.6.1.3.2025.1.1.1");

        System.out.println("\n=== TABELA viaDestEntry ===");
        walkTable(snmp, target, "1.3.6.1.3.2025.1.2.1");

        System.out.println("=== SET VehicleCount lane 1 ===");
        setOID(snmp, target, "1.3.6.1.3.2025.1.1.1.2.1", 42, "Set VehicleCount lane 1 to 42");

        snmp.close();
    }

    private static void testOID(Snmp snmp, Target target, String oid, String description) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            ResponseEvent response = snmp.send(pdu, target);
            if (response != null && response.getResponse() != null) {
                PDU responsePDU = response.getResponse();
                if (responsePDU.getErrorStatus() == PDU.noError) {
                    VariableBinding vb = responsePDU.get(0);
                    System.out.println(description + " (" + oid + "): " + vb.getVariable());
                } else {
                    System.out.println(description + ": ERRO - " + responsePDU.getErrorStatusText());
                }
            } else {
                System.out.println(description + ": Sem resposta");
            }
        } catch (Exception e) {
            System.out.println(description + ": Exception - " + e.getMessage());
        }
    }

    private static void walkTable(Snmp snmp, Target target, String tableOID) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(tableOID)));
            pdu.setType(PDU.GETNEXT);

            OID currentOID = new OID(tableOID);
            boolean tableEnd = false;

            while (!tableEnd) {
                ResponseEvent response = snmp.send(pdu, target);
                if (response != null && response.getResponse() != null) {
                    PDU responsePDU = response.getResponse();
                    if (responsePDU.getErrorStatus() == PDU.noError) {
                        VariableBinding vb = responsePDU.get(0);
                        OID responseOID = vb.getOid();

                        // Verificar se ainda estamos na mesma tabela
                        if (responseOID.startsWith(currentOID)) {
                            System.out.println(responseOID + " = " + vb.getVariable());

                            // Preparar próximo request
                            pdu = new PDU();
                            pdu.add(new VariableBinding(responseOID));
                            pdu.setType(PDU.GETNEXT);
                        } else {
                            tableEnd = true;
                        }
                    } else {
                        System.out.println("Erro no walk: " + responsePDU.getErrorStatusText());
                        tableEnd = true;
                    }
                } else {
                    System.out.println("Sem resposta no walk");
                    tableEnd = true;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception no walk: " + e.getMessage());
        }
    }
    private static void setOID(Snmp snmp, Target target, String oid, int value, String description) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid), new Integer32(value)));
            pdu.setType(PDU.SET);

            ResponseEvent response = snmp.send(pdu, target);
            if (response != null && response.getResponse() != null) {
                PDU responsePDU = response.getResponse();
                if (responsePDU.getErrorStatus() == PDU.noError) {
                    VariableBinding vb = responsePDU.get(0);
                    System.out.println(description + " (" + oid + "): " + vb.getVariable());
                } else {
                    System.out.println(description + ": ERRO - " + responsePDU.getErrorStatusText());
                }
            } else {
                System.out.println(description + ": Sem resposta");
            }
        } catch (Exception e) {
            System.out.println(description + ": Exception - " + e.getMessage());
        }
    }
}
