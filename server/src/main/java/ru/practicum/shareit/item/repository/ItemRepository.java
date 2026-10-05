package ru.practicum.shareit.item.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.item.model.Item;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    @Query("""
            select new ru.practicum.shareit.item.repository.RequestItemView(
                i.request.id, i.id, i.name, i.owner.id)
            from Item i where i.request.id in :requestIds order by i.id
            """)
    List<RequestItemView> findAnswers(@Param("requestIds") Collection<Long> requestIds);

    List<Item> findAllByOwnerIdOrderByIdAsc(long ownerId);

    // LOCATE ищет подстроку буквально: '%' и '_' не становятся шаблонами SQL.
    @Query("""
            select i from Item i
            where i.available = true
              and (locate(lower(:text), lower(i.name)) > 0
                   or locate(lower(:text), lower(i.description)) > 0)
            order by i.id
            """)
    List<Item> searchAvailable(@Param("text") String text);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :itemId")
    Optional<Item> findLockedById(@Param("itemId") long itemId);
}
