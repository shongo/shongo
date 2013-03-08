package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;

import java.util.Collection;
import java.util.Map;

/**
 * Interface to the service handling operations on reservations.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface ReservationService extends Service
{
    /**
     * @param token         token of the user requesting the operation
     * @param specification to checked
     * @param slot          in which the given {@code specification} should be checked
     * @return {@link Boolean#TRUE} when given {@code specification} can be allocated for given date/time {@code slot},
     *         otherwise {@link String} report describing the reason why the specification is not available
     */
    @API
    public Object checkSpecificationAvailability(SecurityToken token, Specification specification, Interval slot)
            throws FaultException;

    /**
     * Creates a new reservation request.
     * <p/>
     * The user with the given {@code token} will be the request owner.
     *
     * @param token              token of the user requesting the operation
     * @param reservationRequest reservation request; should contains all attributes marked as {@link Required}
     * @return the created reservation request shongo-id
     */
    @API
    public String createReservationRequest(SecurityToken token, AbstractReservationRequest reservationRequest)
            throws FaultException;

    /**
     * Modifies a given reservation.
     *
     * @param token              token of the user requesting the operation
     * @param reservationRequest reservation request with attributes to be modified
     */
    @API
    public void modifyReservationRequest(SecurityToken token, AbstractReservationRequest reservationRequest)
            throws FaultException;

    /**
     * Deletes a given reservation.
     *
     * @param token                token of the user requesting the operation
     * @param reservationRequestId shongo-id of the reservation to modify
     */
    @API
    public void deleteReservationRequest(SecurityToken token, String reservationRequestId)
            throws FaultException;

    /**
     * Lists all the reservation requests.
     *
     * @param token  token of the user requesting the operation
     * @param filter attributes for filtering reservation requests (map of name => value pairs)::
     *               -{@code userId}                restricts reservation request owner by his user-id
     *               -{@code technology}            technology or set of technologies of virtual room, compartment or alias
     *               -{@code specificationClass}    specification class or set of specification classes which the requests can specify
     *               -{@code providedReservationId} restricts reservation requests to which must be provided reservation with given identifier(s)
     * @return collection of reservation requests
     */
    @API
    public Collection<ReservationRequestSummary> listReservationRequests(SecurityToken token,
            Map<String, Object> filter) throws FaultException;

    /**
     * Gets the complete Reservation object.
     *
     * @param token                token of the user requesting the operation
     * @param reservationRequestId shongo-id of the reservation request to get
     */
    @API
    public AbstractReservationRequest getReservationRequest(SecurityToken token, String reservationRequestId)
            throws FaultException;

    /**
     * @param token
     * @param reservationId
     * @return reservation with given shongo-id
     * @throws FaultException
     */
    @API
    public Reservation getReservation(SecurityToken token, String reservationId) throws FaultException;

    /**
     * @param token
     * @param reservationIds
     * @return collection of {@link Reservation}s with given identifiers
     * @throws FaultException
     */
    @API
    public Collection<Reservation> getReservations(SecurityToken token, Collection<String> reservationIds)
            throws FaultException;

    /**
     * @param token
     * @param filter attributes for filtering reservations (map of name => value pairs)::
     *               -{@code userId}                restricts reservation owner by his user-id
     *               -{@code reservationRequestId}  restricts reservation request for which the {@link Reservation} is allocated
     *               -{@code reservationClass}      set of allowed reservation classes
     *               -{@code technology}            set of technologies of virtual room or compartment
     * @return collection of already allocated {@link Reservation}s
     * @throws FaultException
     */
    @API
    public Collection<Reservation> listReservations(SecurityToken token, Map<String, Object> filter)
            throws FaultException;
}