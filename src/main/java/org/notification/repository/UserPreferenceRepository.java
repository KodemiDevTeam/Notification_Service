package org.notification.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

import org.notification.model.UserPreference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserPreferenceRepository {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;


    // ✅ SAVE
    public UserPreference save(UserPreference pref) {

        dynamoDBMapper.save(pref);

        return pref;
    }


    // ✅ FIND BY USER ID (PRIMARY KEY)

    public UserPreference findByUserId(String userId) {

        return dynamoDBMapper.load(
                UserPreference.class,
                userId);
    }


    // ✅ OPTIONAL METHOD (same as find)

    public UserPreference getByUserId(String userId) {

        return dynamoDBMapper.load(
                UserPreference.class,
                userId);
    }

}