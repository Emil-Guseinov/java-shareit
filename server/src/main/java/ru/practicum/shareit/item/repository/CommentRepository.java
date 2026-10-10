package ru.practicum.shareit.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.item.Comment;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("""
            select c from Comment c join fetch c.author
            where c.item.id in :itemIds
            order by c.created, c.id
            """)
    List<Comment> findForItems(@Param("itemIds") Collection<Long> itemIds);
}
