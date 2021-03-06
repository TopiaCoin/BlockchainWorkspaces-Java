package io.topiacoin.chunks.intf;

public interface ReservationID {

    /**
     * Returns the current validity of this reservation.  A reservation is valid if and only if, it has not expired and
     * all of its reserved storage has not been consumed.
     *
     * @return True if the reservation is valid.  False if the reservation is not valid.
     */
    boolean isValid();

    /**
     * Returns the time when this reservation will expire.  This time may change as the reservation is used.  Expiration
     * is based on elapsed time since last use of the reservation.  If a reservation sits idle, it will expire faster
     * than one that is being actively used to store data.
     *
     * @return The time when this reservation is currently set to expire, in milliseconds since the epoch.
     */
    long getExpirationTime();

    /**
     * Returns the amount of storage space this reservation reserved.
     *
     * @return The amount of storage space reserved by this reservation.
     */
    long getReservedSpace();

    /**
     * Returns the amount of reserved space remaining in this reservation.  This amount is decremented everytime a chunk
     * is added tot he chunk storage against this reservation.
     *
     * @return The amount of reserved storage space remaining.
     */
    long getRemainingSpace();
}
