package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.EntityRole;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link cz.cesnet.shongo.controller.api.request.ListRequest} for {@link cz.cesnet.shongo.controller.api.AclRecord}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclRecordListRequest extends ListRequest
{
    /**
     * Identifiers of the ACL records.
     */
    private Set<String> aclRecordIds = new HashSet<String>();

    /**
     * Identifiers of the Shongo user.
     */
    private Set<String> userIds = new HashSet<String>();

    /**
     * Identifier of the Shongo public entity.
     */
    private Set<String> entityIds = new HashSet<String>();

    /**
     * Role which the user has for the entity.
     */
    private Set<EntityRole> entityRoles = new HashSet<EntityRole>();

    /**
     * Constructor.
     */
    public AclRecordListRequest()
    {
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public AclRecordListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param userId to be added to the {@link #userIds}
     */
    public AclRecordListRequest(SecurityToken securityToken, String userId)
    {
        super(securityToken);
        addUserId(userId);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param userId to be added to the {@link #userIds}
     */
    public AclRecordListRequest(SecurityToken securityToken, String userId, String entityId, EntityRole entityRole)
    {
        super(securityToken);
        addUserId(userId);
        addEntityId(entityId);
        addEntityRole(entityRole);
    }

    public Set<String> getAclRecordIds()
    {
        return aclRecordIds;
    }

    public void setAclRecordIds(Set<String> aclRecordIds)
    {
        this.aclRecordIds = aclRecordIds;
    }

    public void addAclRecordId(String aclRecordId)
    {
        aclRecordIds.add(aclRecordId);
    }

    public Set<String> getUserIds()
    {
        return userIds;
    }

    public void setUserIds(Set<String> userIds)
    {
        this.userIds = userIds;
    }

    public void addUserId(String userId)
    {
        userIds.add(userId);
    }

    public Set<String> getEntityIds()
    {
        return entityIds;
    }

    public void setEntityIds(Set<String> entityIds)
    {
        this.entityIds = entityIds;
    }

    public void addEntityId(String entityId)
    {
        entityIds.add(entityId);
    }

    public Set<EntityRole> getEntityRoles()
    {
        return entityRoles;
    }

    public void setEntityRoles(Set<EntityRole> entityRoles)
    {
        this.entityRoles = entityRoles;
    }

    public void addEntityRole(EntityRole entityRole)
    {
        entityRoles.add(entityRole);
    }

    private static final String ACL_RECORD_IDS = "aclRecordIds";
    private static final String USER_IDS = "userIds";
    private static final String ENTITY_IDS = "entityIds";
    private static final String ENTITY_ROLES = "entityRoles";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ACL_RECORD_IDS, aclRecordIds);
        dataMap.set(USER_IDS, userIds);
        dataMap.set(ENTITY_IDS, entityIds);
        dataMap.set(ENTITY_ROLES, entityRoles);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        aclRecordIds = dataMap.getSet(ACL_RECORD_IDS, String.class);
        userIds = dataMap.getSet(USER_IDS, String.class);
        entityIds = dataMap.getSet(ENTITY_IDS, String.class);
        entityRoles = dataMap.getSet(ENTITY_ROLES, EntityRole.class);
    }
}
