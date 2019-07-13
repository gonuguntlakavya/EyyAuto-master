package in.ac.nitc.eyyauto;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

import in.ac.nitc.eyyauto.handlers.CustomerHandler;
import in.ac.nitc.eyyauto.handlers.DriverHandler;
import in.ac.nitc.eyyauto.models.User;

import static in.ac.nitc.eyyauto.Constants.INTENT_HAS_PHONE_NUMBER;
import static in.ac.nitc.eyyauto.Constants.INTENT_USER;

public class MainActivity extends AppCompatActivity {

    private static int RC_SIGN_IN = 123;
    private FirebaseUser mUser;
    private CustomerHandler mCustomerHandler;
    private DriverHandler mDriverHandler;

    private EditText mNameField;
    private String mUserId;
    private Boolean hasPhoneNumber;
    private User user;
    private RadioGroup mGroup;
    private RadioButton mCustomer;
    private RadioButton mDriver;
    private Button mConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set view according to need
        hasPhoneNumber = getIntent().getBooleanExtra(INTENT_HAS_PHONE_NUMBER, false);
        mCustomerHandler = new CustomerHandler();
        mDriverHandler = new DriverHandler();
        if(!hasPhoneNumber) {
            setContentView(R.layout.activity_main);
            signIn();
        } else {
            setDetailsView();
        }
    }

    private void signIn() {
        /* TODO: Custom theme for FirebaseUI here */
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build());
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                setDetailsView();
            } else {
                if (response == null) {
                    Toast.makeText(this, "Sign in Cancelled", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(this, "Error trying to Sign in", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setDetailsView() {
        setContentView(R.layout.personal_details);
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserId = mUser.getUid();
        mNameField = findViewById(R.id.name);
        mConfirm = findViewById(R.id.confirmButton);
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInformation();
            }
        });
    }

    private void saveUserInformation() {
        String mName = mNameField.getText().toString();
        mGroup = findViewById(R.id.myGroup);
        mCustomer = findViewById(R.id.customer);
        mDriver = findViewById(R.id.driver);
        if (mName.isEmpty() || mGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(MainActivity.this,"Please fill all the details",Toast.LENGTH_SHORT).show();
            return;
        }
        // switch to customer maps activity here
        if(mCustomer.isChecked()) {
            user = new User(mName, mUser.getPhoneNumber());
            mCustomerHandler.putValue(mUserId, user);
            Toast.makeText(this, "User Registered successfully", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(MainActivity.this, CustomerMapActivity.class);
            i.putExtra(INTENT_USER, user);
            startActivity(i);
            finish();
        }
        if(mDriver.isChecked()){
            user = new User(mName, mUser.getPhoneNumber());
            mDriverHandler.putValue(mUserId, user);
            //checkDriverInformation();
            Intent i = new Intent(MainActivity.this, DriverMapActivity.class);
            i.putExtra(INTENT_USER, user);
            startActivity(i);
            finish();
        }
    }

    private void checkDriverInformation(){

        //TODO: have to check whether driver is registered or not

        Toast.makeText(this, "Driver Registered successfully", Toast.LENGTH_SHORT).show();

    }
}
