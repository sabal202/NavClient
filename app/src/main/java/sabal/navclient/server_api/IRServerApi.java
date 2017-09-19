package sabal.navclient.server_api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import sabal.navclient.server_api.models.CityBeacon;
import sabal.navclient.server_api.models.LastUpdateModel;

/**
 * Created by sabal202 sabal2000@mail.ru on 19.09.2017.
 */

public interface IRServerApi {
    @GET("beacons.json")
    Call<List<CityBeacon>> getAllBeaconsFromCity(@Query("city_id") int id);

    @GET("cities/{id}/check_updates.json")
    Call<LastUpdateModel> getLastUpdateDate(@Path("id") int cityId);


}
