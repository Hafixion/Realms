package eu.hafixion.realms.utils.database;

public enum SerializationType {
    /**
     * Serializes the value directly into the stream as bytes
     */
    VALUE,
    /**
     * Serializes the value as a uuid pointer to the object in the eu.hafixion.realms.utils.database
     */
    OBJECT,
    /**
     * Serializes the value as a list of uuid pointers to objects
     */
    LIST
}
