package eu.hafixion.realms.utils;

import java.io.*;
import java.util.Base64;

public class Serializer {

    public static String serialize(Object obj) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream dataOutput = new ObjectOutputStream(outputStream);



        dataOutput.writeObject(obj);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static Object deserialize(String string) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(string));
        ObjectInputStream dataInput = new ObjectInputStream(inputStream);

        try {
            return dataInput.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
