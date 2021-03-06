package com.clanout.application.module.user.context;

import com.clanout.application.framework.di.ModuleScope;
import com.clanout.application.module.user.domain.use_case.*;
import dagger.Component;

@ModuleScope
@Component(modules = UserDependencyProvider.class)
interface UserDependencyInjector
{
    CreateUser createUser();

    FetchUser fetchUser();

    FetchUserFromUsername fetchUserFromUsername();

    AddFriends addFriends();

    FetchFriends fetchFriends();

    UpdateLocation updateLocation();

    UpdateMobile updateMobile();

    BlockFriends blockFriends();

    FetchRegisteredContacts fetchRegisteredContacts();
}
