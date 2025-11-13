import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.Scanner;

public class TestAgent2 {

    public static void main(String[] args) throws IOException {
        // === 1) Configurar SNMP (manager) ===
        TransportMapping<?> transport = new DefaultUdpTransportMapping();
        Snmp snmp = new Snmp(transport);
        transport.listen();

        // Endereço do agente (o teu SNMP4J-Agent que está a correr em 127.0.0.1/555)
        Address targetAddress = new UdpAddress("127.0.0.1/555");
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));            // SNMPv2c community
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);                // versão SNMPv2c

        System.out.println("=== Mini cliente SNMPv2c em Java (GET / SET / WALK / GETNEXT) ===");
        System.out.println("Target: " + targetAddress.toString() + " community='public'");
        System.out.println("Comandos:");
        System.out.println("  get <oid>");
        System.out.println("  set <oid> <valorInt>");
        System.out.println("  walk <oidBase>");
        System.out.println("  getnext <oid>");
        System.out.println("  help");
        System.out.println("  exit / quit");
        System.out.println("===============================================================");

        Scanner scanner = new Scanner(System.in);

        try {
            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                String cmd = parts[0].toLowerCase();

                if (cmd.equals("exit") || cmd.equals("quit")) {
                    System.out.println("A sair...");
                    break;
                } else if (cmd.equals("help")) {
                    printHelp();
                } else if (cmd.equals("get")) {
                    if (parts.length < 2) {
                        System.out.println("Uso: get <oid>");
                        continue;
                    }
                    String oid = parts[1];
                    getOID(snmp, target, oid);
                } else if (cmd.equals("set")) {
                    if (parts.length < 3) {
                        System.out.println("Uso: set <oid> <valorInt>");
                        continue;
                    }
                    String oid = parts[1];
                    try {
                        int value = Integer.parseInt(parts[2]);
                        setOID(snmp, target, oid, value);
                    } catch (NumberFormatException e) {
                        System.out.println("Valor inválido, tem de ser inteiro.");
                    }
                } else if (cmd.equals("walk")) {
                    if (parts.length < 2) {
                        System.out.println("Uso: walk <oidBase>");
                        continue;
                    }
                    String oidBase = parts[1];
                    walkTable(snmp, target, oidBase);
                } else if (cmd.equals("getnext")) {
                    if (parts.length < 2) {
                        System.out.println("Uso: getnext <oid>");
                        continue;
                    }
                    String oid = parts[1];
                    getNextOID(snmp, target, oid);
                } else {
                    System.out.println("Comando desconhecido. Escreve 'help' para ver comandos.");
                }
            }
        } finally {
            snmp.close();
            transport.close();
            scanner.close();
        }
    }

    private static void printHelp() {
        System.out.println("Comandos disponíveis:");
        System.out.println("  get <oid>             - Faz um SNMP GET");
        System.out.println("  set <oid> <valorInt>  - Faz um SNMP SET (Integer32)");
        System.out.println("  walk <oidBase>        - Faz um 'walk' (GETNEXT em loop) a partir de oidBase");
        System.out.println("  getnext <oid>         - Um único GETNEXT a partir de oid");
        System.out.println("  exit / quit           - Sair do programa");
    }

    // === GET simples (um único OID) ===
    private static void getOID(Snmp snmp, Target target, String oid) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            ResponseEvent response = snmp.send(pdu, target);
            if (response != null && response.getResponse() != null) {
                PDU responsePDU = response.getResponse();
                if (responsePDU.getErrorStatus() == PDU.noError) {
                    VariableBinding vb = responsePDU.get(0);
                    System.out.println("GET " + oid + " = " + vb.getVariable());
                } else {
                    System.out.println("Erro GET: " + responsePDU.getErrorStatusText());
                }
            } else {
                System.out.println("GET " + oid + ": Sem resposta");
            }
        } catch (Exception e) {
            System.out.println("GET " + oid + ": Exception - " + e.getMessage());
        }
    }

    // === GETNEXT (um passo) ===
    private static void getNextOID(Snmp snmp, Target target, String oid) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GETNEXT);

            ResponseEvent response = snmp.send(pdu, target);
            if (response != null && response.getResponse() != null) {
                PDU responsePDU = response.getResponse();
                if (responsePDU.getErrorStatus() == PDU.noError) {
                    VariableBinding vb = responsePDU.get(0);
                    System.out.println("GETNEXT from " + oid + " => " +
                            vb.getOid() + " = " + vb.getVariable());
                } else {
                    System.out.println("Erro GETNEXT: " + responsePDU.getErrorStatusText());
                }
            } else {
                System.out.println("GETNEXT " + oid + ": Sem resposta");
            }
        } catch (Exception e) {
            System.out.println("GETNEXT " + oid + ": Exception - " + e.getMessage());
        }
    }

    // === WALK (repetir GETNEXT até sair da subtree) ===
    private static void walkTable(Snmp snmp, Target target, String tableOID) {
        try {
            OID rootOID = new OID(tableOID);
            OID currentOID = new OID(rootOID);

            while (true) {
                PDU pdu = new PDU();
                pdu.add(new VariableBinding(currentOID));
                pdu.setType(PDU.GETNEXT);

                ResponseEvent response = snmp.send(pdu, target);
                if (response == null || response.getResponse() == null) {
                    System.out.println("Sem resposta no walk.");
                    break;
                }

                PDU responsePDU = response.getResponse();
                if (responsePDU.getErrorStatus() != PDU.noError) {
                    System.out.println("Erro no walk: " + responsePDU.getErrorStatusText());
                    break;
                }

                VariableBinding vb = responsePDU.get(0);
                OID nextOID = vb.getOid();

                // Se sair da subtree, termina
                if (!nextOID.startsWith(rootOID)) {
                    break;
                }

                System.out.println(nextOID + " = " + vb.getVariable());

                currentOID = nextOID;
            }
        } catch (Exception e) {
            System.out.println("Exception no walk: " + e.getMessage());
        }
    }

    // === SET (Integer32) ===
    private static void setOID(Snmp snmp, Target target, String oid, int value) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid), new Integer32(value)));
            pdu.setType(PDU.SET);

            ResponseEvent response = snmp.send(pdu, target);
            if (response != null && response.getResponse() != null) {
                PDU responsePDU = response.getResponse();
                if (responsePDU.getErrorStatus() == PDU.noError) {
                    VariableBinding vb = responsePDU.get(0);
                    System.out.println("SET " + oid + " OK, novo valor = " + vb.getVariable());
                } else {
                    System.out.println("Erro SET: " + responsePDU.getErrorStatusText());
                }
            } else {
                System.out.println("SET " + oid + ": Sem resposta");
            }
        } catch (Exception e) {
            System.out.println("SET " + oid + ": Exception - " + e.getMessage());
        }
    }
}
