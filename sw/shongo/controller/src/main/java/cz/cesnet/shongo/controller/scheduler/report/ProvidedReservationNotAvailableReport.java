package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class ProvidedReservationNotAvailableReport extends AbstractReservationReport
{
    /**
     * Constructor.
     */
    public ProvidedReservationNotAvailableReport()
    {
    }

    /**
     * Constructor.
     *
     * @param reservation
     */
    public ProvidedReservationNotAvailableReport(Reservation reservation)
    {
        setReservation(reservation);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("%s is not available.", getReservationDescription());
    }
}