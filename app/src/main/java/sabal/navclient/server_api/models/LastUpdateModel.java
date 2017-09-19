package sabal.navclient.server_api.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by sabal202 sabal2000@mail.ru on 19.09.2017.
 */

public class LastUpdateModel {

    @SerializedName("last_update")
    @Expose
    private String lastUpdate;

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

}