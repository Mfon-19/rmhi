package com.mfon.rmhi.infrastructure.persistence;

import com.mfon.rmhi.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
