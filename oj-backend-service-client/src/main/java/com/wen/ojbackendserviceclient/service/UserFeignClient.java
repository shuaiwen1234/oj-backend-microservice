package com.wen.ojbackendserviceclient.service;

import com.wen.ojbackendmodel.model.entity.User;
import com.wen.ojbackendmodel.model.enums.UserRoleEnum;
import com.wen.ojbackendmodel.model.vo.LoginUserVO;
import com.wen.ojbackendmodel.model.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

/**
 * 用户服务
 *这里的path指的是定义的全局路径前缀 即  servlet: context-path: /api/user
 */
@FeignClient(name = "oj-backend-user-service",path = "/api/user/inner")
public interface UserFeignClient {

    /**
     * 获取当前登录用户
     *
     *通过请求头中的cookie获取到session进而获取对应的信息
     * @return
     */
    @GetMapping("/get/login")
    LoginUserVO getLoginUser();

    
    /**
     * 是否为管理员
     *通过请求头中的cookie获取到session进而获取对应的信息
     * @return
     */
    @GetMapping("/isAdmin")
    boolean isAdmin();

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    default boolean isAdmin(User user){
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    default UserVO getUserVO(User user){
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

   /**
    * 根据id获取用户信息
    * @param userId
    * @return
    */
   @GetMapping("/get/user")
   User getById(@RequestParam("userId") Long userId);

   /**
    * 根据多个id获取多个用户信息
    * @param ids
    * @return
    */
   @GetMapping("/get/users")
   List<User> listByIds(@RequestParam("ids") Collection<Long> ids);

    /**
     * 获取用户登录信息
     * @return
     */
    @GetMapping("/get/login/request")
    User getLoginUserByHttpServletRequest();
    

}
