

//--AgentGen BEGIN=_BEGIN
//--AgentGen END

import org.snmp4j.smi.*;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.*;
import org.snmp4j.agent.mo.snmp.*;
import org.snmp4j.agent.mo.snmp.smi.*;
import org.snmp4j.agent.request.*;
import org.snmp4j.log.LogFactory;
import org.snmp4j.log.LogAdapter;
import org.snmp4j.agent.mo.snmp.tc.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


//--AgentGen BEGIN=_IMPORT
//--AgentGen END

public class GVRTPMIBG1
//--AgentGen BEGIN=_EXTENDS
//--AgentGen END
        implements MOGroup
//--AgentGen BEGIN=_IMPLEMENTS
//--AgentGen END
{

    private static final LogAdapter LOGGER =
            LogFactory.getLogger(GVRTPMIBG1.class);

//--AgentGen BEGIN=_STATIC
//--AgentGen END

    // Factory
    private MOFactory moFactory =
            DefaultMOFactory.getInstance();

    // Constants

    /**
     * OID of this MIB module for usage which can be
     * used for its identification.
     */
    public static final OID oidGvrtpmibg1 =
            new OID(new int[] { 1,3,6,1,3,2025 });

    // Identities
    // Scalars
    // Tables

    // Notifications

    // Enumerations

    public static final class ViaterminalEnum {
        public static final int truth = 1;
        public static final int _false = 2;
    }



    // TextualConventions
    private static final String TC_MODULE_GVRTPMIBG1 = "GVRTPMIBG1";
    private static final String TC_SEMAPHORECOLOR = "SemaphoreColor";


    // Scalars

    // Tables
    public static final OID oidLaneEntry =
            new OID(new int[] { 1,3,6,1,3,2025,1,1,1 });

    // Index OID definitions
    public static final OID oidLaneID =
            new OID(new int[] { 1,3,6,1,3,2025,1,1,1,1 });

    // Column TC definitions for laneEntry:
    public static final String tcModuleGvrtpmibg1 = "GVRTPMIBG1";
    public static final String tcDefSemaphoreColor = "SemaphoreColor";

    // Column sub-identifier definitions for laneEntry:
    public static final int colVehicleCount = 2;
    public static final int colSemaphoreColor = 3;
    public static final int colTimeToChange = 4;
    public static final int colCapacity = 5;
    public static final int colRgt = 6;
    public static final int colTotalVeh = 7;
    public static final int colViaterminal = 8;

    // Column index definitions for laneEntry:
    public static final int idxVehicleCount = 0;
    public static final int idxSemaphoreColor = 1;
    public static final int idxTimeToChange = 2;
    public static final int idxCapacity = 3;
    public static final int idxRgt = 4;
    public static final int idxTotalVeh = 5;
    public static final int idxViaterminal = 6;

    private MOTableSubIndex[] laneEntryIndexes;
    private MOTableIndex laneEntryIndex;

    private MOTable<LaneEntryRow,
            MOColumn,
            MOTableModel<LaneEntryRow>> laneEntry;
    private MOTableModel<LaneEntryRow> laneEntryModel;
    public static final OID oidViaDestEntry =
            new OID(new int[] { 1,3,6,1,3,2025,1,2,1 });

    // Index OID definitions
    public static final OID oidId =
            new OID(new int[] { 1,3,6,1,3,2025,1,2,1,1 });
    //public static final OID oidLaneID =
    //  new OID(new int[] { 1,3,6,1,3,2025,1,1,1,1 });

    // Column TC definitions for viaDestEntry:

    // Column sub-identifier definitions for viaDestEntry:
    
    public static final int colDestVia = 2;
    public static final int colFluxoDest = 3;

    // Column index definitions for viaDestEntry:
    public static final int idxDestVia = 0;
    public static final int idxFluxoDest = 1;

    private MOTableSubIndex[] viaDestEntryIndexes;
    private MOTableIndex viaDestEntryIndex;

    private MOTable<ViaDestEntryRow,
            MOColumn,
            MOTableModel<ViaDestEntryRow>> viaDestEntry;
    private MOTableModel<ViaDestEntryRow> viaDestEntryModel;


//--AgentGen BEGIN=_MEMBERS
//--AgentGen END

    /**
     * Constructs a GVRTPMIBG1 instance without actually creating its
     * <code>ManagedObject</code> instances. This has to be done in a
     * sub-class constructor or after construction by calling
     * {@link #createMO(MOFactory moFactory)}.
     */
    protected GVRTPMIBG1() {
//--AgentGen BEGIN=_DEFAULTCONSTRUCTOR
//--AgentGen END
    }

    /**
     * Constructs a GVRTPMIBG1 instance and actually creates its
     * <code>ManagedObject</code> instances using the supplied
     * <code>MOFactory</code> (by calling
     * {@link #createMO(MOFactory moFactory)}).
     * @param moFactory
     *    the <code>MOFactory</code> to be used to create the
     *    managed objects for this module.
     */
    public GVRTPMIBG1(MOFactory moFactory) {
        this();
        //--AgentGen BEGIN=_FACTORYCONSTRUCTOR::factoryWrapper
        //--AgentGen END
        this.moFactory = moFactory;
        createMO(moFactory);
//--AgentGen BEGIN=_FACTORYCONSTRUCTOR
//--AgentGen END
    }

//--AgentGen BEGIN=_CONSTRUCTORS
//--AgentGen END

    /**
     * Create the ManagedObjects defined for this MIB module
     * using the specified {@link MOFactory}.
     * @param moFactory
     *    the <code>MOFactory</code> instance to use for object
     *    creation.
     */
    protected void createMO(MOFactory moFactory) {
        addTCsToFactory(moFactory);
        createLaneEntry(moFactory);
        createViaDestEntry(moFactory);
    }



    public MOTable<LaneEntryRow,MOColumn,MOTableModel<LaneEntryRow>> getLaneEntry() {
        return laneEntry;
    }


    @SuppressWarnings(value={"unchecked"})
    private void createLaneEntry(MOFactory moFactory) {
        // Index definition
        laneEntryIndexes =
                new MOTableSubIndex[] {
                        moFactory.createSubIndex(oidLaneID,
                                SMIConstants.SYNTAX_INTEGER, 1, 1)
                };

        laneEntryIndex =
                moFactory.createIndex(laneEntryIndexes,
                        false,
                        new MOTableIndexValidator() {
                            public boolean isValidIndex(OID index) {
                                boolean isValidIndex = true;
                                //--AgentGen BEGIN=laneEntry::isValidIndex
                                //--AgentGen END
                                return isValidIndex;
                            }
                        });

        // Columns
        MOColumn[] laneEntryColumns = new MOColumn[7];
        laneEntryColumns[idxVehicleCount] =
                moFactory.createColumn(colVehicleCount,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE));
        laneEntryColumns[idxSemaphoreColor] =
                moFactory.createColumn(colSemaphoreColor,
                        SMIConstants.SYNTAX_INTEGER,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                        tcModuleGvrtpmibg1,
                        tcDefSemaphoreColor);
        laneEntryColumns[idxTimeToChange] =
                moFactory.createColumn(colTimeToChange,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY));
        laneEntryColumns[idxCapacity] =
                moFactory.createColumn(colCapacity,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY));
        laneEntryColumns[idxRgt] =
                moFactory.createColumn(colRgt,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY));
        laneEntryColumns[idxTotalVeh] =
                moFactory.createColumn(colTotalVeh,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY));
        laneEntryColumns[idxViaterminal] =
                moFactory.createColumn(colViaterminal,
                        SMIConstants.SYNTAX_INTEGER,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY));
        // Table model
        laneEntryModel =
                moFactory.createTableModel(oidLaneEntry,
                        laneEntryIndex,
                        laneEntryColumns);
        ((MOMutableTableModel<LaneEntryRow>)laneEntryModel).setRowFactory(
                new LaneEntryRowFactory());
        laneEntry =
                moFactory.createTable(oidLaneEntry,
                        laneEntryIndex,
                        laneEntryColumns,
                        laneEntryModel);
    }

    public MOTable<ViaDestEntryRow,MOColumn,MOTableModel<ViaDestEntryRow>> getViaDestEntry() {
        return viaDestEntry;
    }


    @SuppressWarnings(value={"unchecked"})
    private void createViaDestEntry(MOFactory moFactory) {
        // Index definition
        viaDestEntryIndexes =
                new MOTableSubIndex[] {
                        moFactory.createSubIndex(oidId,
                                SMIConstants.SYNTAX_INTEGER, 1, 1),
                        moFactory.createSubIndex(oidLaneID,
                                SMIConstants.SYNTAX_INTEGER, 1, 1)
                };

        viaDestEntryIndex =
                moFactory.createIndex(viaDestEntryIndexes,
                        false,
                        new MOTableIndexValidator() {
                            public boolean isValidIndex(OID index) {
                                boolean isValidIndex = true;
                                //--AgentGen BEGIN=viaDestEntry::isValidIndex
                                //--AgentGen END
                                return isValidIndex;
                            }
                        });

        // Columns
        MOColumn[] viaDestEntryColumns = new MOColumn[2];
        viaDestEntryColumns[idxDestVia] =
                moFactory.createColumn(colDestVia,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY));
        viaDestEntryColumns[idxFluxoDest] =
                moFactory.createColumn(colFluxoDest,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY));
        // Table model
        viaDestEntryModel =
                moFactory.createTableModel(oidViaDestEntry,
                        viaDestEntryIndex,
                        viaDestEntryColumns);
        ((MOMutableTableModel<ViaDestEntryRow>)viaDestEntryModel).setRowFactory(
                new ViaDestEntryRowFactory());
        viaDestEntry =
                moFactory.createTable(oidViaDestEntry,
                        viaDestEntryIndex,
                        viaDestEntryColumns,
                        viaDestEntryModel);
    }



    public void registerMOs(MOServer server, OctetString context)
            throws DuplicateRegistrationException
    {
        // Scalar Objects
        server.register(this.laneEntry, context);
        server.register(this.viaDestEntry, context);
//--AgentGen BEGIN=_registerMOs
//--AgentGen END
    }

    public void unregisterMOs(MOServer server, OctetString context) {
        // Scalar Objects
        server.unregister(this.laneEntry, context);
        server.unregister(this.viaDestEntry, context);
//--AgentGen BEGIN=_unregisterMOs
//--AgentGen END
    }

    // Notifications

    // Scalars

    // Value Validators


    // Rows and Factories

    public class LaneEntryRow extends DefaultMOMutableRow2PC {

        //--AgentGen BEGIN=laneEntry::RowMembers
        //--AgentGen END

        public LaneEntryRow(OID index, Variable[] values) {
            super(index, values);
            //--AgentGen BEGIN=laneEntry::RowConstructor
            //--AgentGen END
        }

        public Integer32 getVehicleCount() {
            //--AgentGen BEGIN=laneEntry::getVehicleCount
            //--AgentGen END
            return (Integer32) super.getValue(idxVehicleCount);
        }

        public void setVehicleCount(Integer32 newColValue) {
            //--AgentGen BEGIN=laneEntry::setVehicleCount
            //--AgentGen END
            super.setValue(idxVehicleCount, newColValue);
        }

        public Integer32 getSemaphoreColor() {
            //--AgentGen BEGIN=laneEntry::getSemaphoreColor
            //--AgentGen END
            return (Integer32) super.getValue(idxSemaphoreColor);
        }

        public void setSemaphoreColor(Integer32 newColValue) {
            //--AgentGen BEGIN=laneEntry::setSemaphoreColor
            //--AgentGen END
            super.setValue(idxSemaphoreColor, newColValue);
        }

        public Integer32 getTimeToChange() {
            //--AgentGen BEGIN=laneEntry::getTimeToChange
            //--AgentGen END
            return (Integer32) super.getValue(idxTimeToChange);
        }

        public void setTimeToChange(Integer32 newColValue) {
            //--AgentGen BEGIN=laneEntry::setTimeToChange
            //--AgentGen END
            super.setValue(idxTimeToChange, newColValue);
        }

        public Integer32 getCapacity() {
            //--AgentGen BEGIN=laneEntry::getCapacity
            //--AgentGen END
            return (Integer32) super.getValue(idxCapacity);
        }

        public void setCapacity(Integer32 newColValue) {
            //--AgentGen BEGIN=laneEntry::setCapacity
            //--AgentGen END
            super.setValue(idxCapacity, newColValue);
        }

        public Integer32 getRgt() {
            //--AgentGen BEGIN=laneEntry::getRgt
            //--AgentGen END
            return (Integer32) super.getValue(idxRgt);
        }

        public void setRgt(Integer32 newColValue) {
            //--AgentGen BEGIN=laneEntry::setRgt
            //--AgentGen END
            super.setValue(idxRgt, newColValue);
        }

        public Integer32 getTotalVeh() {
            //--AgentGen BEGIN=laneEntry::getTotalVeh
            //--AgentGen END
            return (Integer32) super.getValue(idxTotalVeh);
        }

        public void setTotalVeh(Integer32 newColValue) {
            //--AgentGen BEGIN=laneEntry::setTotalVeh
            //--AgentGen END
            super.setValue(idxTotalVeh, newColValue);
        }

        public Integer32 getViaterminal() {
            //--AgentGen BEGIN=laneEntry::getViaterminal
            //--AgentGen END
            return (Integer32) super.getValue(idxViaterminal);
        }

        public void setViaterminal(Integer32 newColValue) {
            //--AgentGen BEGIN=laneEntry::setViaterminal
            //--AgentGen END
            super.setValue(idxViaterminal, newColValue);
        }

        public Variable getValue(int column) {
            //--AgentGen BEGIN=laneEntry::RowGetValue
            //--AgentGen END
            switch(column) {
                case idxVehicleCount:
                    return getVehicleCount();
                case idxSemaphoreColor:
                    return getSemaphoreColor();
                case idxTimeToChange:
                    return getTimeToChange();
                case idxCapacity:
                    return getCapacity();
                case idxRgt:
                    return getRgt();
                case idxTotalVeh:
                    return getTotalVeh();
                case idxViaterminal:
                    return getViaterminal();
                default:
                    return super.getValue(column);
            }
        }

        public void setValue(int column, Variable value) {
            //--AgentGen BEGIN=laneEntry::RowSetValue
            //--AgentGen END
            switch(column) {
                case idxVehicleCount:
                    setVehicleCount((Integer32) value);
                    break;
                case idxSemaphoreColor:
                    setSemaphoreColor((Integer32)value);
                    break;
                case idxTimeToChange:
                    setTimeToChange((Integer32)value);
                    break;
                case idxCapacity:
                    setCapacity((Integer32)value);
                    break;
                case idxRgt:
                    setRgt((Integer32)value);
                    break;
                case idxTotalVeh:
                    setTotalVeh((Integer32)value);
                    break;
                case idxViaterminal:
                    setViaterminal((Integer32)value);
                    break;
                default:
                    super.setValue(column, value);
            }
        }

        //--AgentGen BEGIN=laneEntry::Row
        //--AgentGen END
    }

    class LaneEntryRowFactory
            implements MOTableRowFactory<LaneEntryRow>
    {
        public synchronized LaneEntryRow createRow(OID index, Variable[] values)
                throws UnsupportedOperationException
        {
            LaneEntryRow row =
                    new LaneEntryRow(index, values);
            //--AgentGen BEGIN=laneEntry::createRow
            //--AgentGen END
            return row;
        }

        public synchronized void freeRow(LaneEntryRow row) {
            //--AgentGen BEGIN=laneEntry::freeRow
            //--AgentGen END
        }

        //--AgentGen BEGIN=laneEntry::RowFactory
        //--AgentGen END
    }

    public class ViaDestEntryRow extends DefaultMOMutableRow2PC {

        //--AgentGen BEGIN=viaDestEntry::RowMembers
        //--AgentGen END

        public ViaDestEntryRow(OID index, Variable[] values) {
            super(index, values);
            //--AgentGen BEGIN=viaDestEntry::RowConstructor
            //--AgentGen END
        }

        public Integer32 getDestVia() {
            //--AgentGen BEGIN=viaDestEntry::getDestVia
            //--AgentGen END
            return (Integer32) super.getValue(idxDestVia);
        }

        public void setDestVia(Integer32 newColValue) {
            //--AgentGen BEGIN=viaDestEntry::setDestVia
            //--AgentGen END
            super.setValue(idxDestVia, newColValue);
        }

        public Integer32 getFluxoDest() {
            //--AgentGen BEGIN=viaDestEntry::getFluxoDest
            //--AgentGen END
            return (Integer32) super.getValue(idxFluxoDest);
        }

        public void setFluxoDest(Integer32 newColValue) {
            //--AgentGen BEGIN=viaDestEntry::setFluxoDest
            //--AgentGen END
            super.setValue(idxFluxoDest, newColValue);
        }

        public Variable getValue(int column) {
            //--AgentGen BEGIN=viaDestEntry::RowGetValue
            //--AgentGen END
            switch(column) {
                case idxDestVia:
                    return getDestVia();
                case idxFluxoDest:
                    return getFluxoDest();
                default:
                    return super.getValue(column);
            }
        }

        public void setValue(int column, Variable value) {
            //--AgentGen BEGIN=viaDestEntry::RowSetValue
            //--AgentGen END
            switch(column) {
                case idxDestVia:
                    setDestVia((Integer32)value);
                    break;
                case idxFluxoDest:
                    setFluxoDest((Integer32)value);
                    break;
                default:
                    super.setValue(column, value);
            }
        }

        //--AgentGen BEGIN=viaDestEntry::Row
        //--AgentGen END
    }

    class ViaDestEntryRowFactory
            implements MOTableRowFactory<ViaDestEntryRow>
    {
        public synchronized ViaDestEntryRow createRow(OID index, Variable[] values)
                throws UnsupportedOperationException
        {
            ViaDestEntryRow row =
                    new ViaDestEntryRow(index, values);
            //--AgentGen BEGIN=viaDestEntry::createRow
            //--AgentGen END
            return row;
        }

        public synchronized void freeRow(ViaDestEntryRow row) {
            //--AgentGen BEGIN=viaDestEntry::freeRow
            //--AgentGen END
        }

        //--AgentGen BEGIN=viaDestEntry::RowFactory
        //--AgentGen END
    }


