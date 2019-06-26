package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.rmi.ServerError;
import java.util.UUID;

@Service("iUserService")
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUserName(username);
        if (resultCount == 0) {
            return ServerResponse.createByError("用户名不存在");
        }
        String md5Password = MD5Util.MD5EncodeUtf8(password);
        User user = userMapper.selectLogin(username, md5Password);
        if (user == null) {
            return ServerResponse.createByError("密码错误");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登陆成功", user);
    }

    public ServerResponse<String> register(User user) {
        ServerResponse<String> checkValid = checkValid(user.getUsername(), Const.USERNAME);
        if (!checkValid.isSuccess()) {
            return checkValid;
        }
        ServerResponse<String> checkValidE = checkValid(user.getEmail(), Const.EMAIL);
        if (!checkValidE.isSuccess()) {
            return checkValidE;
        }

        user.setRole(Const.Role.ROLE_CUSTOMER);
        //MD5加密
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));

        int resultCount = userMapper.insert(user);
        if (resultCount == 0) {
            return ServerResponse.createByError("注册失败");
        }
        return ServerResponse.createBySuccess("注册成功");
    }

    public ServerResponse<String> checkValid(String str, String type) {
        if (org.apache.commons.lang3.StringUtils.isNotBlank(type)) {
            // 开始校验
            if (Const.USERNAME.equals(type)) {
                int resultCount = userMapper.checkUserName(str);
                if (resultCount > 0) {
                    return ServerResponse.createByError("用户名已存在");
                }
            }
            if (Const.EMAIL.equals(type)) {
                int resultCount = userMapper.checkEmail(str);
                if (resultCount > 0) {
                    return ServerResponse.createByError("email已存在");
                }
            }
        } else {
            return ServerResponse.createByError("参数错误");
        }

        return ServerResponse.createBySuccess("校验成功");
    }

    public ServerResponse<String> selectQuestion(String username) {
        ServerResponse<String> checkValid = checkValid(username, Const.USERNAME);
        if (checkValid.isSuccess()) {
            return ServerResponse.createByError("用户不存在");
        }
        String question = userMapper.selectQuestionByUserName(username);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(question)) {
            return ServerResponse.createBySuccess(question);
        }
        return ServerResponse.createByError("找回密码的问题是空的");
    }

    public ServerResponse<String> checkAnswer(String username, String question, String answer) {
        int resultCount = userMapper.checkAnswer(username, question, answer);
        if (resultCount > 0) {
            // 说明问题、答案都正确
            String forgetToken = UUID.randomUUID().toString();
            TokenCache.setKey(TokenCache.TOKEN_PREFIX + username, forgetToken);
            return ServerResponse.createBySuccess(forgetToken);
        }
        return ServerResponse.createByError("问题的答案错误");
    }
    public ServerResponse<String> forgetResetPassword(String username,String passwordNew,String forgetToken) {
        if (org.apache.commons.lang3.StringUtils.isBlank(forgetToken)) {
            return ServerResponse.createByError("参数错误，需要传递token");
        }
        ServerResponse<String> checkValid = checkValid(username, Const.USERNAME);
        if (checkValid.isSuccess()) {
            return ServerResponse.createByError("用户不存在");
        }
        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX + username);
        if (org.apache.commons.lang3.StringUtils.isBlank(token)) {
            return ServerResponse.createByError("token无效或过期");
        }
        if (org.apache.commons.lang3.StringUtils.equals(forgetToken, token)) {
            String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
            int resultCount = userMapper.updatePassWordByUsername(username, md5Password);
            if (resultCount > 0) {
                return ServerResponse.createBySuccess("修改密码成功");
            }
        } else {
            return ServerResponse.createByError("token错误，请重新获取重置密码的错误");
        }

        return ServerResponse.createByError("修改密码失败");
    }

    public ServerResponse<String> resetPassword(String passwordOld,String passwordNew,User user) {
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld),user.getId());
        if (resultCount == 0) {
            return ServerResponse.createByError("旧密码错误");
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        resultCount = userMapper.updateByPrimaryKeySelective(user);
        if (resultCount > 0) {
            return ServerResponse.createBySuccess("密码更新成功");
        }
        return ServerResponse.createByError("密码更新失败");
    }

    public ServerResponse<User> updateInformation(User user) {
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(), user.getId());
        if (resultCount > 0) {
            return ServerResponse.createByError("email已经存在，请更换email");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());
        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if(updateCount > 0){
            return ServerResponse.createBySuccess("更新个人信息成功",updateUser);
        }
        return ServerResponse.createByError("更新个人信息失败");
    }

    public ServerResponse<User> getInformation(Integer userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            return ServerResponse.createByError("找不到当前用户");
        }
        user.setPassword(org.apache.commons.lang3.StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }





















}
