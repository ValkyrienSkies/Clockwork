package org.valkyrienskies.clockwork.effekseer.client.loader

/**
 * @author ChloePrime
 */
class EffekLoadException : RuntimeException {
    /**
     * Constructs a new runtime exception with `null` as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to [.initCause].
     */
    constructor()

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to [.initCause].
     *
     * @param message the detail message. The detail message is saved for
     * later retrieval by the [.getMessage] method.
     */
    constructor(message: String?) : super(message)

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.
     *
     *Note that the detail message associated with
     * `cause` is *not* automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     * by the [.getMessage] method).
     * @param cause   the cause (which is saved for later retrieval by the
     * [.getCause] method).  (A <tt>null</tt> value is
     * permitted, and indicates that the cause is nonexistent or
     * unknown.)
     * @since 1.4
     */
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    /**
     * Constructs a new runtime exception with the specified cause and a
     * detail message of <tt>(cause==null ? null : cause.toString())</tt>
     * (which typically contains the class and detail message of
     * <tt>cause</tt>).  This constructor is useful for runtime exceptions
     * that are little more than wrappers for other throwables.
     *
     * @param cause the cause (which is saved for later retrieval by the
     * [.getCause] method).  (A <tt>null</tt> value is
     * permitted, and indicates that the cause is nonexistent or
     * unknown.)
     * @since 1.4
     */
    constructor(cause: Throwable?) : super(cause)
}