//--AgentGen BEGIN=_METHODS
//--AgentGen END

    // Textual Definitions of MIB module GVRTPMIBG1
    protected void addTCsToFactory(MOFactory moFactory) {
        moFactory.addTextualConvention(new SemaphoreColor());
    }


    public class SemaphoreColor implements TextualConvention {
        public static final int green = 1;
        public static final int yellow = 2;
        public static final int red = 3;
        private ValueConstraintValidator valueConstraintValidator;

        public SemaphoreColor() {
            ValueConstraint vc = new EnumerationConstraint(
                    new int[] { green,
                            yellow,
                            red });
            valueConstraintValidator = new ValueConstraintValidator(vc);
        }

        public String getModuleName() {
            return TC_MODULE_GVRTPMIBG1;
        }

        public String getName() {
            return TC_SEMAPHORECOLOR;
        }

        public Variable createInitialValue() {
            Variable v = new Integer32();
            if (v instanceof AssignableFromLong) {
                ((AssignableFromLong)v).setValue(1);
            }
            // further modify value to comply with TC constraints here:
            //--AgentGen BEGIN=SemaphoreColor::createInitialValue
            //--AgentGen END
            return v;
        }

        public MOScalar createScalar(OID oid, MOAccess access, Variable value) {
            MOScalar scalar = moFactory.createScalar(oid, access, value);
            scalar.addMOValueValidationListener(valueConstraintValidator);
            //--AgentGen BEGIN=SemaphoreColor::createScalar
            //--AgentGen END
            return scalar;
        }

        public MOColumn createColumn(int columnID, int syntax, MOAccess access,
                                     Variable defaultValue, boolean mutableInService) {
            MOColumn col = moFactory.createColumn(columnID, syntax, access,
                    defaultValue, mutableInService);
            if (col instanceof MOMutableColumn) {
                MOMutableColumn mcol = (MOMutableColumn)col;
                mcol.addMOValueValidationListener(valueConstraintValidator);
            }
            //--AgentGen BEGIN=SemaphoreColor::createColumn
            //--AgentGen END
            return col;
        }
    }


