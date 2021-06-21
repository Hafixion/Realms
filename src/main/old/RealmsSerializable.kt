package eu.hafixion.realms

/**
 * A core part of storing data in the [RealmsDataSource], it allows easy serialization of data for the main plugin.
 *
 * When saving data to disk or SQL, the RealmsSerializable is serialized to a string (using the serialize function.), and then stored there.
 * When loading data from disk or SQL, a string is provided and saved into the eu.hafixion.realms.utils.database as a [StringSerializable], then other implementations will be free to use it to deserialize it for themselves.
 *
 * @author Hafixion
 * @since 4/25/2021
 *
 * @example [StringSerializable] (Although it has some other uses as well...)
 */
interface RealmsSerializable {

    /**
     * Method used to serialize, should (serializedData):(deserializerClassName)
     *
     * @return Serialized String, with name of Deserializer embedded.
     */
    fun serialize(): String

    override fun equals(other: Any?): Boolean
}

/**
 * Important class for data management. By default, all data types in [RealmsDataSource] will be set to this class.
 * It is them up to other implementations to deserialize it from the string parameter and set them to be their deserialized class versions.
 *
 * @param s The string stored in the class
 *
 * @author Hafixion
 * @since 4/26/2021
 */
class StringSerializable(val s: String) : RealmsSerializable {

    override fun serialize(): String {
        return s
    }

    override fun equals(other: Any?): Boolean {
        return if (other is StringSerializable) other.s == s
    }

    override fun hashCode() = s.hashCode()

}
