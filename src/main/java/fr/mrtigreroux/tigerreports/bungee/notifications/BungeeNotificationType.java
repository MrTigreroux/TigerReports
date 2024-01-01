package fr.mrtigreroux.tigerreports.bungee.notifications;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * @author MrTigreroux
 */
public class BungeeNotificationType {

    private static final Field creationTimeField;
    private static final BungeeNotificationType[] notificationTypeById;
    private static final Map<Class<? extends BungeeNotification>, BungeeNotificationType> notificationTypeByDataClass = new HashMap<>(); // BungeeNotificationType specifically depends on the data class instance and not only on the data class name

    static {
        try {
            creationTimeField = Objects.requireNonNull(BungeeNotification.class.getDeclaredField("creationTime"));
            creationTimeField.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException | NullPointerException ex) {
            throw new IllegalStateException("The creationTime field cannot be found", ex);
        }

        List<BungeeNotificationType> notifTypeById = new ArrayList<>();
        addBungeeNotificationType(NewReportBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(StatusBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(CommentBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(ProcessBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(ProcessPunishBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(ProcessAbusiveBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(ArchiveBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(UnarchiveBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(DeleteBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(CooldownBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(StopCooldownBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(ImmunityBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(PlayerOnlineBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(PlayerLastMessagesBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(UsersDataChangedBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(TeleportToLocationBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(TeleportToPlayerBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(StatisticBungeeNotification.class, notifTypeById);
        addBungeeNotificationType(PunishBungeeNotification.class, notifTypeById);

        notificationTypeById = notifTypeById.toArray(new BungeeNotificationType[notifTypeById.size()]);
    }

    private static void addBungeeNotificationType(Class<? extends BungeeNotification> dataClass,
            List<BungeeNotificationType> notifTypeById) {
        byte id = (byte) notifTypeById.size();
        BungeeNotificationType type = new BungeeNotificationType(id, dataClass);
        notifTypeById.add(type);

        if (notificationTypeByDataClass.containsKey(dataClass)) {
            throw new IllegalStateException(
                    "The notification data class " + dataClass + " has already been registered");
        }
        notificationTypeByDataClass.put(dataClass, type);
    }

    public static BungeeNotificationType[] getNotificationTypeByIdArray() {
        return notificationTypeById;
    }

    public static BungeeNotificationType getByDataClass(
            Class<? extends BungeeNotification> bungeeNotificationDataClass) {
        return notificationTypeByDataClass.get(bungeeNotificationDataClass);
    }

    public static BungeeNotificationType getById(byte id) throws IllegalArgumentException {
        if (id < 0 || id >= getNotificationTypeByIdArray().length) {
            throw new IllegalArgumentException("Invalid id: " + id);
        }
        return getNotificationTypeByIdArray()[id];
    }

    private final byte id;
    private final Class<? extends BungeeNotification> dataClass;
    private final NotificationDataField[] dataFields;
    private final Constructor<?> dataClassConstructor;
    private final Object[] dataClassConstructorParametersDefaultValue;

    public BungeeNotificationType(byte id, Class<? extends BungeeNotification> dataClass) {
        this.id = id;
        this.dataClass = Objects.requireNonNull(dataClass);

        Field[] fields = dataClass.getDeclaredFields();
        List<Field> rawDataFields = new ArrayList<>();
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isFinal(modifiers) && !Modifier.isStatic(modifiers)) {
                rawDataFields.add(field);
            }
        }
        rawDataFields.sort(Comparator.comparing(Field::getName));

        dataFields = new NotificationDataField[rawDataFields.size()];
        int i = 0;
        Set<Class<?>> constructorSimpleFieldsType = Sets.newHashSet(creationTimeField.getType());
        for (Field field : rawDataFields) {
            Class<?> fieldType = field.getType();
            if (fieldType == null) {
                throw new IllegalStateException("The notification data field " + field.getName() + " of "
                        + dataClass.getSimpleName() + " has an unknown (null) type");
            }
            try {
                dataFields[i] = new NotificationDataField(field, FieldSerializer.getByClass(fieldType));
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("The notification data field " + field.getName() + " of "
                        + dataClass.getSimpleName() + " has an invalid type: " + fieldType.getSimpleName(), ex);
            }

            constructorSimpleFieldsType.add(fieldType);
            i++;
        }

        try {
            dataClassConstructor = Arrays.stream(dataClass.getDeclaredConstructors())
                    .filter((constructor) -> constructorSimpleFieldsType
                            .equals(Sets.newHashSet(constructor.getParameterTypes())))
                    .findFirst()
                    .get();
        } catch (SecurityException | NullPointerException | NoSuchElementException ex) {
            throw new IllegalStateException(
                    "The constructor of the " + dataClass.getSimpleName() + " class cannot be found", ex);
        }
        if (dataClassConstructor.getParameterCount() != dataFields.length + 1) {
            throw new IllegalStateException("The " + dataClass.getSimpleName() + " class has " + dataFields.length
                    + " data fields but its constructor has " + dataClassConstructor.getParameterCount()
                    + " parameters (1 for creationTime)");
        }

        List<Object> defParams = new ArrayList<>();
        for (Class<?> pType : dataClassConstructor.getParameterTypes()) {
            defParams.add(FieldSerializer.getByClass(pType).getUndefinedValue());
        }
        dataClassConstructorParametersDefaultValue = defParams.toArray();
    }

    public static class NotificationDataField {

        public final Field field;
        public final FieldSerializer fieldSerializer;

        private NotificationDataField(Field field, FieldSerializer fieldSerializer) {
            this.field = field;
            this.field.setAccessible(true);
            this.fieldSerializer = fieldSerializer;
        }

        void write(BungeeNotification notif, DataOutputStream outputStream) throws IllegalAccessException, IOException {
            fieldSerializer.write(field, notif, outputStream);
        }

        Object read(DataInputStream inputStream) throws EOFException, IOException {
            return fieldSerializer.read(inputStream);
        }

        public void setValue(BungeeNotification notif, Object value) {
            try {
                field.set(notif, value);
            } catch (IllegalAccessException ex) {
                throw new IllegalArgumentException(ex);
            }
        }

    }

    public static abstract class FieldSerializer {

        public static final String NULL_VALUE = "@null#";
        public static final Map<String, FieldSerializer> fieldSerializerByClassName = new HashMap<>();

        public static FieldSerializer getByClass(Class<?> clazz) {
            return getByClassName(clazz.getName());
        }

        public static FieldSerializer getByClassName(String className) {
            FieldSerializer fieldSerializer = fieldSerializerByClassName.get(className);

            if (fieldSerializer == null) {
                if (Boolean.TYPE.getName().equals(className)) {
                    fieldSerializer = new FieldSerializer() {

                        @Override
                        void write(Field field, BungeeNotification notif, DataOutputStream outputStream)
                                throws IOException, IllegalAccessException {
                            outputStream.writeBoolean(field.getBoolean(notif));
                        }

                        @Override
                        Object read(DataInputStream inputStream) throws EOFException, IOException {
                            return inputStream.readBoolean();
                        }

                        @Override
                        Object getUndefinedValue() {
                            return false;
                        }

                    };
                } else if (Integer.TYPE.getName().equals(className)) {
                    fieldSerializer = new FieldSerializer() {

                        @Override
                        void write(Field field, BungeeNotification notif, DataOutputStream outputStream)
                                throws IOException, IllegalAccessException {
                            outputStream.writeInt(field.getInt(notif));
                        }

                        @Override
                        Object read(DataInputStream inputStream) throws EOFException, IOException {
                            return inputStream.readInt();
                        }

                        @Override
                        Object getUndefinedValue() {
                            return 2;
                        }

                    };
                } else if (Long.TYPE.getName().equals(className)) {
                    fieldSerializer = new FieldSerializer() {

                        @Override
                        void write(Field field, BungeeNotification notif, DataOutputStream outputStream)
                                throws IOException, IllegalAccessException {
                            outputStream.writeLong(field.getLong(notif));
                        }

                        @Override
                        Object read(DataInputStream inputStream) throws EOFException, IOException {
                            return inputStream.readLong();
                        }

                        @Override
                        Object getUndefinedValue() {
                            return 2L;
                        }

                    };
                } else if (Double.TYPE.getName().equals(className)) {
                    fieldSerializer = new FieldSerializer() {

                        @Override
                        void write(Field field, BungeeNotification notif, DataOutputStream outputStream)
                                throws IOException, IllegalAccessException {
                            outputStream.writeDouble(field.getDouble(notif));
                        }

                        @Override
                        Object read(DataInputStream inputStream) throws EOFException, IOException {
                            return inputStream.readDouble();
                        }

                        @Override
                        Object getUndefinedValue() {
                            return 2d;
                        }

                    };
                } else if (Float.TYPE.getName().equals(className)) {
                    fieldSerializer = new FieldSerializer() {

                        @Override
                        void write(Field field, BungeeNotification notif, DataOutputStream outputStream)
                                throws IOException, IllegalAccessException {
                            outputStream.writeFloat(field.getFloat(notif));
                        }

                        @Override
                        Object read(DataInputStream inputStream) throws EOFException, IOException {
                            return inputStream.readFloat();
                        }

                        @Override
                        Object getUndefinedValue() {
                            return 2f;
                        }

                    };
                } else if (Short.TYPE.getName().equals(className)) {
                    fieldSerializer = new FieldSerializer() {

                        @Override
                        void write(Field field, BungeeNotification notif, DataOutputStream outputStream)
                                throws IOException, IllegalAccessException {
                            outputStream.writeShort(field.getShort(notif));
                        }

                        @Override
                        Object read(DataInputStream inputStream) throws EOFException, IOException {
                            return inputStream.readShort();
                        }

                        @Override
                        Object getUndefinedValue() {
                            return (short) 2;
                        }

                    };
                } else if (Byte.TYPE.getName().equals(className)) {
                    fieldSerializer = new FieldSerializer() {

                        @Override
                        void write(Field field, BungeeNotification notif, DataOutputStream outputStream)
                                throws IOException, IllegalAccessException {
                            outputStream.writeByte(field.getByte(notif));
                        }

                        @Override
                        Object read(DataInputStream inputStream) throws EOFException, IOException {
                            return inputStream.readByte();
                        }

                        @Override
                        Object getUndefinedValue() {
                            return (byte) 2;
                        }

                    };
                } else if (String.class.getName().equals(className)) {
                    fieldSerializer = new FieldSerializer() {

                        @Override
                        void write(Field field, BungeeNotification notif, DataOutputStream outputStream)
                                throws IOException, IllegalAccessException {
                            writeString((String) field.get(notif), outputStream);
                        }

                        @Override
                        Object read(DataInputStream inputStream) throws EOFException, IOException {
                            return readString(inputStream);
                        }

                        @Override
                        Object getUndefinedValue() {
                            return "@Undefined#";
                        }

                    };
                } else if (String[].class.getName().equals(className)) {
                    fieldSerializer = new FieldSerializer() {

                        @Override
                        void write(Field field, BungeeNotification notif, DataOutputStream outputStream)
                                throws IOException, IllegalAccessException {
                            String[] toWrite = (String[]) field.get(notif);
                            int length = toWrite != null ? toWrite.length : -1;
                            outputStream.writeInt(length);
                            if (length > 0) {
                                for (String item : toWrite) {
                                    writeString(item, outputStream);
                                }
                            }
                        }

                        @Override
                        Object read(DataInputStream inputStream) throws EOFException, IOException {
                            int length = inputStream.readInt();
                            if (length == -1) {
                                return null;
                            }

                            String[] res = new String[length];
                            for (int i = 0; i < length; i++) {
                                res[i] = readString(inputStream);
                            }
                            return res;
                        }

                        @Override
                        Object getUndefinedValue() {
                            return new String[] { "@Undefined#" };
                        }

                    };
                } else {
                    throw new IllegalArgumentException("Missing field serializer for class " + className);
                }

                fieldSerializerByClassName.put(className, fieldSerializer);
            }

            return fieldSerializer;
        }

        private static void writeString(String toWrite, DataOutputStream outputStream) throws IOException {
            if (toWrite == null) {
                toWrite = NULL_VALUE;
            } else if (NULL_VALUE.equals(toWrite)) {
                throw new IllegalArgumentException("Cannot write the null value as a normal string value");
            }
            outputStream.writeUTF(toWrite);
        }

        private static String readString(DataInputStream inputStream) throws EOFException, IOException {
            String read = inputStream.readUTF();
            return NULL_VALUE.equals(read) ? null : read;
        }

        private FieldSerializer() {}

        abstract void write(Field field, BungeeNotification notif, DataOutputStream outputStream)
                throws IOException, IllegalAccessException;

        abstract Object read(DataInputStream inputStream) throws EOFException, IOException;

        /**
         * This value is used to create an empty notification, but is actually never kept.
         * Because some notification types have some constraints (not null, not empty, not negative...) that are checked in the constructor, the undefined value should not have a simple value like 0 or null.
         * If needed, this method could be changed to return a set of different undefined values that could be tested until one works to create a certain type of notifications.
         * 
         * <h5>Why this method exists:</h5>
         * Since it is not possible to know what constructor parameter is associated to a certain field (there is no rule, the parameter can be named differently, parameters can be declared in a different order than the fields declaration order, the parameter can be induced by another parameter...), it is preferable to associate fields value without using the constructor.
         * This method defines the value to use (for a certain field type) in the notification constructor to create a notification instance, before filling its fields with their real value.
         * By defining this method in this class, each field type is guaranteed to have an undefined value, and no oversight is possible when adding the support of a new field type.
         */
        abstract Object getUndefinedValue();

    }

    public byte getId() {
        return id;
    }

    public void writeNotification(DataOutputStream outputStream, BungeeNotification notif)
            throws IllegalArgumentException, IllegalAccessException, IOException {
        if (!getDataClass().isInstance(notif)) {
            throw new IllegalArgumentException();
        }

        outputStream.writeLong(notif.creationTime);

        for (NotificationDataField notifField : getDataFields()) {
            notifField.write(notif, outputStream);
        }
    }

    public BungeeNotification readNotification(DataInputStream inputStream)
            throws IllegalArgumentException, IllegalAccessException, EOFException, IOException {
        BungeeNotification notif = newEmptyNotification();

        setNotificationCreationTime(inputStream.readLong(), notif);

        for (NotificationDataField notifField : getDataFields()) {
            notifField.setValue(notif, notifField.read(inputStream));
        }
        return notif;
    }

    public String toString(BungeeNotification notif) {
        if (!getDataClass().isInstance(notif)) {
            throw new IllegalArgumentException();
        }

        StringBuilder res = new StringBuilder(dataClass.getSimpleName());
        String fieldsSeparator = ", ";
        res.append("{");
        try {
            for (NotificationDataField notifField : getDataFields()) {
                res.append(notifField.field.getName());
                res.append("=");
                res.append(notifField.field.get(notif));
                res.append(fieldsSeparator);
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }
        int length = res.length();
        if (length >= fieldsSeparator.length()) {
            res.setLength(length - fieldsSeparator.length()); // Remove last ", "
        }
        res.append("}");
        return res.toString();
    }

    public BungeeNotification newEmptyNotification() {
        BungeeNotification notif;
        try {
            notif = (BungeeNotification) dataClassConstructor.newInstance(dataClassConstructorParametersDefaultValue);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException ex) {
            throw new IllegalStateException(ex);
        }
        return notif;
    }

    public static void setNotificationCreationTime(long creationTime, BungeeNotification notif) {
        try {
            creationTimeField.set(notif, creationTime);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public String toString() {
        return getDataClass().getSimpleName() + "-BungeeNotificationType";
    }

    public Class<? extends BungeeNotification> getDataClass() {
        return dataClass;
    }

    public NotificationDataField[] getDataFields() {
        return dataFields;
    }

}
