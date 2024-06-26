package generic.fixture;

import com.github.curiousoddman.rgxgen.RgxGen;
import enums.AnnotationsEnum;
import exceptions.TypeNotRecognizedException;
import org.apache.commons.lang3.RandomStringUtils;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Email;
import javax.validation.constraints.Future;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Negative;
import javax.validation.constraints.NegativeOrZero;
import javax.validation.constraints.Past;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.Temporal;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

import static enums.AnnotationsEnum.DECIMAL_MAX;
import static enums.AnnotationsEnum.DECIMAL_MIN;
import static enums.AnnotationsEnum.DIGITS;
import static enums.AnnotationsEnum.EMAIL;
import static enums.AnnotationsEnum.FUTURE;
import static enums.AnnotationsEnum.FUTURE_OR_PRESENT;
import static enums.AnnotationsEnum.MAX;
import static enums.AnnotationsEnum.MIN;
import static enums.AnnotationsEnum.NEGATIVE;
import static enums.AnnotationsEnum.NEGATIVE_OR_ZERO;
import static enums.AnnotationsEnum.PAST;
import static enums.AnnotationsEnum.PAST_OR_PRESENT;
import static enums.AnnotationsEnum.PATTERN;
import static enums.AnnotationsEnum.POSITIVE;
import static enums.AnnotationsEnum.POSITIVE_OR_ZERO;
import static enums.AnnotationsEnum.SIZE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static utils.UtilsAnnotations.hasAnnotation;
import static utils.UtilsAnnotations.limitateDefaultMaxValue;
import static utils.UtilsBigDecimal.returnValueByPattern;
import static utils.UtilsDate.returnValueByPatternAndType;

/**
 * This class provides static methods that receive the class definition for which the fixture will be generated,
 * and returns an instance of that class with fields populated with random values or user defined values for
 * specific fields.
 * <p>User defined classes are recursively generated until there's only java classes to instantiate.</p>
 * <p>If the class to instantiate contains a Java Type that the library is not prepared to handle, it will be set to null.</p>
 */
public final class GenericFixture {

    private GenericFixture() {}

    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates an instance of the specified class with all fields populated with random values.
     *
     * @param clazz Class to instantiate.
     * @return Instance of the desired class with all fields randomly populated.
     */
    public static <T> T generate(Class<T> clazz) {
        return doGenerate(clazz, new HashMap<>(), "", 1, new HashSet<>());
    }

    /**
     * Generates an instance of the specified class with fields populated with random values or user defined values
     * for specific fields.
     *
     * <pre>{@code
     *     public class Person {
     *         private String name;
     *         private String email;
     *         private Integer age;
     *         private Pet pet;
     *     }
     *
     *     public class Pet {
     *         private String name;
     *         private String breed;
     *     }
     *
     *     public static void main(String[] args) {
     *         HashMap<String, Object> customFields = new HashMap<>();
     *         customFields.put("name", "Fábio Brazza");
     *         customFields.put("pet.name", "Jack");
     *         customFields.put("age", null);
     *
     *         Person person = GenericFixture.generate(Person.class, customFields);
     *
     *         assertTrue(person.getName().equals("Fábio Brazza"));
     *         assertTrue(person.getPet().getName().equals("Jack"));
     *         assertNull(person.getAge());
     *     }
     * }</pre>
     *
     * @param clazz Class to instantiate.
     * @param customFields Map where the key represents the path to a specific attribute which will
     *                     be set with the map value.
     * @return Instance of the desired class with custom fields defined. Fields not present in the map will still be
     * randomly populated.
     */
    public static <T> T generate(Class<T> clazz, Map<String, Object> customFields) {
        return doGenerate(clazz, customFields, "", 1, new HashSet<>());
    }

    /**
     * Generates an instance of the specified class with fields populated with random values. Iterable fields will
     * contain the determined number of items.
     *
     * <pre>{@code
     *     public class Library {
     *         private List<String> clients;
     *         private Map<String, Integer> prices;
     *         private Book[] books;
     *     }
     *
     *     public static void main(String[] args) {
     *         Library lib = GenericFixture.generate(Library.class, 3);
     *         assertTrue(lib.getClients().size().equals(3));
     *         assertTrue(lib.getPrices().size().equals(3));
     *         assertTrue(lib.getBooks().length == 3);
     *     }
     * }</pre>
     *
     * @param clazz Class to instantiate.
     * @param numberOfItems Number of itens to be inserted in each Collection or Array field of the class.
     * @return Instance of the desired class with all fields randomly populated and iterables containing the specified number of items.
     */
    public static <T> T generate(Class<T> clazz, Integer numberOfItems) {
        return doGenerate(clazz, new HashMap<>(), "", ofNullable(numberOfItems).orElse(1), new HashSet<>());
    }

