package sabal.navclient;

import android.content.Context;

import io.realm.Realm;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import sabal.navclient.persistance.PreferenceManager;
import sabal.navclient.server_api.IRServerApi;

/**
 * Created by sabal202 sabal2000@mail.ru on 19.09.2017.
 */

public class MyApplication extends android.app.Application {

    private static IRServerApi irServerApi;
    private static Context context;
    private Retrofit retrofit;

    public static IRServerApi getApi() {
        return irServerApi;
    }

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        retrofit = new Retrofit.Builder()
                .baseUrl("https://saval-server.herokuapp.com/") //Базовая часть адреса
                .addConverterFactory(GsonConverterFactory.create()) //Конвертер, необходимый для преобразования JSON'а в объекты
                .build();
        irServerApi = retrofit.create(IRServerApi.class); //Создаем объект, при помощи которого будем выполнять запросы

        PreferenceManager.with(getApplicationContext());
        context = this;
        Realm.init(this);
    }
}