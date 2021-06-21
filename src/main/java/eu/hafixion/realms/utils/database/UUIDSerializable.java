package eu.hafixion.realms.utils.database;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class UUIDSerializable implements Serializable {
    private static final long serialVersionUID = 4960485264638688379L;
    private transient UUID uuid = UUID.randomUUID();
    private transient List<SerializableElement> elements;
    private transient Database database;

    private transient int serializedCount;
    private transient List<Object> serializedObjects;

    private transient int deserializedCount;
    private transient List<Object> deserializedObjects;

    private void writeObject(ObjectOutputStream stream) throws ClassNotFoundException, IOException {
        // i think this is more efficient than using writeObject(), whatever it doesnt really matter since this code can be changed very easily anyway
        stream.writeUTF(uuid.toString());
        for (int i = 0; i < elements.size(); i++) {
            SerializableElement elem = elements.get(i);
            Object val = serializedObjects.get(i);
            switch (elem.serializationType) {
                case VALUE:
                    if (elem.classType == Integer.class) {
                        stream.writeInt((int)val);
                    } else if (elem.classType == String.class) {
                        stream.writeUTF((String)val);
                    } else if (elem.classType == Boolean.class) {
                        stream.writeBoolean((boolean)val);
                    } else {
                        System.out.println(elem.classType);
                        System.out.println("Type is not supported for serialization");
                    }
                    break;
                case OBJECT:
                    stream.writeUTF((String)val);
                    break;
                case LIST:
                    stream.writeObject(val);
                    break;
            }
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        elements = new ArrayList<>();
        initSerializableValues();
        //
        uuid = UUID.fromString(stream.readUTF());
        //
        deserializedCount = 0;
        deserializedObjects = new ArrayList<>();
        for (SerializableElement elem : elements) {
            Object val = 0; // initializing so sonarlint doesn't freak out
            switch (elem.serializationType) {
                case VALUE:
                    if (elem.classType == Integer.class) {
                        val = stream.readInt();
                    } else if (elem.classType == String.class) {
                        val = stream.readUTF();
                    } else if (elem.classType == Boolean.class) {
                        val = stream.readBoolean();
                    } else {
                        System.out.println(elem.classType);
                        System.out.println("Type is not supported for deserialization");
                        return;
                    }
                    break;
                case OBJECT:
                    val = stream.readUTF();
                    break;
                case LIST:
                    val = stream.readObject();
                    break;
            }
            deserializedObjects.add(val);
        }
    }

    /**
     * Starts the serialization of the object
     * @param database the eu.hafixion.realms.utils.database to save object links to
     */
    public void startSerialization(Database database) {
        this.database = database;
        elements = new ArrayList<>();
        initSerializableValues();
        //
        serializedCount = 0;
        serializedObjects = new ArrayList<>();
        serializeObject();
    }

    /**
     * Ends the deserialization of the object
     * @param database the eu.hafixion.realms.utils.database to get linked objects from
     */
    public void endDeserialization(Database database) {
        this.database = database;
        deserializeObject();
    }

    /**
     * Gets the uuid of this serializable object
     * @return the uuid of this object
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Adds a serializable value to the class serialization table
     * @param <T> the type to serialize
     * @param type the class type to serialize
     */
    protected <T> void addSerializableValue(Class<T> type) {
        SerializableElement elem = new SerializableElement();
        elem.serializationType = SerializationType.VALUE;
        elem.classType = type;
        elements.add(elem);
    }

    /**
     * Adds a serializable object link to the class serialization table
     * @param <T> the type to serialize
     * @param type the class type to serialize
     */
    protected <T> void addSerializableObject(Class<T> type) {
        SerializableElement elem = new SerializableElement();
        elem.serializationType = SerializationType.OBJECT;
        elem.classType = type;
        elements.add(elem);
    }

    /**
     * Adds a serializable list of object links to the class serialization table
     * @param <T> the type to serialize
     * @param type the class type to serialize
     */
    protected <T> void addSerializableList(Class<T> type) {
        SerializableElement elem = new SerializableElement();
        elem.serializationType = SerializationType.LIST;
        elem.classType = type;
        elements.add(elem);
    }

    /**
     * Serializes a value
     * @param <T> the type to serialize
     * @param val the value to serialize
     */
    protected <T> void serialize(T val) {
        Class<?> type = val.getClass();
        SerializableElement elem = elements.get(serializedCount);
        serializedCount++;
        if (elem.classType != type && elem.serializationType != SerializationType.LIST) {
            System.out.println(elem.classType);
            System.out.println(type);
            System.out.println("Element type does not match given type");
            return;
        }
        switch (elem.serializationType) {
            case VALUE:
                serializedObjects.add(val);
                break;
            case OBJECT:
                UUIDSerializable obj = (UUIDSerializable)val;
                database.putObject(obj);
                serializedObjects.add(obj.getUUID().toString());
                break;
            case LIST:
                List<UUIDSerializable> objList = (List<UUIDSerializable>)val;
                List<String> uuidList = new ArrayList<>();
                for (UUIDSerializable obj_ : objList) {
                    database.putObject(obj_);
                    uuidList.add(obj_.getUUID().toString());
                }
                serializedObjects.add(uuidList);
                break;
        }
    }

    /**
     * Deserializes a value
     * @param <T> the type to deserialize
     * @return the deserialized value
     */
    protected <T> T deserialize() {
        Object val = deserializedObjects.get(deserializedCount);
        SerializableElement elem = elements.get(deserializedCount);
        deserializedCount++;
        switch (elem.serializationType) {
            case VALUE:
                return (T)val;
            case OBJECT:
                UUID objUUID = UUID.fromString((String)val);
                return (T)database.getObject(objUUID);
            case LIST:
                List<String> list = (List<String>)val;
                List<Object> objList = new ArrayList<>();
                for (String uuidString : list) {
                    UUID objListUUID = UUID.fromString(uuidString);
                    objList.add(database.getObject(objListUUID));
                }
                return (T)objList;
        }
        return null;
    }

    /**
     * Abstract method called when initializing the serializable values
     */
    protected abstract void initSerializableValues();

    /**
     * Abstract method called when serializing the object
     */
    protected abstract void serializeObject();

    /**
     * Abstract method called when deserializing the object
     */
    protected abstract void deserializeObject();
}
