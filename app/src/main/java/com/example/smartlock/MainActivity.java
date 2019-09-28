package com.example.smartlock;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.media.Image;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;


public class MainActivity extends AppCompatActivity {
    private static final String KEY_NAME = "yourKey";
    private Cipher cipher;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private TextView textView;
    private FingerprintManager.CryptoObject cryptoObject;
    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;
    private FingerprintHandler helper;
    private RequestQueue queue;
    private String url;
    private StringRequest stringRequest;
    private Boolean is_locked = false;
    private SharedPreferences sharedPref;
    public static final String mypreference = "mypref";
    private TextView lock;
    private LinearLayout main_layout;
    private ImageView lock_image;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


         lock = findViewById(R.id.lock_status);
        main_layout = findViewById(R.id.main_layout);
        lock_image = findViewById(R.id.lock_icon);

        sharedPref = getSharedPreferences(mypreference,Context.MODE_PRIVATE);
        is_locked = sharedPref.getBoolean("state",false);
        Log.i("ONCREATE", "onCreate: "+is_locked.toString());
        if (sharedPref.contains("state")){
            Log.i("YES", "onCreate: Contains state");
            if (!is_locked){
                main_layout.setBackgroundResource(R.color.unlocked_screen);
                lock.setText("  Lock  ");
                lock_image.setImageResource(R.drawable.ic_group_2);
                is_locked = false;
            }else{
                main_layout.setBackgroundResource(R.color.locked_screen);
                lock.setText(" Unlock");
                lock_image.setImageResource(R.drawable.ic_group_1);
                is_locked = true;
            }
        }

        queue = Volley.newRequestQueue(this);





        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

        if (!fingerprintManager.isHardwareDetected()) {
            // If a fingerprint sensor isn’t available, then inform the user that they’ll be unable to use your app’s fingerprint functionality//
            textView.setText("Your device doesn't support fingerprint authentication");
        }
        //Check whether the user has granted your app the USE_FINGERPRINT permission//
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            // If your app doesn't have this permission, then display the following text//
            textView.setText("Please enable the fingerprint permission");
        }

        //Check that the user has registered at least one fingerprint//
        if (!fingerprintManager.hasEnrolledFingerprints()) {
            // If the user hasn’t configured any fingerprints, then display the following message//
            textView.setText("No fingerprint configured. Please register at least one fingerprint in your device's Settings");
        }

        if (!keyguardManager.isKeyguardSecure()) {
            // If the user hasn’t secured their lockscreen with a PIN password or pattern, then display the following text//
            textView.setText("Please enable lockscreen security in your device's Settings");
        } else {
            try {
                generateKey();
            } catch (FingerprintException e) {
                e.printStackTrace();
            }

            if (initCipher()) {
                //If the cipher is initialized successfully, then create a CryptoObject instance//
                cryptoObject = new FingerprintManager.CryptoObject(cipher);

                // Here, I’m referencing the FingerprintHandler class that we’ll create in the next section. This class will be responsible
                // for starting the authentication process (via the startAuth method) and processing the authentication process events//
                 helper = new FingerprintHandler(this);

            }
        }
        lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MainActivity", "onClick: "+lock.getText());
                if (lock.getText()==" Unlock"){
                    helper.startAuth(fingerprintManager, cryptoObject);
                    if (helper.getAuthStatus()){
                        url ="";//Thingspeak url to update value to 0.
                        makeStringRequest(url,0);
                        queue.add(stringRequest);


                    }else{
                        Toast.makeText(getApplicationContext(),"Unlock failed",Toast.LENGTH_SHORT).show();
                    }

                }else{
                    helper.startAuth(fingerprintManager, cryptoObject);
                    if (helper.getAuthStatus()){
                        url ="";//Thingspeak URL to update value to 1.
                        makeStringRequest(url,1);
                        queue.add(stringRequest);


                    }else{
                        Toast.makeText(getApplicationContext(),"Unlock failed",Toast.LENGTH_SHORT).show();
                    }

                }


            }});
    }
    public void makeStringRequest(String url, final int inte){
        stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onResponse(String response) {
                        if (Integer.parseInt(response)==0){
                            Toast.makeText(getApplicationContext(),"Thinkspeak error", Toast.LENGTH_LONG);
                        }else{
                            Log.i("String request", "onResponse: Response successful "+response);
                            if (inte==0){
                                main_layout.setBackgroundResource(R.color.unlocked_screen);
                                lock.setText("  Lock  ");
                                lock_image.setImageResource(R.drawable.ic_group_2);
                                helper.resetAuthStatus();
                                is_locked = false;
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putBoolean("state",is_locked);
                                editor.commit();
                                Log.i("ONCLICK", "onClick: "+is_locked.toString());
                        }else if (inte==1){
                                main_layout.setBackgroundResource(R.color.locked_screen);
                                lock.setText(" Unlock");
                                lock_image.setImageResource(R.drawable.ic_group_1);
                                helper.resetAuthStatus();
                                is_locked = true;
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putBoolean("state",is_locked);
                                editor.commit();
                                Log.i("ONCLICK", "onClick: "+is_locked.toString());
                            }

                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(),"Couldnt write to cloud",Toast.LENGTH_SHORT);
            }
        });
    }
//Create the generateKey method that we’ll use to gain access to the Android keystore and generate the encryption key//

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void generateKey() throws FingerprintException {
        try {
            // Obtain a reference to the Keystore using the standard Android keystore container identifier (“AndroidKeystore”)//
            keyStore = KeyStore.getInstance("AndroidKeyStore");

            //Generate the key//
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            //Initialize an empty KeyStore//
            keyStore.load(null);

            //Initialize the KeyGenerator//
            keyGenerator.init(new

                    //Specify the operation(s) this key can be used for//
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)

                    //Configure this key so that the user has to confirm their identity with a fingerprint each time they want to use it//
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());

            //Generate the key//
            keyGenerator.generateKey();

        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException
                | CertificateException
                | IOException exc) {
            exc.printStackTrace();
            throw new FingerprintException(exc);
        }
    }

    //Create a new method that we’ll use to initialize our cipher//
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean initCipher() {
        try {
            //Obtain a cipher instance and configure it with the properties required for fingerprint authentication//
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            //Return true if the cipher has been initialized successfully//
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {

            //Return false if cipher initialization failed//
            return false;
        } catch (KeyStoreException | CertificateException
                | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    private class FingerprintException extends Exception {
        public FingerprintException(Exception e) {
            super(e);
        }


    }

}