package in.ac.nitc.eyyauto.handlers;

import androidx.annotation.NonNull;

import in.ac.nitc.eyyauto.models.User;

import static in.ac.nitc.eyyauto.Constants.DRIVER_INFO_ROOT_PATH;
import static in.ac.nitc.eyyauto.Constants.USER_INFO_ROOT_PATH;

public final class DriverHandler extends DatabaseHandler<User> {

    @Override
    public void readOnce(@NonNull String uid, @NonNull Event<User> event) {
        String path = USER_INFO_ROOT_PATH + DRIVER_INFO_ROOT_PATH + uid;
        getDataOnce(path, event);
    }

    @Override
    public void putValue(@NonNull String uid, @NonNull User data) {
        String path = USER_INFO_ROOT_PATH + DRIVER_INFO_ROOT_PATH + uid;
        putData(path, data);
    }
}
