package com.example.securing_web.repository;

import com.example.securing_web.entity.PostVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostVoteRepository extends JpaRepository<PostVote, Long> {
    Optional<PostVote> findByUsernameAndPostId(String username, Long postId);
}