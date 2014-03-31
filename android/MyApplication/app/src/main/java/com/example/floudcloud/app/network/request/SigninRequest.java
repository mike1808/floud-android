package com.example.floudcloud.app.network.request;

import com.example.floudcloud.app.model.User;
import com.example.floudcloud.app.model.UserSignin;
import com.example.floudcloud.app.network.Floud;
import com.octo.android.robospice.request.retrofit.RetrofitSpiceRequest;

public class SigninRequest extends RetrofitSpiceRequest<User, Floud> {
    private UserSignin user;

    public SigninRequest(String username, String password) {
        super(User.class, Floud.class);

        user = new UserSignin(username, password);
    }

    @Override
    public User loadDataFromNetwork() {
        return getService().singin(user);
    }
}
