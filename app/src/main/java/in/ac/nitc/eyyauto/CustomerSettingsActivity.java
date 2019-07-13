package in.ac.nitc.eyyauto;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

import in.ac.nitc.eyyauto.models.User;

import static in.ac.nitc.eyyauto.Constants.CUSTOMER_INFO_ROOT_PATH;
import static in.ac.nitc.eyyauto.Constants.USER_INFO_ROOT_PATH;

public class CustomerSettingsActivity extends AppCompatActivity {

    private EditText mNameField;
    private Button mBack,mConfirm;

    private FirebaseAuth mAuth;
    private DatabaseReference mCustomerDatabase;
    private String mName;
    private String mUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);
        mNameField= findViewById(R.id.customer_name_edit);
        mConfirm=findViewById(R.id.confirm_changes);
        mBack=findViewById(R.id.back_to_map);

        mAuth=FirebaseAuth.getInstance();
        mUserId=mAuth.getCurrentUser().getUid();
        mCustomerDatabase=FirebaseDatabase.getInstance().getReference(USER_INFO_ROOT_PATH).child("Customers").child(mUserId);

        getUserInfo();

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInfo();
                Toast.makeText(CustomerSettingsActivity.this, "Customer User Name Updated", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });


    }

    private void saveUserInfo() {
        mName=mNameField.getText().toString();
        mCustomerDatabase.child("name").removeValue();
        mCustomerDatabase.child("name").setValue(mName);

    }

    private void getUserInfo() {

        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        mName = map.get("name").toString();
                        mNameField.setText(mName);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



    }

}
