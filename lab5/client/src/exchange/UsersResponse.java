package exchange;

import com.google.gson.annotations.Expose;
import client.User;

import java.util.List;

public class UsersResponse {
    @Expose
    List<User> users;

    public UsersResponse(List<User> users) {
        this.users = users;
    }

    public List<User> getUsers() {
        return users;
    }
}
