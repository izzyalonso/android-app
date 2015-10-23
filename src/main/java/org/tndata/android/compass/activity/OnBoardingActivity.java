package org.tndata.android.compass.activity;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import org.tndata.android.compass.CompassApplication;
import org.tndata.android.compass.R;
import org.tndata.android.compass.adapter.ChooseCategoriesAdapter;
import org.tndata.android.compass.fragment.ChooseCategoriesFragment;
import org.tndata.android.compass.fragment.InstrumentFragment;
import org.tndata.android.compass.model.Category;
import org.tndata.android.compass.model.User;
import org.tndata.android.compass.model.UserData;
import org.tndata.android.compass.task.AddCategoryTask;
import org.tndata.android.compass.task.GetUserDataTask;
import org.tndata.android.compass.task.UpdateProfileTask;
import org.tndata.android.compass.util.Constants;

import java.util.ArrayList;
import java.util.List;


/**
 * Activity to present the user the onboarding process, which includes a short survey and
 * a category picker.
 *
 * @author Edited by Ismael Alonso
 * @version 1.1.0
 */
public class OnBoardingActivity
        extends AppCompatActivity
        implements
                AddCategoryTask.AddCategoryTaskListener,
                InstrumentFragment.InstrumentFragmentCallback,
                ChooseCategoriesAdapter.OnCategoriesSelectedListener,
                GetUserDataTask.GetUserDataCallback{

    private static final int STAGE_PROFILE = 0;
    private static final int STAGE_CHOOSE_CATEGORIES = 1;


    private CompassApplication mApplication;
    private Fragment mFragment = null;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        mApplication = (CompassApplication)getApplication();

        Toolbar toolbar = (Toolbar)findViewById(R.id.tool_bar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().hide();
        }

        swapFragments(STAGE_PROFILE);
    }

    /**
     * Replaces the current fragment for a new one.
     *
     * @param index the fragment id.
     */
    private void swapFragments(int index){
        switch (index){
            case STAGE_PROFILE:
                mFragment = InstrumentFragment.newInstance(Constants.INITIAL_PROFILE_INSTRUMENT_ID, 3);
                break;

            case STAGE_CHOOSE_CATEGORIES:
                Bundle args = new Bundle();
                args.putBoolean(ChooseCategoriesFragment.RESTRICTIONS_KEY, true);
                mFragment = new ChooseCategoriesFragment();
                mFragment.setArguments(args);
                break;
        }

        if (mFragment != null){
            getFragmentManager().beginTransaction().replace(R.id.base_content, mFragment).commit();
        }
    }

    @Override
    public void onInstrumentFinished(int instrumentId){
        //When the user is done with the survey, he is taken to the category picker
        if (instrumentId == Constants.INITIAL_PROFILE_INSTRUMENT_ID){
            swapFragments(STAGE_CHOOSE_CATEGORIES);
        }
    }

    @Override
    public void onCategoriesSelected(List<Category> selection){
        //Process and log the selection, and save it
        ArrayList<String> cats = new ArrayList<>();
        for (Category cat:selection){
            Log.d("Category", cat.toString());
            cats.add(String.valueOf(cat.getId()));
        }
        new AddCategoryTask(this, this, cats).execute();
    }

    @Override
    public void categoriesAdded(ArrayList<Category> categories){
        //Load all the user content from the API
        new GetUserDataTask(this, this).execute(mApplication.getToken());
    }

    @Override
    public void userDataLoaded(UserData userData){
        if (userData != null){
            mApplication.setUserData(userData);
        }
        User user = mApplication.getUser();
        user.setOnBoardingComplete();
        new UpdateProfileTask(null).execute(user);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }
}
