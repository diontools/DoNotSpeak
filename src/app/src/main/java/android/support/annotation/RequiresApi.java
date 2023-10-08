package android.support.annotation;

public @interface RequiresApi {
    int value() default 1;
    int api() default 1;
}
