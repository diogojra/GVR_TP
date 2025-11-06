

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



//--AgentGen BEGIN=_IMPORT
//--AgentGen END

public class GvrTpMibG1
//--AgentGen BEGIN=_EXTENDS
//--AgentGen END
        implements MOGroup
//--AgentGen BEGIN=_IMPLEMENTS
//--AgentGen END
{

    private static final LogAdapter LOGGER =
            LogFactory.getLogger(GvrTpMibG1.class);

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
    public static final OID oidGvrTpMibG1 =
            new OID(new int[] { 1,3,6,1,3,2025 });

    // Identities
    // Scalars
    public static final OID oidSysUpTime =
            new OID(new int[] { 1,3,6,1,3,2025,1,3,0 });
    public static final OID oidStepTime =
            new OID(new int[] { 1,3,6,1,3,2025,1,4,0 });
    // Tables

    // Notifications

    // Enumerations

    public static final class ViaterminalEnum {
        public static final int truth = 1;
        public static final int _false = 2;
    }



    // TextualConventions
    private static final String TC_MODULE_GVR_TP_MIB_G1 = "GVR-TP-MIB-G1";
    private static final String TC_SEMAPHORECOLOR = "SemaphoreColor";
    private static final String TC_LANEID = "LaneID";

    // Scalars
    private MOScalar<TimeTicks> sysUpTime;
    private MOScalar<Integer32> stepTime;

    // Tables
    public static final OID oidLaneEntry =
            new OID(new int[] { 1,3,6,1,3,2025,1,1,1 });

    // Index OID definitions
    public static final OID oidLaneID =
            new OID(new int[] { 1,3,6,1,3,2025,1,1,1,1 });

    // Column TC definitions for laneEntry:
    public static final String tcModuleGvrTpMibG1 = "GVR-TP-MIB-G1";
    public static final String tcDefLaneID = "LaneID";
    public static final String tcDefSemaphoreColor = "SemaphoreColor";

    // Column sub-identifier definitions for laneEntry:
    public static final int colLaneID = 1;
    public static final int colVehicleCount = 2;
    public static final int colSemaphoreColor = 3;
    public static final int colTimeToChange = 4;
    public static final int colCapacity = 5;
    public static final int colFluxo = 6;
    public static final int colTotalVeh = 7;
    public static final int colViaterminal = 8;
    public static final int colRtg = 10;

    // Column index definitions for laneEntry:
    public static final int idxLaneID = 0;
    public static final int idxVehicleCount = 1;
    public static final int idxSemaphoreColor = 2;
    public static final int idxTimeToChange = 3;
    public static final int idxCapacity = 4;
    public static final int idxFluxo = 5;
    public static final int idxTotalVeh = 6;
    public static final int idxViaterminal = 7;
    public static final int idxRtg = 8;

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
     * Constructs a GvrTpMibG1 instance without actually creating its
     * <code>ManagedObject</code> instances. This has to be done in a
     * sub-class constructor or after construction by calling
     * {@link #createMO(MOFactory moFactory)}.
     */
    protected GvrTpMibG1() {
//--AgentGen BEGIN=_DEFAULTCONSTRUCTOR
//--AgentGen END
    }

    /**
     * Constructs a GvrTpMibG1 instance and actually creates its
     * <code>ManagedObject</code> instances using the supplied
     * <code>MOFactory</code> (by calling
     * {@link #createMO(MOFactory moFactory)}).
     * @param moFactory
     *    the <code>MOFactory</code> to be used to create the
     *    managed objects for this module.
     */
    public GvrTpMibG1(MOFactory moFactory) {
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
        sysUpTime =
                moFactory.createScalar(oidSysUpTime,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                        new TimeTicks());
        stepTime =
                moFactory.createScalar(oidStepTime,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                        new Integer32(5));
        createLaneEntry(moFactory);
        createViaDestEntry(moFactory);
    }

    public MOScalar<TimeTicks> getSysUpTime() {
        return sysUpTime;
    }
    public MOScalar<Integer32> getStepTime() {
        return stepTime;
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
        MOColumn[] laneEntryColumns = new MOColumn[9];
        laneEntryColumns[idxLaneID] =
                moFactory.createColumn(colLaneID,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                        tcModuleGvrTpMibG1,
                        tcDefLaneID);
        laneEntryColumns[idxVehicleCount] =
                new MOMutableColumn<Gauge32>(colVehicleCount,
                        SMIConstants.SYNTAX_GAUGE32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE),
                        (Gauge32)null
                        //--AgentGen BEGIN=vehicleCount::auxInit
                        //--AgentGen END
                );
        ((MOMutableColumn)laneEntryColumns[idxVehicleCount]).
                addMOValueValidationListener(new VehicleCountValidator());
        laneEntryColumns[idxSemaphoreColor] =
                new MOMutableColumn<Integer32>(colSemaphoreColor,
                        SMIConstants.SYNTAX_INTEGER,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE),
                        (Integer32)null
                        //--AgentGen BEGIN=semaphoreColor::auxInit
                        //--AgentGen END
                );
        ValueConstraint semaphoreColorVC = new EnumerationConstraint(
                new int[] { 1,
                        2,
                        3 });
        ((MOMutableColumn)laneEntryColumns[idxSemaphoreColor]).
                addMOValueValidationListener(new ValueConstraintValidator(semaphoreColorVC));
        ((MOMutableColumn)laneEntryColumns[idxSemaphoreColor]).
                addMOValueValidationListener(new SemaphoreColorValidator());
        laneEntryColumns[idxTimeToChange] =
                new MOMutableColumn<Integer32>(colTimeToChange,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE),
                        (Integer32)null
                        //--AgentGen BEGIN=timeToChange::auxInit
                        //--AgentGen END
                );
        ValueConstraint timeToChangeVC = new ConstraintsImpl();
        ((ConstraintsImpl)timeToChangeVC).add(new Constraint(0L, 2147483647L));
        ((MOMutableColumn)laneEntryColumns[idxTimeToChange]).
                addMOValueValidationListener(new ValueConstraintValidator(timeToChangeVC));
        ((MOMutableColumn)laneEntryColumns[idxTimeToChange]).
                addMOValueValidationListener(new TimeToChangeValidator());
        laneEntryColumns[idxCapacity] =
                moFactory.createColumn(colCapacity,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY));
        laneEntryColumns[idxFluxo] =
                moFactory.createColumn(colFluxo,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY));
        laneEntryColumns[idxTotalVeh] =
                new MOMutableColumn<Integer32>(colTotalVeh,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE),
                        (Integer32)null
                        //--AgentGen BEGIN=totalVeh::auxInit
                        //--AgentGen END
                );
        ((MOMutableColumn)laneEntryColumns[idxTotalVeh]).
                addMOValueValidationListener(new TotalVehValidator());
        laneEntryColumns[idxViaterminal] =
                moFactory.createColumn(colViaterminal,
                        SMIConstants.SYNTAX_INTEGER,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY));
        laneEntryColumns[idxRtg] =
                moFactory.createColumn(colRtg,
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
        server.register(this.sysUpTime, context);
        server.register(this.stepTime, context);
        server.register(this.laneEntry, context);
        server.register(this.viaDestEntry, context);
//--AgentGen BEGIN=_registerMOs
//--AgentGen END
    }

    public void unregisterMOs(MOServer server, OctetString context) {
        // Scalar Objects
        server.unregister(this.sysUpTime, context);
        server.unregister(this.stepTime, context);
        server.unregister(this.laneEntry, context);
        server.unregister(this.viaDestEntry, context);
//--AgentGen BEGIN=_unregisterMOs
//--AgentGen END
    }

    // Notifications

    // Scalars

    // Value Validators

    /**
     * The <code>VehicleCountValidator</code> implements the value
     * validation for <code>VehicleCount</code>.
     */
    static class VehicleCountValidator implements MOValueValidationListener {

        public void validate(MOValueValidationEvent validationEvent) {
            Variable newValue = validationEvent.getNewValue();
            //--AgentGen BEGIN=vehicleCount::validate
            //--AgentGen END
        }
    }
    /**
     * The <code>SemaphoreColorValidator</code> implements the value
     * validation for <code>SemaphoreColor</code>.
     */
    static class SemaphoreColorValidator implements MOValueValidationListener {

        public void validate(MOValueValidationEvent validationEvent) {
            Variable newValue = validationEvent.getNewValue();
            //--AgentGen BEGIN=semaphoreColor::validate
            //--AgentGen END
        }
    }
    /**
     * The <code>TimeToChangeValidator</code> implements the value
     * validation for <code>TimeToChange</code>.
     */
    static class TimeToChangeValidator implements MOValueValidationListener {

        public void validate(MOValueValidationEvent validationEvent) {
            Variable newValue = validationEvent.getNewValue();
            long v = ((Integer32)newValue).getValue();
            if (!(((v >= 0L) && (v <= 2147483647L)))) {
                validationEvent.setValidationStatus(SnmpConstants.SNMP_ERROR_WRONG_VALUE);
                return;
            }
            //--AgentGen BEGIN=timeToChange::validate
            //--AgentGen END
        }
    }
    /**
     * The <code>TotalVehValidator</code> implements the value
     * validation for <code>TotalVeh</code>.
     */
    static class TotalVehValidator implements MOValueValidationListener {

        public void validate(MOValueValidationEvent validationEvent) {
            Variable newValue = validationEvent.getNewValue();
            //--AgentGen BEGIN=totalVeh::validate
            //--AgentGen END
        }
    }

    // Rows and Factories

    public class LaneEntryRow extends DefaultMOMutableRow2PC {

        //--AgentGen BEGIN=laneEntry::RowMembers
        //--AgentGen END

        public LaneEntryRow(OID index, Variable[] values) {
            super(index, values);
            //--AgentGen BEGIN=laneEntry::RowConstructor
            //--AgentGen END
        }

        public Integer32 getLaneID() {
            //--AgentGen BEGIN=laneEntry::getLaneID
            //--AgentGen END
            return (Integer32) super.getValue(idxLaneID);
        }

        public void setLaneID(Integer32 newColValue) {
            //--AgentGen BEGIN=laneEntry::setLaneID
            //--AgentGen END
            super.setValue(idxLaneID, newColValue);
        }

        public Gauge32 getVehicleCount() {
            //--AgentGen BEGIN=laneEntry::getVehicleCount
            //--AgentGen END
            return (Gauge32) super.getValue(idxVehicleCount);
        }

        public void setVehicleCount(Gauge32 newColValue) {
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

        public Integer32 getFluxo() {
            //--AgentGen BEGIN=laneEntry::getFluxo
            //--AgentGen END
            return (Integer32) super.getValue(idxFluxo);
        }

        public void setFluxo(Integer32 newColValue) {
            //--AgentGen BEGIN=laneEntry::setFluxo
            //--AgentGen END
            super.setValue(idxFluxo, newColValue);
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

        public Integer32 getRtg() {
            //--AgentGen BEGIN=laneEntry::getRtg
            //--AgentGen END
            return (Integer32) super.getValue(idxRtg);
        }

        public void setRtg(Integer32 newColValue) {
            //--AgentGen BEGIN=laneEntry::setRtg
            //--AgentGen END
            super.setValue(idxRtg, newColValue);
        }

        public Variable getValue(int column) {
            //--AgentGen BEGIN=laneEntry::RowGetValue
            //--AgentGen END
            switch(column) {
                case idxLaneID:
                    return getLaneID();
                case idxVehicleCount:
                    return getVehicleCount();
                case idxSemaphoreColor:
                    return getSemaphoreColor();
                case idxTimeToChange:
                    return getTimeToChange();
                case idxCapacity:
                    return getCapacity();
                case idxFluxo:
                    return getFluxo();
                case idxTotalVeh:
                    return getTotalVeh();
                case idxViaterminal:
                    return getViaterminal();
                case idxRtg:
                    return getRtg();
                default:
                    return super.getValue(column);
            }
        }

        public void setValue(int column, Variable value) {
            //--AgentGen BEGIN=laneEntry::RowSetValue
            //--AgentGen END
            switch(column) {
                case idxLaneID:
                    setLaneID((Integer32)value);
                    break;
                case idxVehicleCount:
                    setVehicleCount((Gauge32)value);
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
                case idxFluxo:
                    setFluxo((Integer32)value);
                    break;
                case idxTotalVeh:
                    setTotalVeh((Integer32)value);
                    break;
                case idxViaterminal:
                    setViaterminal((Integer32)value);
                    break;
                case idxRtg:
                    setRtg((Integer32)value);
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

    // Textual Definitions of MIB module GvrTpMibG1
    protected void addTCsToFactory(MOFactory moFactory) {
        moFactory.addTextualConvention(new SemaphoreColor());
        moFactory.addTextualConvention(new LaneID());
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
            return TC_MODULE_GVR_TP_MIB_G1;
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


    public class LaneID implements TextualConvention {
        private ValueConstraintValidator valueConstraintValidator;

        public LaneID() {
            ValueConstraint vc = new ConstraintsImpl();
            ((ConstraintsImpl)vc).add(new Constraint(1L, 2147483647L));
            valueConstraintValidator = new ValueConstraintValidator(vc);
        }

        public String getModuleName() {
            return TC_MODULE_GVR_TP_MIB_G1;
        }

        public String getName() {
            return TC_LANEID;
        }

        public Variable createInitialValue() {
            Variable v = new Integer32();
            if (v instanceof AssignableFromLong) {
                ((AssignableFromLong)v).setValue(1L);
            }
            // further modify value to comply with TC constraints here:
            //--AgentGen BEGIN=LaneID::createInitialValue
            //--AgentGen END
            return v;
        }

        public MOScalar createScalar(OID oid, MOAccess access, Variable value) {
            MOScalar scalar = moFactory.createScalar(oid, access, value);
            scalar.addMOValueValidationListener(valueConstraintValidator);
            //--AgentGen BEGIN=LaneID::createScalar
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
            //--AgentGen BEGIN=LaneID::createColumn
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
}


