package com.example.learnlybacked;

import jakarta.transaction.Transactional;
import org.hibernate.sql.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserLoginDTO, Long> {
    


    @Query("SELECT count(u.userEmail) FROM UserLoginDTO u WHERE u.userEmail = :email")
    int findByCustomEmail(@Param("email") String email);

    @Query("SELECT count(u.userNameAndSurname) FROM UserLoginDTO u WHERE u.userNameAndSurname = :username")
    int findUserByUsername(@Param("username") String username);

    @Query("SELECT u.id FROM UserLoginDTO u WHERE u.userNameAndSurname = :username")
    Long getUserIdByUsername(@Param("username") String username);

    @Query("SELECT u.password ,u.userNameAndSurname from UserLoginDTO u where u.userEmail=:email")
    List<Object[]> findPasswordMatchEmail(@Param("email") String email);

    @Query("SELECT u from UserLoginDTO u where u.userNameAndSurname = :username")
    UserLoginDTO takeAllDataByName(@Param("username") String username);

    @Query("SELECT u.userNameAndSurname,u.bio,u.likes,u.materials,u.accountType from UserLoginDTO u where u.userNameAndSurname = :username")
    UserProfileController.UserProfileData takeUserProfileDataByUsername(@Param("username") String username);

    @Modifying
    @Transactional
    @Query("UPDATE UserLoginDTO u SET u.userNameAndSurname = :name WHERE u.id = :id")
    void updateUsername(@Param("id") Long id, @Param("name") String name);


    @Modifying
    @Transactional
    @Query("UPDATE UserLoginDTO u SET u.bio = :bio WHERE u.id = :id")
    void updateUserBio(@Param("id") Long id, @Param("bio") String bio);



   //Lajkowanie ststystyki!!!!
    @Modifying
    @Transactional
    @Query("UPDATE UserLoginDTO u set u.likes = u.likes + 1 where u.id = :id")
    void giveLike(@Param("id") Long id);



    @Modifying
    @Transactional
    @Query("UPDATE UserLoginDTO u set u.likes = u.likes - 1 where u.id = :id")
    void takeAwayLike(@Param("id") Long id);


}