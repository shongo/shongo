package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.FaultException;
import jade.content.Concept;

import java.io.IOException;
import java.util.*;

/**
 * Tracks changes for some object.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ChangesTracking implements Concept
{
    /**
     * Keys that are used in map for collection changes.
     */
    public static final String COLLECTION_NEW = "new";
    public static final String COLLECTION_MODIFIED = "modified";
    public static final String COLLECTION_DELETED = "deleted";

    /**
     * Key whose value contains the whole {@link java.util.Map} data.
     */
    public static final String MAP_DATA = "__map";

    /**
     * Set of properties which are marked as filled.
     */
    private Set<String> filledProperties = new HashSet<String>();

    /**
     * Specifies whether all collection items are by default new (when new entity is being created this should be true).
     */
    private boolean collectionItemIsByDefaultNew = false;

    /**
     * Stores state of collection property.
     */
    public static class CollectionChanges
    {
        /**
         * Set of collection items marked as new
         */
        public Set<Object> newItems = new HashSet<Object>();

        /**
         * Set of collection items marked as deleted
         */
        public Set<Object> deletedItems = new HashSet<Object>();

        /**
         * @return true if all changes are empty, false otherwise
         */
        public boolean isEmpty()
        {
            return newItems.size() == 0 && deletedItems.size() == 0;
        }
    }

    /**
     * Map of changes for collection properties.
     */
    private Map<String, CollectionChanges> collectionChangesMap = new HashMap<String, CollectionChanges>();

    /**
     * @param property
     * @return true if given field was marked as filled,
     *         false otherwise
     */
    public boolean isPropertyFilled(String property)
    {
        return filledProperties.contains(property);
    }

    /**
     * @return changes for collections
     */
    public Map<String, CollectionChanges> getCollectionChanges()
    {
        return collectionChangesMap;
    }

    /**
     * @param propertyName
     * @return collection changes for given property
     */
    public CollectionChanges getCollectionChanges(String propertyName)
    {
        return collectionChangesMap.get(propertyName);
    }

    /**
     * Mark given property as filled.
     *
     * @param property
     */
    public void markPropertyAsFilled(String property)
    {
        filledProperties.add(property);
    }

    /**
     * Mark item in collection as new.
     *
     * @param property
     * @param item
     */
    public void markPropertyItemAsNew(String property, Object item)
    {
        CollectionChanges collectionChanges = collectionChangesMap.get(property);
        if (collectionChanges == null) {
            collectionChanges = new CollectionChanges();
            collectionChangesMap.put(property, collectionChanges);
        }
        collectionChanges.newItems.add(item);
    }

    /**
     * Mark item in collection as removed.
     *
     * @param property
     * @param item
     */
    public void markPropertyItemAsDeleted(String property, Object item)
    {
        CollectionChanges collectionChanges = collectionChangesMap.get(property);
        if (collectionChanges == null) {
            collectionChanges = new CollectionChanges();
            collectionChangesMap.put(property, collectionChanges);
        }
        if (collectionChanges.newItems.contains(item)) {
            collectionChanges.newItems.remove(item);
        }
        else {
            collectionChanges.deletedItems.add(item);
        }
    }

    /**
     * @param property
     * @param item
     * @return true if collection item is marked as new or when all not marked items are by default new,
     *         false otherwise
     */
    public boolean isPropertyItemMarkedAsNew(String property, Object item)
    {
        CollectionChanges collectionChanges = collectionChangesMap.get(property);
        if (collectionChanges != null) {
            return collectionChanges.newItems.contains(item);
        }
        return collectionItemIsByDefaultNew;
    }

    /**
     * @param property
     * @return set of items from given collection which are marked as deleted
     */
    public <T> Set<T> getPropertyItemsMarkedAsNew(String property)
    {
        CollectionChanges collectionChanges = collectionChangesMap.get(property);
        if (collectionChanges != null) {
            @SuppressWarnings("unchecked")
            Set<T> newItems = (Set) collectionChanges.newItems;
            return newItems;
        }
        else {
            return new HashSet<T>();
        }
    }

    /**
     * @param property
     * @return set of items from given collection which are marked as deleted
     */
    public <T> Set<T> getPropertyItemsMarkedAsDeleted(String property)
    {
        CollectionChanges collectionChanges = collectionChangesMap.get(property);
        if (collectionChanges != null) {
            @SuppressWarnings("unchecked")
            Set<T> deletedItems = (Set) collectionChanges.deletedItems;
            return deletedItems;
        }
        else {
            return new HashSet<T>();
        }
    }

    /**
     * Clear all filled/collection marks
     */
    public void clearMarks()
    {
        filledProperties.clear();
        collectionChangesMap.clear();
    }

    /**
     * Check {@link cz.cesnet.shongo.api.annotation.Required} in all properties of {@link cz.cesnet.shongo.api.util.ChangesTracking} or in all items of
     * arrays and collections (recursive).
     *
     * @param object
     * @throws cz.cesnet.shongo.fault.FaultException
     *
     */
    public static void setupNewEntity(Object object) throws FaultException
    {
        if (object instanceof Changeable) {
            ChangesTracking changesTrackingObject = ((Changeable) object).getChangesTracking();
            changesTrackingObject.collectionItemIsByDefaultNew = true;
            Class type = object.getClass();
            String[] propertyNames = Property.getPropertyNames(type);
            for (String propertyName : propertyNames) {
                Property property = Property.getProperty(object.getClass(), propertyName);
                int propertyTypeFlags = property.getTypeFlags();
                Object value = property.getValue(object);
                boolean required = property.isRequired();
                if (value instanceof Changeable) {
                    setupNewEntity(value);
                }
                else if (TypeFlags.isArray(propertyTypeFlags)) {
                    Object[] array = (Object[]) value;
                    if (required && array.length == 0) {
                        throw new FaultException(CommonFault.CLASS_ATTRIBUTE_COLLECTION_IS_REQUIRED, propertyName,
                                type);
                    }
                    for (Object item : array) {
                        setupNewEntity(item);
                    }
                }
                else if (TypeFlags.isCollection(propertyTypeFlags)) {
                    Collection collection = (Collection) value;
                    if (required && collection.isEmpty()) {
                        throw new FaultException(CommonFault.CLASS_ATTRIBUTE_COLLECTION_IS_REQUIRED, propertyName,
                                type);
                    }
                    for (Object item : collection) {
                        setupNewEntity(item);
                    }
                }
                else if (required && value == null) {
                    throw new FaultException(CommonFault.CLASS_ATTRIBUTE_IS_REQUIRED, propertyName,
                            type);
                }
            }
        }
        else if (object instanceof Object[]) {
            Object[] array = (Object[]) object;
            for (Object item : array) {
                setupNewEntity(item);
            }
        }
        else if (object instanceof Collection) {
            Collection collection = (Collection) object;
            for (Object item : collection) {
                setupNewEntity(item);
            }
        }
    }

    /**
     * @param changesTrackingObject to be filled from
     */
    public void fill(ChangesTracking changesTrackingObject)
    {
        for (String property : changesTrackingObject.filledProperties) {
            filledProperties.add(property);
        }
        for (String collection : changesTrackingObject.collectionChangesMap.keySet()) {
            CollectionChanges collectionChanges = changesTrackingObject.collectionChangesMap.get(collection);
            for (Object object : collectionChanges.newItems) {
                markPropertyItemAsNew(collection, object);
            }
            for (Object object : collectionChanges.deletedItems) {
                markPropertyItemAsDeleted(collection, object);
            }
        }
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException
    {
        Map<String, Object> replaceChanges = new HashMap<String, Object>();
        Map<String, ChangesTracking.CollectionChanges> changes =
                getCollectionChanges();
        for (String collection : changes.keySet()) {
            ChangesTracking.CollectionChanges sourceCollectionChanges = changes.get(collection);
            Map<String, Collection<Object>> replaceCollectionChanges = new HashMap<String, Collection<Object>>();
            replaceCollectionChanges.put("new", sourceCollectionChanges.newItems);
            replaceCollectionChanges.put("deleted", sourceCollectionChanges.deletedItems);
            replaceChanges.put(collection, replaceCollectionChanges);
        }
        out.writeObject(replaceChanges);
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException
    {
        filledProperties = new HashSet<String>();
        collectionChangesMap = new HashMap<String, CollectionChanges>();

        Map<String, Object> changes = (Map<String, Object>) in.readObject();
        for (String collection : changes.keySet()) {
            Map<String, Collection<Object>> collectionChanges =
                    (Map<String, Collection<Object>>) changes.get(collection);
            if (collectionChanges.containsKey("new")) {
                for (Object object : collectionChanges.get("new")) {
                    markPropertyItemAsNew(collection, object);
                }
            }
            if (collectionChanges.containsKey("deleted")) {
                for (Object object : collectionChanges.get("deleted")) {
                    markPropertyItemAsDeleted(collection, object);
                }
            }
        }
    }

    /**
     * Interface which should be implemented by objects which provides {@link ChangesTracking}.
     */
    public static interface Changeable
    {
        /**
         * @return {@link ChangesTracking} for this object
         */
        public ChangesTracking getChangesTracking();
    }
}