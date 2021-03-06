package com.clanout.application.module.auth.domain.model;

import com.clanout.application.module.user.domain.model.User;

public class RegisteredUser
{
    private User user;
    private boolean isNew;

    public RegisteredUser(User user, boolean isNew)
    {
        this.user = user;
        this.isNew = isNew;
    }

    public String getUserId()
    {
        return user.getUserId();
    }

    public boolean isNew()
    {
        return isNew;
    }
}
