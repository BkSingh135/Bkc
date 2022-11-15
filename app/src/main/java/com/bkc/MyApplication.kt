package com.bkc

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

// Application Class
class MyApplication : Application() {
    var appEnvironment: AppEnvironment? = null

    /*
   @Override
   protected void attachBaseContext(Context base) {
       MultiDex.install(base);
       String language = SharedPrefUtils.getInstance().getAppLanguage(base, SharedPrefUtils.SELECTED_LANGUAGE);
       if (language != null)
           super.attachBaseContext(LocaleHelper.onAttach(base, language));
       else
           super.attachBaseContext(LocaleHelper.onAttach(base, LocaleHelper.HINDI));
   }*/
    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        // VideoSDK.initialize(getApplicationContext());
        // Init the Razorpay Sdk
        // Checkout.preload(this);
        //MultiDex.install(this);
        // Init the Google Ads Sdk
        /*MobileAds.initialize(this,new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {

            }
        });*/

//        MobileAds.setRequestConfiguration(
//                new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("E91521E781FBC1DA94D728005D03049B"))
//                        .build());
        context = applicationContext
        instance = this@MyApplication

        //   FirebaseApp.initializeApp(sContext);
        //  FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();


        // The default Realm file is "default.realm" in Context.getFilesDir();
        // we'll change it to "myrealm.realm"
        //Realm.init(this);
        /* RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .name(Realm.DEFAULT_REALM_NAME)
                .schemaVersion(0)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
*/appEnvironment = if (BuildConfig.DEBUG) AppEnvironment.SANDBOX else AppEnvironment.PRODUCTION
        /*     RxJavaPlugins.setErrorHandler(Throwable::printStackTrace);*/
        /* RxJavaPlugins.setErrorHandler { error: Throwable ->
                Timber.e(error) // you can do some task with the Throwable here if you need
        }*/
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
          //  private set
        @SuppressLint("StaticFieldLeak")
        var instance: MyApplication? = null
          //  private set
    }
}