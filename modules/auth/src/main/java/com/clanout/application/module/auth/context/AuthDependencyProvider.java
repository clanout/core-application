package com.clanout.application.module.auth.context;

import com.clanout.application.framework.di.ModuleScope;
import com.clanout.application.library.async.AsyncPool;
import com.clanout.application.module.auth.data.postgres.PostgresTokenRepository;
import com.clanout.application.module.auth.domain.repository.TokenRepository;
import com.clanout.application.module.user.context.UserContext;
import com.clanout.application.module.user.domain.use_case.CreateUser;
import com.clanout.application.module.user.domain.use_case.FetchUserFromUsername;
import dagger.Module;
import dagger.Provides;

import java.util.concurrent.ExecutorService;

@Module
class AuthDependencyProvider
{
    private UserContext userContext;

    public AuthDependencyProvider(UserContext userContext)
    {
        this.userContext = userContext;
    }

    @Provides
    @ModuleScope
    public ExecutorService backgroundPool()
    {
        return AsyncPool.getInstance().getBackgroundPool();
    }

    @Provides
    @ModuleScope
    public CreateUser provideCreateUser()
    {
        return userContext.createUser();
    }

    @Provides
    @ModuleScope
    public FetchUserFromUsername provideFetchUserFromUsername()
    {
        return userContext.fetchUserFromUsername();
    }

    @Provides
    @ModuleScope
    public TokenRepository provideTokenRepository()
    {
        return new PostgresTokenRepository();
    }
}
