package com.example.securing_web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostVoteRepository voteRepository;

    public void likePost(Long postId, String username) {
        Post post = postRepository.findById(postId).orElseThrow();

        voteRepository.findByUsernameAndPostId(username, postId).ifPresentOrElse(vote -> {
            // пользователь уже голосовал
        }, () -> {
            post.setLikes(post.getLikes() + 1);
            PostVote vote = new PostVote();
            vote.setUsername(username);
            vote.setPost(post);
            vote.setVoteType(VoteType.LIKE);
            voteRepository.save(vote);
            postRepository.save(post);
        });
    }

    public void dislikePost(Long postId, String username) {
        Post post = postRepository.findById(postId).orElseThrow();

        voteRepository.findByUsernameAndPostId(username, postId).ifPresentOrElse(vote -> {
            // пользователь уже голосовал
        }, () -> {
            post.setDislikes(post.getDislikes() + 1);
            PostVote vote = new PostVote();
            vote.setUsername(username);
            vote.setPost(post);
            vote.setVoteType(VoteType.DISLIKE);
            voteRepository.save(vote);
            postRepository.save(post);
        });
    }
}