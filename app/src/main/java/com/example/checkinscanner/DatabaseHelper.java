package com.example.checkinscanner;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CheckInScanner.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "accounts";

    // Table Columns
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_PASSWORD_SALT = "salt";

    // Table query
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME + "("
            + COLUMN_EMAIL + " TEXT,"
            + COLUMN_PASSWORD + " TEXT,"
            + COLUMN_PASSWORD_SALT + " TEXT" + ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    // Drop all data on db version update
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Get raw data from database
    public Cursor getData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        return res;
    }

    // Insert account credentials into database
    public boolean insertData(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Delete existing records
        db.delete(TABLE_NAME, null, null);

        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_EMAIL, email);
        Random random = new Random();
        String salt = generateSalt(random.nextInt((16-8) + 1) + 8);
        String encryptedPassword = encryptPassword(password, salt);
        if (encryptedPassword != null) {
            contentValues.put(COLUMN_PASSWORD, encryptedPassword);
            contentValues.put(COLUMN_PASSWORD_SALT, salt);
        }
        long result = db.insert(TABLE_NAME, null, contentValues);
        db.close();

        // If data is inserted incorrectly it will return -1
        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }

    // Retrieve account credentials from database
    public ArrayList<String> getAccount() {
        ArrayList<String> account = new ArrayList<>();
        String email = "";
        String password = "";
        String salt = "";
        Cursor cursor = this.getData();
        if (cursor.moveToFirst()) {
            do {
                int emailIndex = cursor.getColumnIndex("email");
                int passwordIndex = cursor.getColumnIndex("password");
                int saltIndex = cursor.getColumnIndex("salt");
                if (emailIndex != -1 || passwordIndex != -1) {
                    email = cursor.getString(emailIndex);
                    salt = cursor.getString(saltIndex);
                    password = decryptPassword(cursor.getString(passwordIndex), salt);
                }
                else {
                    email = cursor.getString(emailIndex);
                    salt = cursor.getString(saltIndex);
                    password = decryptPassword(cursor.getString(passwordIndex), salt);
                }
            }
            while (cursor.moveToNext());
        }
        else {
            email = "";
            password = "";
        }
        cursor.close();
        account.add(email);
        account.add(password);
        return account;
    }

    // Generate encryption key using Android KeyStore
    public void generateKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(new KeyGenParameterSpec.Builder("PasswordKey",
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        } catch (Exception e) {
            Log.e("DatabaseHelper", e.getMessage());
        }
    }

    public String generateSalt(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            stringBuilder.append(characters.charAt(randomIndex));
        }

        return stringBuilder.toString();
    }

    public String encryptPassword(String password, String salt) {
        try {
            // Generate the key
            generateKey();
            // Combine the password and salt
            password = password + salt;
            // Get the key from the keystore
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey("PasswordKey", null);
            // Encrypt the password with Cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] bytes = cipher.doFinal(password.getBytes("UTF-8")); // Encryption operation
            byte[] iv = cipher.getIV(); // Initialization Vector
            return Base64.encodeToString(iv, Base64.DEFAULT) + ":" + Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("DatabaseHelper", e.getMessage());
        }
        return null;
    }

    public String decryptPassword(String encryptedPassword, String salt) {
        try {
            // Retrieve the key from the keystore
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey("PasswordKey", null);
            // Split the encrypted password into initialization vector and ciphertext
            String[] parts = encryptedPassword.split(":");
            // Decode the initialization vector and ciphertext
            byte[] iv = Base64.decode(parts[0], Base64.DEFAULT);
            byte[] cipherText = Base64.decode(parts[1], Base64.DEFAULT);
            // Decrypt the password with Cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] decryptedText = cipher.doFinal(cipherText);
            String password = new String(decryptedText, "UTF-8");
            // Remove the salt from the password
            return password.substring(0, password.length() - salt.length());
        }
        catch (Exception e) {
            Log.e("DatabaseHelper", e.getMessage());
        }
        return null;
    }
}