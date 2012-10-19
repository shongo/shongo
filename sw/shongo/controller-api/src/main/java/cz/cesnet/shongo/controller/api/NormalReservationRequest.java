package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;

import java.util.List;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class NormalReservationRequest extends AbstractReservationRequest
{
    /**
     * @see cz.cesnet.shongo.controller.ReservationRequestPurpose
     */
    public static final String PURPOSE = "purpose";

    /**
     * Specifies whether the scheduler should try allocate resources from other domains.
     */
    public static final String INTER_DOMAIN = "interDomain";

    /**
     * Collection of identifiers for {@link cz.cesnet.shongo.controller.api.Reservation}s which are provided to the {@link cz.cesnet.shongo.controller.api.NormalReservationRequest}.
     */
    public static final String PROVIDED_RESERVATION_IDENTIFIERS = "providedReservationIdentifiers";

    /**
     * Constructor.
     */
    public NormalReservationRequest()
    {
    }

    /**
     * @return {@link #PURPOSE}
     */
    @Required
    public ReservationRequestPurpose getPurpose()
    {
        return getPropertyStorage().getValue(PURPOSE);
    }

    /**
     * @param purpose sets the {@link #PURPOSE}
     */
    public void setPurpose(ReservationRequestPurpose purpose)
    {
        getPropertyStorage().setValue(PURPOSE, purpose);
    }

    /**
     * @return {@link #INTER_DOMAIN}
     */
    public Boolean getInterDomain()
    {
        return getPropertyStorage().getValue(INTER_DOMAIN);
    }

    /**
     * @param interDomain sets the {@link #INTER_DOMAIN}
     */
    public void setInterDomain(Boolean interDomain)
    {
        getPropertyStorage().setValue(INTER_DOMAIN, interDomain);
    }

    /**
     * @return {@link #PROVIDED_RESERVATION_IDENTIFIERS}
     */
    public List<String> getProvidedReservationIdentifiers()
    {
        return getPropertyStorage().getCollection(PROVIDED_RESERVATION_IDENTIFIERS, List.class);
    }

    /**
     * @param providedReservationIdentifiers sets the {@link #PROVIDED_RESERVATION_IDENTIFIERS}
     */
    public void setProvidedReservationIdentifiers(List<String> providedReservationIdentifiers)
    {
        getPropertyStorage().setCollection(PROVIDED_RESERVATION_IDENTIFIERS, providedReservationIdentifiers);
    }

    /**
     * @param providedReservationIdentifier to be added to the {@link #PROVIDED_RESERVATION_IDENTIFIERS}
     */
    public void addProvidedReservationIdentifier(String providedReservationIdentifier)
    {
        getPropertyStorage().addCollectionItem(PROVIDED_RESERVATION_IDENTIFIERS, providedReservationIdentifier,
                List.class);
    }

    /**
     * @param providedReservationIdentifier to be removed from the {@link #PROVIDED_RESERVATION_IDENTIFIERS}
     */
    public void removeProvidedReservationIdentifier(String providedReservationIdentifier)
    {
        getPropertyStorage().removeCollectionItem(PROVIDED_RESERVATION_IDENTIFIERS, providedReservationIdentifier);
    }
}