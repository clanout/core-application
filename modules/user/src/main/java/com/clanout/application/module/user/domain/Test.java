package com.clanout.application.module.user.domain;

import com.clanout.application.library.postgres.PostgresDataSource;
import com.clanout.application.library.util.gson.GsonProvider;
import com.clanout.application.module.user.data.user.PostgresUserRepository;
import com.clanout.application.module.user.domain.model.User;
import com.clanout.application.module.user.domain.repository.UserRepository;
import com.clanout.application.module.user.domain.service.UserService;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;

public class Test
{
    public static void main(String[] args) throws Exception
    {
        System.out.println(new UserService().generateUserId());

//        GsonProvider.init();
//
//        Gson gson = GsonProvider.getGsonBuilder().setPrettyPrinting().create();
//
//        PostgresDataSource.getInstance().init();
//
//        UserRepository userRepository = new PostgresUserRepository();
////        User aditya = userRepository.fetch("9276fdbb-df34-44a6-93b4-f26147738227");
////        User harsh = userRepository.fetch("b08f854b-91ef-4871-8cc4-233c13dd4504");
//
////        userRepository.block(aditya.getUserId(), Arrays.asList(harsh.getUserId()));
////        userRepository.unblock(aditya.getUserId(), Arrays.asList(harsh.getUserId()));
//
//        System.out.println(gson.toJson(userRepository.fetchRegisteredContacts(Arrays.asList("95efca796914f28265610490ea97c351", "12"))));
//
//        PostgresDataSource.getInstance().close();
    }
}
