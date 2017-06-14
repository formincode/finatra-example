package test.annotation;

import com.twitter.collection.RecordSchema;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finatra.http.AbstractController;
import com.twitter.util.Future;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import static scala.compat.java8.JFunction.func;


/**
 * Provides an abstraction over FitbitController allowing subclasses to use
 * Annotated Methods in place of the builder functions
 *
 * @see Get
 */
public abstract class AnnotatedController extends AbstractController {

    private final String pathPrefix;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected AnnotatedController() {
        this("");
    }

    protected AnnotatedController(String pathPrefix) {
        if (pathPrefix == null) {
            pathPrefix = "";
        }
        if (pathPrefix.endsWith("/")) {
            pathPrefix = pathPrefix.substring(0, pathPrefix.length() - 1);
        }
        this.pathPrefix = pathPrefix;
    }

    @Override
    public void configureRoutes() {
        // Discover methods annotated with one of the HttpMethod annotations
        for (Method method : getClass().getMethods()) {
            if (method.isAnnotationPresent(Get.class)) {
                Get annotation = method.getAnnotation(Get.class);
                get(prefixPath(annotation.value()), request ->  createCallbackFromMethod(method).apply(request));
            }
        }
    }

    private String prefixPath(String path) {
        if (pathPrefix.isEmpty()) {
            return path;
        }
        if (path.startsWith("/")) {
            return pathPrefix + path;
        } else {
            return pathPrefix + "/" + path;
        }
    }

    private Function<Request, Object> createCallbackFromMethod(Method method) {
        Function<Request, Object> mapper = createMapperFromMethod(method);
        return request -> {
            Object r = mapper.apply(request);
            if (r instanceof Future) {
                return ((Future)r).map(func(value -> {
                    if (value == null) {
                        return response().noContent();
                    }
                    if (value instanceof Response) {
                        return (Response)value;
                    }
                    return response().ok().json(value);
                }));
            }
            return Future.value(r == null ? response().noContent()
                    : ( r instanceof Response ? (Response) r : response().ok().json(r)));
        };
    }

    private Function<Request, Object> createMapperFromMethod(Method method) {
        //create an argument builder
        Function<Request, Object>[] argBuilders = Arrays.stream(method.getParameters()).map(parameter -> {
            Type type = parameter.getParameterizedType();
            if (type == Request.class) {
                return (Function<Request, Object>) request -> request;
            }
            throw new IllegalArgumentException("Unable to determine argument type for "
                    + method.getName() + " " + parameter.getName());
        }).toArray((IntFunction<Function<Request, Object>[]>) Function[]::new);

        return request -> {
            try {
                return method.invoke(this, Arrays.stream(argBuilders)
                        .map(builder -> builder.apply(request)).toArray());
            } catch (IllegalAccessException e) {
                throw new RuntimeException("server error");
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof RuntimeException) {
                    throw (RuntimeException) e.getTargetException();
                }
                throw new RuntimeException(e.getTargetException());
            }
        };
    }

    private Function<Request, Object> makeFromBuilder(String fieldOrMethodName, Type paramType) {
        Field field = getField(fieldOrMethodName);
        if (field == null) {
            Pair<Class, String> getSearchAndName = getSearchClassAndName(fieldOrMethodName);
            Class toSearch = getSearchAndName.getKey();
            String methodName = getSearchAndName.getValue();
            //search for a method with the given name
            Method m = Stream.concat(Arrays.stream(toSearch.getMethods()),
                    toSearch == getClass() ? Arrays.stream(toSearch.getDeclaredMethods()) : Stream.empty())
                    .filter(method -> method.getName().equals(methodName))
                    .filter(method -> (Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers()))
                            || method.getDeclaringClass() == getClass())
                    .filter(method -> method.getGenericReturnType().equals(paramType))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unable to find field or method named "
                            + fieldOrMethodName));
            if (!m.isAccessible()) {
                m.setAccessible(true);
            }
            return createMapperFromMethod(m);
        }
        //check the type of the field
        Pair<Function, ParameterizedType> fieldValue = getFieldCheckedValue(field, Function.class);
        if (fieldValue.getValue().getActualTypeArguments()[0] != Request.class
                || !fieldValue.getValue().getActualTypeArguments()[1].equals(paramType)) {
            throw new IllegalArgumentException("Expected Function<Request," + paramType.getTypeName() + "> but was" +
                fieldValue.getValue().getTypeName());
        }
        return (Function<Request, Object>) fieldValue.getKey();
    }

    private Function<Request,Object> makeContextBuilder(String fieldName, Type paramType) {
        Field field = getField(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Unable to find field " + fieldName);
        }
        //get the value out of the field
        Pair<RecordSchema.Field, ParameterizedType> fieldValue = getFieldCheckedValue(field, RecordSchema.Field.class);
        Type contextFieldType = fieldValue.getValue().getActualTypeArguments()[0];
        if (!contextFieldType.equals(paramType)) {
            throw new IllegalArgumentException("Context parameter type mistach, expected "
                    + contextFieldType.getTypeName() + " but was " + paramType.getTypeName());
        }
        return request -> request.ctx().apply(fieldValue.getKey());
    }

    private Field getField(String fieldName){
        Pair<Class, String> searchAndName = getSearchClassAndName(fieldName);
        Class toSearch = searchAndName.getKey();
        String actualFieldName = searchAndName.getValue();
        try {
            return toSearch.getField(actualFieldName);
        } catch (NoSuchFieldException e) {
            if (toSearch == getClass()) {
                try {
                    return toSearch.getDeclaredField(actualFieldName);
                } catch (NoSuchFieldException ignored) {}
            }
            return null;
        }
    }

    private <T> Pair<T,ParameterizedType> getFieldCheckedValue(Field field, Class<T> rawType) {
        try {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            Object o = field.get(this);
            if (!rawType.isInstance(o)) {
                throw new IllegalArgumentException("Expected field to be of type " + rawType.getName());
            }
            //verify that the field type matches the desired type
            Stack<Type> toCheck = new Stack<>();
            toCheck.add(field.getGenericType());
            while (!toCheck.isEmpty()) {
                Type check = toCheck.pop();
                if (check instanceof ParameterizedType) {
                    if (((ParameterizedType) check).getRawType() == rawType) {
                        return new Pair<>((T)o, (ParameterizedType)check);
                    }
                    check = ((ParameterizedType) check).getRawType();
                }
                if (check instanceof Class) {
                    Type superClass = ((Class)check).getGenericSuperclass();
                    if (superClass != null) {
                        toCheck.add(superClass);
                    }
                    if (rawType.isInterface()) {
                        toCheck.addAll(Arrays.asList(((Class) check).getGenericInterfaces()));
                    }
                }
            }
            throw new IllegalArgumentException("Unable to obtain parameterized type from field");
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to obtain field value " + field.getName(), e);
        }
    }

    private Pair<Class, String> getSearchClassAndName(String fieldOrMethodName) {
        //split the property name on dots.
        String[] fieldParts = fieldOrMethodName.split("\\.");
        Class toSearch;
        if (fieldParts.length == 1) {
            toSearch = getClass();
        } else {
            //rejoin all but the last value
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < fieldParts.length - 1; i++) {
                if (b.length() > 0) {
                    b.append(".");
                }
                b.append(fieldParts[i]);
            }
            try {
                toSearch = Class.forName(b.toString());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Unable to load class " + b.toString(), e);
            }
        }
        return new Pair<>(toSearch, fieldParts[fieldParts.length - 1]);
    }
}