    /**
     * Generates an instance of the specified class with fields populated with random values or user defined values
     * for specific fields. Iterable fields will contain the determined number of items.
     * Combines the functionality of {@link GenericFixture#generate(Class, Map)} and {@link GenericFixture#generate(Class, Integer)}
     *
     * @param clazz Class to instantiate.
     * @param customFields Map where the key represents the path to a specific attribute which
     *                     will be set with the map value.
     * @param numberOfItems Number of itens to be inserted in each Collection or Array field of the class. If null is
     *                      passed as an argument, the default value is 1.
     * @return Instance of the desired class with fields randomly populated or with user defined values and iterables.
     *
     * containing the specified number of items.
     */
    public static <T> T generate(Class<T> clazz, Map<String, Object> customFields, Integer numberOfItems) {
        return doGenerate(clazz, customFields, "", ofNullable(numberOfItems).orElse(1), new HashSet<>());
    }

    /**
     * Generates a List of instances of the specified class with all fields populated with random values.
     *
     * <pre>{@code
     *     public static void main(String[] args) {
     *         List<Dummy> fixtureList = GenericFixture.generateMany(Dummy.class, 3);
     *         assertTrue(fixtureList.size().equals(3));
     *     }
     * }</pre>
     *
     * @param clazz Class to instantiate.
     * @param numberOfFixtures Number of fixtures to generate.
     * @return List of instances of the desired class with fields randomly populated.
     */
    public static <T> List<T> generateMany(Class<T> clazz, Integer numberOfFixtures) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < numberOfFixtures; i++) {
            list.add(doGenerate(clazz, new HashMap<>(), "", 1, new HashSet<>()));
        }
        return list;
    }

    /**
     * Generates a List of instances of the specified class, with fields populated with random values or user defined values
     * for specific fields. Iterable fields will contain the determined number of items.
     * This method extends the functionality of {@link GenericFixture#generate(Class, Map, Integer)}.
     *
     * <pre>{@code
     *     public static void main(String[] args) {
     *         List<Dummy> fixtureList = GenericFixture
     *             .generateMany(Dummy.class, new HashMap<>(), 1, 3);
     *         assertTrue(fixtureList.size().equals(3));
     *     }
     * }</pre>
     *
     * @param clazz Class to instantiate.
     * @param customFields Map where the key represents the path to a specific attribute which
     *                     will be set with the map value.
     * @param numberOfItems Number of itens to be inserted in each Collection or Array field of the class. If null is
     *                      passed as an argument, the default value is 1.
     * @param numberOfFixtures Number of fixtures to generate.
     * @return List of instances of the desired class with fields randomly populated or with user defined values and
     * iterables containing the specified number of items.
     */
    public static <T> List<T> generateMany(Class<T> clazz, Map<String, Object> customFields, Integer numberOfItems, Integer numberOfFixtures) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < numberOfFixtures; i++) {
            list.add(doGenerate(clazz, customFields, "", ofNullable(numberOfItems).orElse(1), new HashSet<>()));
        }
        return list;
    }

    private static <T> T generate(Class<T> clazz, Map<String, Object> customFields, String attributesPath, Integer numberOfItems, Set<Class<?>> visitedClasses) {
        return doGenerate(clazz, customFields, attributesPath, numberOfItems, visitedClasses);
    }

    private static <T> T doGenerate(Class<T> clazz, Map<String, Object> customFields, String attributesPath, Integer numberOfItems, Set<Class<?>> visitedClasses) {

        try {

            T type = instantiateType(clazz);

            if (isComplexClass(clazz)) {
                visitedClasses.add(clazz);
            }

            List<Field> fieldsList = getFieldsList(clazz);

            for (Field field : fieldsList) {

                //Avoid circular attribute generation
                if (visitedClasses.contains(field.getType())) {
                    continue;
                }

                field.setAccessible(true);

                String fieldName = field.getName();
                String currentPath = handleAttributesPath(fieldName, attributesPath);

                if (nonNull(customFields) && !customFields.isEmpty() && isCustomField(customFields, currentPath)) {
                    field.set(type, customFields.get(currentPath));
                    visitedClasses.remove(clazz);
                    continue;
                }

                //Only set field value if not already defined.
                if (isNull(field.get(type)) || field.getType().isPrimitive()) {
                    Map<AnnotationsEnum, Annotation> map = getAnnotationsMap(field);
                    Object result = getRandomForType(field.getType(), field.getGenericType(), map, customFields,
                            currentPath, numberOfItems, visitedClasses);
                    field.set(type, result);
                }
            }

            //Enable GenericFixture to generate another instance of this clazz, because circular generation will not happen.
            visitedClasses.remove(clazz);
            return type;

        } catch (Exception e) {
            System.out.println("\nError ocurred ".concat(e.toString()));
            throw new RuntimeException(e.getMessage());
        }
    }

    private static <T> List<Field> getFieldsList(Class<T> clazz) {
        List<Field> fieldsList = new ArrayList<>();
        Class<? super T> superClass = clazz;

        do {
            Field[] fields = superClass.getDeclaredFields();
            fieldsList.addAll(ignoreFinalAndStaticFields(fields));
            superClass = superClass.getSuperclass();
        } while (isComplexClass(superClass));

        return fieldsList;
    }

    private static <T> T instantiateType(Class<T> clazz) throws Exception {
        T type;
        if (hasNoArgsConstructor(clazz)) {
            type = clazz.getDeclaredConstructor().newInstance();
        } else {
            type = getInstanceForConstructorWithLessArguments(clazz);
        }
        return type;
    }

    private static List<Field> ignoreFinalAndStaticFields(Field[] fields) {
        return Arrays.stream(fields)
                .filter(f -> !Modifier.isFinal(f.getModifiers()))
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .collect(Collectors.toList());
    }

    private static <T> T getInstanceForConstructorWithLessArguments(Class<?> clazz) throws Exception {

        Constructor<?>[] constructors = clazz.getConstructors();

        //Order constructor array by lesser parameter count
        Object[] orderedConstructors = Arrays.stream(constructors)
                .sorted(Comparator.comparing(Constructor::getParameterCount))
                .toArray();

        Constructor<?> constructor = (Constructor<?>) orderedConstructors[0];

        //Get array of parameter types of the constructor
        Class<?>[] parameterTypes = constructor.getParameterTypes();

        //Array for storing the values for each argument
        Object[] arguments = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {

            if (parameterTypes[i].isPrimitive()) {
                //Generates a value for each primitive argument
                arguments[i] = getRandomForType(parameterTypes[i], parameterTypes[i], new HashMap<>(), new HashMap<>(),
                        "", null, null);
            } else {
                arguments[i] = null;
            }

        }

        return (T) constructor.newInstance(arguments);
    }

    private static boolean isCustomField(Map<String, Object> customFields, String currentPath) {
        //If customFields contains "A.B.C" and currentPath is exactly "A.B.C"
        return customFields.keySet().stream().anyMatch(f -> f.equals(currentPath));
    }

    private static String handleAttributesPath(String fieldName, String attributesPath) {
        if (attributesPath.isEmpty()) {
            //First iteration, no recursion
            return fieldName;
        } else {
            return attributesPath.concat(".").concat(fieldName);
        }
    }

    private static HashMap<AnnotationsEnum, Annotation> getAnnotationsMap(Field field) {
        HashMap<AnnotationsEnum, Annotation> hashMap = new HashMap<>();

        for (Annotation annotation : field.getAnnotations()) {
            if (annotation instanceof Pattern) {
                hashMap.put(PATTERN, annotation);
            }

            if (annotation instanceof Size) {
                hashMap.put(SIZE, annotation);
            }

            if (annotation instanceof Past) {
                hashMap.put(PAST, annotation);
            }

            if (annotation instanceof PastOrPresent) {
                hashMap.put(PAST_OR_PRESENT, annotation);
            }

            if (annotation instanceof Digits) {
                hashMap.put(DIGITS, annotation);
            }

            if (annotation instanceof Positive) {
                hashMap.put(POSITIVE, annotation);
            }

            if (annotation instanceof PositiveOrZero) {
                hashMap.put(POSITIVE_OR_ZERO, annotation);
            }

            if (annotation instanceof Negative) {
                hashMap.put(NEGATIVE, annotation);
            }

            if (annotation instanceof NegativeOrZero) {
                hashMap.put(NEGATIVE_OR_ZERO, annotation);
            }

            if (annotation instanceof Future) {
                hashMap.put(FUTURE, annotation);
            }

            if (annotation instanceof FutureOrPresent) {
                hashMap.put(FUTURE_OR_PRESENT, annotation);
            }

            if (annotation instanceof Email) {
                hashMap.put(EMAIL, annotation);
            }

            if (annotation instanceof DecimalMin) {
                hashMap.put(DECIMAL_MIN, annotation);
            }

            if (annotation instanceof DecimalMax) {
                hashMap.put(DECIMAL_MAX, annotation);
            }

            if (annotation instanceof Min) {
                hashMap.put(MIN, annotation);
            }

            if (annotation instanceof Max) {
                hashMap.put(MAX, annotation);
            }
        }

        return hashMap;
    }

    private static Object getRandomForType(Class<?> fieldType,
                                           Type genericType,
                                           Map<AnnotationsEnum, Annotation> hashMap,
                                           Map<String, Object> customFields,
                                           String currentPath,
                                           Integer numberOfItems,
                                           Set<Class<?>> visitedClasses) throws Exception {

        if (fieldType == String.class) {
            String string = RandomStringUtils.randomAlphanumeric(10);

            if (hashMap.containsKey(PATTERN)) {
                Pattern pattern = (Pattern) hashMap.get(PATTERN);
                string = new RgxGen(pattern.regexp()).generate();
            }

            if (hashMap.containsKey(SIZE)) {
                Size size = (Size) hashMap.get(SIZE);
                int max = limitateDefaultMaxValue(size);
                string = RandomStringUtils.randomAlphanumeric(size.min(), max);
            }

            if (hashMap.containsKey(PAST)
                    || hashMap.containsKey(PAST_OR_PRESENT)
                    || hashMap.containsKey(FUTURE)
                    || hashMap.containsKey(FUTURE_OR_PRESENT)) {
                string = returnValueByPatternAndType(hashMap, LocalDateTime.class).toString();
            }

            if (hashMap.containsKey(EMAIL)) {
                string = new RgxGen("^[a-zA-Z0-9_.]{1,10}@email\\.com$").generate();
            }

            if (hashMap.containsKey(POSITIVE)
                || hashMap.containsKey(POSITIVE_OR_ZERO)
                || hashMap.containsKey(NEGATIVE)
                || hashMap.containsKey(NEGATIVE_OR_ZERO)
                || hashMap.containsKey(DECIMAL_MIN)
                || hashMap.containsKey(DECIMAL_MAX)
                || hashMap.containsKey(DIGITS)
                || hashMap.containsKey(MIN)
                || hashMap.containsKey(MAX)) {
                string = returnValueByPattern(hashMap).toString();
            }

            return string;
        }

        if (fieldType == Long.class || fieldType == long.class) {
            if (hashMap.containsKey(DIGITS)
                || hashMap.containsKey(POSITIVE)
                || hashMap.containsKey(POSITIVE_OR_ZERO)
                || hashMap.containsKey(NEGATIVE)
                || hashMap.containsKey(NEGATIVE_OR_ZERO)
                || hashMap.containsKey(DECIMAL_MAX)
                || hashMap.containsKey(MIN)
                || hashMap.containsKey(MAX)) {
                //The transformation to long discards decimal places
                return returnValueByPattern(hashMap).longValue();
            }

            return random.nextLong();
        }

        if (fieldType == Integer.class || fieldType == int.class) {
            if (hashMap.containsKey(DIGITS)
                || hashMap.containsKey(POSITIVE)
                || hashMap.containsKey(POSITIVE_OR_ZERO)
                || hashMap.containsKey(NEGATIVE)
                || hashMap.containsKey(NEGATIVE_OR_ZERO)
                || hashMap.containsKey(DECIMAL_MAX)
                || hashMap.containsKey(MIN)
                || hashMap.containsKey(MAX)) {
                return returnValueByPattern(hashMap).intValue();
            }

            return random.nextInt(100000);
        }

        if (fieldType == Double.class || fieldType == double.class) {
            if (hashMap.containsKey(DIGITS)
                || hashMap.containsKey(POSITIVE)
                || hashMap.containsKey(POSITIVE_OR_ZERO)
                || hashMap.containsKey(NEGATIVE)
                || hashMap.containsKey(NEGATIVE_OR_ZERO)
                || hashMap.containsKey(DECIMAL_MIN)
                || hashMap.containsKey(DECIMAL_MAX)
                || hashMap.containsKey(MIN)
                || hashMap.containsKey(MAX)) {
                return returnValueByPattern(hashMap).doubleValue();
            }

            return random.nextDouble();
        }

        if (fieldType == Boolean.class || fieldType == boolean.class) {
            return random.nextBoolean();
        }

        if (fieldType == Character.class || fieldType == char.class) {
            return randomAlphabetic(1).charAt(0);
        }

        if (fieldType == BigDecimal.class) {
            if (hasAnnotation(hashMap)) {
               return returnValueByPattern(hashMap);
            }
            return BigDecimal.valueOf(random.nextDouble());
        }

        if (fieldType == Date.class ) {
            if(hasAnnotation(hashMap)) {
                return returnValueByPatternAndType(hashMap, Date.class);
            }
            return new Date();
        }

        if (implementsTemporal(fieldType)) {
            if (fieldType.isInterface()) {

                if (fieldType == ChronoLocalDate.class) {
                    return LocalDate.now();
                }
                if (fieldType == ChronoLocalDateTime.class) {
                    return LocalDateTime.now();
                }
                if (fieldType == ChronoZonedDateTime.class) {
                    return ZonedDateTime.now();
                }

            } else {

                if (fieldType == LocalDateTime.class) {
                    if(hasAnnotation(hashMap)) {
                        return returnValueByPatternAndType(hashMap, LocalDateTime.class);
                    }
                    return LocalDateTime.now();
                }

                if (fieldType == OffsetDateTime.class) {
                    if(hasAnnotation(hashMap)) {
                        return returnValueByPatternAndType(hashMap, OffsetDateTime.class);
                    }
                    return OffsetDateTime.now();
                }

                if (fieldType == Instant.class) {
                    if(hasAnnotation(hashMap)) {
                        return returnValueByPatternAndType(hashMap, Instant.class);
                    }
                    return Instant.now();
                }

                if (fieldType == ZonedDateTime.class) {
                    if(hasAnnotation(hashMap)) {
                        return returnValueByPatternAndType(hashMap, ZonedDateTime.class);
                    }
                    return ZonedDateTime.now();
                }

                if (fieldType == LocalDate.class) {
                    if(hasAnnotation(hashMap)) {
                        return returnValueByPatternAndType(hashMap, LocalDate.class);
                    }
                    return LocalDate.now();
                }

                if (fieldType == LocalTime.class) {
                    if(hasAnnotation(hashMap)) {
                        return returnValueByPatternAndType(hashMap, LocalTime.class);
                    }
                    return LocalTime.now();
                }

                if (fieldType == OffsetTime.class) {
                    if(hasAnnotation(hashMap)) {
                        return returnValueByPatternAndType(hashMap, OffsetTime.class);
                    }
                    return OffsetTime.now();
                }

            }
        }

        if (fieldType == UUID.class) {
            return UUID.randomUUID();
        }

        if (fieldType.isEnum()) {
            return fieldType.getEnumConstants()[0];
        }

        //Here we can identify what types are not POJOs
        if (isComplexClass(fieldType)) {
            return generate(fieldType, customFields, currentPath, numberOfItems, visitedClasses);
        }

        if (implementsCollection(fieldType)) {

            Class<?>[] innerClasses = getInnerClasses(genericType); //Get the Generic type inside List<T>
            Collection<Object> collection = null;

            if (fieldType.isInterface()) {

                //Verify all possible Interfaces that extends Collection
                //and choose default implementation
                if (fieldType == List.class) {
                    collection = new ArrayList<>();
                }
                if (fieldType == Queue.class) {
                    collection = new PriorityQueue<>();
                }
                if (fieldType == Deque.class) {
                    collection = new ArrayDeque<>();
                }
                if (fieldType == Set.class) {
                    collection = new HashSet<>();
                }
                if (fieldType == SortedSet.class) {
                    collection = new TreeSet<>();
                }

            } else {
                collection = (Collection<Object>) fieldType.getDeclaredConstructor().newInstance();
            }

            assert collection != null;

            for (int i = 0; i < numberOfItems; i++) {
                Object obj = getObjectByClass(innerClasses[0], customFields, currentPath, numberOfItems, visitedClasses);
                collection.add(obj);
            }

            return collection;
        }

        if (implementsMap(fieldType) || isDictionary(fieldType)) {

            Class<?>[] innerClasses = getInnerClasses(genericType); //Get the Generic type inside Map<K, V>
            Map<Object, Object> map = null;

            if (fieldType.isInterface() || Modifier.isAbstract(fieldType.getModifiers())) {

                //Verify all possible Interfaces/AbstractClasses that extends Map
                //and choose default implementation
                if (fieldType == Map.class || fieldType == AbstractMap.class) {
                    map = new HashMap<>();
                }

                if (fieldType == ConcurrentMap.class) {
                    map = new ConcurrentHashMap<>();
                }

                //These types require that all keys inserted must implement the Comparable interface
                if (fieldType == SortedMap.class || fieldType == NavigableMap.class) {
                    map = new TreeMap<>();
                }

                //This type require that all keys inserted must implement the Comparable interface
                if (fieldType == ConcurrentNavigableMap.class) {
                    map = new ConcurrentSkipListMap<>();
                }

                if (fieldType == Dictionary.class) {
                    map = new Hashtable<>();
                }

            } else {
                map = (Map<Object, Object>) fieldType.getDeclaredConstructor().newInstance();
            }

            assert map != null;

            for (int i = 0; i < numberOfItems; i++) {
                Object key = getObjectByClass(innerClasses[0], customFields, currentPath, numberOfItems, visitedClasses);
                Object value = getObjectByClass(innerClasses[1], customFields, currentPath, numberOfItems, visitedClasses);
                tryPutOnMap(genericType, map, key, value);
            }

            return map;
        }

        if (fieldType.isArray()) {

            Class<?> arrayType = fieldType.getComponentType();
            Object array = Array.newInstance(arrayType, numberOfItems);

            for (int i = 0; i < numberOfItems; i++) {
                if (hasTypeParameters(arrayType)) {
                    //This cast is necessary to parse Map<K,V>[] to Map<K,V>, or List<E>[] to List<E>
                    Type genericComponentType = (((GenericArrayType) genericType).getGenericComponentType());
                    Array.set(array, i, getRandomForType(arrayType, genericComponentType, hashMap, customFields, currentPath, numberOfItems, visitedClasses));
                } else {
                    Array.set(array, i, getObjectByClass(arrayType, customFields, currentPath, numberOfItems, visitedClasses));
                }
            }

            return array;
        }

        return null;
    }

    private static boolean hasTypeParameters(Class<?> clazz) {
        return clazz.getTypeParameters().length > 0;
    }

    private static void tryPutOnMap(Type type, Map<Object, Object> map, Object key, Object value) {
        try {
            map.put(key, value);
        } catch (ClassCastException e) {
            System.out.printf("\nIt's necessary to implement Comparable<?> Interface in Key Class of the Map: %s ", type.toString());
            throw new ClassCastException("It's necessary to implement Comparable<?> Interface in Key Class of the Map: ".concat(type.toString()));
        }
    }


    private static boolean implementsCollection(Class<?> fieldType) {
        return Collection.class.isAssignableFrom(fieldType);
    }

    private static boolean implementsTemporal(Class<?> fieldType) {
        return Temporal.class.isAssignableFrom(fieldType);
    }

    private static boolean implementsMap(Class<?> fieldType) {
        return Map.class.isAssignableFrom(fieldType);
    }

    private static boolean isDictionary(Class<?> fieldType) {
        return fieldType == Dictionary.class;
    }

    private static boolean hasNoArgsConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        for (Constructor<?> c : constructors) {
            if (c.getParameterCount() == 0) {
                return true;
            }
        }
        return false;
    }

    private static Object getObjectByClass(Class<?> innerClass, Map<String, Object> customFields, String currentPath, Integer numberOfItems, Set<Class<?>> visitedClasses) throws Exception {
        return isComplexClass(innerClass) ?
                generate(innerClass, customFields, currentPath, numberOfItems, visitedClasses) :
                getRandomForType(innerClass, null, new HashMap<>(), null, currentPath, numberOfItems, visitedClasses);
    }

    private static boolean isComplexClass(Class<?> clazz) {
        if (nonNull(clazz.getPackage())) {
            return !clazz.getPackageName().startsWith("java") && !clazz.isEnum();
        }

        return false;
    }

    private static Class<?>[] getInnerClasses(Type type) throws ClassNotFoundException {
        //If the field has generics, we need to get the generic types
        ParameterizedType parameterizedType = (ParameterizedType) type;
        //Generic types, like T of List<T> or K, V of Map<K, V>
        Type[] types = parameterizedType.getActualTypeArguments();

        Class<?>[] innerClasses = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            Type t = types[i];
            Class<?> innerClass = Class.forName(t.getTypeName()); //Fully qualified name
            innerClasses[i] = innerClass;
        }

        return innerClasses;
    }
}
