package org.sopt.server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.sopt.server.dto.Comment;
import org.sopt.server.dto.CommentLike;
import org.sopt.server.dto.Content;
import org.sopt.server.dto.ContentLike;
import org.sopt.server.mapper.CommentLikeMapper;
import org.sopt.server.mapper.CommentMapper;
import org.sopt.server.model.DefaultRes;
import org.sopt.server.service.CommentService;
import org.sopt.server.service.ContentService;
import org.sopt.server.utils.ResponseMessage;
import org.sopt.server.utils.StatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;

/**
 * Created by ds on 2018-10-23.
 */

@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final ContentService contentService;

    public CommentServiceImpl(final CommentMapper commentMapper, final CommentLikeMapper commentLikeMapper, final ContentService contentService) {
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.contentService = contentService;
    }

    /**
     * 글에 작성된 댓글 조회
     * 각 댓글마다 좋아요 여부 표시 추가
     *
     * @param contentIdx 게시글 고유 번호
     * @return 댓글 리스트
     */
    @Override
    public DefaultRes<List<Comment>> findByContentIdx(final int contentIdx) {
        List<Comment> commentList = commentMapper.findAllByContentIdx(contentIdx);
        if (commentList.isEmpty()) return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_COMMENT);
        return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_ALL_COMMENTS, commentList);
    }

    /**
     * 댓글 상세 조회
     *
     * @param commentIdx 댓글 고유 번호
     * @return 댓글
     */
    @Override
    public DefaultRes<Comment> findByCommentIdx(final int commentIdx) {
        final Comment comment = commentMapper.findByCommentIdx(commentIdx);
        if (comment == null) return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_COMMENT);
        //if (comment.getU_id() == auth) comment.setAuth(true);
        CommentLike commentLike;
        return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_COMMENT, comment);
    }

    /**
     * 댓글 작성
     *
     * @param comment 댓글
     * @return DefaultRes
     */
    @Transactional
    @Override
    public DefaultRes save(final Comment comment) {
        if (contentService.findByContentIdx(comment.getB_id()).getData() == null)
            return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_CONTENT);
        try {
            commentMapper.save(comment);
            return DefaultRes.res(StatusCode.CREATED, ResponseMessage.CREATE_COMMENT);
        } catch (Exception e) {
            log.error(e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    /**
     * 댓글 좋아요
     *
     * @param auth
     * @param commentIdx
     * @return DefaultRes
     */
    @Transactional
    @Override
    public DefaultRes likes(final int userIdx, final int commentIdx) {
        Comment comment = commentMapper.findByCommentIdx(commentIdx);
        if (comment == null) return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_CONTENT);

        CommentLike commentLike = commentLikeMapper.findByUserIdxAndCommentIdx(userIdx, commentIdx);

        try {
            if (commentLike == null) {
                comment.likes();
                commentMapper.like(commentIdx, comment.getC_like());
                commentLikeMapper.save(userIdx, commentIdx);
            } else {
                comment.unLikes();
                commentMapper.like(commentIdx, comment.getC_like());
                commentLikeMapper.deleteByUserIdxAndCommentIdx(userIdx, commentIdx);
            }
            comment = findByCommentIdx(commentIdx).getData();
            return DefaultRes.res(StatusCode.OK, ResponseMessage.LIKE_COMMENT, comment);
        } catch (Exception e) {
            log.error(e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    /**
     * 댓글 수정
     *
     * @param comment    댓글 내용
     * @return DefaultRes
     */
    @Transactional
    @Override
    public DefaultRes<Comment> update(final Comment comment) {
        Comment temp = commentMapper.findByCommentIdx(comment.getC_id());
        if (temp == null)
            return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_COMMENT);

        try {
            commentMapper.updateByCommentIdx(comment);
            temp = commentMapper.findByCommentIdx(comment.getC_id());
            temp.setAuth(true);
            return DefaultRes.res(StatusCode.OK, ResponseMessage.UPDATE_COMMENT, temp);
        } catch (Exception e) {
            log.error(e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    /**
     * 댓글 삭제
     *
     * @param commentIdx 댓글 고유 번호
     * @return DefaultRes
     */
    @Transactional
    @Override
    public DefaultRes deleteByCommentIdx(final int commentIdx) {
        final Comment comment = commentMapper.findByCommentIdx(commentIdx);
        if (comment == null)
            return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_COMMENT);

        try {
            commentMapper.deleteByConmmentIdx(commentIdx);
            return DefaultRes.res(StatusCode.NO_CONTENT, ResponseMessage.DELETE_COMMENT);
        } catch (Exception e) {
            log.error(e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    /**
     * 댓글 권한 확인
     *
     * @param userIdx    사용자 고유 번호
     * @param commentIdx 댓글 고유 번호
     * @return boolean
     */
    @Override
    public boolean checkAuth(final int userIdx, final int commentIdx) {
        return userIdx == findByCommentIdx(commentIdx).getData().getU_id();
    }

    @Override
    public boolean checkLike(final int userIdx, final int commentIdx) {
        return commentLikeMapper.findByUserIdxAndCommentIdx(userIdx, commentIdx) != null;
    }
}
