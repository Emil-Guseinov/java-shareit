package ru.practicum.shareit.booking.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Override
    @EntityGraph(attributePaths = {"item", "booker"})
    Optional<Booking> findById(Long id);

    @Query("select b.item.id from Booking b where b.id = :bookingId")
    Optional<Long> findItemId(@Param("bookingId") long bookingId);

    @EntityGraph(attributePaths = {"item", "booker"})
    @Query("""
            select b from Booking b
            where b.booker.id = :userId
              and (:state = 'ALL'
                   or (:state = 'CURRENT' and b.start <= :now and b.end > :now)
                   or (:state = 'PAST' and b.end <= :now)
                   or (:state = 'FUTURE' and b.start > :now)
                   or (:state = 'WAITING' and b.status = :waiting)
                   or (:state = 'REJECTED' and b.status = :rejected))
            order by b.start desc, b.id desc
            """)
    List<Booking> findForBooker(@Param("userId") long userId, @Param("state") String state,
                                @Param("now") LocalDateTime now,
                                @Param("waiting") BookingStatus waiting,
                                @Param("rejected") BookingStatus rejected);

    @EntityGraph(attributePaths = {"item", "booker"})
    @Query("""
            select b from Booking b
            where b.item.owner.id = :userId
              and (:state = 'ALL'
                   or (:state = 'CURRENT' and b.start <= :now and b.end > :now)
                   or (:state = 'PAST' and b.end <= :now)
                   or (:state = 'FUTURE' and b.start > :now)
                   or (:state = 'WAITING' and b.status = :waiting)
                   or (:state = 'REJECTED' and b.status = :rejected))
            order by b.start desc, b.id desc
            """)
    List<Booking> findForOwner(@Param("userId") long userId, @Param("state") String state,
                               @Param("now") LocalDateTime now,
                               @Param("waiting") BookingStatus waiting,
                               @Param("rejected") BookingStatus rejected);

    boolean existsByItemIdAndBookerIdAndStatusAndEndLessThanEqual(
            long itemId, long bookerId, BookingStatus status, LocalDateTime now);

    @Query("""
            select (count(b) > 0) from Booking b
            where b.item.id = :itemId and b.status = :status and b.id <> :bookingId
              and b.start < :end and b.end > :start
            """)
    boolean existsOverlapping(@Param("itemId") long itemId, @Param("bookingId") long bookingId,
                               @Param("status") BookingStatus status,
                               @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Выбираем не более одного бронирования на вещь, не загружая всю историю.
    @Query("""
            select b from Booking b
            where b.item.id in :itemIds and b.status = :status and b.start <= :now
              and not exists (
                select newer.id from Booking newer
                where newer.item.id = b.item.id and newer.status = :status and newer.start <= :now
                  and (newer.start > b.start or (newer.start = b.start and newer.id > b.id))
              )
            """)
    List<Booking> findLastForItems(@Param("itemIds") Collection<Long> itemIds,
                                   @Param("status") BookingStatus status,
                                   @Param("now") LocalDateTime now);

    @Query("""
            select b from Booking b
            where b.item.id in :itemIds and b.status = :status and b.start > :now
              and not exists (
                select earlier.id from Booking earlier
                where earlier.item.id = b.item.id and earlier.status = :status and earlier.start > :now
                  and (earlier.start < b.start or (earlier.start = b.start and earlier.id < b.id))
              )
            """)
    List<Booking> findNextForItems(@Param("itemIds") Collection<Long> itemIds,
                                   @Param("status") BookingStatus status,
                                   @Param("now") LocalDateTime now);
}
