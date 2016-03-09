package org.tndata.android.compass.fragment;

import org.json.JSONException;
import org.json.JSONObject;
import org.tndata.android.compass.R;
import org.tndata.android.compass.model.User;
import org.tndata.android.compass.parser.Parser;
import org.tndata.android.compass.parser.ParserModels;
import org.tndata.android.compass.util.API;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import es.sandwatch.httprequests.HttpRequest;
import es.sandwatch.httprequests.HttpRequestError;


/**
 * Fragment that displays a log in screen.
 *
 * @author Edited and documented by Ismael Alonso
 * @version 1.0.0
 */
public class LogInFragment
        extends Fragment
        implements
                HttpRequest.RequestCallback,
                Parser.ParserCallback,
                OnClickListener{

    //Listener interface.
    private LogInFragmentCallback mCallback;

    //UI components
    private EditText mEmail;
    private EditText mPassword;
    private TextView mError;
    private ProgressBar mProgress;
    private Button mLogIn;

    //Attributes
    private String mErrorString = "";
    private int mLogInRC;


    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        //This makes sure that the host activity has implemented the callback interface.
        //  If not, it throws an exception
        try{
            mCallback = (LogInFragmentCallback)context;
        }
        catch (ClassCastException ccx){
            throw new ClassCastException(context.toString()
                    + " must implement LoginFragmentListener");
        }
    }

    @Override
    public void onDetach(){
        super.onDetach();
        HttpRequest.cancel(mLogInRC);
        mCallback = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.fragment_login, container, false);

        mEmail = (EditText)rootView.findViewById(R.id.login_email);
        mPassword = (EditText)rootView.findViewById(R.id.login_password);
        mError = (TextView)rootView.findViewById(R.id.login_error);
        mProgress = (ProgressBar)rootView.findViewById(R.id.login_progress);
        mLogIn = (Button)rootView.findViewById(R.id.login_button);

        mLogIn.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.login_button:
                doLogin();
        }
    }

    /**
     * Checks the fields and starts the log in process if everything checks.
     */
    private void doLogin(){
        String email = mEmail.getText().toString().trim();
        String password = mPassword.getText().toString().trim();
        if (isValidEmail(email) && isValidPassword(password)){
            setFormEnabled(false);

            mLogInRC = HttpRequest.post(this, API.getLogInUrl(), API.getLogInBody(email, password));
        }
        else{
            setFormEnabled(true);
        }
    }

    /**
     * Checks if the email address provided by the user has a valid format.
     *
     * @param email the address to be checked.
     * @return true if the address provided has valid email format, false otherwise.
     */
    private boolean isValidEmail(@NonNull String email){
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            mErrorString = getActivity().getResources().getString(R.string.login_email_error);
            return false;
        }
        mErrorString = "";
        return true;
    }

    /**
     * Checks if the user introduced a password.
     *
     * @param password the password to be checked.
     * @return true if the user introduced a password, false otherwise.
     */
    private boolean isValidPassword(@NonNull String password){
        if (password.isEmpty()){
            mErrorString = getActivity().getResources().getString(R.string.login_pass_error);
            return false;
        }
        mErrorString = "";
        return true;
    }

    /**
     * Sets the state of the form and sets the error message if necessary.
     *
     * @param enabled true if the form should be enabled, false otherwise.
     */
    private void setFormEnabled(boolean enabled){
        mEmail.setEnabled(enabled);
        mPassword.setEnabled(enabled);
        mLogIn.setEnabled(enabled);
        if (enabled){
            mProgress.setVisibility(View.GONE);
            mError.setVisibility(View.VISIBLE);
            mError.setText(mErrorString);
        }
        else{
            mProgress.setVisibility(View.VISIBLE);
            mError.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onRequestComplete(int requestCode, String result){
        Parser.parse(result, User.class, this);
    }

    @Override
    public void onRequestFailed(int requestCode, HttpRequestError error){
        if (error.isServerError()){
            if (error.getMessage().contains("non_field_errors")){
                try{
                    mErrorString = new JSONObject(error.getMessage())
                            .getJSONArray("non_field_errors").getString(0);
                }
                catch (JSONException jsonx){
                    jsonx.printStackTrace();
                }
            }
        }
        else{
            mErrorString = getActivity().getResources().getString(R.string.login_error);
        }
        setFormEnabled(true);
    }

    @Override
    public void onProcessResult(int requestCode, ParserModels.ResultSet result){

    }

    @Override
    public void onParseSuccess(int requestCode, ParserModels.ResultSet result){
        if (result instanceof User){
            User user = (User)result;
            user.setPassword(mPassword.getText().toString().trim());
            mCallback.onLoginSuccess(user);
        }
    }


    /**
     * Callback interface for the LogInFragment.
     *
     * @author Edited and documented by Ismael Alonso
     * @version 1.0.0
     */
    public interface LogInFragmentCallback{
        /**
         * Called when the user logs in successfully.
         *
         * @param user a bundle of data containing the user information.
         */
        void onLoginSuccess(@NonNull User user);
    }
}
