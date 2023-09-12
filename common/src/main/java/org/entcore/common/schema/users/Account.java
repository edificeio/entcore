package org.entcore.common.schema.users;

import org.entcore.common.json.JSONAble;
import org.entcore.common.json.JSONDefault;
import org.entcore.common.json.JSONIgnore;

import java.util.Set;
import java.util.HashSet;

public class Account implements JSONAble
{
    @JSONIgnore
    private final Set<User> users = new HashSet<User>();

    private String login;
    private String loginAlias;
    private String password;
    private String activationCode;
    private String resetCode;

    @JSONDefault("false")
    private Boolean blocked;

    public Account(User... users)
    {
        for(User u : users)
            this.add(u);
    }

    public boolean add(User u)
    {
        boolean wasPresent = users.add(u);
        if(wasPresent == false)
            u.setAccount(this);
        return wasPresent;
    }

    public boolean remove(User u)
    {
        boolean wasPresent = users.remove(u);
        if(wasPresent == true)
            u.setAccount(null);
        return wasPresent;
    }
}
