package com.example.securing_web.service;

import com.example.securing_web.entity.Comment;
import com.example.securing_web.entity.Post;
import com.example.securing_web.repository.CommentRepository;
import com.example.securing_web.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public Comment addComment(Long postId, String author, String content) {
        return addComment(postId, null, author, content);
    }

    @Transactional
    public Comment addReply(Long parentCommentId, String author, String content) {
        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new IllegalArgumentException("Родительский комментарий не найден"));

        return addComment(parentComment.getPost().getId(), parentCommentId, author, content);
    }

    private Comment addComment(Long postId, Long parentCommentId, String author, String content) {
        // 1. Находим пост
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Пост не найден"));

        // 2. Проверяем входные данные
        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя автора не может быть пустым");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Комментарий не может быть пустым");
        }

        // 3. Экранируем HTML и обрезаем
        String safeAuthor = escapeHtml(author.trim());
        String safeContent = escapeHtml(content.trim());

        // 4. Проверяем длину
        if (safeContent.length() > 2000) {
            throw new IllegalArgumentException("Комментарий слишком длинный (максимум 2000 символов)");
        }

        if (safeAuthor.length() > 100) {
            safeAuthor = safeAuthor.substring(0, 100);
        }

        // 5. Создаем комментарий
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(safeAuthor);
        comment.setContent(safeContent);

        // 6. Если это ответ, устанавливаем родительский комментарий
        if (parentCommentId != null) {
            Comment parentComment = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new IllegalArgumentException("Родительский комментарий не найден"));
            comment.setParentComment(parentComment);
        }

        return commentRepository.save(comment);
    }

    public List<Comment> getCommentsByPostId(Long postId) {
        if (postId == null) {
            return new ArrayList<>();
        }
        List<Comment> comments = commentRepository.findByPostId(postId);
        return comments != null ? comments : new ArrayList<>();
    }

    public List<Comment> getCommentTreeByPostId(Long postId) {
        if (postId == null) {
            System.out.println("ERROR: Post ID is null");
            return new ArrayList<>();
        }

        System.out.println("DEBUG: Loading comment tree for post " + postId);

        // 1. Получаем корневые комментарии (без родителей)
        List<Comment> rootComments = commentRepository.findRootCommentsByPostIdOrdered(postId);

        System.out.println("DEBUG: Found " + (rootComments != null ? rootComments.size() : "null") +
                " root comments");

        if (rootComments == null || rootComments.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Для каждого корневого комментария загружаем ответы
        for (Comment rootComment : rootComments) {
            if (rootComment != null) {
                // Инициализируем коллекцию, если она null
                if (rootComment.getReplies() == null) {
                    rootComment.setReplies(new ArrayList<>());
                }
                loadRepliesRecursively(rootComment);

                System.out.println("DEBUG: Comment " + rootComment.getId() +
                        " has " + rootComment.getReplies().size() + " replies");
            }
        }

        return rootComments;
    }

    private void loadRepliesRecursively(Comment comment) {
        if (comment == null || comment.getId() == null) {
            return;
        }

        List<Comment> replies = commentRepository.findRepliesByParentIdOrdered(comment.getId());

        if (replies != null && !replies.isEmpty()) {
            comment.setReplies(replies);

            for (Comment reply : replies) {
                if (reply != null) {
                    loadRepliesRecursively(reply);
                }
            }
        }
    }

    public String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Недавно";
        }

        LocalDateTime now = LocalDateTime.now();

        // Сегодня
        if (dateTime.toLocalDate().equals(now.toLocalDate())) {
            return "сегодня в " + dateTime.format(TIME_FORMATTER);
        }

        // Вчера
        if (dateTime.toLocalDate().equals(now.minusDays(1).toLocalDate())) {
            return "вчера в " + dateTime.format(TIME_FORMATTER);
        }

        // В этом году
        if (dateTime.getYear() == now.getYear()) {
            return dateTime.format(DateTimeFormatter.ofPattern("dd.MM в HH:mm"));
        }

        // Другая дата
        return dateTime.format(DATE_FORMATTER);
    }

    // ★ ДОБАВЛЕНО: метод для форматирования дат постов ★
    public String formatPostDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Недавно";
        }

        LocalDateTime now = LocalDateTime.now();

        // Менее 1 часа назад
        if (dateTime.isAfter(now.minusHours(1))) {
            long minutes = java.time.Duration.between(dateTime, now).toMinutes();
            return minutes + " минут назад";
        }

        // Сегодня
        if (dateTime.toLocalDate().equals(now.toLocalDate())) {
            return "сегодня в " + dateTime.format(TIME_FORMATTER);
        }

        // Вчера
        if (dateTime.toLocalDate().equals(now.minusDays(1).toLocalDate())) {
            return "вчера в " + dateTime.format(TIME_FORMATTER);
        }

        // В этом году
        if (dateTime.getYear() == now.getYear()) {
            return dateTime.format(DateTimeFormatter.ofPattern("dd MMM в HH:mm"));
        }

        // Другая дата
        return dateTime.format(DATE_FORMATTER);
    }

    // ★ ДОБАВЛЕНО: метод для безопасного получения первой буквы имени ★
    public String getFirstLetter(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }

        String trimmed = name.trim();
        return trimmed.substring(0, 1).toUpperCase();
    }

    // ★ ДОБАВЛЕНО: метод для безопасного получения имени автора ★
    public String getSafeAuthorName(String author) {
        if (author == null || author.trim().isEmpty()) {
            return "Аноним";
        }
        return author;
    }

    // ★ ДОБАВЛЕНО: метод для безопасного получения содержания ★
    public String getSafeContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "Нет текста";
        }
        return content;
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    // ★ ДОБАВЛЕНО: метод для подсчета всех комментариев к посту ★
    public int countTotalComments(Long postId) {
        if (postId == null) {
            return 0;
        }
        List<Comment> allComments = commentRepository.findByPostId(postId);
        return allComments != null ? allComments.size() : 0;
    }

    // ★ ДОБАВЛЕНО: метод для подсчета корневых комментариев ★
    public int countRootComments(Long postId) {
        if (postId == null) {
            return 0;
        }
        List<Comment> rootComments = commentRepository.findRootCommentsByPostIdOrdered(postId);
        return rootComments != null ? rootComments.size() : 0;
    }

    // ★ ДОБАВЛЕНО: метод для получения последних комментариев ★
    public List<Comment> getRecentComments(int limit) {
        List<Comment> allComments = commentRepository.findAll();

        if (allComments == null || allComments.isEmpty()) {
            return new ArrayList<>();
        }

        // Сортируем по дате создания (новые сначала)
        allComments.sort((c1, c2) -> {
            LocalDateTime date1 = c1.getCreatedAt() != null ? c1.getCreatedAt() : LocalDateTime.MIN;
            LocalDateTime date2 = c2.getCreatedAt() != null ? c2.getCreatedAt() : LocalDateTime.MIN;
            return date2.compareTo(date1);
        });

        // Возвращаем не более limit комментариев
        return allComments.size() > limit ? allComments.subList(0, limit) : allComments;
    }

    // ★ ДОБАВЛЕНО: метод для проверки существования комментария ★
    public boolean commentExists(Long commentId) {
        if (commentId == null) {
            return false;
        }
        return commentRepository.existsById(commentId);
    }

    // ★ ДОБАВЛЕНО: метод для получения комментария с проверкой ★
    public Comment getCommentSafe(Long commentId) {
        if (commentId == null) {
            return null;
        }
        return commentRepository.findById(commentId).orElse(null);
    }
}