package eu.hafixion.realms.utils.database;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Represents a eu.hafixion.realms.utils.database for all serialized objects to be retrieved fromn
 */
public class Database implements Serializable {
    private static final long serialVersionUID = 99731382684596684L;
    private transient boolean serializing = false;
    private transient HashMap<String, UUIDSerializable> serializableObjects = new HashMap<>();

    /**
     * Puts an object to the eu.hafixion.realms.utils.database
     * @param obj the object to put into the databse
     */
    public void putObject(UUIDSerializable obj) {
        if (!serializableObjects.containsKey(obj.getUUID().toString())) {
            serializableObjects.put(obj.getUUID().toString(), obj);
            if (serializing) {
                obj.startSerialization(this);
            }
        }
    }

    /**
     * Puts a list of objects in the eu.hafixion.realms.utils.database
     * @param list the list of objects to put into the eu.hafixion.realms.utils.database
     */
    public void putList(List<UUIDSerializable> list) {
        for (UUIDSerializable obj : list) {
            putObject(obj);
        }
    }

    /**
     * Gets an object from the eu.hafixion.realms.utils.database with some uuid
     * @param uuid the uuid of the object to read from
     * @return the object that has the uuid
     */
    public UUIDSerializable getObject(UUID uuid) {
        return serializableObjects.get(uuid.toString());
    }

    /**
     * Gets a list of objects from a list of uuids
     * @param list the list of object uuids
     * @return the list of objects that have the uuids
     */
    public List<UUIDSerializable> getList(List<UUID> list) {
        List<UUIDSerializable> objs = new ArrayList<>();
        for (UUID uuid : list) {
            objs.add(getObject(uuid));
        }
        return objs;
    }

    private void startSerialization() {
        Collection<UUIDSerializable> objs = ((HashMap<String, UUIDSerializable>)serializableObjects.clone()).values();
        for (UUIDSerializable obj : objs) {
            obj.startSerialization(this);
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        serializing = true;
        startSerialization();
        stream.writeInt(serializableObjects.size());
        for (UUIDSerializable obj : serializableObjects.values()) {
            stream.writeObject(obj);
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        // initialize constructor values
        serializableObjects = new HashMap<>();
        serializing = false;
        // loop through all eu.hafixion.realms.utils.database objects
        int size = stream.readInt();
        for (int i = 0; i < size; i++) {
            UUIDSerializable obj = (UUIDSerializable)stream.readObject();
            putObject(obj);
        }
        for (UUIDSerializable obj : serializableObjects.values()) {
            obj.endDeserialization(this);
        }
    }
}
