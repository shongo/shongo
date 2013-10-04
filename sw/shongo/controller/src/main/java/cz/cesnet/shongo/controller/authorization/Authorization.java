package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.common.UserPerson;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.settings.UserSessionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Provides methods for performing authentication and authorization.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Authorization
{
    private static Logger logger = LoggerFactory.getLogger(Authorization.class);

    /**
     * Root user-id.
     */
    public static final String ROOT_USER_ID = "0";

    /**
     * Root user {@link cz.cesnet.shongo.controller.common.Person}.
     */
    public static final UserInformation ROOT_USER_INFORMATION;

    /**
     * Static initialization.
     */
    static {
        ROOT_USER_INFORMATION = new UserInformation();
        ROOT_USER_INFORMATION.setUserId(ROOT_USER_ID);
        ROOT_USER_INFORMATION.setFirstName("root");
    }

    /**
     * @see EntityManagerFactory
     */
    protected EntityManagerFactory entityManagerFactory;

    /**
     * @see AuthorizationCache
     */
    private AuthorizationCache cache = new AuthorizationCache();

    /**
     * Set of access-tokens which has administrator access.
     */
    protected Set<String> adminAccessTokens = new HashSet<String>();

    /**
     * Set of user-ids who can use the {@link cz.cesnet.shongo.controller.settings.UserSessionSettings#adminMode}.
     */
    protected Set<String> adminModeEnabledUserIds = new HashSet<String>();

    /**
     * {@link cz.cesnet.shongo.controller.settings.UserSessionSettings}s.
     */
    private Map<SecurityToken, UserSessionSettings> userSessionSettings = new HashMap<SecurityToken, UserSessionSettings>();

    /**
     * Constructor.
     *
     * @param config to load authorization configuration from
     */
    protected Authorization(Configuration config)
    {
        cache.setUserIdExpiration(config.getDuration(Configuration.SECURITY_EXPIRATION_USER_ID));
        cache.setUserInformationExpiration(config.getDuration(Configuration.SECURITY_EXPIRATION_USER_INFORMATION));
        cache.setAclExpiration(config.getDuration(Configuration.SECURITY_EXPIRATION_ACL));
    }

    /**
     * Destroy this {@link Authorization} (and you be able to create another {@link Authorization} instance again)
     */
    public void destroy()
    {
        authorization = null;
    }

    /**
     * @param entityManagerFactory sets the {@link #entityManagerFactory}
     */
    public final void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Clear the {@link AuthorizationCache}.
     */
    public void clearCache()
    {
        cache.clear();
    }

    /**
     * Validate given {@code securityToken}.
     *
     * @param securityToken to be validated
     * @return user-id
     */
    public final UserInformation validate(SecurityToken securityToken)
    {
        // Check not empty
        if (securityToken == null || securityToken.getAccessToken() == null) {
            throw new ControllerReportSet.SecurityMissingTokenException();
        }
        return onValidate(securityToken);
    }

    /**
     * Retrieve {@link UserInformation} for given {@code securityToken}.
     *
     * @param securityToken of an user
     * @return {@link UserInformation} for the user with given {@code securityToken}
     */
    public final UserInformation getUserInformation(SecurityToken securityToken)
    {
        UserInformation userInformation = securityToken.getUserInformation();
        if (userInformation != null) {
            return userInformation;
        }
        String accessToken = securityToken.getAccessToken();

        // Try to use the user-id from access token cache to get the user information
        String userId = cache.getUserIdByAccessToken(accessToken);
        if (userId != null) {
            logger.trace("Using cached user-id '{}' for access token '{}'...", userId, accessToken);
            userInformation = getUserInformation(userId);

            // Store the user information inside the security token
            securityToken.setUserInformation(userInformation);

            return userInformation;
        }
        else {
            logger.debug("Retrieving user information by access token '{}'...", accessToken);

            userInformation = onGetUserInformationByAccessToken(accessToken);
            userId = userInformation.getUserId();
            cache.putUserIdByAccessToken(accessToken, userId);
            cache.putUserInformationByUserId(userId, userInformation);

            // Store the user information inside the security token
            securityToken.setUserInformation(userInformation);

            return userInformation;
        }
    }

    /**
     * Retrieve {@link UserInformation} for given {@code userId}.
     *
     * @param userId of an user
     * @return {@link UserInformation} for the user with given {@code userId}
     */
    public final UserInformation getUserInformation(String userId)
    {
        // Root user
        if (userId.equals(ROOT_USER_ID)) {
            return ROOT_USER_INFORMATION;
        }

        // Try to use the user information from the cache
        UserInformation userInformation = cache.getUserInformationByUserId(userId);
        if (userInformation != null) {
            logger.trace("Using cached user information for user-id '{}'...", userId);
            return userInformation;
        }
        else {
            logger.debug("Retrieving user information by user-id '{}'...", userId);

            userInformation = onGetUserInformationByUserId(userId);
            cache.putUserInformationByUserId(userId, userInformation);
            return userInformation;
        }
    }

    /**
     * Checks whether user with given {@code userId} exists.
     *
     * @param userId of the user to be checked for existence
     * @throws ControllerReportSet.UserNotExistException
     *          when the user doesn't exist
     */
    public void checkUserExistence(String userId) throws ControllerReportSet.UserNotExistException
    {
        try {
            UserInformation userInformation = getUserInformation(userId);
            if (userInformation == null) {
                throw new ControllerReportSet.UserNotExistException(userId);
            }
        }
        catch (Exception exception) {
            throw new ControllerReportSet.UserNotExistException(exception, userId);
        }
    }

    /**
     * Retrieve a {@link UserPerson} by given {@code userId}.
     *
     * @param userId of an user
     * @return {@link UserInformation} for the user with given {@code userId}
     */
    public final UserPerson getUserPerson(String userId)
    {
        return new UserPerson(userId, getUserInformation(userId));
    }

    /**
     * Retrieve all {@link UserInformation}s.
     *
     * @return collection of {@link UserInformation}s
     */
    public final Collection<UserInformation> listUserInformation()
    {
        logger.debug("Retrieving list of user information...");

        return onListUserInformation();
    }

    /**
     * @param securityToken
     * @return true if the user is Shongo admin (should have all permissions),
     *         false otherwise
     */
    public final boolean isAdmin(SecurityToken securityToken)
    {
        return adminAccessTokens.contains(securityToken.getAccessToken());
    }

    /**
     * @param securityToken of the user
     * @param entityId      of the entity
     * @param permission    which the user must have for the entity
     * @return true if the user has given {@code permission} for the entity,
     *         false otherwise
     */
    public boolean hasPermission(SecurityToken securityToken, AclRecord.EntityId entityId, Permission permission)
    {
        if (isAdmin(securityToken)) {
            // Administrator has all possible permissions
            return true;
        }
        String userId = securityToken.getUserId();
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        return aclUserState.hasPermission(entityId, permission);
    }

    /**
     * @param securityToken of the user
     * @param entity        the entity
     * @param permission    which the user must have for the entity
     * @return true if the user has given {@code permission} for the entity,
     *         false otherwise
     */
    public boolean hasPermission(SecurityToken securityToken, PersistentObject entity, Permission permission)
    {
        return hasPermission(securityToken, new AclRecord.EntityId(entity), permission);
    }

    /**
     * @param securityToken of the user
     * @param entity        the entity
     * @return set of {@link Permission}s which the user have for the entity
     */
    public Set<Permission> getPermissions(SecurityToken securityToken, PersistentObject entity)
    {
        AclRecord.EntityId entityId = new AclRecord.EntityId(entity);
        if (isAdmin(securityToken)) {
            // Administrator has all possible permissions
            EntityType entityType = entityId.getEntityType().getEntityType();
            return entityType.getPermissions();
        }
        String userId = securityToken.getUserId();
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        Set<Permission> permissions = aclUserState.getPermissions(entityId);
        if (permissions == null) {
            return Collections.emptySet();
        }
        return permissions;
    }

    /**
     * @param securityToken of the user
     * @param entityType    for entities which should be returned
     * @param permission    which the user must have for the entities
     * @return set of entity identifiers for which the user with given {@code userId} has given {@code permission}
     *         or null if the user can view all entities
     */
    public Set<Long> getEntitiesWithPermission(SecurityToken securityToken, AclRecord.EntityType entityType,
            Permission permission)
    {
        if (isAdmin(securityToken)) {
            return null;
        }
        String userId = securityToken.getUserId();
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        Set<Long> entities = aclUserState.getEntitiesByPermission(entityType, permission);
        if (entities == null) {
            return Collections.emptySet();
        }
        return entities;
    }

    /**
     * @param persistentObject for which the users must have given {@code role}
     * @param role             which the users must have for given {@code persistentObject}
     * @return collection of {@link UserInformation} of users which have given {@code role}
     *         for given {@code persistentObject}
     */
    public Collection<UserInformation> getUsersWithRole(PersistentObject persistentObject, Role role)
    {
        AclRecord.EntityId entityId = new AclRecord.EntityId(persistentObject);
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState == null) {
            aclEntityState = fetchAclEntityState(entityId);
            cache.putAclEntityStateByEntityId(entityId, aclEntityState);
        }
        Set<String> userIds = aclEntityState.getUserIdsByRole(role);
        if (userIds == null) {
            return Collections.emptySet();
        }
        List<UserInformation> users = new LinkedList<UserInformation>();
        for (String userId : userIds) {
            users.add(getUserInformation(userId));
        }
        return users;
    }

    /**
     * @param securityToken
     * @return {@link UserSessionSettings} for given {@code securityToken}
     */
    public UserSessionSettings getUserSessionSettings(SecurityToken securityToken)
    {
        UserSessionSettings userSessionSettings = this.userSessionSettings.get(securityToken);
        if (userSessionSettings == null) {
            // Create new user session settings
            userSessionSettings = new UserSessionSettings(securityToken);
            String userId = securityToken.getUserId();
            if (adminModeEnabledUserIds.contains(userId)) {
                userSessionSettings.setAdminMode(Boolean.FALSE);
            }
            // Store it
            this.userSessionSettings.put(securityToken, userSessionSettings);
        }
        return userSessionSettings;
    }

    /**
     * @param userSessionSettings to be updated
     */
    public void updateUserSessionSettings(UserSessionSettings userSessionSettings)
    {
        SecurityToken securityToken = userSessionSettings.getSecurityToken();
        Boolean adminMode = userSessionSettings.getAdminMode();
        if (Boolean.TRUE.equals(adminMode)) {
            adminAccessTokens.add(securityToken.getAccessToken());
        }
        else if (Boolean.FALSE.equals(adminMode)) {
            adminAccessTokens.remove(securityToken.getAccessToken());
        }
    }

    /**
     * Validate given {@code securityToken}.
     *
     * @param securityToken to be validated
     * @return user-id
     * @throws ControllerReportSet.SecurityInvalidTokenException
     *          when the validation fails
     */
    protected UserInformation onValidate(SecurityToken securityToken)
            throws ControllerReportSet.SecurityInvalidTokenException
    {
        // Validate access token by getting user info
        try {
            UserInformation userInformation = getUserInformation(securityToken);
            logger.trace("Access token '{}' is valid for {} (id: {}).",
                    new Object[]{securityToken.getAccessToken(), userInformation.getFullName(),
                            userInformation.getUserId()
                    });
            return userInformation;
        }
        catch (Exception exception) {
            String message = String.format("Access token '%s' cannot be validated.", securityToken.getAccessToken());
            Reporter.reportInternalError(Reporter.AUTHORIZATION, message, exception);
            throw new ControllerReportSet.SecurityInvalidTokenException(securityToken.getAccessToken());
        }
    }

    /**
     * Retrieve {@link UserInformation} for given {@code accessToken}.
     *
     * @param accessToken of an user
     * @return {@link UserInformation} for the user with given {@code accessToken}
     */
    protected abstract UserInformation onGetUserInformationByAccessToken(String accessToken);

    /**
     * Retrieve {@link UserInformation} for given {@code userId}.
     *
     * @param userId of an user
     * @return {@link UserInformation} for the user with given {@code userId}
     */
    protected abstract UserInformation onGetUserInformationByUserId(String userId);

    /**
     * Retrieve all {@link UserInformation}s.
     *
     * @return collection of {@link UserInformation}s
     */
    protected abstract Collection<UserInformation> onListUserInformation();

    /**
     * Fetch {@link AclUserState} for given {@code userId}.
     *
     * @param userId of user for which the ACL should be fetched
     * @return fetched {@link AclUserState} for given {@code userId}
     */
    private AclUserState fetchAclUserState(String userId)
    {
        AclUserState aclUserState = new AclUserState();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            for (AclRecord aclRecord : authorizationManager.listAclRecords(userId)) {
                aclUserState.addAclRecord(aclRecord);
                cache.putAclRecordById(aclRecord);
            }
        }
        finally {
            entityManager.close();
        }
        return aclUserState;
    }

    /**
     * Fetch {@link AclEntityState} for given {@code entityId}.
     *
     * @param entityId of entity for which the ACL should be fetched
     * @return fetched {@link AclEntityState} for given {@code entityId}
     */
    private AclEntityState fetchAclEntityState(AclRecord.EntityId entityId)
    {
        AclEntityState aclEntityState = new AclEntityState();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            for (AclRecord aclRecord : authorizationManager.listAclRecords(entityId)) {
                aclEntityState.addAclRecord(aclRecord);
                cache.putAclRecordById(aclRecord);
            }
        }
        finally {
            entityManager.close();
        }
        return aclEntityState;
    }

    /**
     * Add given {@code aclRecord} to the {@link AuthorizationCache}.
     *
     * @param aclRecord to be added
     */
    void addAclRecordToCache(AclRecord aclRecord)
    {
        String userId = aclRecord.getUserId();
        AclRecord.EntityId entityId = aclRecord.getEntityId();

        // Update AclRecord cache
        cache.putAclRecordById(aclRecord);

        // Update AclUserState cache
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        else {
            aclUserState.addAclRecord(aclRecord);
        }

        // Update AclEntityState cache
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState == null) {
            aclEntityState = fetchAclEntityState(entityId);
            cache.putAclEntityStateByEntityId(entityId, aclEntityState);
        }
        else {
            aclEntityState.addAclRecord(aclRecord);
        }
    }

    /**
     * Remove given {@code aclRecord} to the {@link AuthorizationCache}.
     *
     * @param aclRecord to be deleted
     */
    void removeAclRecordFromCache(AclRecord aclRecord)
    {
        // Update AclRecord cache
        cache.removeAclRecordById(aclRecord);

        // Update AclUserState cache
        String userId = aclRecord.getUserId();
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState != null) {
            aclUserState.removeAclRecord(aclRecord);
        }

        // Update AclEntityState cache
        AclRecord.EntityId entityId = aclRecord.getEntityId();
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState != null) {
            aclEntityState.removeAclRecord(aclRecord);
        }
    }

    /**
     * Single instance of {@link Authorization}.
     */
    protected static Authorization authorization;

    /**
     * @param authorization sets the {@link #authorization}
     * @throws IllegalStateException when the {@link #authorization} is already set
     */
    protected static void setInstance(Authorization authorization)
    {
        if (Authorization.authorization != null) {
            throw new IllegalStateException("Another instance of " + Authorization.class.getSimpleName()
                    + "has been created and wasn't destroyed.");
        }
        Authorization.authorization = authorization;
    }

    /**
     * @return {@link #authorization}
     * @throws IllegalStateException when the no {@link Authorization} has been created
     */
    public static Authorization getInstance() throws IllegalStateException
    {
        if (authorization == null) {
            throw new IllegalStateException("No instance of " + Authorization.class.getSimpleName()
                    + "has been created.");
        }
        return authorization;
    }
}
