package com.se1.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.se1.dao.DatabaseOperation;
import com.se1.dao.User;
import com.se1.main.R;

public class RegisterUserActivity extends SherlockFragmentActivity {
    private DatabaseOperation datasource;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);
        getSupportActionBar().setTitle(R.string.title_activity_register_user);
        datasource = new DatabaseOperation(this);
        datasource.open();
        Button registerUser = (Button)findViewById(R.id.registerUser);
        final EditText password   = (EditText)findViewById(R.id.password);
        final EditText firstName   = (EditText)findViewById(R.id.firstNameId);
        final EditText lastName   = (EditText)findViewById(R.id.lastNameId);
        final EditText email   = (EditText)findViewById(R.id.email);
        final CheckBox loggedIn=(CheckBox)findViewById(R.id.loggedIn);

        registerUser.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        int loggedInValue = (loggedIn.isChecked())? 1 : 0;
                        User user=datasource.getUserDetail();

                        if(user!=null && (user.getEmailId()!= null || user.getEmailId().equalsIgnoreCase("")) )
                        {

                            Toast.makeText(getApplicationContext(),user.getEmailId() + " is already registered",
                                    Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            if(email.getText().toString() == null || password.getText().toString() == null )
                            {
                                Toast.makeText(getApplicationContext(),"Please enter email id and password",
                                        Toast.LENGTH_LONG).show();
                            }
                            else
                            {
                                Toast.makeText(getApplicationContext(), "You have been successfully registered!",
                                        Toast.LENGTH_LONG).show();
                                datasource.insertUser(email.getText().toString(),password.getText().toString(),loggedInValue,firstName.getText().toString(),lastName.getText().toString()) ;
                                goToHomePage();
                            }

                        }


                    }
                });
    }

    //Navigate to Home page
    public void goToHomePage()
    {
        Intent intent = new Intent(this, com.se1.navdrawer.NavigationMainActivity.class);
        startActivity(intent);
    }


}
