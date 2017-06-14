package test.annotation;

import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finatra.http.AbstractController;
import com.twitter.util.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static scala.compat.java8.JFunction.func;


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
            if (method.isAnnotationPresent(Post.class)) {
                Post annotation = method.getAnnotation(Post.class);
                post(prefixPath(annotation.value()), request ->  createCallbackFromMethod(method).apply(request));
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
            if (parameter.isAnnotationPresent(Param.class)) {
                return makeParamBuilder(parameter.getAnnotation(Param.class).value(), type);
            }
            if (parameter.isAnnotationPresent(Body.class)) {
                return makeBodyBuilder(type);
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

    private Function<Request, Object> makeBodyBuilder(Type paramType) {
        if (paramType == String.class) {
            return request -> request.contentString();
        } else if (paramType instanceof Class && ((Class) paramType).isArray()
                && ((Class) paramType).getComponentType() == byte.class) {
            return request -> request.content().copiedByteArray();
        }
        throw new IllegalArgumentException("Unable to convert body to " + paramType.getTypeName()
                + " allowed types are String or byte[]");
    }

    private Function<Request, Object> makeParamBuilder(String paramName, Type paramType) {
        if (paramType == short.class) {
            return request -> request.getShortParam(paramName);
        } else if (paramType == int.class) {
            return request -> request.getIntParam(paramName);
        } else if (paramType == long.class) {
            return request -> request.getLongParam(paramName);
        } else if (paramType == boolean.class) {
            return request -> request.getBooleanParam(paramName);
        } else if (paramType == byte.class) {
            //Use the short converter already supplied
            return request -> Short.valueOf(request.getShortParam(paramName)).shortValue();
        } else {
            //Try Collections
            Optional<Function<Stream<String>, Object>> maybeCollection =
                    tryCollection(paramType, logger);
            if (maybeCollection.isPresent()) {
                Function<Stream<String>, Object> collectionBuilder = maybeCollection.get();
                return request -> collectionBuilder.apply(request.getParams(paramName).stream());
            }
            Function<String, Object> converter = makeValueConverter(paramType, logger);
            return request -> converter.apply(request.getParam(paramName));
        }
    }

    static Optional<Function<Stream<String>,Object>> tryCollection(Type targetType, Logger logger) {
        if (targetType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) targetType;
            Type raw = pt.getRawType();
            // Collections
            if (raw == Set.class) {
                Function<String, Object> inner = makeValueConverter(pt.getActualTypeArguments()[0], logger);
                return Optional.of(values -> values.map(inner).collect(Collectors.toSet()) );
            } else if (raw == List.class) {
                Function<String, Object> inner = makeValueConverter(pt.getActualTypeArguments()[0], logger);
                return Optional.of(values -> values.map(inner).collect(Collectors.toList()));
            }
            return Optional.empty();
        }

        if (targetType instanceof Class) {
            Class targetClass = (Class) targetType;
            //arrays
            if (targetClass.isArray()) {
                Class componentType = targetClass.getComponentType();
                Function<String, Object> inner = makeValueConverter(componentType, logger);
                return Optional.of(values -> values.map(inner)
                        .toArray(length -> (Object[]) Array.newInstance(componentType, length)));
            }
        }

        return Optional.empty();

    }

    static Function<String, Object> makeValueConverter(Type targetType, Logger logger) {
        // Special case for Void, Object, String, and CharSequence since they're no-ops
        if (targetType == Void.TYPE || targetType == Void.class || targetType == Object.class
                || targetType == String.class || targetType == CharSequence.class) {
            return value -> value;
        }

        //Primitive handlers
        if (targetType == byte.class) {
            return value -> {
                try {
                    return Byte.valueOf(value).byteValue();
                } catch (NumberFormatException nfe) {
                    logger.info("Unable to coerce value of " + value + " to byte");
                    return (byte)0;
                }
            };
        } else if (targetType == short.class) {
            return value -> {
                try {
                    return Short.valueOf(value).shortValue();
                } catch (NumberFormatException nfe) {
                    logger.info("Unable to coerce value of " + value + " to short");
                    return (short)0;
                }
            };
        } else if (targetType == int.class) {
            return value -> {
                try {
                    return Integer.valueOf(value).intValue();
                } catch (NumberFormatException nfe) {
                    logger.info("Unable to coerce value of " + value + " to int");
                    return (int)0;
                }
            };
        } else if (targetType == long.class) {
            return value -> {
                try {
                    return Long.valueOf(value).longValue();
                } catch (NumberFormatException nfe) {
                    logger.info("Unable to coerce value of " + value + " to long");
                    return (long)0;
                }
            };
        } else if (targetType == float.class) {
            return value -> {
                try {
                    return Float.valueOf(value).floatValue();
                } catch (NumberFormatException nfe) {
                    logger.info("Unable to coerce value of " + value + " to float");
                    return (float)0.0;
                }
            };
        } else if (targetType == double.class) {
            return value -> {
                try {
                    return Double.valueOf(value).doubleValue();
                } catch (NumberFormatException nfe) {
                    logger.info("Unable to coerce value of " + value + " to double");
                    return (double)0.0;
                }
            };
        }

        //Anything not covered above is a Reference Type and can take a Null value
        Function<String, Object> inner = makeRefConverter(targetType, logger);
        return value -> {
            // If null comes in null goes out
            if (value == null) {
                return null;
            }
            return inner.apply(value);
        };
    }

    private static Function<String,Object> makeRefConverter(Type targetType, Logger logger) {
        //collections
        Optional<Function<Stream<String>, Object>> maybeCollection = tryCollection(targetType, logger);
        if (maybeCollection.isPresent()) {
            Function<Stream<String>, Object> builder = maybeCollection.get();
            return value -> builder.apply(Stream.of(value));
        }

        //Parameterized Types
        if (targetType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) targetType;
            Type raw = pt.getRawType();
            if (raw instanceof Class) {
                Class rawClass = (Class) raw;
                Type[] typeArgs = pt.getActualTypeArguments();
                TypeVariable<Class>[] typeParameters = rawClass.getTypeParameters();
                Map<String, Type> generics = new HashMap<>(typeParameters.length);
                for (int i = 0; i < typeParameters.length; i++) {
                    generics.put(typeParameters[i].getName(), typeArgs[i]);
                }
                return makeFactory(rawClass, generics, logger);
            }
        }

        //Everything else
        if (targetType instanceof Class) {
            Class targetClass = (Class) targetType;
            return makeFactory(targetClass, new HashMap<>(), logger);
        }

        throw new IllegalArgumentException("Unable to build value converter");
    }

    private static Function<String, Object> makeFactory(Class<?> targetType,
                                                        Map<String, Type> genericTypes,
                                                        Logger logger) {
        //look for a static factory function
        List<Method> factories = Arrays.stream(targetType.getMethods()).filter(method ->
                Modifier.isStatic(method.getModifiers()) && method.getReturnType() == targetType
                        && method.getParameterCount() == 1
        ).collect(Collectors.toList());
        if (!factories.isEmpty()) {
            //find the best matching Method
            Method bestMatch = null;
            for (Method m : factories) {
                if (bestMatch == null) {
                    bestMatch = m;
                    continue;
                }
                if (isBetter(bestMatch.getName(), m.getName(),
                        bestMatch.getParameters()[0], m.getParameters()[0], genericTypes)) {
                    bestMatch = m;
                }
            }
            final Method f = bestMatch;
            Type p = f.getParameters()[0].getParameterizedType();
            if (p instanceof TypeVariable) {
                p = genericTypes.get(((TypeVariable) p).getName());
            }
            Function<String, Object> inner = makeValueConverter(p, logger);
            return value -> {
                try {
                    return f.invoke(null, inner.apply(value));
                } catch (InvocationTargetException ite) {
                    throw new RuntimeException(ite.getTargetException());
                } catch (IllegalAccessException e) {
                    logger.info("Unable to coerce value of " + value + " to " + targetType.getName());
                    return null;
                }
            };
        }

        //Look for single argument constructors, don't care about the type
        List<Constructor<?>> constructors = Arrays.stream(targetType.getConstructors())
                .filter(c -> c.getParameterCount() == 1).collect(Collectors.toList());
        if (!constructors.isEmpty()) {
            //find the best matching Constructor
            Constructor<?> bestMatch = null;
            for (Constructor<?> c : constructors) {
                if (bestMatch == null) {
                    bestMatch = c;
                    continue;
                }
                if (isBetter(bestMatch.getName(), c.getName(), bestMatch.getParameters()[0],
                        c.getParameters()[0], genericTypes)) {
                    bestMatch = c;
                }
            }
            final Constructor<?> c = bestMatch;
            Type p = c.getParameters()[0].getParameterizedType();
            if (p instanceof TypeVariable) {
                p = genericTypes.get(((TypeVariable) p).getName());
            }
            Function<String, Object> inner = makeValueConverter(p, logger);
            return value -> {
                try {
                    return c.newInstance(inner.apply(value));
                } catch (InvocationTargetException ite) {
                    throw new RuntimeException(ite.getTargetException());
                } catch (IllegalAccessException | InstantiationException e) {
                    logger.info("Unable to coerce value of " + value + " to " + targetType.getName());
                    return null;
                }
            };
        }
        throw new IllegalArgumentException("Unable to find factory method");
    }

    private static Boolean isBetter(String currentName, String otherName,
                                    Parameter current, Parameter other, Map<String, Type> generics) {
        //resolve to actual types
        Type c = current.getParameterizedType();
        Type o = other.getParameterizedType();
        if (c instanceof TypeVariable) {
            c = generics.get(((TypeVariable) c).getName());
        }
        if (o instanceof TypeVariable) {
            o = generics.get(((TypeVariable) o).getName());
        }
        //prefer String
        if (c == String.class) {
            if (o != String.class) {
                return false;
            }
            //prefer valueOf
            if (!currentName.equals("valueOf") && otherName.equals("valueOf")) {
                return true;
            }
        } else {
            if (o == String.class) {
                return true;
            }
            //prefer valueOf
            if (!currentName.equals("valueOf") && otherName.equals("valueOf")) {
                return true;
            }
        }
        return false;
    }
}