//--AgentGen BEGIN=_TC_CLASSES_IMPORTED_MODULES_BEGIN
//--AgentGen END

    // Textual Definitions of other MIB modules
    public void addImportedTCsToFactory(MOFactory moFactory) {
    }


//--AgentGen BEGIN=_TC_CLASSES_IMPORTED_MODULES_END
//--AgentGen END

//--AgentGen BEGIN=_CLASSES
//--AgentGen END

//--AgentGen BEGIN=_END
//--AgentGen END

public void initializeSampleData() {

    try {
        // Read from CSV files

        readLaneDataFromCSV("lanes.txt");
        readViaDestDataFromCSV("viasdestino.txt");

        System.out.println("Sample data initialized successfully from files");
    } catch (Exception e) {
        System.err.println("Error initializing sample data: " + e.getMessage());
        e.printStackTrace();
    }
}
    private void readLaneDataFromCSV(String filename) throws IOException, IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));

        // Skip header line
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length == 8) {
                int laneID = Integer.parseInt(parts[0]);
                int vehicleCount = Integer.parseInt(parts[1]);
                int semaphoreColor = Integer.parseInt(parts[2]);
                int timeToChange = Integer.parseInt(parts[3]);
                int capacity = Integer.parseInt(parts[4]);
                int rgt = Integer.parseInt(parts[5]);
                int totalVeh = Integer.parseInt(parts[6]);
                int viaterminal = Integer.parseInt(parts[7]);

                addLaneEntryRow(laneID, vehicleCount, semaphoreColor, timeToChange,capacity, rgt, totalVeh, viaterminal);
            }
        }
    }

    private void readViaDestDataFromCSV(String filename) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));

        // Skip header line
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length == 4) {
                int id = Integer.parseInt(parts[0]);
                int laneID = Integer.parseInt(parts[1]);
                int destVia = Integer.parseInt(parts[2]);
                int fluxoDest = Integer.parseInt(parts[3]);

                addViaDestEntryRow(id, laneID, destVia, fluxoDest);
            }
        }
    }

    // Helper methods to add rows
    private void addLaneEntryRow(int laneID, int vehicleCount, int semaphoreColor,
                                 int timeToChange, int capacity, int rgt, int totalVeh, int viaterminal) {
        OID index = new OID(new int[] { laneID });
        Variable[] values = new Variable[7];

        values[0] = new Integer32(vehicleCount);      // VehicleCount
        values[1] = new Integer32(semaphoreColor);  // SemaphoreColor (1=green, 2=yellow, 3=red)
        values[2] = new Integer32(timeToChange);    // TimeToChange
        values[3] = new Integer32(capacity);        // Capacity
        values[4] = new Integer32(rgt);             // Rgt
        values[5] = new Integer32(totalVeh);        // TotalVeh
        values[6] = new Integer32(viaterminal);     // Viaterminal

        LaneEntryRow row = new LaneEntryRow(index, values);
        ((MOMutableTableModel<LaneEntryRow>)laneEntryModel).addRow(row);
    }

    private void addViaDestEntryRow(int id, int laneID, int destVia, int fluxoDest) {
        OID index = new OID(new int[] { id, laneID });
        Variable[] values = new Variable[2];

        values[0] = new Integer32(destVia);    // DestVia
        values[1] = new Integer32(fluxoDest);  // FluxoDest

        ViaDestEntryRow row = new ViaDestEntryRow(index, values);
        ((MOMutableTableModel<ViaDestEntryRow>)viaDestEntryModel).addRow(row);
    }
}


