package com.mfon.rmhi.repository;

import com.mfon.rmhi.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
