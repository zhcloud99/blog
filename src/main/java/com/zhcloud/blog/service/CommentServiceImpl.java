package com.zhcloud.blog.service;



import com.zhcloud.blog.dao.CommentRepository;
import com.zhcloud.blog.po.Comment;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Override
    public List<Comment> listCommentByBlogId(Long blogId) {
        Sort sort =  Sort.by("createTime");
        List<Comment> comments = commentRepository.findByBlogIdAndParentCommentNull(blogId,sort);
        return eachComment(comments);  //查询加载所有评论
    }

    @Transactional
    @Override
    public Comment saveComment(Comment comment) {
        Long parentCommentId = comment.getParentComment().getId();
        if (parentCommentId != -1) {
            comment.setParentComment(commentRepository.getOne(parentCommentId));
        } else {         //等于-1为顶级评论，将父级评论id设为null
            comment.setParentComment(null);
        }
        comment.setCreateTime(new Date());
        return commentRepository.save(comment);
    }

    //获取顶级评论（parentid == null）
    private List<Comment> eachComment(List<Comment> comments) {
        //为防止对持久层数据产生影响，复制一份所有顶级评论
        List<Comment> commentsView = new ArrayList<>();
        for (Comment comment : comments) {
            Comment c = new Comment();
            BeanUtils.copyProperties(comment,c);
            commentsView.add(c);
        }
        //此方法最终将顶级评论所有相关回复评论放入到自身的ReplyComments中
        combineChildren(commentsView);
        return commentsView;
    }

    //对顶级评论进行处理
    private List<Comment> tempReplys = new ArrayList<>();
    private void combineChildren(List<Comment> comments) {
        for (Comment comment : comments) {
            //获取顶级评论的下一级评论
            List<Comment> replys1 = comment.getReplyComments();
            //对所有的下级评论做循环
            for(Comment reply1 : replys1) {
                recursively(reply1);
            }

            //将查找到的某一顶级评论的所有相关回复放入自己的ReplyComments中
            comment.setReplyComments(tempReplys);
            //清除临时存放区
            tempReplys = new ArrayList<>();
        }
    }

    //递归获取某一顶级评论下所有相关评论
    private void recursively(Comment comment) {
        tempReplys.add(comment);//顶节点添加到临时存放集合
        if (comment.getReplyComments().size()>0) {
            List<Comment> replys = comment.getReplyComments();
            for (Comment reply : replys) {
                tempReplys.add(reply);
                if (reply.getReplyComments().size()>0) {
                    recursively(reply);
                }
            }
        }
    }
}
