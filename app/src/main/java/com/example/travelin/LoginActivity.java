package com.example.travelin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
//import android.media.Rating;
import com.example.travelin.MyRating;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmAsyncTask;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.SyncConfiguration;
import io.realm.SyncCredentials;
import io.realm.SyncUser;
import io.realm.ObjectServerError;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.mail.*;
import javax.mail.internet.*;
//import javax.activation.*;



import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    //private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    private Realm realm = null;
    private RealmAsyncTask realmAsyncTask;
    private static SyncConfiguration config;
    private SyncUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder() //
                .name("travelin.realm") //
                .build();
        Realm.setDefaultConfiguration(config);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    try {
                        attemptLogin();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Button pressed");
                try {
                    attemptLogin();
                    System.out.println("HERE3");

                    //Tag tag=new Tag("CHICAGO");
                    //addTagsQuery("ushrivas@purdue.edu",tag);
                }
                catch(Exception e){
                    e.printStackTrace();
                }

            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }


    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() throws InvalidKeySpecException, NoSuchAlgorithmException {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        Map<String,SyncUser> map=SyncUser.all();
        if(map.size()!=0){
            for(Map.Entry<String,SyncUser> entry : map.entrySet()){
                entry.getValue().logOut();
            }
        }
        map=SyncUser.all();

        // Store values at the time of the login attempt.
        final String email = mEmailView.getText().toString();

        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!isPasswordValid(password)) {
            mPasswordView.setError("Login failed: invalid email or password");
            focusView = mPasswordView;
            cancel = true;
        }

        final String hashpassword=generateHash(password);

        // Check for a valid email address.
        if (!isEmailValid(email)) {
            mEmailView.setError("Login failed: invalid email or password");
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        }

        //I'm not entirely sure what this does but the UserLoginTask cannot be used
        else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            //final String authURL = "https://unbranded-metal-bacon.us1a.cloud.realm.io";
            final String authURL = "https://travelin.us1a.cloud.realm.io";
            final SyncCredentials credentials = SyncCredentials.usernamePassword(email, hashpassword, true);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    user = SyncUser.logIn(credentials, authURL);
                    //String url = "realms://unbranded-metal-bacon.us1a.cloud.realm.io/travelin";
                    String url="realms://travelin.us1a.cloud.realm.io/travelin";

                    //this is supposed to create the realm for this user at our specific URL
                    config = user.createConfiguration(url).build();
                    realm = Realm.getInstance(config);

                    //RealmQuery<User> query = realm.where(User.class);
                    //query.equalTo("email", email);
                    //RealmResults<User> results = query.findAll();
                    //User user = results.get(0);
                    System.out.println("REACHED HERE");
                    System.out.println("USER IS:"+user.toString());

                    realm.beginTransaction();
                    User user1 = realm.createObject(User.class, getPK());
                    user1.setEmail(email);
                    user1.setPassword(hashpassword);
                    realm.commitTransaction();
                    System.out.println("HERE2");
                    System.out.println("USER DEETS: "+user1.getEmail());

                    Tag tag=new Tag("CHICAGO");
                    addTagsQuery(user1.getEmail(),tag);

                }
            });

            thread.start();
            //return;

        }
    }

    /**
     * must be purdue email
     */
    private boolean isEmailValid(String email) {
        return email.matches(".*@purdue\\.edu");
    }


    /**
     * password requires a number, special character,
     * upper case letter, lower case letter,
     * must be longer than 4 characters and shorter than 32 characters
     */
    private boolean isPasswordValid(String password) {
        boolean containsNum;
        boolean correctLength = false;
        boolean containsSpecial = true;
        boolean containsUpper;
        boolean containsLower;

        containsNum = password.matches(".*[0-9].*");
        if ((password.length() > 4) && (password.length() < 32)) {
            correctLength = true;
        }
        if (password.matches("[a-zA-Z0-9]*")) {
            containsSpecial = false;
        }
        containsUpper = password.matches(".*[A-Z].*");
        containsLower = password.matches(".*[a-z].*");

        if ((containsNum == true) && (correctLength == true) && (containsSpecial == true)
                && (containsUpper == true) && (containsLower == true)) {
            return true;
        }
        return false;
    }


    public int generatePass() {
        // TODO: write algorithm to generate random password
        return 0;
    }

    /**
     * sends email to user for resetting password
     * @param email
     */
    public void resetPassword(String email) {
        // Recipient's email ID needs to be mentioned.
        String recipient = email;

        // Sender's email ID needs to be mentioned
        String from = "web@gmail.com";

        // Assuming you are sending email from localhost
        String host = "localhost";

        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.setProperty("mail.smtp.host", host);

        // Get the default Session object.
        Session session = Session.getDefaultInstance(properties);

        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

            // Set Subject: header field
            message.setSubject("Temporary Password");

            // Now set the actual message
            message.setText("Below is the temporary password for your travelin account. " +
                    "You may use this password to login and then change your password from your " +
                    "profile settings.\n" + generatePass());

            // Send message
            Transport.send(message);
            System.out.println("Sent message successfully....");
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    //@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }



    /**
     * TODO: move to correct class
     * returns all users whose gender matches the gender
     * in the filter
     */
    public RealmResults<User> genderFilter(String gender) {
        RealmQuery<User> query = realm.where(User.class);
        query.equalTo("gender", gender);

        RealmResults<User> resultGender = query.findAll();
        return resultGender;
    }


    public RealmResults<User> ratingFilter(double rating) {
        RealmQuery<User> query = realm.where(User.class);
        query.between("avgRating",rating,5.0);

        RealmResults<User> resultRatings = query.findAll();
        return resultRatings;
    }


    /**
     * TODO: move to correct class
     * returns the reviews for the user with a given username
     * @param username
     * @return
     */
    public RealmList<MyRating> reviewQuery(String username) {
        RealmQuery<User> query = realm.where(User.class);
        query.equalTo("username", username);

        RealmResults<User> userReviews = query.findAll();
        //return userReviews.get(0).getReviews();
        return userReviews.get(0).getRatings();
    }

    /**
     * TODO: move to correct class
     * returns the ratings for the user with a given username
     * @param username
     * @return
     */
    public double ratingQuery(String username){

        RealmQuery<User> query = realm.where(User.class);
        query.equalTo("username", username);

        RealmResults<User> userReviews = query.findAll();
        return userReviews.get(0).getAvgRating();

    }


    /**
     * TODO: move to correct class
     * algo for adding tags for the user with a given username
     * @param email
     * @return
     */
    public void addTagsQuery(String email,Tag tag){

        RealmQuery<User> query = realm.where(User.class);
        query.equalTo("email", email);

        RealmResults<User> results = query.findAll();
        User user=results.get(0);

        realm.beginTransaction();
        user.addInterest(tag);
        realm.commitTransaction();

        System.out.println("REACHED HERE SUCCESS BISH");
        System.out.println("USERTAGS: "+user.getInterests().toString());

    }

    public void addImg(String email, Bitmap bitmap){
        ByteArrayOutputStream stream=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);

        byte[] byteArray=stream.toByteArray();

        RealmQuery<User> query = realm.where(User.class);
        query.equalTo("email", email);

        RealmResults<User> results = query.findAll();
        User user=results.get(0);

        realm.beginTransaction();
        user.setImg(byteArray);
        realm.commitTransaction();

    }

    public void addProfileImg(String email, Bitmap bitmap){
        ByteArrayOutputStream stream=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);

        byte[] byteArray=stream.toByteArray();

        RealmQuery<User> query = realm.where(User.class);
        query.equalTo("email", email);

        RealmResults<User> results = query.findAll();
        User user=results.get(0);

        realm.beginTransaction();
        user.addProfileImage(byteArray);
        realm.commitTransaction();
    }

    public Bitmap getImg(String email){
        RealmQuery<User> query = realm.where(User.class);
        query.equalTo("email", email);

        RealmResults<User> results = query.findAll();
        User user=results.get(0);

        byte[] byteArray=user.getImg();
        Bitmap bitmap= BitmapFactory.decodeByteArray(byteArray,0,byteArray.length);

        return bitmap;
    }


    public void resetPassManually(String email,String oldPass,String newPass) throws InvalidKeySpecException, NoSuchAlgorithmException {

        RealmQuery<User> query = realm.where(User.class);
        query.equalTo("email", email);

        RealmResults<User> results = query.findAll();
        User user=results.get(0);

        if(!isPasswordValid(newPass)){
            System.out.println("ERRROR: PASSWORD NOT VALID");
        }
        else{
            String hashpassword=generateHash(oldPass);
            if(user.getPassword().equals(hashpassword)){
                String hashpasswordNew=generateHash(newPass);

                realm.beginTransaction();
                user.setPassword(hashpasswordNew);
                realm.commitTransaction();
                System.out.println("Password was changed");
            }
            else{
                System.out.println("Old Password entered was incorrect");
            }

        }

    }

    public RealmList<Tag> getTagsQuery(String email){

        RealmQuery<User> query = realm.where(User.class);
        query.equalTo("email", email);

        RealmResults<User> results = query.findAll();
        User user=results.get(0);

        return user.getInterests();

    }

    private static String generateHash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        int iterations = 1000;
        char[] chars = password.toCharArray();
        byte[] salt = getSalt();

        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return (iterations + ":" + toHex(salt) + ":" + toHex(hash));
    }

    private static byte[] getSalt() throws NoSuchAlgorithmException
    {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }

    private static String toHex(byte[] array) throws NoSuchAlgorithmException
    {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0)
        {
            return String.format("%0"  +paddingLength + "d", 0) + hex;
        }else{
            return hex;
        }
    }

    private static boolean validate(String originalPassword, String storedPassword) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        String[] parts = storedPassword.split(":");
        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = fromHex(parts[1]);
        byte[] hash = fromHex(parts[2]);

        PBEKeySpec spec = new PBEKeySpec(originalPassword.toCharArray(), salt, iterations, hash.length * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] testHash = skf.generateSecret(spec).getEncoded();

        int diff = hash.length ^ testHash.length;
        for(int i = 0; i < hash.length && i < testHash.length; i++)
        {
            diff |= hash[i] ^ testHash[i];
        }
        return diff == 0;
    }

    private static byte[] fromHex(String hex) throws NoSuchAlgorithmException
    {
        byte[] bytes = new byte[hex.length() / 2];
        for(int i = 0; i<bytes.length ;i++)
        {
            bytes[i] = (byte)Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    private int getPK(){

        RealmQuery<User> query = realm.where(User.class);
        query.isNotNull("email");
        RealmResults<User> results = query.findAll();

        String email =results.get(0).getEmail();
        System.out.println("PK: "+email);
        return 4;

    }
}



