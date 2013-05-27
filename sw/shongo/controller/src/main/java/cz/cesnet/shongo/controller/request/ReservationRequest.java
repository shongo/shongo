package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.api.ReservationRequestState;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.scheduler.SchedulerReport;
import cz.cesnet.shongo.controller.scheduler.SchedulerReportSet;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.util.ObjectHelper;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a request created by an user to get allocated some resources for video conference calls.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ReservationRequest extends AbstractReservationRequest implements Reporter.ReportContext
{
    /**
     * {@link Allocation} for which this {@link ReservationRequest} has been created as child.
     */
    private Allocation parentAllocation;

    /**
     * Start date/time from which the reservation is requested.
     */
    private DateTime slotStart;

    /**
     * End date/time to which the reservation is requested.
     */
    private DateTime slotEnd;

    /**
     * State of the compartment request.
     */
    private State state;

    /**
     * List of {@link SchedulerReport}s for this {@link ReservationRequest}.
     */
    private List<SchedulerReport> reports = new ArrayList<SchedulerReport>();

    /**
     * Constructor.
     */
    public ReservationRequest()
    {
    }

    /**
     * Constructor.
     *
     * @param userId sets the {@link #setUserId(String)}
     */
    public ReservationRequest(String userId, ReservationRequestPurpose purpose)
    {
        setUserId(userId);
        setPurpose(purpose);
    }

    /**
     * @return {@link #parentAllocation}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public Allocation getParentAllocation()
    {
        return parentAllocation;
    }

    /**
     * @param parentAllocation sets the {@link #parentAllocation}
     */
    public void setParentAllocation(Allocation parentAllocation)
    {
        // Manage bidirectional association
        if (parentAllocation != this.parentAllocation) {
            if (this.parentAllocation != null) {
                Allocation oldParentAllocation = this.parentAllocation;
                this.parentAllocation = null;
                oldParentAllocation.removeChildReservationRequest(this);
            }
            if (parentAllocation != null) {
                this.parentAllocation = parentAllocation;
                this.parentAllocation.addChildReservationRequest(this);
            }
        }
    }

    /**
     * @return {@link #slotStart}
     */
    @Column
    @org.hibernate.annotations.Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getSlotStart()
    {
        return slotStart;
    }

    /**
     * @param slotStart sets the {@link #slotStart}
     */
    public void setSlotStart(DateTime slotStart)
    {
        this.slotStart = slotStart;
    }

    /**
     * @return {@link #slotEnd}
     */
    @Column
    @org.hibernate.annotations.Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getSlotEnd()
    {
        return slotEnd;
    }

    /**
     * @param slotEnd sets the {@link #slotEnd}
     */
    public void setSlotEnd(DateTime slotEnd)
    {
        this.slotEnd = slotEnd;
    }

    /**
     * @return requested slot ({@link #slotStart}, {@link #slotEnd})
     */
    @Transient
    public Interval getSlot()
    {
        return new Interval(slotStart, slotEnd);
    }

    /**
     * @param slot sets the requested slot
     */
    @Transient
    public void setSlot(Interval slot)
    {
        setSlotStart(slot.getStart());
        setSlotEnd(slot.getEnd());
    }

    /**
     * @param dateTime sets date/ime of the requested slot
     * @param duration sets duration of the requested slot
     */
    @Transient
    public void setSlot(DateTime dateTime, Period duration)
    {
        setSlot(new Interval(dateTime, duration));
    }

    /**
     * @return {@link #state}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    /**
     * Clear {@link #state}, useful for removing {@link State#ALLOCATED} state.
     */
    public void clearState()
    {
        this.state = null;
    }

    /**
     * @return {@link #reports}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<SchedulerReport> getReports()
    {
        return Collections.unmodifiableList(reports);
    }

    /**
     * @param reports sets the {@link #reports}
     */
    public void setReports(List<SchedulerReport> reports)
    {
        this.reports.clear();
        for (SchedulerReport report : reports) {
            this.reports.add(report);
        }
    }

    /**
     * @param report to be added to the {@link #reports}
     */
    public void addReport(SchedulerReport report)
    {
        reports.add(report);
    }

    /**
     * @param report to be removed from the {@link #reports}
     */
    public void removeReport(SchedulerReport report)
    {
        reports.remove(report);
    }

    /**
     * Remove all {@link SchedulerReport}s from the {@link #reports}.
     */
    public void clearReports()
    {
        reports.clear();
    }

    /**
     * @return report string containing report header and all {@link #reports}
     */
    @Transient
    public String getReportText(Report.MessageType messageType)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (SchedulerReport report : reports) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("\n");
                stringBuilder.append("\n");
            }
            stringBuilder.append(report.getMessageRecursive(messageType));
        }
        return (stringBuilder.length() > 0 ? stringBuilder.toString() : null);
    }

    /**
     * Update state of the {@link ReservationRequest} based on {@link #specification}.
     * <p/>
     * If {@link #specification} is instance of {@link StatefulSpecification} and it's
     * {@link StatefulSpecification#getCurrentState()} is {@link StatefulSpecification.State#NOT_READY}
     * the state of {@link ReservationRequest} is set to {@link State#NOT_COMPLETE}.
     * Otherwise the state is not changed or forced to {@link State#COMPLETE} in incorrect cases.
     *
     * @see State
     */
    public void updateStateBySpecification()
    {
        State newState = getState();
        if (newState == null || newState == State.NOT_COMPLETE) {
            newState = State.COMPLETE;
        }
        List<SchedulerReport> reports = new ArrayList<SchedulerReport>();
        Specification specification = getSpecification();
        if (specification instanceof StatefulSpecification) {
            StatefulSpecification statefulSpecification = (StatefulSpecification) specification;
            if (statefulSpecification.getCurrentState().equals(StatefulSpecification.State.NOT_READY)) {
                newState = State.NOT_COMPLETE;
                reports.add(new SchedulerReportSet.SpecificationNotReadyReport(specification));
            }
        }

        if (newState != getState()) {
            setState(newState);
            setReports(reports);
        }
    }

    @Override
    protected void onCreate()
    {
        super.onCreate();

        if (state == null) {
            updateStateBySpecification();
        }
    }

    /**
     * @return {@link #state} converted to API
     */
    @Transient
    public cz.cesnet.shongo.controller.api.ReservationRequestState getStateAsApi()
    {
        switch (getState()) {
            case NOT_COMPLETE:
                return cz.cesnet.shongo.controller.api.ReservationRequestState.NOT_COMPLETE;
            case COMPLETE:
                return ReservationRequestState.NOT_ALLOCATED;
            case ALLOCATED:
                Reservation reservation = getAllocation().getCurrentReservation();
                if (reservation == null) {
                    throw new IllegalStateException("Allocated reservation request should have a reservation.");
                }
                Executable executable = reservation.getExecutable();
                if (executable != null) {
                    switch (executable.getState()) {
                        case STARTED:
                            return cz.cesnet.shongo.controller.api.ReservationRequestState.STARTED;
                        case MODIFIED:
                            return cz.cesnet.shongo.controller.api.ReservationRequestState.STARTED;
                        case STARTING_FAILED:
                            return cz.cesnet.shongo.controller.api.ReservationRequestState.STARTING_FAILED;
                        case STOPPED:
                            return cz.cesnet.shongo.controller.api.ReservationRequestState.FINISHED;
                        case STOPPING_FAILED:
                            return cz.cesnet.shongo.controller.api.ReservationRequestState.STARTED;
                        default:
                            return cz.cesnet.shongo.controller.api.ReservationRequestState.ALLOCATED;
                    }
                }
                return cz.cesnet.shongo.controller.api.ReservationRequestState.ALLOCATED;
            case ALLOCATION_FAILED:
                return cz.cesnet.shongo.controller.api.ReservationRequestState.ALLOCATION_FAILED;
            default:
                throw new TodoImplementException();
        }
    }

    @Transient
    @Override
    public String getReportContextName()
    {
        return "reservation request " + EntityIdentifier.formatId(this);
    }

    @Transient
    @Override
    public String getReportContextDetail()
    {
        return null;
    }

    @Override
    public void validate() throws CommonReportSet.EntityInvalidException
    {
        validateSlotDuration(getSlot().toPeriod());

        super.validate();
    }

    @Override
    public ReservationRequest clone()
    {
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.synchronizeFrom(this);
        return reservationRequest;
    }

    @Override
    public boolean synchronizeFrom(AbstractReservationRequest abstractReservationRequest)
    {
        boolean modified = super.synchronizeFrom(abstractReservationRequest);
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;

            modified |= !ObjectHelper.isSame(getSlotStart(), reservationRequest.getSlotStart())
                    || !ObjectHelper.isSame(getSlotEnd(), reservationRequest.getSlotEnd());

            setSlotStart(reservationRequest.getSlotStart());
            setSlotEnd(reservationRequest.getSlotEnd());
        }
        return modified;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AbstractReservationRequest createApi()
    {
        return new cz.cesnet.shongo.controller.api.ReservationRequest();
    }

    @Override
    public final cz.cesnet.shongo.controller.api.ReservationRequest toApi(Report.MessageType messageType)
    {
        return (cz.cesnet.shongo.controller.api.ReservationRequest) super.toApi(messageType);
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, Report.MessageType messageType)
    {
        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequestApi =
                (cz.cesnet.shongo.controller.api.ReservationRequest) api;
        reservationRequestApi.setSlot(getSlot());
        reservationRequestApi.setState(getStateAsApi());
        reservationRequestApi.setStateReport(getReportText(messageType));
        for (Reservation reservation : getAllocation().getReservations()) {
            reservationRequestApi.addReservationId(EntityIdentifier.formatId(reservation));
        }
        super.toApi(api, messageType);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequestApi =
                (cz.cesnet.shongo.controller.api.ReservationRequest) api;
        if (reservationRequestApi.isPropertyFilled(cz.cesnet.shongo.controller.api.ReservationRequest.SLOT)) {
            setSlot(reservationRequestApi.getSlot());
        }
        super.fromApi(api, entityManager);
    }

    /**
     * Enumeration of {@link ReservationRequest} state.
     */
    public static enum State
    {
        /**
         * Specification is instance of {@link StatefulSpecification} and it's
         * {@link StatefulSpecification#getCurrentState()} is {@link StatefulSpecification.State#NOT_READY}.
         * <p/>
         * A {@link ReservationRequest} in {@link #NOT_COMPLETE} state become {@link #COMPLETE} when
         * the {@link Specification} become {@link StatefulSpecification.State#READY}
         * or {@link StatefulSpecification.State#SKIP}.
         */
        NOT_COMPLETE,

        /**
         * Specification is not instance of {@link StatefulSpecification} or it has
         * {@link StatefulSpecification#getCurrentState()} {@link StatefulSpecification.State#READY} or
         * {@link StatefulSpecification.State#SKIP}.
         * <p/>
         * The {@link ReservationRequest} hasn't been allocated by the {@link Scheduler} yet or
         * the {@link ReservationRequest} has been modified so the {@link Scheduler} must reallocate it.
         * <p/>
         * The {@link Scheduler} processes only {@link #COMPLETE} {@link ReservationRequest}s.
         */
        COMPLETE,

        /**
         * {@link ReservationRequest} has been successfully allocated. If the {@link ReservationRequest} becomes
         * modified, it's state changes back to {@link #COMPLETE}.
         */
        ALLOCATED,

        /**
         * Allocation of the {@link ReservationRequest} failed. The reason can be found from
         * the {@link ReservationRequest#getReports()}
         */
        ALLOCATION_FAILED,
    }
}
