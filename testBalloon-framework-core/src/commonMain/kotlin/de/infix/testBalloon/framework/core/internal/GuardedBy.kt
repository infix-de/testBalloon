package de.infix.testBalloon.framework.core.internal

/**
 * The property or method to which this annotation is applied can only be accessed when holding
 * particular locks, each of which may be a built-in (synchronization) lock, or an explicit one.
 *
 * The arguments determine which locks guard the annotated property, variable or method:
 *
 * - `"this"`: The string literal `"this"` means that this property is guarded by the class in which it is defined.
 * - `"`_class-name_`.this"`: For inner classes, it may be necessary to disambiguate `this`; the
 *     _class-name_`.this` designation allows you to specify which `this` reference is intended.
 * - `"itself"`: For properties and variables only; the object to which the property or variable refers.
 * - `"`_property-name_`"`: The lock object is referenced by the property specified by _property-name_.
 * - `"`_class-name_`.`_property-name_`"`: The lock object is referenced by the companion object property specified by
 *      _class-name_`.`_property-name_.
 * - `"`_method-name_`()"`: The lock object is returned by calling the named method.
 * - `"`_class-name_`.`_class_`"`: The Class object for the specified class should be used as the lock object.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.SOURCE)
internal annotation class GuardedBy(
    /** The name of the object guarding the target.  */
    vararg val locks: String
)
